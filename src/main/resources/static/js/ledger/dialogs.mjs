import { byId, categoryOptions, dayView, status } from './view.mjs';
import { draftChanged } from './model.mjs';
export function createDialogs(state, mutate) {
  let editing = null;
  let original = {};
  let planOriginal = '';
  let planMonth = '';
  let opener;
  const draft = () => Object.fromEntries(new FormData(byId('spend-form')));
  const dirty = () => (!byId('spend-editor').hidden && draftChanged(original, draft())) || byId('category-name').value !== '';
  const discard = () => !dirty() || confirm('저장하지 않은 입력을 버릴까요?');
  function reset() {
    editing = null; byId('spend-form').reset(); byId('category-form').reset();
    byId('category-form').hidden = true; byId('spend-editor').hidden = true;
  }
  function render() { dayView(state.summary, state.selected, edit, remove); }
  function edit(spend = null) {
    if (state.pending || !discard()) return;
    editing = spend;
    byId('spend-form').reset(); byId('spend-date').value = state.selected;
    categoryOptions(state.summary.categories, spend?.categoryId ?? '', spend?.categoryId);
    byId('spend-amount').value = spend?.amount ?? ''; byId('memo').value = spend?.memo ?? '';
    byId('spend-form-title').textContent = spend ? '지출 수정' : '지출 입력';
    byId('save-spend').textContent = spend ? '수정 저장' : '지출 저장';
    byId('category-form').hidden = true; byId('category-name').value = '';
    byId('spend-editor').hidden = false; original = draft(); byId('category').focus();
  }
  async function remove(spend) {
    if (state.pending || !confirm('이 지출을 삭제할까요?')) return;
    await mutate('day', `/spends/${spend.id}/delete`, {}, () => { if (editing?.id === spend.id) reset(); });
  }
  function open(add = false) {
    if (state.pending || state.loading || !state.summary) return;
    opener = document.activeElement; reset(); status('day'); render();
    byId('day-dialog').showModal();
    if (add) edit(); else byId('day-title').focus();
  }
  function close(kind) {
    if (state.pending) return;
    const changed = kind === 'day' ? dirty() : byId('plan-amount').value !== planOriginal;
    if (changed && !confirm('저장하지 않은 입력을 버릴까요?')) return;
    byId(`${kind}-dialog`).close(); if (kind === 'day') reset(); opener?.focus();
  }
  for (const kind of ['day', 'plan']) {
    byId(`${kind}-dialog`).addEventListener('cancel', event => { event.preventDefault(); close(kind); });
    document.querySelectorAll(`[data-close="${kind}"]`).forEach(button => button.addEventListener('click', () => close(kind)));
  }
  byId('open-detail').addEventListener('click', () => open());
  byId('open-spend').addEventListener('click', () => open(true));
  byId('day-add').addEventListener('click', () => edit());
  byId('cancel-spend').addEventListener('click', () => { if (!state.pending && discard()) { reset(); byId('day-add').focus(); } });
  byId('show-category').addEventListener('click', () => { byId('category-form').hidden = false; byId('category-name').focus(); });
  byId('cancel-category').addEventListener('click', () => { byId('category-form').hidden = true; byId('category-name').value = ''; byId('show-category').focus(); });
  byId('spend-form').addEventListener('submit', async event => {
    event.preventDefault();
    const fields = draft();
    await mutate('day', editing ? `/spends/${editing.id}` : '/spends', fields, () => { reset(); });
    if (byId('spend-editor').hidden) byId('day-add').focus();
  });
  byId('category-form').addEventListener('submit', async event => {
    event.preventDefault();
    await mutate('day', '/categories', { name: byId('category-name').value }, category => {
      state.summary.categories.push(category);
      categoryOptions(state.summary.categories, category.id, editing?.categoryId);
      byId('category-form').reset(); byId('category-form').hidden = true;
    }, false);
    if (byId('category-form').hidden) byId('category').focus();
  });
  byId('open-plan').addEventListener('click', () => {
    if (state.pending || state.loading || !state.summary) return;
    opener = document.activeElement; planOriginal = state.summary.planAmount ?? '';
    planMonth = state.summary.ym;
    byId('plan-amount').value = planOriginal; byId('plan-month').textContent = planMonth;
    status('plan'); byId('plan-dialog').showModal(); byId('plan-amount').focus();
  });
  byId('plan-form').addEventListener('submit', async event => {
    event.preventDefault();
    await mutate('plan', '/plan', { ym: planMonth, amount: byId('plan-amount').value }, () => { byId('plan-dialog').close(); });
    if (!byId('plan-dialog').open) opener?.focus();
  });
  return { open, render };
}
