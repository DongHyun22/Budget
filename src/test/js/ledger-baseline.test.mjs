import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('budget server page retains heading, month navigation and daily table', async () => {
  const source = await readFile('src/main/webapp/WEB-INF/views/budget/index.jsp', 'utf8');
  assert.match(source, /<h1>월별 가계부<\/h1>/);
  assert.match(source, /type="month"/);
  assert.match(source, /name="ym"/);
  assert.match(source, /summary.days/);
  assert.match(source, /scope="col"/);
});
