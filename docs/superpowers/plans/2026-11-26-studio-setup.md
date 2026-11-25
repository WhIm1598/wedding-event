# Studio setup (several managers, logo, contract details) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The platform admin creates a studio with a name, logo, subscription end, optional contract details and 1–10 managers in one step; the logo shows in the sidebar, the contract PDF and the public contract page.

**Architecture:** The logo is a small PNG/JPEG kept in `studio_settings` (`bytea`) and carried in JSON as a data URL; one helper (`StudioLogo`) parses and formats it for every endpoint. `POST /platform/studios` takes an `admins` list and creates everything in one transaction through the existing `UserAccountService.createIn`. The web shrinks the image in the browser (canvas) before sending.

**Tech Stack:** Spring Boot 4 / Java 21 / JPA / Flyway / JUnit 5 + MockMvc (`IntegrationTest`); React 18 + TS + Vite + Tailwind; openhtmltopdf for the PDF.

**Spec:** `docs/superpowers/specs/2026-11-25-studio-setup-design.md` (read it first). Corrections found while planning, which this plan follows:
1. Field errors use code **`BAD_REQUEST`** (`AppException.invalidField`, same as `@Valid`), not `VALIDATION_ERROR`.
2. Logo types are **`image/png` and `image/jpeg` only**: PDFBox can't draw WebP. The browser exports PNG.
3. Two clear rules for "keep vs remove", following the house style that edit forms send every field:
   `PUT /admin/settings/studio` always sends `logo` (`null` removes it); `PATCH /platform/studios/{id}` takes optional
   `name` and `logo`, where `null` means "keep" and `""` means "remove" (the status toggle and extend dialog keep sending only
   `status` + `subscriptionExpiresAt`).
4. The sidebar reads the logo from `GET /admin/settings/studio` (staff can already read it); `auth/me` does not change.

## Global Constraints

- Schema changes only through a new migration `backend-common/src/main/resources/db/migration/V19__studio_logo.sql`; never edit V1–V18.
- Logo: data URL `data:image/png;base64,…` or `data:image/jpeg;base64,…`, decoded size ≤ **204 800 bytes** (200 KB); the browser shrinks it to a longest side of **256 px**.
- `admins`: **1–10** rows of `{ email (≤255, email), name (1–100) }`; duplicate emails in the list (ignoring case) → `400 BAD_REQUEST`, field `admins`; an existing email → `409 EMAIL_EXISTS`, nothing created.
- Contract detail fields at creation use the same limits as FN-ADM-SET-01: `address` ≤255, `taxCode` ≤20, `legalRepresentative` ≤100, `bankInfo` ≤500; blank/missing → `"Chưa cập nhật"` (the existing `TO_FILL`).
- User-facing text in Vietnamese. JSON field names identical at every layer.
- Still two studio roles (`ROLE_ADMIN`, `ROLE_STAFF`). The form doesn't create staff.
- Don't change `package.json`, `*.gradle.kts` or lock files; no new libraries (resize with `<canvas>`).
- Mock mode (`npm run dev`) must keep working, logo included.
- Backend: tests first; `./gradlew :admin-console:backend:build` must stay ≥ 85 % line coverage. Tests need Docker running or `TEST_DATABASE_URL`.
- A new folder or pattern → update the module's `CLAUDE.md` in the same task.

## Review Focus

1. **Settings form saved without touching the logo** → the logo must stay. The form always sends the current `logo` back; test that `PUT` with the same logo keeps it (Task 2).
2. **Logo input that is odd but not malicious**: `IMAGE/PNG` in upper case, line breaks inside the base64, a `data:image/svg+xml` file. Expected: a clean `400` with field `logo`, never a `500` (Task 1).
3. **Exactly 200 KB**: 204 800 decoded bytes are accepted, 204 801 rejected (Task 1).
4. **Same manager email typed twice in different case** (`A@x.vn`, `a@x.vn`) → `400`, nothing created (Task 3).
5. **Platform admin's sidebar**: this account has no studio, so it must not call `/admin/settings/studio` (that answers 403) and must not show a broken image (Task 7).

---

### Task 1: Logo column + `StudioLogo` helper

**Files:**
- Create: `backend-common/src/main/resources/db/migration/V19__studio_logo.sql`
- Modify: `backend-common/src/main/java/com/weddingevent/common/domain/StudioSettings.java` (two fields + getters/setters)
- Create: `admin-console/backend/src/main/java/com/weddingevent/admin/settings/StudioLogo.java`
- Test: `admin-console/backend/src/test/java/com/weddingevent/admin/settings/StudioLogoTest.java`

