# client-console/mobile — WedPlanner app

Flutter (Dart 3.5+) · go_router · provider · dio · flutter_lints. Package name `wedplanner`. UI text is Vietnamese.
Platform folders (`android/`, `ios/`) are generated locally with `flutter create . --platforms=android,ios` and not committed.
The client backend doesn't exist yet, so the app runs on mock data by default.

## Where things go (`lib/`)
| Path | Contents |
|---|---|
| `main.dart`, `app.dart` | Bootstrap; `MultiProvider` registering `ApiClient`, `SessionController` and every repository |
| `core/config/env.dart` | `Env.useMock` (`--dart-define=USE_MOCK=false`), `Env.apiBaseUrl` (`API_BASE_URL`, default `http://10.0.2.2:8081/api/v1`) |
| `core/network/` | `ApiClient.get/post/put/patch(path, {body/query, parse:})` unwrapping the `{success, code, message, data}` envelope; `ApiException(code:, message:, statusCode:)`; `mockDelay(value, [ms])` |
| `core/router/app_router.dart` | `Routes` path constants + `createRouter(session)`: auth redirects, `ShellRoute` for bottom-nav tabs, plain `GoRoute`s for full-screen pages |
| `core/theme/` | `AppColors` (rose/pink brand, slate neutrals), `AppTheme.light` |
| `core/widgets/common.dart` | `AppCard`, `ScreenHeader`, `TabHeader`, `IconBubble`, `StatusChip`, `PrimaryButton`, `BottomActionBar`, `FieldLabel`, `AsyncView<T>` |
| `core/utils/` | `Fmt` (money, dates), `Validators` (email, VN phone, required) |
| `data/models/` | Immutable models with `const` constructors and `fromJson`/`toJson` using the spec's JSON names |
| `data/repositories/` | One per API module (`auth`, `plan`, `guest`, `service`, `vendor`, `notification`) |
| `data/mock/mock_data.dart` | `MockData` seed data used by every mock branch |
| `state/session_controller.dart` | `SessionController` (ChangeNotifier): user, `isLoggedIn`, `needsRole`, `isVendor`, `hasPlan`, login/OTP/role/profile |
| `features/<feature>/` | Screens (`*_screen.dart`) + `widgets/` for that feature: `auth`, `home`, `plan`, `ai_chat`, `guests`, `sync`, `services`, `notifications`, `profile`, `vendor`, `shell` |
| `test/` | Unit tests for pure logic (`formatters_test.dart`) |

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

## Rules (`flutter analyze` must report zero issues)
- No deprecated APIs: use `Color.withValues(alpha:)` not `withOpacity`, `WidgetStateProperty` not `MaterialStateProperty`,
  `DropdownButtonFormField(initialValue:)` not `value:`, `Switch(activeThumbColor:)` not `activeColor:`.
- Check `if (!mounted) return;` (or `context.mounted`) after every `await` before using `context`.
- Never call `context.read` inside `build`; always pass `super.key`; dispose controllers in `dispose()`.
- Add unit tests in `test/` for new pure logic (formatters, validators, calculations).
- Check: `flutter analyze && flutter test`.
