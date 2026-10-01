/// Build-time configuration, override with --dart-define:
///   flutter run --dart-define=USE_MOCK=false --dart-define=API_BASE_URL=https://api.example.com/api/v1
class Env {
  Env._();

  /// client-console/backend (Spring Boot, port 8081). 10.0.2.2 = host machine from the Android emulator.
  static const String apiBaseUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8081/api/v1');

  /// true -> repositories return in-memory mock data (lib/data/mock), no backend required.
  static const bool useMock = bool.fromEnvironment('USE_MOCK', defaultValue: true);
}