**Interfaces:**
- Produces:
  - `StudioSettings.getLogo(): byte[]`, `setLogo(byte[])`, `getLogoContentType(): String`, `setLogoContentType(String)`
  - `StudioLogo.MAX_BYTES = 204_800`
  - `static void StudioLogo.apply(StudioSettings s, String dataUrl)`: `null` or `""` clears both columns; otherwise parses and sets them; bad input → `AppException.invalidField("logo", "Logo phải là ảnh PNG hoặc JPEG, tối đa 200 KB")`
  - `static String StudioLogo.dataUrl(StudioSettings s)`: `"data:<type>;base64,<b64>"`, or `null` when there is no logo (also when `s` is `null`)

- [ ] **Step 1: Write the failing unit test** (plain JUnit, no Spring)

```java
class StudioLogoTest {
    private static String png(int bytes) { return "data:image/png;base64," + Base64.getEncoder().encodeToString(new byte[bytes]); }

    @Test void pngRoundTrips() { var s = new StudioSettings(); StudioLogo.apply(s, png(10));
        assertThat(s.getLogoContentType()).isEqualTo("image/png"); assertThat(s.getLogo()).hasSize(10);
        assertThat(StudioLogo.dataUrl(s)).isEqualTo(png(10)); }
    @Test void jpegIsAccepted() { var s = new StudioSettings();
        StudioLogo.apply(s, "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(new byte[3]));
        assertThat(s.getLogoContentType()).isEqualTo("image/jpeg"); }
    @ParameterizedTest @NullAndEmptySource void nullOrEmptyClears(String v) { var s = new StudioSettings(); StudioLogo.apply(s, png(5));
        StudioLogo.apply(s, v); assertThat(s.getLogo()).isNull(); assertThat(s.getLogoContentType()).isNull(); assertThat(StudioLogo.dataUrl(s)).isNull(); }
    @Test void exactly200KbIsAccepted() { StudioLogo.apply(new StudioSettings(), png(204_800)); }
    @ParameterizedTest @ValueSource(strings = { "data:image/webp;base64,AAAA", "data:image/svg+xml;base64,AAAA", "data:IMAGE/PNG;base64,AAAA",
        "data:image/png;base64,AA\nAA", "data:image/png;base64,***", "image/png;base64,AAAA", "data:image/png,AAAA" })
    void rejected(String v) { assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), v))
        .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrors()).extracting(ApiResponse.FieldError::field).containsExactly("logo")); }
    @Test void over200KbIsRejected() { assertThatThrownBy(() -> StudioLogo.apply(new StudioSettings(), png(204_801))).isInstanceOf(AppException.class); }
    @Test void noSettingsNoLogo() { assertThat(StudioLogo.dataUrl(null)).isNull(); }
}
```

(Check the getter names on `AppException` / `ApiResponse.FieldError` and adjust the extractor to them.)

- [ ] **Step 2: Run it, see it fail to compile**
  Run: `./gradlew :admin-console:backend:test --tests '*StudioLogoTest'`. Expected: compilation error, `StudioLogo` not found.

- [ ] **Step 3: Migration and entity.** `V19__studio_logo.sql`:
```sql
ALTER TABLE studio_settings ADD COLUMN logo BYTEA, ADD COLUMN logo_content_type VARCHAR(20);
```
`StudioSettings`: `@Column(name = "logo") private byte[] logo;` and `@Column(name = "logo_content_type", length = 20) private String logoContentType;`. Use plain `byte[]` (maps to `bytea` in Hibernate 7); no `@Lob`, because on PostgreSQL that maps to `oid`.

- [ ] **Step 4: Implement `StudioLogo`.** Match the prefix exactly: `^data:(image/png|image/jpeg);base64,(.+)$`. Decode with the strict `Base64.getDecoder()`; an `IllegalArgumentException` becomes the field error. Check the decoded length against `MAX_BYTES`. The class is final with a private constructor.

- [ ] **Step 5: Run it, see it pass.** Same command. Expected: all tests PASS.

