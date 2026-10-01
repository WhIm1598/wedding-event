import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';
import '../../state/session_controller.dart';

class _NavItem {
  const _NavItem(this.path, this.icon, this.label);

  final String path;
  final IconData icon;
  final String label;
}

const _coupleNav = [
  _NavItem(Routes.home, Icons.home_outlined, 'Trang chủ'),
  _NavItem(Routes.search, Icons.search, 'Khám phá'),
  _NavItem(Routes.plan, Icons.list_alt, 'Kế hoạch'),
  _NavItem(Routes.profile, Icons.person_outline, 'Cá nhân'),
];

const _vendorNav = [
  _NavItem(Routes.home, Icons.home_outlined, 'Tổng quan'),
  _NavItem(Routes.vendorProjects, Icons.work_outline, 'Dự án'),
  _NavItem(Routes.vendorMessages, Icons.chat_bubble_outline, 'Tin nhắn'),
  _NavItem(Routes.profile, Icons.person_outline, 'Cá nhân'),
];

/// Bottom navigation scaffold; tabs depend on the user's role.
class MainShell extends StatelessWidget {
  const MainShell({super.key, required this.location, required this.child});

  final String location;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final session = context.watch<SessionController>();
    final items = session.isVendor ? _vendorNav : _coupleNav;
    final index = items.indexWhere((i) => i.path == location);

    void onTap(_NavItem item) {
      // Couple without a plan: "Kế hoạch" opens the create-plan flow
      if (item.path == Routes.plan && !session.hasPlan) {
        context.push(Routes.createPlan);
      } else {
        context.go(item.path);
      }
    }

    return Scaffold(
      body: child,
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: AppColors.white,
          border: Border(top: BorderSide(color: AppColors.slate100)),
          boxShadow: [BoxShadow(color: AppColors.shadow, blurRadius: 20, offset: Offset(0, -4))],
        ),
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                for (var i = 0; i < items.length; i++) _NavButton(item: items[i], active: i == index, onTap: () => onTap(items[i])),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NavButton extends StatelessWidget {
  const _NavButton({required this.item, required this.active, required this.onTap});

  final _NavItem item;
  final bool active;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final color = active ? AppColors.pink600 : AppColors.slate400;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: SizedBox(
        width: 72,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(6),
              decoration: BoxDecoration(color: active ? AppColors.pink50 : null, borderRadius: BorderRadius.circular(12)),
              child: Icon(item.icon, color: color, size: 24),
            ),
            const SizedBox(height: 4),
            Text(
              item.label,
              style: TextStyle(fontSize: 10, fontWeight: FontWeight.w600, color: active ? AppColors.pink600 : AppColors.slate500),
            ),
          ],
        ),
      ),
    );
  }
}
