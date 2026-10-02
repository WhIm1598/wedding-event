# admin-console/frontend — Lumière Studios admin web

React 18 · TypeScript strict (`noUnusedLocals/Parameters`) · Vite 5 · Tailwind 3 · React Router 6 · lucide-react.
No state library, no form library, no test runner. Import alias `@/` = `src/`. UI text is Vietnamese.

## Where things go
| Path | Contents |
|---|---|
| `src/types/index.ts` | All domain types, mirroring the detailed spec JSON (enums = string unions, money = `number` VND, dates = ISO strings) |
| `src/api/http.ts` | `http.get/post/put/patch/delete/download`, envelope unwrapping, Bearer token + single-flight refresh, `ApiError`, `errorMessage(err)` |
| `src/api/<module>.api.ts` | One object per spec module (`bookingsApi`, `crmApi`, `contractsApi`, …; notifications/search/settings in `system.api.ts`) |
| `src/api/mock/db.ts` | In-memory demo data (`db`), `delay()`, `nextId()`; dates relative to today |
| `src/features/<module>/` | `XxxPage.tsx` + `components/` (modals, tables, slide-overs) for that screen |
| `src/components/ui/` | `Badge`, `Button`, `Card`, `Field/Input/Select/Textarea` (Form.tsx), `Modal`/`SlideOver`, `PageHeader`, `Spinner/ErrorState/EmptyState` |
| `src/components/layout/` | `AppLayout` (+ `useLayout()` outlet context), `Sidebar`, `Header`, `GlobalSearch` (Ctrl+K), `NotificationBell` |
| `src/app/router.tsx` | Routes; admin-only pages wrapped with `adminOnly(...)` |
| `src/app/navigation.ts` | Sidebar menu + allowed roles per entry (`NAVIGATION`) |
| `src/constants/labels.ts` | Enum → Vietnamese label + Badge variant maps, dropdown option lists |
| `src/contexts/` | `useAuth()` (user, role, login/logout), `useToast()` |
| `src/hooks/useAsync.ts` | `useAsync(fn, deps)` → `{ data, error, loading, reload, setData }` |
| `src/lib/` | `format.ts` (`formatVND`, `formatDate`, `parseMoney`, …), `date.ts` (`todayISO`, `weekRange`, …), `cn.ts` |
| `src/config/env.ts` | `env.useMock` (`VITE_USE_MOCK`), API base; `.env.api` switches to the real backend |

## Recipe: new screen or endpoint
1. Add/extend types in `src/types/index.ts` with the spec's exact field names.
2. Add the function to `src/api/<module>.api.ts` with **both** branches, and a JSDoc line naming the endpoint:
   ```ts
   /** POST /admin/bookings (FN-ADM-CAL-02) */
   create(req: CreateBookingRequest): Promise<Booking> {
     if (!env.useMock) return http.post('/admin/bookings', req);
     // mock: validate like the backend, throw new ApiError(409, 'STAFF_SCHEDULE_CONFLICT', '...'), mutate db, return delay(x)
   }
   ```
   Mock branches must enforce the same business rules and error codes as the spec, so demo mode behaves like production.
   Add seed data to `mock/db.ts` for new entities.
3. Build the UI in `src/features/<module>/` from the shared `ui/` components; load data with `useAsync`, read form values
   with `FormData`, report with `showToast(msg)` / `showToast(errorMessage(err), 'error')`, disable buttons while saving (`loading`).
4. New page: route in `router.tsx` (+ `adminOnly` if staff must not see it) **and** entry in `navigation.ts` with the same roles.
5. New enum: labels + badge colours in `constants/labels.ts`.

## Rules
- Role-based access is enforced by the backend; the UI only hides what a role can't use (see spec §1.5).
- Never call `fetch` directly or put URLs in components — always go through `src/api`.
- Optimistic updates must revert on error (see `CustomersPage.moveLead`).
- Check: `npm run build` (runs `tsc --noEmit` + Vite build) must pass.
