enum ProjectStatus {
  upcoming('Sắp tới'),
  inProgress('Đang làm'),
  completed('Hoàn thành');

  const ProjectStatus(this.label);
  final String label;
}

class VendorProject {
  const VendorProject({required this.id, required this.name, required this.service, required this.date, required this.status, this.venue});

  final String id;
  final String name;
  final String service;
  final DateTime date;
  final ProjectStatus status;
  final String? venue;
}

/// `QuoteRequestStatus` (spec §1.6); the API uses the upper-case names
enum QuoteRequestStatus {
  pending('Chờ báo giá'),
  quoted('Đã báo giá'),
  accepted('Đã chốt'),
  declined('Từ chối'),
  cancelled('Đã hủy');

  const QuoteRequestStatus(this.label);
  final String label;

  String get apiName => name.toUpperCase();
  bool get isOpen => this == pending || this == quoted;

  static QuoteRequestStatus fromApi(String value) => values.firstWhere((s) => s.apiName == value);
}

/// `QuoteRequestDto` (FN-VND-01..03): a couple asks a vendor to quote one of its packages
class QuoteRequest {
  const QuoteRequest({
    required this.id,
    required this.coupleName,
    required this.packageId,
    required this.packageName,
    required this.vendorName,
    required this.status,
    required this.createdAt,
    this.eventDate,
    this.venue,
    this.guestCount,
    this.message,
    this.quotedPrice,
    this.vendorNote,
    this.respondedAt,
  });

  final String id;
  final String coupleName;
  final String packageId;
  final String packageName;
  final String vendorName;
  final QuoteRequestStatus status;
  final DateTime createdAt;
  final DateTime? eventDate;
  final String? venue;
  final int? guestCount;
  final String? message;
  final int? quotedPrice;
  final String? vendorNote;
  final DateTime? respondedAt;

  factory QuoteRequest.fromJson(Map<String, dynamic> json) => QuoteRequest(
        id: json['id'] as String,
        coupleName: json['coupleName'] as String,
        packageId: json['packageId'] as String,
        packageName: json['packageName'] as String,
        vendorName: json['vendorName'] as String,
        status: QuoteRequestStatus.fromApi(json['status'] as String),
        createdAt: DateTime.parse(json['createdAt'] as String),
        eventDate: json['eventDate'] == null ? null : DateTime.parse(json['eventDate'] as String),
        venue: json['venue'] as String?,
        guestCount: json['guestCount'] as int?,
        message: json['message'] as String?,
        quotedPrice: (json['quotedPrice'] as num?)?.toInt(),
        vendorNote: json['vendorNote'] as String?,
        respondedAt: json['respondedAt'] == null ? null : DateTime.parse(json['respondedAt'] as String),
      );
}

class VendorDashboard {
  const VendorDashboard({
    required this.monthlyRevenue,
    required this.newRequestCount,
    required this.rating,
    required this.quoteRequests,
    required this.upcoming,
  });

  final int monthlyRevenue;
  final int newRequestCount;
  final double rating;
  final List<QuoteRequest> quoteRequests;
  final List<VendorProject> upcoming;
}

class ChatThread {
  const ChatThread({required this.id, required this.name, required this.lastMessage, required this.timeLabel, this.unread = 0});

  final String id;
  final String name;
  final String lastMessage;
  final String timeLabel;
  final int unread;
}

class VendorService {
  const VendorService({
    required this.id,
    required this.name,
    required this.price,
    required this.isActive,
    required this.bookings,
    required this.imageUrl,
  });

  final String id;
  final String name;
  final int price;
  final bool isActive;
  final int bookings;
  final String imageUrl;

  VendorService copyWith({bool? isActive}) =>
      VendorService(id: id, name: name, price: price, isActive: isActive ?? this.isActive, bookings: bookings, imageUrl: imageUrl);
}
