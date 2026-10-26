import 'package:flutter/material.dart';

import '../../data/models/page_result.dart';
import '../theme/app_colors.dart';

/// A list over a paged endpoint (spec §1.2): loads page 0, then the next page when the user scrolls near the end.
/// Pull to refresh starts again from page 0; give it a new `key` to reload after a filter change.
class PagedListView<T> extends StatefulWidget {
  const PagedListView({
    super.key,
    required this.fetch,
    required this.itemBuilder,
    this.emptyText = 'Chưa có dữ liệu',
    this.padding = const EdgeInsets.all(16),
  });

  final Future<PageResult<T>> Function(int page) fetch;
  final Widget Function(BuildContext context, T item) itemBuilder;
  final String emptyText;
  final EdgeInsetsGeometry padding;

  @override
  State<PagedListView<T>> createState() => _PagedListViewState<T>();
}

class _PagedListViewState<T> extends State<PagedListView<T>> {
  final _scroll = ScrollController();
  final List<T> _items = [];
  int _nextPage = 0;
  bool _hasMore = true;
  bool _loading = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(() {
      if (_scroll.position.extentAfter < 300) _loadMore();
    });
    _loadMore();
  }

  @override
  void dispose() {
    _scroll.dispose();
    super.dispose();
  }

  Future<void> _loadMore() async {
    if (_loading || !_hasMore) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final page = await widget.fetch(_nextPage);
      if (!mounted) return;
      setState(() {
        _items.addAll(page.items);
        _nextPage = page.page + 1;
        _hasMore = page.hasMore;
      });
      // A first page shorter than the screen can't be scrolled: keep loading until it fills or runs out
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && _scroll.hasClients && _scroll.position.extentAfter < 300) _loadMore();
      });
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _refresh() async {
    setState(() {
      _items.clear();
      _nextPage = 0;
      _hasMore = true;
    });
    await _loadMore();
  }

  @override
  Widget build(BuildContext context) {
    if (_items.isEmpty && !_hasMore && _error == null) {
      return RefreshIndicator(
        onRefresh: _refresh,
        child: ListView(children: [
          const SizedBox(height: 120),
          Center(child: Text(widget.emptyText, style: const TextStyle(color: AppColors.slate400))),
        ]),
      );
    }
    return RefreshIndicator(
      onRefresh: _refresh,
      child: ListView.separated(
        controller: _scroll,
        physics: const AlwaysScrollableScrollPhysics(),
        padding: widget.padding,
        itemCount: _items.length + 1,
        separatorBuilder: (_, __) => const SizedBox(height: 12),
        itemBuilder: (context, i) {
          if (i < _items.length) return widget.itemBuilder(context, _items[i]);
          if (_error != null) {
            return Center(
              child: TextButton.icon(onPressed: _loadMore, icon: const Icon(Icons.refresh), label: Text('$_error — Thử lại')),
            );
          }
          return _hasMore
              ? const Padding(padding: EdgeInsets.all(16), child: Center(child: CircularProgressIndicator()))
              : const SizedBox.shrink();
        },
      ),
    );
  }
}
