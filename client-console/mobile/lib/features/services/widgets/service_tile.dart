import 'package:flutter/material.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/service_item.dart';

/// Visual style per catalog category
({IconData icon, Color color, Color bg}) categoryStyle(ServiceCategory c) => switch (c) {
      ServiceCategory.restaurant => (icon: Icons.restaurant, color: AppColors.orange500, bg: AppColors.orange50),
      ServiceCategory.photo => (icon: Icons.photo_camera_outlined, color: AppColors.blue500, bg: AppColors.blue50),
      ServiceCategory.dress => (icon: Icons.checkroom, color: AppColors.purple500, bg: AppColors.purple50),
      ServiceCategory.decor => (icon: Icons.auto_awesome, color: AppColors.amber500, bg: AppColors.amber50),
    };

class ServiceTile extends StatelessWidget {
  const ServiceTile({super.key, required this.service, required this.onTap});

  final ServiceItem service;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final style = categoryStyle(service.category);
    return AppCard(
      padding: const EdgeInsets.all(16),
      onTap: onTap,
      child: Row(
        children: [
          IconBubble(icon: style.icon, color: style.color, background: style.bg, radius: 12),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(service.name, style: const TextStyle(fontWeight: FontWeight.w600, color: AppColors.slate800)),
                const SizedBox(height: 2),
                Row(
                  children: [
                    Flexible(
                      child: Text('${service.provider} • ', overflow: TextOverflow.ellipsis, style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                    ),
                    const Icon(Icons.star, size: 12, color: AppColors.amber400),
                    Text(' ${service.rating}', style: const TextStyle(fontSize: 12, color: AppColors.slate500)),
                  ],
                ),
              ],
            ),
          ),
          const Icon(Icons.chevron_right, color: AppColors.slate400),
        ],
      ),
    );
  }
}
