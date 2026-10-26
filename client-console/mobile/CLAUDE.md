# client-console/mobile — WedPlanner app

Flutter (Dart 3.5+) · go_router · provider · dio · google_sign_in · url_launcher · flutter_secure_storage · firebase_messaging · flutter_lints. Package name `wedplanner`. UI text is Vietnamese.
Platform folders (`android/`, `ios/`) are generated locally with `flutter create . --platforms=android,ios --org vn.wedplanner` and not committed.
After generating: revert `analysis_options.yaml`, delete the sample `test/widget_test.dart`, add
`<application android:usesCleartextTraffic="true"/>` to `android/app/src/debug/AndroidManifest.xml` (local backend is plain http)
Keep `PUB_CACHE` on the project's drive: Kotlin incremental compile fails on plugin sources from another drive
(`this and base files have different roots`); otherwise add `kotlin.incremental=false` to `android/gradle.properties`.
The app runs on mock data by default; `--dart-define=USE_MOCK=false` calls `client-console/backend` (port 8081, see its
CLAUDE.md). Real mode covers auth, plans, guests, catalog, notifications and the vendor workspace (`VendorRepository`, spec
FN-VND-01..05). In-app chat is the AI assistant only: couples reach the studio or a vendor on Zalo (`ServiceItem.zaloUrl`,
FN-SVC-02, opened with `launchUrl`); FN-MSG-01 stays in the backend but the app doesn't use it. List endpoints return the spec's page shape
`{items, page, size, totalElements, totalPages}`.

## Where things go (`lib/`)
| Path | Contents |
|---|---|
| `main.dart`, `app.dart` | Bootstrap; `MultiProvider` registering `ApiClient`, `SessionController` and every repository |
| `core/config/env.dart` | `Env.useMock` (`--dart-define=USE_MOCK=false`), `Env.apiBaseUrl` (`API_BASE_URL`, default `http://10.0.2.2:8081/api/v1`), Google Sign-In ids `GOOGLE_SERVER_CLIENT_ID` (Web client) / `GOOGLE_IOS_CLIENT_ID` |
| `core/network/` | `ApiClient.get/post/put/patch(path, {body/query, parse:})` unwrapping the `{success, code, message, data}` envelope; session (FN-AUTH-07): `setTokens`/`clearTokens`/`restoreTokens` persisted by `TokenStore` (secure storage), a 401 triggers one single-flight `/auth/refresh` and a retry, a refused refresh calls `onSessionExpired`; `ApiException(code:, message:, statusCode:)`; `mockDelay(value, [ms])` |
| `core/push/push_service.dart` | `PushService` (FN-NOTI-02): only with `--dart-define=FCM_ENABLED=true` and the Firebase app files (`docs/setup/google-cloud-firebase.md`); registers the device after sign-in (`SessionController`), unregisters before logout, `planUpdates` (FN-PLAN-05 data message) makes the plan screen reload, foreground notifications show a snack |
| `core/router/app_router.dart` | `Routes` path constants + `createRouter(session)`: auth redirects, `ShellRoute` for bottom-nav tabs, plain `GoRoute`s for full-screen pages |
| `core/theme/` | `AppColors` (rose/pink brand, slate neutrals), `AppTheme.light` |
| `core/widgets/common.dart` | `AppCard`, `ScreenHeader`, `TabHeader`, `IconBubble`, `StatusChip`, `PrimaryButton`, `BottomActionBar`, `FieldLabel`, `AsyncView<T>`, `showAppSnack`, `showComingSoon` (Vietnamese "sắp ra mắt" for template buttons without a spec endpoint; dev notes stay in code comments, never in UI text) |
| `core/widgets/paged_list.dart` | `PagedListView<T>(fetch: (page) => repo.list(page: page), itemBuilder:)`: infinite scroll + pull to refresh over a §1.2 page endpoint; new `key` to restart from page 0 |
| `core/utils/` | `Fmt` (money, dates), `Validators` (email, VN phone, required) |
| `data/models/` | Immutable models with `const` constructors and `fromJson`/`toJson` using the spec's JSON names |
| `data/repositories/` | One per API module (`auth`, `plan`, `guest`, `service`, `vendor`, `notification`) |
| `data/models/page_result.dart` | `PageResult<T>` (`items`, `page`, `totalPages`, `hasMore`): `PageResult.fromJson(json, Item.fromJson)`; mock branches return `PageResult.slice(all, page, size)` |
| `data/mock/mock_data.dart` | `MockData` seed data used by every mock branch |
| `state/session_controller.dart` | `SessionController` (ChangeNotifier): user, `isLoggedIn`, `needsRole`, `isVendor`, `hasPlan`, login/OTP/role/profile |
| `features/<feature>/` | Screens (`*_screen.dart`) + `widgets/` for that feature: `auth`, `home`, `plan`, `ai_chat`, `guests`, `sync`, `services`, `quotes` (quote requests, both roles), `notifications`, `profile`, `vendor`, `shell` |
| `test/` | Unit tests for pure logic (`formatters_test.dart`: `Fmt` and `Validators`) |

## Recipe: new API call or screen
1. Model in `data/models/` with the spec's JSON field names (see Part A of the detailed spec).
2. Repository method with **both** branches, and a doc comment naming the endpoint:
   ```dart
   /// PATCH /client/plans/tasks/{taskId} (FN-PLAN-05)
   Future<void> setTaskCompleted(String taskId, bool completed) async {
     if (Env.useMock) return mockDelay(null);
     await _api.patch('/client/plans/tasks/$taskId', body: {'isCompleted': completed}, parse: (_) {});
   }
   ```
   Paths are relative to `Env.apiBaseUrl` (already `/api/v1`). Mock branches enforce the spec's rules and throw
   `ApiException(code: 'SPEC_CODE', message: '...')`. New repository → register it in `app.dart`.
3. Screen in `features/<feature>/` built from `common.dart` widgets and `AppColors`; read repositories with
   `context.read<XRepository>()` in callbacks/`initState`, watch state with `context.watch<SessionController>()` in `build`.
4. Route: add a `Routes` constant and a `GoRoute` (inside the `ShellRoute` only if it is a bottom-nav tab).

## Google Sign-In (`google_sign_in` 7, `AuthRepository.loginWithGoogle`)
Mock mode needs nothing. Real mode needs OAuth clients in one Google Cloud project:
- **Web** client → `--dart-define=GOOGLE_SERVER_CLIENT_ID=...` and in the backend's `app.google.client-ids` (it is the ID token's audience).
- **Android** client: package name + SHA-1 of the signing key (`keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android` for debug). No code, only registration.
- **iOS** client (macOS + Xcode only): `--dart-define=GOOGLE_IOS_CLIENT_ID=...` (or `GIDClientID` in `ios/Runner/Info.plist`) and its reversed client id as a URL scheme (`CFBundleURLTypes`) in `ios/Runner/Info.plist`.
`initialize()` runs once per app run; closing the Google sheet returns null (no error); logout also signs out of Google.

## Rules (`flutter analyze` must report zero issues)
- No deprecated APIs: use `Color.withValues(alpha:)` not `withOpacity`, `WidgetStateProperty` not `MaterialStateProperty`,
  `DropdownButtonFormField(initialValue:)` not `value:`, `Switch(activeThumbColor:)` not `activeColor:`.
- Check `if (!mounted) return;` (or `context.mounted`) after every `await` before using `context`.
- Never call `context.read` inside `build`; always pass `super.key`; dispose controllers in `dispose()`.
- Add unit tests in `test/` for new pure logic (formatters, validators, calculations).
- Check: `flutter analyze && flutter test`.
