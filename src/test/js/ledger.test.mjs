import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { money, initialDate, visibleCategories, parseSummary, latestRequest, exclusive, draftChanged } from '../../main/resources/static/js/ledger/model.mjs';
import { createApi, ApiError } from '../../main/resources/static/js/ledger/api.mjs';

const fixture = () => ({ ym: '2027-01', today: '2027-01-02', planAmount: null, baseAmount: '0', totalSpent: '0', remaining: '0', unallocated: '0', days: [{ date: '2027-01-01', base: '0', allowed: '0', spent: '0', remaining: '0' }], spends: [], categories: [] });

test('money renders exact strings beyond Number precision, including negative carry', () => {
  assert.equal(money('9007199254740993'), '9,007,199,254,740,993원');
  assert.equal(money('-9223372036854775808'), '-9,223,372,036,854,775,808원');
  assert.equal(money(null), '미설정');
  assert.throws(() => money('12.3'));
});
test('date selection uses server today only within viewed month', () => {
  assert.equal(initialDate('2027-01', '2027-01-02'), '2027-01-02');
  assert.equal(initialDate('2024-02', '2027-01-02'), '2024-02-01');
});
test('inactive category is selectable only for the existing spend', () => {
  const categories = [{ id: '1', active: true }, { id: '2', active: false }];
  assert.equal(visibleCategories(categories).length, 1);
  assert.equal(visibleCategories(categories, '2').length, 2);
});
test('summary boundary rejects malformed and numeric money values', () => {
  assert.equal(parseSummary(fixture()).planAmount, null);
  assert.throws(() => parseSummary({ ...fixture(), totalSpent: 3 }));
  assert.throws(() => parseSummary({ ...fixture(), days: [{ date: '<script>' }] }));
  assert.throws(() => parseSummary({ ...fixture(), categories: [{ id: 1, name: 'A', active: true }] }));
});
test('late summary responses cannot overwrite a newer month', async () => {
  const request = latestRequest();
  let resolveFirst;
  const first = request(() => new Promise(resolve => { resolveFirst = resolve; }));
  const second = await request(async () => 'February');
  resolveFirst('January');
  assert.deepEqual(second, { current: true, value: 'February' });
  assert.deepEqual(await first, { current: false, value: 'January' });
});
test('pending mutation ignores double submit and releases after failure', async () => {
  const once = exclusive();
  let release;
  const pending = once(() => new Promise(resolve => { release = resolve; }));
  let duplicates = 0;
  await once(async () => { duplicates += 1; });
  assert.equal(duplicates, 0);
  release();
  await pending;
  await assert.rejects(once(async () => { throw new Error('fail'); }));
  await once(async () => { duplicates += 1; });
  assert.equal(duplicates, 1);
});
test('dirty detection supports canceling close without clearing draft', () => {
  const draft = { amount: '12000', memo: '점심', categoryId: '1' };
  assert.equal(draftChanged(draft, { ...draft }), false);
  assert.equal(draftChanged(draft, { ...draft, memo: '저녁' }), true);
  assert.equal(draft.memo, '점심');
});
test('POST sends server CSRF metadata once and preserves string precision', async () => {
  const calls = [];
  const api = createApi('/app', 'X-CSRF-TOKEN', 'token', async (url, options) => {
    calls.push({ url, options });
    return new Response(null, { status: 204 });
  });
  await api.post('/spends', { amount: '9007199254740993' });
  assert.equal(calls.length, 1);
  assert.equal(calls[0].options.headers['X-CSRF-TOKEN'], 'token');
  assert.equal(calls[0].options.body.get('amount'), '9007199254740993');
});
test('session and csrf failures expose status, network errors never retry POST', async () => {
  for (const status of [401, 403]) {
    const api = createApi('', 'X-CSRF', 'token', async () => new Response(JSON.stringify({ message: 'error' }), { status, headers: { 'Content-Type': 'application/json' } }));
    await assert.rejects(api.post('/plan', {}), error => error instanceof ApiError && error.status === status);
  }
  let calls = 0;
  const api = createApi('', 'X-CSRF', 'token', async () => { calls += 1; throw new TypeError('offline'); });
  await assert.rejects(api.post('/plan', {}), /저장 결과/);
  assert.equal(calls, 1);
});
test('malformed success response is rejected instead of clearing form', async () => {
  const api = createApi('', 'X-CSRF', 'token', async () => new Response('<html>login</html>', { headers: { 'Content-Type': 'text/html' } }));
  await assert.rejects(api.summary('2027-01'), /응답/);
});
test('timeout remains active while response body is being read', async () => {
  let observedSignal;
  const api = createApi('', 'X-CSRF', 'token', async (_url, options) => {
    observedSignal = options.signal;
    return {
      ok: true, status: 200,
      json: () => new Promise((resolve, reject) => {
        options.signal.addEventListener('abort', () => reject(new Error('aborted')), { once: true });
        setTimeout(() => resolve(fixture()), 30);
      }),
    };
  }, 5);
  await assert.rejects(api.summary('2027-01'), /응답|조회/);
  assert.equal(observedSignal.aborted, true);
});
test('native dialogs, csrf and safe rendering contracts', async () => {
  const page = await readFile('src/main/webapp/WEB-INF/views/budget/index.jsp', 'utf8');
  const dialogs = await readFile('src/main/webapp/WEB-INF/views/budget/dialogs.jspf', 'utf8');
  const view = await readFile('src/main/resources/static/js/ledger/view.mjs', 'utf8');
  assert.match(page, /csrf-header/);
  assert.match(page, /noscript/);
  assert.match(dialogs, /<dialog id="day-dialog"/);
  assert.match(dialogs, /<dialog id="plan-dialog"/);
  assert.match(dialogs, /aria-live="polite"/);
  assert.doesNotMatch(view, /innerHTML|insertAdjacentHTML/);
  assert.match(view, /textContent/);
});
