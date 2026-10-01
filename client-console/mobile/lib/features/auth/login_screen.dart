import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/config/env.dart';
import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/validators.dart';
import '../../core/widgets/common.dart';
import '../../data/mock/mock_data.dart';
import '../../state/session_controller.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController(text: Env.useMock ? MockData.demoEmail : '');
  final _password = TextEditingController(text: Env.useMock ? 'Password123@' : '');
  bool _loading = false;
  bool _googleLoading = false;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    try {
      final needsOtp = await context.read<SessionController>().login(_email.text.trim(), _password.text);
      if (!mounted) return;
      if (needsOtp) context.push(Routes.otp);
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _loginWithGoogle() async {
    setState(() => _googleLoading = true);
    try {
      await context.read<SessionController>().loginWithGoogle(); // router redirects to /role
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _googleLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 40),
                const Text('Đăng nhập', style: TextStyle(fontSize: 30, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                const SizedBox(height: 8),
                const Text('Chào mừng bạn đến với WedPlanner!', style: TextStyle(color: AppColors.slate500)),
                const SizedBox(height: 32),
                const FieldLabel('Email'),
                TextFormField(
                  controller: _email,
                  keyboardType: TextInputType.emailAddress,
                  validator: Validators.email,
                  decoration: const InputDecoration(hintText: 'Nhập email của bạn', prefixIcon: Icon(Icons.mail_outline, color: AppColors.slate400)),
                ),
                const SizedBox(height: 16),
                const FieldLabel('Mật khẩu'),
                TextFormField(
                  controller: _password,
                  obscureText: true,
                  validator: Validators.password,
                  decoration: const InputDecoration(hintText: '••••••••', prefixIcon: Icon(Icons.lock_outline, color: AppColors.slate400)),
                ),
                const SizedBox(height: 24),
                PrimaryButton(label: 'Đăng nhập', loading: _loading, onPressed: _login),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 16),
                  child: Text('hoặc', textAlign: TextAlign.center, style: TextStyle(color: AppColors.slate500)),
                ),
                OutlinedButton.icon(
                  onPressed: _googleLoading ? null : _loginWithGoogle,
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size.fromHeight(52),
                    foregroundColor: AppColors.slate700,
                    side: const BorderSide(color: AppColors.slate200),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                    textStyle: const TextStyle(fontWeight: FontWeight.bold),
                  ),
                  icon: _googleLoading
                      ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2))
                      : const Text('G', style: TextStyle(fontSize: 18, fontWeight: FontWeight.w900, color: AppColors.blue500)),
                  label: const Text('Tiếp tục với Google'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
