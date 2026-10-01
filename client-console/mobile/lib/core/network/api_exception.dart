/// Error raised for failed API calls. `code` follows the spec's error codes
/// (e.g. INVALID_CREDENTIALS, PLAN_ALREADY_EXISTS, NETWORK_ERROR).
class ApiException implements Exception {
  const ApiException({required this.code, required this.message, this.statusCode});

  final String code;
  final String message;
  final int? statusCode;

  @override
  String toString() => message;
}
