/// Build-time configuration, override with --dart-define:
///   flutter run --dart-define=USE_MOCK=false --dart-define=API_BASE_URL=https://api.example.com/api/v1
class Env {
  Env._();

  /// client-console/backend (Spring Boot, port 8081). 10.0.2.2 = host machine from the Android emulator.
  static const String apiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8081/api/v1');

  /// true -> repositories return in-memory mock data (lib/data/mock), no backend required.
  static const bool useMock = bool.fromEnvironment('USE_MOCK', defaultValue: true);

  /// FN-NOTI-02: Firebase Cloud Messaging; needs the Firebase app files (docs/setup/google-cloud-firebase.md)
  static const bool fcmEnabled = bool.fromEnvironment('FCM_ENABLED');

  /// Google Sign-In: the **Web** OAuth client id. Android needs it to issue an ID token, and it must be one of the
  /// backend's `app.google.client-ids` (the token's audience). Empty = Google login unavailable in real mode.
  static const String googleServerClientId = String.fromEnvironment('GOOGLE_SERVER_CLIENT_ID');

  /// Google Sign-In: the **iOS** OAuth client id (iOS only; can also come from Info.plist `GIDClientID`).
  static const String googleIosClientId = String.fromEnvironment('GOOGLE_IOS_CLIENT_ID');
}
