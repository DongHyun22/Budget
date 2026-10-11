import test from 'node:test';
import assert from 'node:assert/strict';
import { createApi } from '../../main/resources/static/js/ledger/api.mjs';

test('timed out POST reports unknown outcome without retrying', async () => {
  let calls = 0;
  const api = createApi('', 'X-CSRF', 'token', async (_url, options) => {
    calls += 1;
    return new Promise((_resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('aborted'))));
  }, 5);
  await assert.rejects(api.post('/spends', { amount: '12000' }), /저장 결과/);
  assert.equal(calls, 1);
});