- [ ] **Step 6: Commit**
```bash
git add backend-common/src/main/resources/db/migration/V19__studio_logo.sql backend-common/src/main/java/com/weddingevent/common/domain/StudioSettings.java admin-console/backend/src/main/java/com/weddingevent/admin/settings/StudioLogo.java admin-console/backend/src/test/java/com/weddingevent/admin/settings/StudioLogoTest.java
git commit -m "Studio logo column (V19) and StudioLogo data URL helper"
```

---

### Task 2: Logo in studio settings (FN-ADM-SET-01)

**Files:**
- Modify: `admin-console/backend/src/main/java/com/weddingevent/admin/settings/SettingsController.java`
- Test: `admin-console/backend/src/test/java/com/weddingevent/admin/StudioIsolationApiTest.java` (one new test), `admin-console/backend/src/test/java/com/weddingevent/admin/DashboardAndMiscApiTest.java` (or wherever the settings GET/PUT tests are now: `grep -rn "settings/studio" src/test`)

**Interfaces:**
- Consumes: `StudioLogo.apply`, `StudioLogo.dataUrl` (Task 1)
- Produces: `StudioSettingsDto` gets a last component `String logo` (no annotations; checked by `StudioLogo.apply`). `GET` returns it; `PUT` always applies it (`null` removes).

- [ ] **Step 1: Write the failing integration tests**
  - `settingsLogoIsSavedReadAndKeptWhenSentBack`: as `adminToken()`, PUT the current settings plus `logo = png(10)` → 200 and `$.data.logo` equals it. GET → same. PUT again with the same `logo` and a changed `address` → `$.data.logo` unchanged. PUT with `logo: null` → `$.data.logo` is null.
  - `settingsLogoTooBigOrWrongTypeIs400`: PUT with `data:image/webp;base64,AAAA` → 400, `$.code == 'BAD_REQUEST'`, `$.errors[0].field == 'logo'`.
  - `staffCannotChangeTheLogo`: PUT as `staffToken()` → 403 (check whether an existing test already covers PUT 403; if so, only make sure it still passes).
  - In `StudioIsolationApiTest`, `aStudiosLogoIsItsOwn`: studio B's admin sets a logo; the demo studio's `GET /admin/settings/studio` (`adminToken()`) still has its old `logo` (null in the seed).

  Every test that restores the demo studio's settings must send `logo: null` back, so the shared database stays clean.

- [ ] **Step 2: Run them, see them fail.** Run: `./gradlew :admin-console:backend:test --tests '*StudioIsolationApiTest' --tests '<settings test class>'`. Expected: FAIL (`$.data.logo` missing).

- [ ] **Step 3: Implement.** Add `logo` to the record, to `of(...)` via `StudioLogo.dataUrl(s)`, and call `StudioLogo.apply(s, req.logo())` in `update` (the row is already locked).

- [ ] **Step 4: Run them, see them pass.** Same command. Expected: PASS.

- [ ] **Step 5: Commit** with message `Studio settings carry the logo (FN-ADM-SET-01)`.

---

### Task 3: Create a studio with several managers, details and logo; edit name and logo (FN-PLT-01/02)

**Files:**
- Modify: `admin-console/backend/src/main/java/com/weddingevent/admin/platform/PlatformStudioService.java`, `PlatformStudioController.java` (only if the signatures change)
- Test: `admin-console/backend/src/test/java/com/weddingevent/admin/StudioIsolationApiTest.java` (helper `createStudio` + new tests); every test that reads `$.data.admin.temporaryPassword` (`grep -rn "data.admin" admin-console/backend/src/test`)

**Interfaces:**
- Consumes: `StudioLogo` (Task 1), the existing `UserAccountService.createIn(UUID, CreateUserRequest)`
- Produces:
  - `record NewAdmin(@NotBlank @Email @Size(max = 255) String email, @NotBlank @Size(max = 100) String name)`
  - `record CreateStudioRequest(@NotBlank @Size(max = 200) String name, LocalDate subscriptionExpiresAt, String logo, @Size(max = 255) String address, @Size(max = 20) String taxCode, @Size(max = 100) String legalRepresentative, @Size(max = 500) String bankInfo, @NotNull @Size(min = 1, max = 10) List<@Valid NewAdmin> admins)`
  - `record CreatedStudioDto(StudioDto studio, List<CreatedAccountDto> admins)`
  - `StudioDto` gets a `String logo` component after `name`
  - `record UpdateStudioRequest(@NotNull StudioStatus status, LocalDate subscriptionExpiresAt, @Size(min = 1, max = 200) String name, String logo)`: `name` null keeps it, blank → 400 (handled by `@Size(min = 1)` together with a `trim` check), `logo` null keeps it, `""` removes it

