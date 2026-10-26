import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/vendor.dart';
import '../../../data/repositories/vendor_repository.dart';

/// FN-VND-03, vendor side: quote (again) or decline a request. Pops `true` when something changed.
class QuoteReplySheet extends StatefulWidget {
  const QuoteReplySheet({super.key, required this.request});

  final QuoteRequest request;

  @override
  State<QuoteReplySheet> createState() => _QuoteReplySheetState();
}

class _QuoteReplySheetState extends State<QuoteReplySheet> {
  final _formKey = GlobalKey<FormState>();
  late final _price = TextEditingController(text: widget.request.quotedPrice?.toString() ?? '');
  late final _note = TextEditingController(text: widget.request.vendorNote ?? '');
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    _price.dispose();
    _note.dispose();
    super.dispose();
  }

  Future<void> _run(Future<void> Function(VendorRepository repo) action) async {
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      await action(context.read<VendorRepository>());
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  void _quote() {
    if (!_formKey.currentState!.validate()) return;
    _run((repo) => repo.quote(widget.request.id, Fmt.parseMoney(_price.text), vendorNote: _note.text.trim()));
  }

  @override
  Widget build(BuildContext context) {
    final r = widget.request;
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, 24 + MediaQuery.viewInsetsOf(context).bottom),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(r.status == QuoteRequestStatus.quoted ? 'Báo giá lại' : 'Báo giá',
                style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
            const SizedBox(height: 4),
            Text('${r.coupleName} • ${r.packageName}', style: const TextStyle(color: AppColors.slate500)),
            const SizedBox(height: 16),
            TextFormField(
              controller: _price,
              keyboardType: TextInputType.number,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              validator: (v) => Fmt.parseMoney(v ?? '') > 0 ? null : 'Giá báo phải lớn hơn 0',
              decoration: const InputDecoration(labelText: 'Giá báo (₫)'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _note,
              maxLength: 1000,
              maxLines: 3,
              decoration: const InputDecoration(labelText: 'Ghi chú cho cặp đôi'),
            ),
            const SizedBox(height: 12),
            FormError(_error),
            PrimaryButton(label: 'Gửi báo giá', loading: _saving, onPressed: _quote),
            const SizedBox(height: 8),
            TextButton(
              style: TextButton.styleFrom(foregroundColor: AppColors.red600),
              onPressed: _saving ? null : () => _run((repo) => repo.decline(r.id, vendorNote: _note.text.trim())),
              child: const Text('Từ chối yêu cầu'),
            ),
          ],
        ),
      ),
    );
  }
}
