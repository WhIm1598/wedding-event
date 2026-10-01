import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../data/models/service_item.dart';
import '../../data/repositories/service_repository.dart';
import 'widgets/service_tile.dart';

/// FN-SVC-01: service catalog with category filter and keyword search
class SearchScreen extends StatefulWidget {
  const SearchScreen({super.key});

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  ServiceCategory? _category;
  String _keyword = '';
  Timer? _debounce;
  late Future<List<ServiceItem>> _future;

  @override
  void initState() {
    super.initState();
    _reload();
  }

  @override
  void dispose() {
    _debounce?.cancel();
    super.dispose();
  }

  void _reload() {
    _future = context.read<ServiceRepository>().search(category: _category, keyword: _keyword);
  }

  void _onKeyword(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () => setState(() {
          _keyword = value;
          _reload();
        }));
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TabHeader(
          title: 'Khám phá',
          bottom: TextField(
            onChanged: _onKeyword,
            decoration: InputDecoration(
              hintText: 'Tìm kiếm dịch vụ, nhà hàng...',
              prefixIcon: const Icon(Icons.search, color: AppColors.slate400),
              fillColor: AppColors.slate100,
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none),
              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(16), borderSide: BorderSide.none),
            ),
          ),
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (final c in ServiceCategory.values) _CategoryButton(category: c, selected: _category == c, onTap: () => _select(c)),
                ],
              ),
              const SizedBox(height: 24),
              AsyncView<List<ServiceItem>>(
                future: _future,
                builder: (context, services) => services.isEmpty
                    ? const Padding(
                        padding: EdgeInsets.all(32),
                        child: Text('Không tìm thấy dịch vụ phù hợp.', textAlign: TextAlign.center, style: TextStyle(color: AppColors.slate400)),
                      )
                    : Column(
                        children: [
                          for (final s in services)
                            Padding(
                              padding: const EdgeInsets.only(bottom: 12),
                              child: ServiceTile(service: s, onTap: () => context.push(Routes.serviceDetailFor(s.id))),
                            ),
                        ],
                      ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _select(ServiceCategory c) => setState(() {
        _category = _category == c ? null : c;
        _reload();
      });
}

class _CategoryButton extends StatelessWidget {
  const _CategoryButton({required this.category, required this.selected, required this.onTap});

  final ServiceCategory category;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final style = categoryStyle(category);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              color: style.bg,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: selected ? style.color : style.bg, width: 2),
            ),
            child: Icon(style.icon, color: style.color),
          ),
          const SizedBox(height: 8),
          Text(category.label, style: TextStyle(fontSize: 12, fontWeight: selected ? FontWeight.bold : FontWeight.w500, color: AppColors.slate600)),
        ],
      ),
    );
  }
}