- [ ] **Step 1: Change the test helper.** `createStudio(token, name, adminEmail)` now posts `{'name':'%s','admins':[{'email':'%s','name':'Chủ Studio B'}]}`; `studioB()` reads `$.data.admins[0].temporaryPassword`. Update the other readers of `$.data.admin…` the same way.

- [ ] **Step 2: Write the failing tests** (in `StudioIsolationApiTest`, as `platformToken()`)
  - `aStudioIsCreatedWithTwoManagersDetailsAndLogo`: POST with 2 admins, `logo = png(10)`, `address = '12 Lê Lợi'` → 201; `$.data.admins.length() == 2`, each has a `temporaryPassword` and `user.role == 'ROLE_ADMIN'`; `$.data.studio.adminCount == 2`, `$.data.studio.logo` equals the sent value. Both managers can log in with their temporary password, and `GET /admin/settings/studio` as either one shows `address == '12 Lê Lợi'`, `taxCode == 'Chưa cập nhật'`.
  - `anExistingEmailCreatesNothing`: the second admin's email already exists (use the demo admin's email) → 409 `EMAIL_EXISTS`; the studio count in `GET /platform/studios` is unchanged, and logging in as the first (new) email fails.
  - `badManagerListsAre400`: `admins: []` → 400; 11 admins → 400; `[{A@x.vn},{a@x.vn}]` → 400 with `$.errors[0].field == 'admins'`; count unchanged after each.
  - `badLogoOnCreateIs400`: `logo: 'data:image/webp;base64,AAAA'` → 400 field `logo`, nothing created.
  - `platformEditsNameAndLogo`: PATCH `{status:'ACTIVE', subscriptionExpiresAt:null, name:'Tên Mới', logo: png(4)}` → 200, `name` and `logo` set; PATCH `{status:'ACTIVE', subscriptionExpiresAt:null}` → name and logo unchanged; PATCH with `logo:''` → `logo` null; PATCH with `name:'  '` → 400.
  - The list (`GET /platform/studios`) includes `logo` for a studio that has one.

- [ ] **Step 3: Run them, see them fail.** Run: `./gradlew :admin-console:backend:test --tests '*StudioIsolationApiTest'`. Expected: FAIL.

- [ ] **Step 4: Implement `create`.**
  1. Check for duplicate emails first (`trim().toLowerCase(Locale.ROOT)`, any repeat → `AppException.invalidField("admins", "Email quản lý bị trùng trong danh sách")`).
  2. Save the studio, then the settings: the given detail fields trimmed, or `TO_FILL` when blank or null, then `StudioLogo.apply(profile, req.logo())`.
  3. Call `accounts.createIn(...)` for each admin, in order.

  The method stays `@Transactional`. The `409` thrown by `createIn` rolls everything back, so the test with an existing email proves it.

- [ ] **Step 5: Implement `update`.** Set `name` if it isn't null; a trimmed empty name → `invalidField("name", "Cần tên studio")`. If `logo` isn't null, call `StudioLogo.apply(settings, logo)`, where `""` clears. Lock the settings row (`findByIdForUpdate`) before writing, as `SettingsController` does. `toDto` fills `logo` through `StudioLogo.dataUrl(profile)`.

- [ ] **Step 6: Run the whole admin test suite, see it pass.** Run: `./gradlew :admin-console:backend:test`. Expected: PASS.

- [ ] **Step 7: Commit** with message `Create a studio with several managers, contract details and logo (FN-PLT-01/02)`.

---

### Task 4: Logo on the contract PDF and the public contract page

**Files:**
- Modify: `admin-console/backend/src/main/java/com/weddingevent/admin/contract/ContractDocumentRenderer.java`, `ContractShareService.java` (`PublicStudioDto`)
- Test: `admin-console/backend/src/test/java/com/weddingevent/admin/ContractShareApiTest.java`, plus the renderer's existing test if there is one (`grep -rn "ContractDocumentRenderer" src/test`)

**Interfaces:**
- Consumes: `StudioLogo.dataUrl` (Task 1)
- Produces: `record PublicStudioDto(String name, String address, String bankInfo, String logo)`; `html(c, studio)` puts `<img class="logo" src="<dataUrl>" alt=""/>` before the party B name when there is a logo, and nothing when there isn't

