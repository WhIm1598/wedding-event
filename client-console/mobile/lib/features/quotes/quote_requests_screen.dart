import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../core/widgets/paged_list.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';
import '../../state/session_controller.dart';
import 'widgets/quote_reply_sheet.dart';

/// FN-VND-02/03: a couple's sent quote requests (accept / cancel), or the requests a vendor received (quote / decline)
class QuoteRequestsScreen extends StatefulWidget {
  const QuoteRequestsScreen({super.key});

  @override
  State<QuoteRequestsScreen> createState() => _QuoteRequestsScreenState();
}

class _QuoteRequestsScreenState extends State<QuoteRequestsScreen> {
  late final bool _asVendor;
  QuoteRequestStatus? _status;

  /// Bumped to restart the list from page 0 (filter change, after an action)
  int _version = 0;

  @override
  void initState() {
    super.initState();
    _asVendor = context.read<SessionController>().isVendor;
  }

  void _load() => _version++;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Column(
        children: [
          ScreenHeader(
            title: 'Yêu cầu báo giá',
            bottom: SizedBox(
              height: 48,
              child: ListView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.fromLTRB(12, 0, 0, 8),
                children: [
                  for (final s in <QuoteRequestStatus?>[null, ...QuoteRequestStatus.values])
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: ChoiceChip(
                        label: Text(s?.label ?? 'Tất cả'),
                        selected: _status == s,
                        onSelected: (_) => setState(() {
                          _status = s;
                          _load();
                        }),
                      ),
                    ),
                ],
              ),
            ),
          ),
          Expanded(
            child: PagedListView<QuoteRequest>(
              key: ValueKey('$_status-$_version'),
              padding: const EdgeInsets.all(24),
              emptyText: 'Chưa có yêu cầu báo giá',
              fetch: (page) => context.read<VendorRepository>().getQuoteRequests(asVendor: _asVendor, status: _status, page: page),
              itemBuilder: (_, q) => _QuoteCard(quote: q, asVendor: _asVendor, onChanged: () => setState(_load)),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuoteCard extends StatelessWidget {
  const _QuoteCard({required this.quote, required this.asVendor, required this.onChanged});

  final QuoteRequest quote;
  final bool asVendor;
  final VoidCallback onChanged;

  Future<void> _reply(BuildContext context) async {
    final changed = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.white,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(24))),
      builder: (_) => QuoteReplySheet(request: quote),
    );
    if (changed == true) onChanged();
  }

  Future<void> _accept(BuildContext context) async {
    final repo = context.read<VendorRepository>();
    DateTime? date = quote.eventDate;
    if (date == null) {
      final now = DateTime.now();
      date = await showDatePicker(
        context: context,
        helpText: 'Chọn ngày tổ chức',
        initialDate: now,
        firstDate: DateTime(now.year, now.month, now.day),
        lastDate: DateTime(now.year + 5),
      );
      if (date == null) return;
    }
    try {
      await repo.acceptQuote(quote, eventDate: quote.eventDate == null ? date : null);
      if (context.mounted) showAppSnack(context, 'Đã chốt báo giá với ${quote.vendorName}');
      onChanged();
    } catch (e) {
      if (context.mounted) showAppSnack(context, e.toString(), error: true);
    }
  }

  Future<void> _cancel(BuildContext context) async {
    final repo = context.read<VendorRepository>();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Hủy yêu cầu báo giá?'),
        content: Text('Yêu cầu gửi ${quote.vendorName} sẽ được đóng lại.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Không')),
          TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Hủy yêu cầu')),
        ],
      ),
    );
    if (ok != true) return;
    try {
      await repo.cancelQuoteRequest(quote.id);
      onChanged();
    } catch (e) {
      if (context.mounted) showAppSnack(context, e.toString(), error: true);
    }
  }

  List<Widget> _actions(BuildContext context) => [
        if (asVendor && quote.status.isOpen)
          TextButton(
            onPressed: () => _reply(context),
            child: Text(quote.status == QuoteRequestStatus.quoted ? 'Báo giá lại' : 'Phản hồi'),
          ),
        if (!asVendor && quote.status.isOpen) TextButton(onPressed: () => _cancel(context), child: const Text('Hủy')),
        if (!asVendor && quote.status == QuoteRequestStatus.quoted)
          FilledButton(onPressed: () => _accept(context), child: const Text('Chấp nhận')),
      ];

  @override
  Widget build(BuildContext context) {
    final details = [
      if (quote.eventDate != null) Fmt.date(quote.eventDate!),
      if (quote.venue != null) quote.venue!,
      if (quote.guestCount != null) '${quote.guestCount} khách',
    ].join(' • ');
    return AppCard(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(asVendor ? quote.coupleName : quote.vendorName,
                    style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.slate800)),
              ),
              StatusChip(
                label: quote.status.label,
                color: quote.status.isOpen ? AppColors.pink600 : AppColors.slate500,
                background: quote.status.isOpen ? AppColors.pink50 : AppColors.slate100,
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(quote.packageName, style: const TextStyle(fontSize: 13, color: AppColors.slate600)),
          if (details.isNotEmpty) Text(details, style: const TextStyle(fontSize: 12, color: AppColors.slate400)),
          if (quote.message != null) ...[
            const SizedBox(height: 8),
            Text('“${quote.message}”', style: const TextStyle(fontSize: 13, fontStyle: FontStyle.italic, color: AppColors.slate500)),
          ],
          if (quote.quotedPrice != null) ...[
            const SizedBox(height: 8),
            Text('Báo giá: ${Fmt.vnd(quote.quotedPrice!)}', style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.pink600)),
          ],
          if (quote.vendorNote != null) Text(quote.vendorNote!, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
          if (_actions(context).isNotEmpty) Row(mainAxisAlignment: MainAxisAlignment.end, children: _actions(context)),
        ],
      ),
    );
  }
}
