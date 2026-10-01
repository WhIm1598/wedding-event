import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../data/models/guest.dart';
import '../../data/repositories/guest_repository.dart';
import 'widgets/add_guest_sheet.dart';

/// FN-GUEST-01: guest list with stats, status filter and name search
class GuestListScreen extends StatefulWidget {
  const GuestListScreen({super.key});

  @override
  State<GuestListScreen> createState() => _GuestListScreenState();
}

class _GuestListScreenState extends State<GuestListScreen> {
  GuestStatus? _status;
  String _search = '';
  Timer? _debounce;
  late Future<GuestListResult> _future;

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
    _future = context.read<GuestRepository>().getGuests(status: _status, search: _search);
  }

  void _onSearch(String value) {
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 300), () => setState(() {
          _search = value;
          _reload();
        }));
  }

  Future<void> _addGuest() async {
    final added = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => const AddGuestSheet(),
    );
    if (added == true && mounted) {
      setState(_reload);
      showAppSnack(context, 'Đã thêm khách mời');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          ScreenHeader(
            title: 'Khách mời',
            actions: [
              IconButton(
                style: IconButton.styleFrom(backgroundColor: AppColors.pink50, foregroundColor: AppColors.pink600),
                onPressed: _addGuest,
                icon: const Icon(Icons.person_add_alt),
              ),
            ],
            bottom: Padding(
              padding: const EdgeInsets.fromLTRB(12, 4, 0, 16),
              child: TextField(
                onChanged: _onSearch,
                decoration: InputDecoration(
                  hintText: 'Tìm tên khách...',
                  isDense: true,
                  prefixIcon: const Icon(Icons.search, color: AppColors.slate400, size: 20),
                  fillColor: AppColors.slate100,
                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                  enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                ),
              ),
            ),
          ),
          Expanded(
            child: AsyncView<GuestListResult>(
              future: _future,
              onRetry: () => setState(_reload),
              builder: (context, result) => ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  Row(
                    children: [
                      _StatCard(label: 'Tổng cộng', value: result.stats.total, color: AppColors.slate800),
                      const SizedBox(width: 12),
                      _StatCard(label: 'Tham gia', value: result.stats.attending, color: AppColors.emerald500),
                      const SizedBox(width: 12),
                      _StatCard(label: 'Chờ', value: result.stats.pending, color: AppColors.orange500),
                    ],
                  ),
                  const SizedBox(height: 24),
                  _StatusFilter(
                    selected: _status,
                    onChanged: (s) => setState(() {
                      _status = s;
                      _reload();
                    }),
                  ),
                  const SizedBox(height: 24),
                  if (result.guests.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(32),
                      child: Text('Không có khách mời phù hợp.', textAlign: TextAlign.center, style: TextStyle(color: AppColors.slate400)),
                    ),
                  for (final g in result.guests)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12),
                      child: AppCard(
                        padding: const EdgeInsets.all(16),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(g.name, style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.slate800)),
                                  Text(g.group, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                                ],
                              ),
                            ),
                            GuestStatusChip(status: g.status),
                          ],
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class GuestStatusChip extends StatelessWidget {
  const GuestStatusChip({super.key, required this.status});

  final GuestStatus status;

  @override
  Widget build(BuildContext context) {
    return switch (status) {
      GuestStatus.attending => StatusChip(label: status.label, color: AppColors.emerald600, background: AppColors.emerald50),
      GuestStatus.pending => StatusChip(label: status.label, color: AppColors.orange600, background: AppColors.orange50),
      GuestStatus.declined => StatusChip(label: status.label, color: AppColors.red600, background: AppColors.red50),
    };
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.label, required this.value, required this.color});

  final String label;
  final int value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: AppCard(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Text(label, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
            const SizedBox(height: 4),
            Text('$value', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: color)),
          ],
        ),
      ),
    );
  }
}

class _StatusFilter extends StatelessWidget {
  const _StatusFilter({required this.selected, required this.onChanged});

  final GuestStatus? selected;
  final ValueChanged<GuestStatus?> onChanged;

  @override
  Widget build(BuildContext context) {
    final options = <(GuestStatus?, String)>[
      (null, 'Tất cả'),
      (GuestStatus.attending, 'Tham gia'),
      (GuestStatus.pending, 'Chờ XN'),
      (GuestStatus.declined, 'Từ chối'),
    ];
    return SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      child: Row(
        children: [
          for (final o in options)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: ChoiceChip(
                label: Text(o.$2),
                selected: selected == o.$1,
                onSelected: (_) => onChanged(o.$1),
                showCheckmark: false,
                selectedColor: AppColors.slate800,
                backgroundColor: AppColors.white,
                side: const BorderSide(color: AppColors.slate200),
                shape: const StadiumBorder(),
                labelStyle: TextStyle(
                  fontWeight: FontWeight.w600,
                  color: selected == o.$1 ? AppColors.white : AppColors.slate500,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
