/// `VendorProjectStatus` (spec §1.6); the API uses the upper-case names
enum ProjectStatus {
  upcoming('Sắp tới'),
  inProgress('Đang làm'),
  completed('Hoàn thành'),
  cancelled('Đã hủy');

  const ProjectStatus(this.label);
  final String label;

  /// `inProgress` → `IN_PROGRESS`
  String get apiName => name.replaceAllMapped(RegExp('[A-Z]'), (m) => '_${m[0]}').toUpperCase();

  static ProjectStatus fromApi(String value) => values.firstWhere((s) => s.apiName == value);
}

/// `VendorProjectDto` (FN-VND-03/04): an accepted quote request
class VendorProject {
  const VendorProject({
    required this.id,
    required this.coupleName,
    required this.vendorName,
    required this.packageName,
    required this.eventDate,
    required this.agreedPrice,
    required this.status,
    this.venue,
    this.quoteRequestId,
    this.cancelRequestedAt,
    this.cancelReason,
  });

  final String id;
  final String coupleName;
  final String vendorName;
  final String packageName;
  final DateTime eventDate;
  final int agreedPrice;
  final ProjectStatus status;
  final String? venue;
  final String? quoteRequestId;

  /// Set while the couple's cancellation request waits for the vendor (FN-VND-04)
  final DateTime? cancelRequestedAt;
  final String? cancelReason;

  factory VendorProject.fromJson(Map<String, dynamic> json) => VendorProject(
        id: json['id'] as String,
        coupleName: json['coupleName'] as String,
        vendorName: json['vendorName'] as String,
        packageName: json['packageName'] as String,
        eventDate: DateTime.parse(json['eventDate'] as String),
        agreedPrice: (json['agreedPrice'] as num).toInt(),
        status: ProjectStatus.fromApi(json['status'] as String),
        venue: json['venue'] as String?,
        quoteRequestId: json['quoteRequestId'] as String?,
        cancelRequestedAt: json['cancelRequestedAt'] == null ? null : DateTime.parse(json['cancelRequestedAt'] as String),
        cancelReason: json['cancelReason'] as String?,
      );

  bool get isOpen => status == ProjectStatus.upcoming || status == ProjectStatus.inProgress;

  /// The vendor's next steps along §1.6 (cancelling is offered separately)
  List<ProjectStatus> get nextSteps => switch (status) {
        ProjectStatus.upcoming => const [ProjectStatus.inProgress, ProjectStatus.completed],
        ProjectStatus.inProgress => const [ProjectStatus.completed],
        _ => const [],
      };

  VendorProject copyWith({ProjectStatus? status, DateTime? cancelRequestedAt, String? cancelReason, bool clearCancel = false}) => VendorProject(
        id: id,
        coupleName: coupleName,
        vendorName: vendorName,
        packageName: packageName,
        eventDate: eventDate,
        agreedPrice: agreedPrice,
        status: status ?? this.status,
        venue: venue,
        quoteRequestId: quoteRequestId,
        cancelRequestedAt: clearCancel ? null : cancelRequestedAt ?? this.cancelRequestedAt,
        cancelReason: clearCancel ? null : cancelReason ?? this.cancelReason,
      );
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

  QuoteRequest copyWith({QuoteRequestStatus? status, int? quotedPrice, String? vendorNote, DateTime? respondedAt, DateTime? eventDate, String? venue}) =>
      QuoteRequest(
        id: id,
        coupleName: coupleName,
        packageId: packageId,
        packageName: packageName,
        vendorName: vendorName,
        status: status ?? this.status,
        createdAt: createdAt,
        eventDate: eventDate ?? this.eventDate,
        venue: venue ?? this.venue,
        guestCount: guestCount,
        message: message,
        quotedPrice: quotedPrice ?? this.quotedPrice,
        vendorNote: vendorNote ?? this.vendorNote,
        respondedAt: respondedAt ?? this.respondedAt,
      );

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
