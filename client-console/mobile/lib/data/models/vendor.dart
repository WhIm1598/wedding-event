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

class QuoteRequest {
  const QuoteRequest({required this.id, required this.customerName, required this.packageName});

  final String id;
  final String customerName;
  final String packageName;
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
