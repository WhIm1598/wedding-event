import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../core/widgets/common.dart';
import '../../state/session_controller.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final session = context.watch<SessionController>();
    final user = session.user;
    final isVendor = session.isVendor;

    final menu = <(IconData, String, VoidCallback)>[
      (Icons.settings_outlined, 'Cài đặt tài khoản', () => context.push(Routes.accountSettings)),
      isVendor
          ? (Icons.description_outlined, 'Quản lý dịch vụ', () => context.push(Routes.serviceManagement))
          : (Icons.credit_card, 'Phương thức thanh toán', () => showAppSnack(context, 'TODO: phương thức thanh toán')),
      (Icons.shield_outlined, 'Bảo mật', () => showAppSnack(context, 'TODO: cài đặt bảo mật')),
    ];

    return Column(
      children: [
        Container(
          width: double.infinity,
          padding: EdgeInsets.fromLTRB(24, MediaQuery.paddingOf(context).top + 24, 24, 24),
          decoration: BoxDecoration(
            color: isVendor ? AppColors.slate900 : AppColors.white,
            borderRadius: const BorderRadius.vertical(bottom: Radius.circular(32)),
            boxShadow: const [BoxShadow(color: AppColors.shadow, blurRadius: 6, offset: Offset(0, 2))],
          ),
          child: Column(
            children: [
              Container(
                width: 96,
                height: 96,
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: isVendor ? AppColors.slate700 : null,
                  gradient: isVendor ? null : AppColors.brandGradient,
                ),
                child: CircleAvatar(
                  backgroundColor: isVendor ? AppColors.slate800 : AppColors.white,
                  child: Icon(Icons.person, size: 40, color: isVendor ? AppColors.slate300 : AppColors.pink200),
                ),
              ),
              const SizedBox(height: 16),
              Text(user?.fullName ?? '',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: isVendor ? AppColors.white : AppColors.slate800)),
              const SizedBox(height: 4),
              Text(user?.email ?? '', style: TextStyle(fontSize: 14, color: isVendor ? AppColors.slate400 : AppColors.slate500)),
              const SizedBox(height: 16),
              StatusChip(
                label: isVendor ? 'Nhiếp ảnh gia' : 'Cặp đôi',
                color: isVendor ? AppColors.slate300 : AppColors.pink600,
                background: isVendor ? AppColors.slate800 : AppColors.pink50,
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              AppCard(
                padding: EdgeInsets.zero,
                child: Column(
                  children: [
                    for (final item in menu)
                      ListTile(
                        leading: Icon(item.$1, color: AppColors.slate400),
                        title: Text(item.$2, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: AppColors.slate700)),
                        trailing: const Icon(Icons.chevron_right, color: AppColors.slate300),
                        onTap: item.$3,
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              TextButton.icon(
                style: TextButton.styleFrom(
                  backgroundColor: AppColors.red50,
                  foregroundColor: AppColors.red600,
                  minimumSize: const Size.fromHeight(56),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
                onPressed: () => context.read<SessionController>().logout(), // router redirects to /login
                icon: const Icon(Icons.logout, size: 18),
                label: const Text('Đăng xuất', style: TextStyle(fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
