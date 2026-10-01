import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../data/models/app_user.dart';
import '../../state/session_controller.dart';

/// FN-AUTH-03: role picker after first login
class RoleSelectScreen extends StatefulWidget {
  const RoleSelectScreen({super.key});

  @override
  State<RoleSelectScreen> createState() => _RoleSelectScreenState();
}

class _RoleSelectScreenState extends State<RoleSelectScreen> {
  UserRole? _loadingRole;

  Future<void> _select(UserRole role) async {
    setState(() => _loadingRole = role);
    try {
      await context.read<SessionController>().selectRole(role); // router redirects to /home
    } catch (e) {
      if (mounted) showAppSnack(context, e.toString(), error: true);
    } finally {
      if (mounted) setState(() => _loadingRole = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.white,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text('Bạn là ai?', textAlign: TextAlign.center, style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.slate800)),
              const SizedBox(height: 8),
              const Text('Hãy cho chúng tôi biết để cá nhân hóa trải nghiệm.', textAlign: TextAlign.center, style: TextStyle(color: AppColors.slate500)),
              const SizedBox(height: 40),
              _RoleCard(
                title: 'Cô Dâu / Chú Rể',
                subtitle: 'Đang lên kế hoạch cho đám cưới',
                icon: Icons.favorite_border,
                iconColor: AppColors.pink500,
                loading: _loadingRole == UserRole.couple,
                onTap: () => _select(UserRole.couple),
              ),
              const SizedBox(height: 16),
              _RoleCard(
                title: 'Freelancer / Dịch vụ',
                subtitle: 'Nhiếp ảnh gia, Trang điểm...',
                icon: Icons.work_outline,
                iconColor: AppColors.slate400,
                loading: _loadingRole == UserRole.vendor,
                onTap: () => _select(UserRole.vendor),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RoleCard extends StatelessWidget {
  const _RoleCard({required this.title, required this.subtitle, required this.icon, required this.iconColor, required this.loading, required this.onTap});

  final String title;
  final String subtitle;
  final IconData icon;
  final Color iconColor;
  final bool loading;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: loading ? null : onTap,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(24),
        decoration: BoxDecoration(
          border: Border.all(color: loading ? AppColors.pink500 : AppColors.slate100, width: 2),
          borderRadius: BorderRadius.circular(16),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                  const SizedBox(height: 4),
                  Text(subtitle, style: const TextStyle(fontSize: 14, color: AppColors.slate500)),
                ],
              ),
            ),
            loading ? const SizedBox(width: 32, height: 32, child: CircularProgressIndicator(strokeWidth: 2.5)) : Icon(icon, color: iconColor, size: 32),
          ],
        ),
      ),
    );
  }
}
