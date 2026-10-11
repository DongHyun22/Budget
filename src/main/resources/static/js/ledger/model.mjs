const integer = value => typeof value === 'string' && /^-?\d+$/.test(value);
const date = value => typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);
const id = value => typeof value === 'string' && /^[1-9]\d*$/.test(value);
export function money(value) {
  if (value === null) return '미설정';
  if (!integer(value)) throw new Error('잘못된 금액 응답입니다.');
  return `${BigInt(value).toLocaleString('ko-KR')}원`;
}
export const initialDate = (ym, today) => today.startsWith(`${ym}-`) ? today : `${ym}-01`;
export const visibleCategories = (categories, existing) => categories.filter(category => category.active || category.id === existing);
export const draftChanged = (before, after) => JSON.stringify(before) !== JSON.stringify(after);
export function parseCategory(value) {
  if (!value || !id(value.id) || typeof value.name !== 'string' || typeof value.active !== 'boolean') throw new Error('카테고리 응답을 확인할 수 없습니다.');
  return { id: value.id, name: value.name, active: value.active };
}
export function parseSummary(value) {
  const fail = () => { throw new Error('가계부 응답을 확인할 수 없습니다.'); };
  if (!value || typeof value.ym !== 'string' || !/^\d{4}-\d{2}$/.test(value.ym) || !date(value.today)) fail();
  if (value.planAmount !== null && !integer(value.planAmount)) fail();
  for (const key of ['baseAmount', 'totalSpent', 'remaining', 'unallocated']) if (!integer(value[key])) fail();
  if (![value.days, value.spends, value.categories].every(Array.isArray)) fail();
  const days = value.days.map(day => {
    if (!day || !date(day.date) || !day.date.startsWith(`${value.ym}-`)) fail();
    for (const key of ['base', 'allowed', 'spent', 'remaining']) if (!integer(day[key])) fail();
    return { date: day.date, base: day.base, allowed: day.allowed, spent: day.spent, remaining: day.remaining };
  });
  const spends = value.spends.map(spend => {
    if (!spend || !id(spend.id) || !id(spend.categoryId) || !date(spend.date) || !integer(spend.amount) || typeof spend.categoryName !== 'string' || (spend.memo !== null && typeof spend.memo !== 'string')) fail();
    return { id: spend.id, date: spend.date, categoryId: spend.categoryId, categoryName: spend.categoryName, amount: spend.amount, memo: spend.memo ?? '' };
  });
  return { ym: value.ym, today: value.today, planAmount: value.planAmount, baseAmount: value.baseAmount, totalSpent: value.totalSpent, remaining: value.remaining, unallocated: value.unallocated, days, spends, categories: value.categories.map(parseCategory) };
}
export function latestRequest() {
  let generation = 0;
  return async operation => {
    const current = ++generation;
    const value = await operation();
    return { current: current === generation, value };
  };
}
export function exclusive() {
  let pending = false;
  return async operation => {
    if (pending) return;
    pending = true;
    try { return await operation(); } finally { pending = false; }
  };
}
