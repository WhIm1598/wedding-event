/// Catalog categories for FN-SVC-01 (`?category=photo|decor|restaurant|dress`)
enum ServiceCategory {
  restaurant('Nhà hàng'),
  photo('Chụp ảnh'),
  dress('Trang phục'),
  decor('Hoa & Decor');

  const ServiceCategory(this.label);
  final String label;
}

class ServiceItem {
  const ServiceItem({
    required this.id,
    required this.name,
    required this.provider,
    required this.price,
    required this.rating,
    required this.reviews,
    required this.location,
    required this.category,
    required this.imageUrl,
    required this.description,
    this.providerId,
    this.discountPrice,
  });

  final String id;
  final String name;
  final String provider;

  /// User id of the vendor (FN-MSG-01 `recipientId`); null for the studio's own packages
  final String? providerId;
  final int price;

  /// Price after the vendor's discount (FN-VND-05); null = no discount
  final int? discountPrice;
  final double rating;
  final int reviews;
  final String location;
  final ServiceCategory category;
  final String imageUrl;
  final String description;

  factory ServiceItem.fromJson(Map<String, dynamic> json) => ServiceItem(
        id: json['id'] as String,
        name: json['name'] as String,
        provider: json['providerName'] as String? ?? '',
        providerId: json['providerId'] as String?,
        price: (json['price'] as num).toInt(),
        discountPrice: (json['discountPrice'] as num?)?.toInt(),
        rating: (json['rating'] as num?)?.toDouble() ?? 0,
        reviews: json['reviewCount'] as int? ?? 0,
        location: json['address'] as String? ?? '',
        category: ServiceCategory.values.firstWhere((c) => c.name == json['category'], orElse: () => ServiceCategory.photo),
        imageUrl: json['thumbnailUrl'] as String? ?? '',
        description: json['description'] as String? ?? '',
      );
}
