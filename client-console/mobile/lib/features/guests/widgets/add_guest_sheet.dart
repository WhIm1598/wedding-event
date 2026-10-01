import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/guest.dart';
import '../../../data/repositories/guest_repository.dart';

/// FN-GUEST-02: add guest form. Pops `true` when saved.
class AddGuestSheet extends StatefulWidget {
  const AddGuestSheet({super.key});

  @override
  State<AddGuestSheet> createState() => _AddGuestSheetState();
}

class _AddGuestSheetState extends State<AddGuestSheet> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _notes = TextEditingController();
  String _group = kGuestGroups.first;
  GuestStatus _status = GuestStatus.pending;
  bool _saving = false;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _notes.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      await context.read<GuestRepository>().addGuest(
            Guest(
              id: '',
              name: _name.text.trim(),
              phone: _phone.text.trim().isEmpty ? null : _phone.text.trim(),
              group: _group,
              status: _status,
              notes: _notes.text.trim().isEmpty ? null : _notes.text.trim(),
            ),
          );
      if (mounted) Navigator.of(context).pop(true);
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _saving = false);
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
            const Text('Thêm khách mời', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
            const SizedBox(height: 16),
            const FieldLabel('Họ tên'),
            TextFormField(controller: _name, validator: Validators.name, decoration: const InputDecoration(hintText: 'VD: Trần Thị C')),
            const SizedBox(height: 12),
            const FieldLabel('Số điện thoại'),
            TextFormField(
              controller: _phone,
              keyboardType: TextInputType.phone,
              validator: Validators.optionalPhone,
              decoration: const InputDecoration(hintText: '09xxxxxxxx'),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: _Dropdown<String>(
                    label: 'Nhóm',
                    value: _group,
                    items: {for (final g in kGuestGroups) g: g},
                    onChanged: (v) => setState(() => _group = v),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _Dropdown<GuestStatus>(
                    label: 'Trạng thái',
                    value: _status,
                    items: {for (final s in GuestStatus.values) s: s.label},
                    onChanged: (v) => setState(() => _status = v),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextFormField(controller: _notes, decoration: const InputDecoration(labelText: 'Ghi chú (VD: Ăn chay)')),
            const SizedBox(height: 20),
            PrimaryButton(label: 'Lưu khách mời', loading: _saving, onPressed: _save),
          ],
        ),
      ),
    );
  }
}

class _Dropdown<T> extends StatelessWidget {
  const _Dropdown({required this.label, required this.value, required this.items, required this.onChanged});

  final String label;
  final T value;
  final Map<T, String> items;
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    return InputDecorator(
      decoration: InputDecoration(labelText: label, contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4)),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<T>(
          value: value,
          isExpanded: true,
          items: [
            for (final e in items.entries) DropdownMenuItem<T>(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)),
          ],
          onChanged: (v) {
            if (v != null) onChanged(v);
          },
        ),
      ),
    );
  }
}
