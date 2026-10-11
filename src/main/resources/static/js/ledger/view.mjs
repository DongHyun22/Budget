import { money, visibleCategories } from './model.mjs';
export const byId = id => document.getElementById(id);
export function element(tag, text, className) {
  const node = document.createElement(tag);
  if (text !== undefined) node.textContent = text;
  if (className) node.className = className;
  return node;
}
export function status(scope, message = '', recover = false) {
  byId(`${scope}-status`).textContent = message;
  byId(`${scope}-recovery`).hidden = !recover;
}
export function selection(selected) {
  byId('selected-date').textContent = `${selected} 선택됨`;
  for (const row of byId('ledger-days').rows) {
    row.classList.toggle('is-selected', row.dataset.date === selected);
    row.querySelector('input').checked = row.dataset.date === selected;
  }
}
export function summaryView(summary, selected) {
  const hasPlan = summary.planAmount !== null;
  for (const [key, value] of Object.entries({ plan: summary.planAmount, spent: summary.totalSpent, remaining: hasPlan ? summary.remaining : null, base: hasPlan ? summary.baseAmount : null })) {
    byId(`metric-${key}`).textContent = money(value);
    byId(`metric-${key}`).classList.toggle('negative', value !== null && BigInt(value) < 0n);
  }
  byId('no-plan').hidden = hasPlan;
  byId('unallocated').textContent = `월말 미배정 잔액 ${money(summary.unallocated)} · 일별 예산에 더하지 않습니다.`;
  const today = summary.days.find(day => day.date === summary.today);
  byId('today-summary').textContent = today ? `오늘 ${summary.today} · 현재 남은 금액 ${money(hasPlan ? today.remaining : null)}` : `조회 월 ${summary.ym} · 오늘 ${summary.today}의 현재 금액은 이번 달을 조회하면 확인할 수 있습니다.`;
  const rows = summary.days.map(day => {
    const row = element('tr'); row.dataset.date = day.date;
    const heading = element('th'); heading.scope = 'row';
    const label = element('label', undefined, 'ledger-date');
    const radio = element('input'); radio.type = 'radio'; radio.name = 'ledger-date'; radio.value = day.date;
    label.append(radio, element('span', `${day.date}${day.date === summary.today ? ' · 오늘' : ''}`)); heading.append(label); row.append(heading);
    for (const key of ['base', 'allowed', 'spent', 'remaining']) {
      const value = hasPlan || key === 'spent' ? day[key] : null;
      row.append(element('td', money(value), `number${value !== null && BigInt(value) < 0n ? ' negative' : ''}`));
    }
    return row;
  });
  byId('ledger-days').replaceChildren(...rows); selection(selected);
}
export function categoryOptions(categories, selected = '', existing) {
  const options = [new Option('선택하세요', '')];
  for (const category of visibleCategories(categories, existing)) options.push(new Option(`${category.name}${category.active ? '' : ' (비활성)'}`, category.id));
  byId('category').replaceChildren(...options); byId('category').value = selected;
}
export function dayView(summary, selected, edit, remove) {
  byId('day-title').textContent = `${selected} 지출`;
  const day = summary.days.find(value => value.date === selected);
  byId('day-totals').textContent = day ? `지출 ${money(day.spent)} · 남은 금액 ${money(summary.planAmount === null ? null : day.remaining)}` : '';
  const spends = summary.spends.filter(spend => spend.date === selected);
  const rows = spends.map(spend => {
    const row = element('article', undefined, 'spend-item');
    row.append(element('strong', `${spend.categoryName} · ${money(spend.amount)}`), element('p', spend.memo));
    const actions = element('div', undefined, 'dialog-actions');
    for (const [title, action] of [['수정', edit], ['삭제', remove]]) {
      const button = element('button', title, 'btn'); button.type = 'button'; button.addEventListener('click', () => action(spend)); actions.append(button);
    }
    row.append(actions); return row;
  });
  byId('day-spends').replaceChildren(...(rows.length ? rows : [element('p', '아직 기록한 지출이 없습니다.', 'muted')]));
}
