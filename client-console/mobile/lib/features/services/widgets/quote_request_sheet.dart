import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/service_item.dart';
import '../../../data/repositories/vendor_repository.dart';

/// FN-VND-01: the couple asks the package's vendor for a quote. Pops `true` when sent.
class QuoteRequestSheet extends StatefulWidget {
  const QuoteRequestSheet({super.key, required this.service});

  final ServiceItem service;

  @override
  State<QuoteRequestSheet> createState() => _QuoteRequestSheetState();
}

class _QuoteRequestSheetState extends State<QuoteRequestSheet> {
  final _formKey = GlobalKey<FormState>();
  final _venue = TextEditingController();
  final _guests = TextEditingController();
  final _message = TextEditingController();
  DateTime? _eventDate;
  bool _sending = false;

  @override
  void dispose() {
    _venue.dispose();
    _guests.dispose();
    _message.dispose();
    super.dispose();
  }

  Future<void> _pickDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _eventDate ?? now,
      firstDate: DateTime(now.year, now.month, now.day), // spec: not before today
      lastDate: DateTime(now.year + 5),
    );
    if (picked != null) setState(() => _eventDate = picked);
  }

  Future<void> _send() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _sending = true);
    try {
      await context.read<VendorRepository>().sendQuoteRequest(
            widget.service,
            eventDate: _eventDate,
            venue: _venue.text.trim(),
            guestCount: int.tryParse(_guests.text.trim()),
            message: _message.text.trim(),
          );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _sending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, 24 + MediaQuery.viewInsetsOf(context).bottom),
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Yêu cầu báo giá', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
            const SizedBox(height: 4),
            Text('${widget.service.name} • ${widget.service.provider}', style: const TextStyle(color: AppColors.slate500)),
            const SizedBox(height: 16),
            const FieldLabel('Ngày tổ chức (không bắt buộc)'),
            OutlinedButton.icon(
              onPressed: _pickDate,
              icon: const Icon(Icons.calendar_today_outlined, size: 18),
              label: Text(_eventDate == null ? 'Chọn ngày' : Fmt.date(_eventDate!)),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _venue,
              maxLength: 255,
              decoration: const InputDecoration(labelText: 'Địa điểm', counterText: ''),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _guests,
              keyboardType: TextInputType.number,
              validator: Validators.optionalGuestCount,
              decoration: const InputDecoration(labelText: 'Số khách'),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _message,
              maxLength: 1000,
              maxLines: 3,
              decoration: const InputDecoration(labelText: 'Lời nhắn cho nhà cung cấp'),
            ),
            const SizedBox(height: 12),
            PrimaryButton(label: 'Gửi yêu cầu', loading: _sending, onPressed: _send),
          ],
        ),
      ),
    );
  }
}
