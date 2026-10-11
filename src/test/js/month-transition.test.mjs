import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

async function harness() {
  const nodes = new Map();
  const byId = id => {
    if (!nodes.has(id)) nodes.set(id, {
      dataset: { context: '', month: '2027-01' }, disabled: false, open: false,
      value: '', hidden: true, listeners: {}, content: 'token',
      addEventListener(type, callback) { this.listeners[type] = callback; },
      querySelectorAll() { return []; }, setAttribute() {}, focus() {}, reset() {},
      showModal() { this.open = true; }, close() { this.open = false; },
    });
    return nodes.get(id);
  };
  const document = { querySelector: () => ({ content: 'token' }), querySelectorAll: () => [], activeElement: null };
  const dialogSource = (await readFile('src/main/resources/static/js/ledger/dialogs.mjs', 'utf8')).replace(/^import .*;\r?\n/gm, '').replace('export function', 'function');
  const createDialogs = new Function('byId', 'document', 'dayView', 'categoryOptions', 'status', 'draftChanged', `${dialogSource}; return createDialogs;`)(byId, document, () => {}, () => {}, () => {}, () => false);
  const calls = [];
  const reads = [];
  const api = { summary: () => new Promise((resolve, reject) => { reads.push({ resolve, reject }); }), post: async (path, fields) => { calls.push({ path, fields }); } };
  const source = (await readFile('src/main/resources/static/js/ledger/app.mjs', 'utf8')).replace(/^import .*;\r?\n/gm, '').replace('await load(requestedMonth, true);', '');
  const app = new Function('createApi', 'byId', 'document', 'initialDate', 'selection', 'status', 'summaryView', 'createDialogs', 'location', 'history', `${source}; return { state, load, dialogs };`)(() => api, byId, document, ym => `${ym}-01`, () => {}, () => {}, () => {}, createDialogs, { href: 'http://localhost/budget' }, { replaceState() {} });
  app.state.summary = { ym: '2027-01', planAmount: '310000' };
  app.state.selected = '2027-01-01';
  return { ...app, byId, calls, reads, resolve: summary => reads.at(-1).resolve(summary) };
}

test('month transition blocks plan and day dialogs until newest summary arrives', async () => {
  const app = await harness();
  const loading = app.load('2027-02', true);
  assert.equal(app.byId('open-plan').disabled, true);
  app.byId('open-plan').listeners.click();
  app.dialogs.open();
  assert.equal(app.byId('plan-dialog').open, false);
  assert.equal(app.byId('day-dialog').open, false);
  app.resolve({ ym: '2027-02', today: '2027-01-01' });
  await loading;
  assert.equal(app.byId('open-plan').disabled, false);
  app.byId('open-plan').listeners.click();
  assert.equal(app.byId('plan-month').textContent, '2027-02');
});

test('plan submission retains the month shown when its dialog opened', async () => {
  const app = await harness();
  app.byId('open-plan').listeners.click();
  app.state.summary = { ym: '2027-02', planAmount: '620000' };
  const saving = app.byId('plan-form').listeners.submit({ preventDefault() {} });
  await Promise.resolve();
  assert.equal(app.calls[0].fields.ym, '2027-01');
  app.resolve({ ym: '2027-02', today: '2027-01-01' });
  await saving;
});

test('older response cannot release the lock while a newer month is loading', async () => {
  const app = await harness();
  const first = app.load('2027-02', true);
  const second = app.load('2027-03', true);
  app.reads[0].resolve({ ym: '2027-02', today: '2027-01-01' });
  await first;
  assert.equal(app.state.loading, true);
  assert.equal(app.state.summary.ym, '2027-01');
  app.dialogs.open();
  assert.equal(app.byId('day-dialog').open, false);
  app.reads[1].resolve({ ym: '2027-03', today: '2027-01-01' });
  await second;
  assert.equal(app.state.loading, false);
  assert.equal(app.state.summary.ym, '2027-03');
});

test('failed month load releases controls and retains the previous month', async () => {
  const app = await harness();
  const loading = app.load('2027-02', true);
  app.reads[0].reject(new Error('offline'));
  await loading;
  assert.equal(app.state.loading, false);
  assert.equal(app.byId('open-plan').disabled, false);
  app.byId('open-plan').listeners.click();
  assert.equal(app.byId('plan-month').textContent, '2027-01');
});
