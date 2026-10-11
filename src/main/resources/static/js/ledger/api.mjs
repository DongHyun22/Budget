import { parseSummary, parseCategory } from './model.mjs';
export class ApiError extends Error {
  constructor(status, message) { super(message); this.status = status; }
}
export function createApi(context, csrfHeader, csrfToken, transport = fetch, timeoutMs = 20000) {
  async function request(path, options) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      let response;
      try { response = await transport(`${context}/budget/ui${path}`, { credentials: 'same-origin', redirect: 'error', signal: controller.signal, ...options }); }
      catch { throw new ApiError(0, options.method === 'POST' ? '저장 결과를 확인할 수 없습니다. 조회를 다시 시도하여 결과를 확인한 뒤 저장하세요.' : '조회에 실패했습니다. 연결을 확인하고 다시 시도하세요.'); }
      if (!response.ok) {
        let message = '요청을 처리하지 못했습니다. 잠시 후 다시 시도하세요.';
        try { const body = await response.json(); if (typeof body.message === 'string') message = body.message; } catch { /* The status still identifies authentication errors. */ }
        if (response.status === 401) message = '로그인이 만료되었습니다. 초안은 이 화면에 유지됩니다. 다시 로그인하세요.';
        if (response.status === 403) message = '요청 권한을 확인할 수 없습니다. 다시 로그인하거나 새로고침하세요. 새로고침 시 초안이 사라집니다.';
        throw new ApiError(response.status, message);
      }
      if (response.status === 204) return null;
      try { return await response.json(); } catch { throw new ApiError(0, '서버 응답을 확인할 수 없습니다. 조회하여 저장 결과를 확인하세요.'); }
    } finally {
      clearTimeout(timeout);
    }
  }
  return {
    summary: async ym => parseSummary(await request(`/summary?ym=${encodeURIComponent(ym)}`, { method: 'GET' })),
    post: async (path, fields) => {
      const result = await request(path, { method: 'POST', headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8' }, body: new URLSearchParams(fields) });
      return path === '/categories' ? parseCategory(result) : result;
    },
  };
}
