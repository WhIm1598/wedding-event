import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/validators.dart';
import '../../core/widgets/common.dart';
import '../../state/session_controller.dart';

/// FN-AUTH-02: 4-digit OTP with auto-advancing inputs
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _controllers = List.generate(4, (_) => TextEditingController());
  final _nodes = List.generate(4, (_) => FocusNode());
  bool _loading = false;

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    for (final n in _nodes) {
      n.dispose();
    }
    super.dispose();
  }

  String get _code => _controllers.map((c) => c.text).join();

  void _onChanged(int i, String value) {
    if (value.isNotEmpty && i < 3) _nodes[i + 1].requestFocus();
    if (value.isEmpty && i > 0) _nodes[i - 1].requestFocus();
    if (Validators.isOtp(_code)) _verify();
  }

  Future<void> _verify() async {
    if (_loading) return;
    if (!Validators.isOtp(_code)) {
      showAppSnack(context, 'Vui lòng nhập đủ 4 chữ số', error: true);
      return;
    }
    setState(() => _loading = true);
    try {
      await context.read<SessionController>().verifyOtp(_code); // router redirects to /role
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _resend() async {
    await context.read<SessionController>().resendOtp();
    if (mounted) showAppSnack(context, 'Đã gửi lại mã xác thực');
  }

  @override
  Widget build(BuildContext context) {
    final email = context.watch<SessionController>().pendingEmail;
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: IconButton(
                  icon: const Icon(Icons.arrow_back, color: AppColors.slate500),
                  onPressed: () => context.canPop() ? context.pop() : context.go(Routes.login),
                ),
              ),
              const SizedBox(height: 16),
              const Text('Xác thực tài khoản', style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.slate800)),
              const SizedBox(height: 8),
              Text('Chúng tôi đã gửi mã 4 số tới $email.', style: const TextStyle(color: AppColors.slate500)),
              const SizedBox(height: 32),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  for (var i = 0; i < 4; i++)
                    SizedBox(
                      width: 64,
                      height: 64,
                      child: TextField(
                        controller: _controllers[i],
                        focusNode: _nodes[i],
                        autofocus: i == 0,
                        textAlign: TextAlign.center,
                        keyboardType: TextInputType.number,
                        maxLength: 1,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                        style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                        decoration: const InputDecoration(counterText: '', contentPadding: EdgeInsets.symmetric(vertical: 18)),
                        onChanged: (v) => _onChanged(i, v),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 32),
              PrimaryButton(label: 'Xác nhận', loading: _loading, onPressed: _verify),
              TextButton(onPressed: _resend, child: const Text('Gửi lại mã', style: TextStyle(color: AppColors.pink600))),
            ],
          ),
        ),
      ),
    );
  }
}
