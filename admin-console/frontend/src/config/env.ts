export const env = {
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  // Mock mode is the default until admin-console/backend is implemented
  useMock: (import.meta.env.VITE_USE_MOCK ?? 'true') !== 'false',
} as const;
