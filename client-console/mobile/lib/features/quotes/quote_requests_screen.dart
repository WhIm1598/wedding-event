import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/formatters.dart';
import '../../core/widgets/common.dart';
import '../../data/models/vendor.dart';
import '../../data/repositories/vendor_repository.dart';
import '../../state/session_controller.dart';

/// FN-VND-02: a couple's sent quote requests, or the requests a vendor received
class QuoteRequestsScreen extends StatefulWidget {
  const QuoteRequestsScreen({super.key});

  @override
  State<QuoteRequestsScreen> createState() => _QuoteRequestsScreenState();
}

class _QuoteRequestsScreenState extends State<QuoteRequestsScreen> {
  late final bool _asVendor;
  late Future<List<QuoteRequest>> _future;
  QuoteRequestStatus? _status;

  @override
  void initState() {
    super.initState();
    _asVendor = context.read<SessionController>().isVendor;
    _load();
  }

  void _load() => _future = context.read<VendorRepository>().getQuoteRequests(asVendor: _asVendor, status: _status);

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
            child: AsyncView<List<QuoteRequest>>(
              future: _future,
              builder: (context, items) => items.isEmpty
                  ? const Center(child: Text('Chưa có yêu cầu báo giá', style: TextStyle(color: AppColors.slate400)))
                  : ListView.separated(
                      padding: const EdgeInsets.all(24),
                      itemCount: items.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 12),
                      itemBuilder: (_, i) => _QuoteCard(quote: items[i], asVendor: _asVendor),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _QuoteCard extends StatelessWidget {
  const _QuoteCard({required this.quote, required this.asVendor});

  final QuoteRequest quote;
  final bool asVendor;

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
        ],
      ),
    );
  }
}