- [ ] **Step 1: Write the failing tests**
  - Renderer: `html` with a settings object that has a real 1×1 PNG logo (bytes from `ImageIO.write` of a `BufferedImage(1,1,TYPE_INT_ARGB)`) contains `src="data:image/png;base64,`; without a logo it contains no `<img`; `pdf(...)` with the logo returns bytes that start with `%PDF`, which shows that openhtmltopdf accepts the data URI.
  - Public page: after the demo studio has a logo (set through `PUT /admin/settings/studio`, restore `null` at the end), `GET /api/v1/public/contracts/{token}` has `$.data.studio.logo` starting with `data:image/png;base64,`.

- [ ] **Step 2: Run them, see them fail.** Run: `./gradlew :admin-console:backend:test --tests '*ContractShareApiTest' --tests '*ContractDocumentRenderer*'`. Expected: FAIL.

- [ ] **Step 3: Implement.** The data URL is built from bytes we validated, so it doesn't need HTML escaping, but keep `esc()` around it anyway for safety. CSS: `.logo { height: 48px; }`. Fill `PublicStudioDto.logo` where the DTO is built.

- [ ] **Step 4: Run them, see them pass.** Then run `./gradlew :admin-console:backend:build` (coverage gate). Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit** with message `Studio logo on the contract PDF and the public contract page`.

---

### Task 5: Web types, API (real + mock) and the logo input

**Files:**
- Modify: `admin-console/frontend/src/types/index.ts`, `src/api/platform.api.ts`, `src/api/system.api.ts` (only if the mock needs it), `src/api/mock/db.ts`
- Create: `admin-console/frontend/src/lib/image.ts`, `admin-console/frontend/src/components/ui/LogoInput.tsx`
- Modify: `admin-console/frontend/CLAUDE.md` (mention `lib/image.ts` + `LogoInput`)

**Interfaces:**
- Produces:
  - Types:
    - `StudioSettings.logo: string | null`
    - `Studio.logo: string | null`
    - `PublicContract.studio.logo: string | null`
    - `interface NewStudioAdmin { email: string; name: string }`
    - `CreateStudioRequest = { name; subscriptionExpiresAt; logo: string | null; address?: string; taxCode?: string; legalRepresentative?: string; bankInfo?: string; admins: NewStudioAdmin[] }`
    - `CreatedStudio = { studio: Studio; admins: CreatedAccount[] }`
    - `UpdateStudioRequest` gets optional `name?: string; logo?: string | null` (send `''` to remove; leave it out to keep)
  - `resizeToDataUrl(file: File, maxSide = 256): Promise<string>` in `lib/image.ts`: draw onto a canvas at most 256 px on the longest side (never upscale), `canvas.toDataURL('image/png')`. Reject non-image files with `new Error('Chọn file ảnh PNG hoặc JPEG')`. If the result is over 200 KB (`dataUrl.length * 3 / 4 > 204800`), retry once with `image/jpeg` quality 0.85, otherwise reject with `'Ảnh quá lớn, chọn ảnh khác'`.
  - `<LogoInput value={string | null} onChange={(v: string | null) => void} />`: a preview (48 px), a "Chọn ảnh" button (`accept="image/png,image/jpeg"`) and a "Xoá" button; errors from `resizeToDataUrl` show as red text under it. Use the existing UI components and Tailwind classes of `components/ui`.
- Mock `platformApi`:
  - `create` checks that `admins` has 1–10 rows and no duplicates ignoring case (`ApiError(400, 'BAD_REQUEST', …)`), checks every email against `db.users` **before** creating anything, then pushes the studio with `logo` and `adminCount = admins.length`.
  - `update` applies `name` and `logo` with the same keep/remove rules as the backend.
  - The mock settings in `db.ts` get `logo: null`.

- [ ] **Step 1: Implement the types and the API.** `npm run typecheck` fails where `PlatformPage` still uses `adminEmail` / `created.admin`; that is fixed in Task 6.
- [ ] **Step 2: Implement `lib/image.ts` and `LogoInput.tsx`.**
- [ ] **Step 3: Commit** with message `Web: studio logo types, API and LogoInput` (the build stays red until Task 6, so do Tasks 5 and 6 back to back).

---

### Task 6: Platform screen: create a studio with managers, edit name and logo

