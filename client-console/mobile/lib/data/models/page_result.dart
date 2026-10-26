/// One page of a list endpoint, spec §1.2 `{items, page, size, totalElements, totalPages}`
class PageResult<T> {
  const PageResult({required this.items, required this.page, required this.totalPages});

  final List<T> items;

  /// Zero-based
  final int page;
  final int totalPages;

  bool get hasMore => page + 1 < totalPages;

  factory PageResult.fromJson(Map<String, dynamic> json, T Function(Map<String, dynamic>) item) => PageResult(
        items: [for (final e in json['items'] as List<dynamic>) item(e as Map<String, dynamic>)],
        page: (json['page'] as num).toInt(),
        totalPages: (json['totalPages'] as num).toInt(),
      );

  /// Mock branches: the [page]-th slice of [all], like the backend would return it
  factory PageResult.slice(List<T> all, int page, int size) => PageResult(
        items: all.skip(page * size).take(size).toList(),
        page: page,
        totalPages: (all.length / size).ceil(),
      );
}
