import { env } from '@/config/env';
import type { ApiResponse } from '@/types';

const TOKEN_KEY = 'lumiere.accessToken';
const REFRESH_KEY = 'lumiere.refreshToken';
const AUTH_PATHS = ['/admin/auth/login', '/admin/auth/refresh'];

export const tokenStorage = {
  get: (): string | null => localStorage.getItem(TOKEN_KEY),
  getRefresh: (): string | null => localStorage.getItem(REFRESH_KEY),
  set: (accessToken: string, refreshToken?: string) => {
    localStorage.setItem(TOKEN_KEY, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH_KEY, refreshToken);
  },
  clear: () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

type Query = Record<string, string | number | boolean | undefined | null>;
type FieldError = { field?: string; message?: string };

const buildUrl = (path: string, query?: Query): string => {
  const url = `${env.apiBaseUrl}${path}`;
  if (!query) return url;
  const params = new URLSearchParams();
  Object.entries(query).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') params.append(k, String(v));
  });
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
};

/** "Dữ liệu không hợp lệ: Số điện thoại không hợp lệ" — surfaces field errors in toasts */
const describe = (payload: ApiResponse<unknown> | null, fallback: string): string => {
  const base = payload?.message ?? fallback;
  const details = ((payload?.errors ?? []) as FieldError[]).map((e) => e.message).filter(Boolean);
  return details.length ? `${base}: ${details.join('; ')}` : base;
};

/** Single in-flight refresh shared by concurrent 401s */
let refreshing: Promise<boolean> | null = null;

async function refreshSession(): Promise<boolean> {
  const refreshToken = tokenStorage.getRefresh();
  if (!refreshToken) return false;
  refreshing ??= fetch(buildUrl('/admin/auth/refresh'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
    .then(async (res) => {
      const payload = (await res.json().catch(() => null)) as ApiResponse<{ accessToken: string; refreshToken: string }> | null;
      if (!res.ok || !payload?.success) return false;
      tokenStorage.set(payload.data.accessToken, payload.data.refreshToken);
      return true;
    })
    .catch(() => false)
    .finally(() => {
      refreshing = null;
    });
  return refreshing;
}

function sessionExpired() {
  tokenStorage.clear();
  if (!window.location.pathname.startsWith('/login')) window.location.assign('/login');
}

async function send(method: string, path: string, body: unknown, query: Query | undefined, accept: string): Promise<Response> {
  const headers: Record<string, string> = { Accept: accept };
  const token = tokenStorage.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  return fetch(buildUrl(path, query), { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
}

/** Sends the request; on 401 refreshes the access token once and retries, otherwise returns to /login. */
async function sendWithAuth(method: string, path: string, body?: unknown, query?: Query, accept = 'application/json'): Promise<Response> {
  let res = await send(method, path, body, query, accept);
  if (res.status === 401 && !AUTH_PATHS.includes(path) && tokenStorage.get()) {
    if (await refreshSession()) {
      res = await send(method, path, body, query, accept);
    }
    if (res.status === 401) sessionExpired();
  }
  return res;
}

async function request<T>(method: string, path: string, body?: unknown, query?: Query): Promise<T> {
  const res = await sendWithAuth(method, path, body, query);
  const payload = (await res.json().catch(() => null)) as ApiResponse<T> | null;
  if (!res.ok || !payload?.success) {
    throw new ApiError(res.status, payload?.code ?? 'INTERNAL_SERVER_ERROR', describe(payload, res.statusText || 'Không thể kết nối máy chủ'));
  }
  return payload.data;
}

/** Downloads a binary endpoint (e.g. contract PDF) and returns a Blob */
async function download(path: string): Promise<Blob> {
  const res = await sendWithAuth('GET', path, undefined, undefined, 'application/pdf, application/json');
  if (!res.ok) {
    const payload = (await res.json().catch(() => null)) as ApiResponse<unknown> | null;
    throw new ApiError(res.status, payload?.code ?? 'DOWNLOAD_FAILED', describe(payload, res.statusText));
  }
  return res.blob();
}

export const http = {
  get: <T>(path: string, query?: Query) => request<T>('GET', path, undefined, query),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  patch: <T>(path: string, body?: unknown) => request<T>('PATCH', path, body),
  delete: <T>(path: string) => request<T>('DELETE', path),
  download,
};

/** User-facing message for any thrown error */
export const errorMessage = (err: unknown): string =>
  err instanceof Error && err.message ? err.message : 'Đã có lỗi xảy ra, vui lòng thử lại';
