import { createApi } from './api.mjs';
import { initialDate } from './model.mjs';
import { byId, selection, status, summaryView } from './view.mjs';
import { createDialogs } from './dialogs.mjs';
const root = byId('main-content');
const api = createApi(root.dataset.context, document.querySelector('meta[name="csrf-header"]').content, document.querySelector('meta[name="csrf-token"]').content);
const state = { summary: null, selected: '', pending: false, loading: false };
let generation = 0;
let requestedMonth = root.dataset.month;
let disabledBefore = [];
function updateOpeners() {
  for (const id of ['open-plan', 'open-detail', 'open-spend']) byId(id).disabled = state.pending || state.loading || !state.summary;
  root.setAttribute('aria-busy', String(state.pending || state.loading));
}
function pending(value) {
  state.pending = value;
  if (value) {
    disabledBefore = [...root.querySelectorAll('button,input,select')].map(control => [control, control.disabled]);
    for (const [control] of disabledBefore) control.disabled = true;
  } else {
    for (const [control, disabled] of disabledBefore) if (control.isConnected) control.disabled = disabled;
  }
  updateOpeners();
}
async function refresh(ym = requestedMonth, resetSelection = false) {
  const request = ++generation; requestedMonth = ym;
  state.loading = true; updateOpeners();
  try {
    const summary = await api.summary(ym);
    if (request !== generation) return false;
    state.selected = resetSelection || !state.selected || !state.selected.startsWith(`${summary.ym}-`) ? initialDate(summary.ym, summary.today) : state.selected;
    state.summary = summary; byId('month').value = summary.ym;
    summaryView(summary, state.selected);
    if (byId('day-dialog').open) dialogs.render();
    const url = new URL(location.href); url.searchParams.set('ym', summary.ym); history.replaceState(null, '', url);
    return true;
  } catch (error) {
    if (request !== generation) return false;
    throw error;
  } finally {
    if (request === generation) { state.loading = false; updateOpeners(); }
  }
}
async function mutate(scope, path, fields, success, reload = true) {
  if (state.pending || state.loading) return;
  pending(true); status(scope, '저장 중…');
  let saved = false;
  try {
    const result = await api.post(path, fields); saved = true;
    success(result);
    if (reload) await refresh(state.summary.ym);
    status(scope, '저장했습니다.'); status('page', '저장했습니다.');
  } catch (error) {
    const message = saved ? '저장 완료, 화면 갱신 실패. 조회 다시 시도로 최신 기록을 확인하세요.' : error.message;
    status(scope, message, true);
    if (saved || !byId(`${scope}-dialog`).open) status('page', message, true);
  } finally {
    pending(false);
  }
}
const dialogs = createDialogs(state, mutate);
async function load(ym, reset = false) {
  status('page', '불러오는 중…');
  try { if (await refresh(ym, reset)) { status('page'); if (byId('day-dialog').open) status('day', '최신 기록을 불러왔습니다.'); } }
  catch (error) { status('page', error.message, true); if (byId('day-dialog').open) status('day', error.message, true); }
}
byId('month-form').addEventListener('submit', event => {
  event.preventDefault(); if (!state.pending) load(byId('month').value, true);
});
byId('retry-summary').addEventListener('click', () => { if (!state.pending) load(requestedMonth); });
document.querySelectorAll('[data-retry]').forEach(button => button.addEventListener('click', () => { if (!state.pending) load(state.summary.ym); }));
byId('ledger-days').addEventListener('click', event => {
  const row = event.target.closest('tr[data-date]');
  if (!row || !state.summary || state.pending) return;
  state.selected = row.dataset.date; selection(state.selected);
});
byId('ledger-days').addEventListener('change', event => {
  if (state.pending || !state.summary) return;
  state.selected = event.target.value; selection(state.selected);
});
byId('ledger-days').addEventListener('dblclick', event => {
  if (event.target.closest('tr[data-date]')) dialogs.open();
});
byId('ledger-days').addEventListener('keydown', event => {
  if (event.key === 'Enter' && event.target.matches('input[type="radio"]')) { event.preventDefault(); dialogs.open(); }
});
await load(requestedMonth, true);
