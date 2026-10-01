enum GuestStatus {
  attending('Tham gia'),
  pending('Chờ XN'),
  declined('Từ chối');

  const GuestStatus(this.label);
  final String label;

  static GuestStatus fromApi(String? v) => GuestStatus.values.firstWhere(
        (s) => s.name == v?.toLowerCase(),
        orElse: () => GuestStatus.pending,
      );
}

class Guest {
  const Guest({required this.id, required this.name, this.phone, required this.group, required this.status, this.notes, this.tableNumber});

  final String id;
  final String name;
  final String? phone;
  final String group;
  final GuestStatus status;
  final String? notes;
  final int? tableNumber;

  factory Guest.fromJson(Map<String, dynamic> json) => Guest(
        id: json['id'] as String,
        name: json['name'] as String,
        phone: json['phone'] as String?,
        group: json['group'] as String? ?? '',
        status: GuestStatus.fromApi(json['status'] as String?),
        notes: json['notes'] as String?,
        tableNumber: (json['tableNumber'] as num?)?.toInt(),
      );

  Map<String, dynamic> toJson() => {
        'name': name,
        if (phone != null) 'phone': phone,
        'group': group,
        'status': status.name,
        if (notes != null) 'notes': notes,
      };
}

class GuestStats {
  const GuestStats({required this.total, required this.attending, required this.pending, required this.declined});

  final int total;
  final int attending;
  final int pending;
  final int declined;

  factory GuestStats.fromGuests(List<Guest> guests) => GuestStats(
        total: guests.length,
        attending: guests.where((g) => g.status == GuestStatus.attending).length,
        pending: guests.where((g) => g.status == GuestStatus.pending).length,
        declined: guests.where((g) => g.status == GuestStatus.declined).length,
      );

  factory GuestStats.fromJson(Map<String, dynamic> json) => GuestStats(
        total: json['total'] as int,
        attending: json['attending'] as int,
        pending: json['pending'] as int,
        declined: json['declined'] as int,
      );
}

/// FN-GUEST-01 response
class GuestListResult {
  const GuestListResult({required this.stats, required this.guests});

  final GuestStats stats;
  final List<Guest> guests;
}

/// Default guest groups (spec §4.2)
const List<String> kGuestGroups = ['Gia đình nhà Gái', 'Gia đình nhà Trai', 'Bạn cấp 3', 'Bạn đại học', 'Đồng nghiệp'];
