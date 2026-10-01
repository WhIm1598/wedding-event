import '../../core/config/env.dart';
import '../../core/network/api_client.dart';
import '../mock/mock_data.dart';
import '../models/guest.dart';

/// Module 3: Guest list & RSVP (FN-GUEST-01..02)
class GuestRepository {
  GuestRepository(this._api);

  final ApiClient _api;

  /// GET /client/guests?status&group&search — stats always cover the full list
  Future<GuestListResult> getGuests({GuestStatus? status, String? group, String search = ''}) {
    if (Env.useMock) {
      final keyword = search.trim().toLowerCase();
      final filtered = MockData.guests
          .where((g) => status == null || g.status == status)
          .where((g) => group == null || g.group == group)
          .where((g) => keyword.isEmpty || g.name.toLowerCase().contains(keyword))
          .toList();
      return mockDelay(GuestListResult(stats: GuestStats.fromGuests(MockData.guests), guests: filtered), 250);
    }
    return _api.get(
      '/client/guests',
      query: {'status': status?.name ?? 'all', 'group': group, 'search': search},
      parse: (d) {
        final map = d! as Map<String, dynamic>;
        return GuestListResult(
          stats: GuestStats.fromJson(map['stats'] as Map<String, dynamic>),
          guests: (map['guests'] as List<dynamic>).map((e) => Guest.fromJson(e as Map<String, dynamic>)).toList(),
        );
      },
    );
  }

  /// POST /client/guests
  Future<Guest> addGuest(Guest draft) {
    if (Env.useMock) {
      final guest = Guest(
        id: 'g-${DateTime.now().millisecondsSinceEpoch}',
        name: draft.name,
        phone: draft.phone,
        group: draft.group,
        status: draft.status,
        notes: draft.notes,
      );
      MockData.guests.insert(0, guest);
      return mockDelay(guest);
    }
    return _api.post('/client/guests', body: draft.toJson(), parse: (d) => Guest.fromJson(d! as Map<String, dynamic>));
  }

  /// PUT /client/guests/{id}
  Future<Guest> updateGuest(Guest guest) {
    if (Env.useMock) {
      final i = MockData.guests.indexWhere((g) => g.id == guest.id);
      if (i >= 0) MockData.guests[i] = guest;
      return mockDelay(guest);
    }
    return _api.put('/client/guests/${guest.id}', body: guest.toJson(), parse: (d) => Guest.fromJson(d! as Map<String, dynamic>));
  }
}
