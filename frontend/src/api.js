const TOKEN_KEY = 'itsm.token';

let token = localStorage.getItem(TOKEN_KEY);
let unauthorizedHandler = () => {};

export class ApiError extends Error {
  constructor(message, status, body) {
    super(message);
    this.status = status;
    this.body = body;
  }
}

export function setToken(value) {
  token = value;
  if (value) localStorage.setItem(TOKEN_KEY, value);
  else localStorage.removeItem(TOKEN_KEY);
}

export function hasToken() {
  return Boolean(token);
}

export function onUnauthorized(handler) {
  unauthorizedHandler = handler;
}

/** Thin wrapper over fetch that adds the JWT, serialises JSON and turns errors into ApiError. */
export async function api(path, { method = 'GET', body, params } = {}) {
  const url = new URL('/api' + path, window.location.origin);
  if (params) {
    Object.entries(params).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '' && v !== false) url.searchParams.append(k, v);
    });
  }
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = 'Bearer ' + token;

  let res;
  try {
    res = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?', 0, null);
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    if (res.status === 401 && token && !path.startsWith('/auth/login')) unauthorizedHandler();
    throw new ApiError(data?.message || res.statusText, res.status, data);
  }
  return data;
}
