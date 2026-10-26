import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/service_item.dart';
import '../../../data/models/vendor.dart';
import '../../../data/repositories/vendor_repository.dart';

/// FN-VND-05: create a package, or edit [service]. Pops the saved package.
class ServiceFormSheet extends StatefulWidget {
  const ServiceFormSheet({super.key, this.service});

  final VendorService? service;

  @override
  State<ServiceFormSheet> createState() => _ServiceFormSheetState();
}

class _ServiceFormSheetState extends State<ServiceFormSheet> {
  final _formKey = GlobalKey<FormState>();
  late final _name = TextEditingController(text: widget.service?.name ?? '');
  late final _type = TextEditingController(text: widget.service?.type ?? '');
  late final _price = TextEditingController(text: widget.service?.price.toString() ?? '');
  late final _discount = TextEditingController(text: widget.service?.discountPrice?.toString() ?? '');
  late final _description = TextEditingController(text: widget.service?.description ?? '');
  late final _features = TextEditingController(text: widget.service?.features.join('\n') ?? '');
  late final _thumbnail = TextEditingController(text: widget.service?.thumbnailUrl ?? '');
  late ServiceCategory _category = widget.service?.category ?? ServiceCategory.photo;
  bool _saving = false;
  String? _error;

  @override
  void dispose() {
    for (final c in [_name, _type, _price, _discount, _description, _features, _thumbnail]) {
      c.dispose();
    }
    super.dispose();
  }

  static String? _orNull(String s) => s.trim().isEmpty ? null : s.trim();

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _saving = true;
      _error = null;
    });
    try {
      final saved = await context.read<VendorRepository>().saveService(VendorService(
            id: widget.service?.id ?? '',
            name: _name.text.trim(),
            category: _category,
            type: _orNull(_type.text),
            price: Fmt.parseMoney(_price.text),
            discountPrice: _discount.text.trim().isEmpty ? null : Fmt.parseMoney(_discount.text),
            description: _orNull(_description.text),
            features: _features.text.split('\n').map((f) => f.trim()).where((f) => f.isNotEmpty).toList(),
            thumbnailUrl: _orNull(_thumbnail.text),
            isActive: widget.service?.isActive ?? true,
            projectCount: widget.service?.projectCount ?? 0,
          ));
      if (mounted) Navigator.of(context).pop(saved);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
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
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(widget.service == null ? 'Thêm gói dịch vụ' : 'Sửa gói dịch vụ',
                  style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
              const SizedBox(height: 16),
              TextFormField(
                controller: _name,
                maxLength: 100,
                validator: Validators.name,
                decoration: const InputDecoration(labelText: 'Tên gói', counterText: ''),
              ),
              const SizedBox(height: 12),
              InputDecorator(
                decoration: const InputDecoration(labelText: 'Danh mục', contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 4)),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<ServiceCategory>(
                    value: _category,
                    isExpanded: true,
                    items: [for (final c in ServiceCategory.values) DropdownMenuItem(value: c, child: Text(c.label))],
                    onChanged: (c) => setState(() => _category = c ?? _category),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(controller: _type, maxLength: 50, decoration: const InputDecoration(labelText: 'Loại gói (VD: Trọn gói)', counterText: '')),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: TextFormField(
                      controller: _price,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      validator: (v) => Fmt.parseMoney(v ?? '') > 0 ? null : 'Giá phải lớn hơn 0',
                      decoration: const InputDecoration(labelText: 'Giá (₫)'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextFormField(
                      controller: _discount,
                      keyboardType: TextInputType.number,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      validator: (v) => Validators.discountPrice(v, Fmt.parseMoney(_price.text)),
                      decoration: const InputDecoration(labelText: 'Giá sau giảm (₫)'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextFormField(controller: _description, maxLength: 2000, maxLines: 3, decoration: const InputDecoration(labelText: 'Mô tả')),
              const SizedBox(height: 12),
              TextFormField(
                controller: _features,
                maxLines: 4,
                validator: Validators.features,
                decoration: const InputDecoration(labelText: 'Quyền lợi (mỗi dòng một mục, tối đa 20)'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _thumbnail,
                maxLength: 500,
                keyboardType: TextInputType.url,
                decoration: const InputDecoration(labelText: 'Link ảnh đại diện', counterText: ''),
              ),
              const SizedBox(height: 16),
              FormError(_error),
              PrimaryButton(label: 'Lưu gói dịch vụ', loading: _saving, onPressed: _save),
            ],
          ),
        ),
      ),
    );
  }
}
