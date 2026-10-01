import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/utils/validators.dart';
import '../../core/widgets/common.dart';
import '../../state/session_controller.dart';

class AccountSettingsScreen extends StatefulWidget {
  const AccountSettingsScreen({super.key});

  @override
  State<AccountSettingsScreen> createState() => _AccountSettingsScreenState();
}

class _AccountSettingsScreenState extends State<AccountSettingsScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name;
  late final TextEditingController _email;
  late final TextEditingController _phone;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final user = context.read<SessionController>().user;
    _name = TextEditingController(text: user?.fullName);
    _email = TextEditingController(text: user?.email);
    _phone = TextEditingController(text: user?.phone ?? '0987654321');
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _phone.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      await context.read<SessionController>().updateProfile(fullName: _name.text.trim(), email: _email.text.trim(), phone: _phone.text.trim());
      if (!mounted) return;
      showAppSnack(context, 'Đã lưu thay đổi');
      context.pop();
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isVendor = context.watch<SessionController>().isVendor;
    return Scaffold(
      body: Column(
        children: [
          const ScreenHeader(title: 'Cài đặt tài khoản'),
          Expanded(
            child: Form(
              key: _formKey,
              child: ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  Center(
                    child: Stack(
                      children: [
                        CircleAvatar(
                          radius: 48,
                          backgroundColor: AppColors.slate100,
                          child: Icon(isVendor ? Icons.work_outline : Icons.person_outline, size: 32, color: AppColors.slate400),
                        ),
                        Positioned(
                          right: 0,
                          bottom: 0,
                          child: IconButton.filled(
                            style: IconButton.styleFrom(backgroundColor: AppColors.pink600, minimumSize: const Size(32, 32)),
                            onPressed: () => showAppSnack(context, 'TODO: chọn ảnh đại diện (image_picker + MinIO upload)'),
                            icon: const Icon(Icons.photo_camera, size: 14, color: AppColors.white),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 24),
                  const FieldLabel('Tên hiển thị'),
                  TextFormField(
                    controller: _name,
                    validator: Validators.name,
                    decoration: const InputDecoration(fillColor: AppColors.white, prefixIcon: Icon(Icons.person_outline, color: AppColors.slate400)),
                  ),
                  const SizedBox(height: 16),
                  const FieldLabel('Email liên hệ'),
                  TextFormField(
                    controller: _email,
                    validator: Validators.email,
                    keyboardType: TextInputType.emailAddress,
                    decoration: const InputDecoration(fillColor: AppColors.white, prefixIcon: Icon(Icons.mail_outline, color: AppColors.slate400)),
                  ),
                  const SizedBox(height: 16),
                  const FieldLabel('Số điện thoại'),
                  TextFormField(
                    controller: _phone,
                    validator: Validators.optionalPhone,
                    keyboardType: TextInputType.phone,
                    decoration: const InputDecoration(fillColor: AppColors.white, prefixIcon: Icon(Icons.phone_outlined, color: AppColors.slate400)),
                  ),
                  const SizedBox(height: 32),
                  PrimaryButton(label: 'Lưu thay đổi', icon: Icons.save_outlined, color: AppColors.slate900, loading: _saving, onPressed: _save),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