**Files:**
- Modify: `admin-console/frontend/src/features/platform/PlatformPage.tsx`

**Interfaces:**
- Consumes: Task 5's types, `platformApi`, `LogoInput`

- [ ] **Step 1: `CreateStudioModal`.**
  - Fields: name*, `LogoInput`, subscription end, a collapsible "Thông tin hợp đồng (tuỳ chọn)" block (address, tax code, legal representative, bank info), and a "Quản lý*" list of email + name rows, starting with one row, with "+ Thêm quản lý" (hidden at 10) and ✕ (hidden when there is one row).
  - Empty optional fields aren't sent.
  - Server errors show the way the modal does today.
- [ ] **Step 2: Result after creating.**
  - `issued` becomes `CreatedAccount[]`. The existing temporary-password dialog lists email + password for each manager, each with a copy button, under the warning that these passwords are shown only once.
  - The single-admin "Thêm quản lý" flow passes `[created]`.
- [ ] **Step 3: List and edit.**
  - Each studio row shows its logo (32 px, initial letter when there is none) before the name.
  - Add an "Sửa" action that opens `EditStudioModal` (name + `LogoInput`). Save calls `platformApi.update(id, { status, subscriptionExpiresAt, name, logo: logo ?? '' })` and replaces the row.
- [ ] **Step 4: Verify.**
  - Run `cd admin-console/frontend && npm run build`. Expected: no type errors, build OK.
  - Then `npm run dev`, log in as the mock platform admin, create a studio with 2 managers and a logo: the dialog lists 2 passwords and the row shows the logo. Edit the logo, then remove it.
  - Two rows with the same email in different case → the error message shows and no studio is added.
- [ ] **Step 5: Commit** with message `Platform: create a studio with several managers, contract details and logo; edit name and logo`.

---

### Task 7: Logo in Settings, the sidebar, the contract preview and the public page

**Files:**
- Modify: `admin-console/frontend/src/features/settings/SettingsPage.tsx`, `src/components/layout/Sidebar.tsx`, `src/features/contracts/ContractDocument.tsx`, `src/features/public/PublicContractPage.tsx`

**Interfaces:**
- Consumes: `StudioSettings.logo`, `PublicContract.studio.logo`, `LogoInput`, the existing `settingsApi.get` (`system.api.ts`)

- [ ] **Step 1: Settings.** Put a `LogoInput` in the studio form (admins only, where the other fields are editable). Save sends the whole settings object, **including the current `logo`** (Review Focus 1).
- [ ] **Step 2: Sidebar.**
  - When `user.role !== 'ROLE_PLATFORM_ADMIN'`, load `settingsApi.get()` once and show logo + studio name at the top. Use the initial when there's no logo; ignore a failed load and show nothing.
  - The platform admin makes no call (Review Focus 5).
  - After saving in Settings the sidebar may show the old logo until the next page load; that's acceptable, don't add global state for it.
- [ ] **Step 3: Contract preview and public page.** Show `<img>` (48 px high) next to the studio name when `logo` is set.
- [ ] **Step 4: Verify.**
  - `npm run build` passes.
  - In `npm run dev`: set a logo in Settings, reload, and check it in the sidebar and the contract preview. Log in as staff: the sidebar shows the logo, and the logo field isn't editable.
  - As the platform admin, the network tab has no `/admin/settings/studio` request.
- [ ] **Step 5: Commit** with message `Web: studio logo in settings, sidebar and contracts`.

---

### Task 8: Docs and end-to-end check

**Files:**
- Modify: `docs/features/detailed_functional_specification.md` (FN-PLT-01/02, FN-ADM-SET-01, FN-PUB-CONTR-01, new history row `1.2.1`), `docs/api/admin-api.yml` (use the `api-documenter` agent or edit by hand: new request/response shapes, `logo`), `docs/guides/web-admin-overview.md` (creating a studio), `deploy/README.md` §3 step 3 (several managers at once)

- [ ] **Step 1: Update the docs** to the shapes in Tasks 2–4, including the keep/remove rules (correction 3) and the PNG/JPEG ≤ 200 KB rule.
- [ ] **Step 2: Full check.** Run `./gradlew build` (repo root) and `cd admin-console/frontend && npm run build`. Expected: both succeed, coverage ≥ 85 %.
- [ ] **Step 3: Commit** with message `Docs: studio setup with several managers and logo`.
