/// ROLE_COUPLE / ROLE_VENDOR (spec §3). Null until the user picks a role (FN-AUTH-03).
enum UserRole {
  couple('ROLE_COUPLE'),
  vendor('ROLE_VENDOR');

  const UserRole(this.apiValue);
  final String apiValue;

  static UserRole? fromApi(String? value) {
    for (final r in UserRole.values) {
      if (r.apiValue == value) return r;
    }
    return null;
  }
}

class AppUser {
  const AppUser({
    required this.id,
    required this.email,
    required this.fullName,
    this.phone,
    this.avatarUrl,
    this.role,
    this.hasPlan = false,
    this.isVerified = false,
  });

  final String id;
  final String email;
  final String fullName;
  final String? phone;
  final String? avatarUrl;
  final UserRole? role;
  final bool hasPlan;
  final bool isVerified;

  factory AppUser.fromJson(Map<String, dynamic> json) => AppUser(
        id: json['id'] as String,
        email: json['email'] as String,
        fullName: json['fullName'] as String? ?? '',
        phone: json['phone'] as String?,
        avatarUrl: json['avatarUrl'] as String?,
        role: UserRole.fromApi(json['role'] as String?),
        hasPlan: json['hasPlan'] as bool? ?? false,
        isVerified: json['isVerified'] as bool? ?? false,
      );

  AppUser copyWith({String? fullName, String? email, String? phone, UserRole? role, bool? hasPlan, bool? isVerified}) => AppUser(
        id: id,
        email: email ?? this.email,
        fullName: fullName ?? this.fullName,
        phone: phone ?? this.phone,
        avatarUrl: avatarUrl,
        role: role ?? this.role,
        hasPlan: hasPlan ?? this.hasPlan,
        isVerified: isVerified ?? this.isVerified,
      );
}
