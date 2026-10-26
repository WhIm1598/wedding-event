import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/utils/formatters.dart';
import '../../../core/widgets/common.dart';
import '../../../data/models/vendor.dart';
import '../../../data/repositories/vendor_repository.dart';

/// One Vendor Project (FN-VND-04) with the caller's actions: the vendor moves it on, cancels it or decides on the
/// couple's cancellation request; the couple asks to cancel. Calls [onChanged] after a successful action.
class ProjectCard extends StatelessWidget {
  const ProjectCard({super.key, required this.project, required this.asVendor, required this.onChanged});

  final VendorProject project;
  final bool asVendor;
  final VoidCallback onChanged;

  static ({Color color, Color bg}) _statusStyle(ProjectStatus s) => switch (s) {
        ProjectStatus.upcoming => (color: AppColors.blue600, bg: AppColors.blue50),
        ProjectStatus.inProgress => (color: AppColors.amber600, bg: AppColors.amber50),
        ProjectStatus.completed => (color: AppColors.emerald600, bg: AppColors.emerald50),
        ProjectStatus.cancelled => (color: AppColors.slate500, bg: AppColors.slate100),
      };

  static String _stepLabel(ProjectStatus s) => s == ProjectStatus.inProgress ? 'Bắt đầu' : 'Hoàn thành';

  Future<void> _run(BuildContext context, Future<void> Function(VendorRepository repo) action) async {
    try {
      await action(context.read<VendorRepository>());
      onChanged();
    } catch (e) {
      if (context.mounted) showAppSnack(context, e.toString(), error: true);
    }
  }

  Future<bool> _confirm(BuildContext context, String title, String message) async =>
      await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(title),
          content: Text(message),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Không')),
            TextButton(onPressed: () => Navigator.pop(ctx, true), child: const Text('Đồng ý')),
          ],
        ),
      ) ??
      false;

  Future<void> _askToCancel(BuildContext context) async {
    final reason = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Xin hủy dự án'),
        content: TextField(
          controller: reason,
          maxLength: 500,
          autofocus: true,
          decoration: const InputDecoration(labelText: 'Lý do (bắt buộc)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false), child: const Text('Không')),
          TextButton(onPressed: () => Navigator.pop(ctx, reason.text.trim().isNotEmpty), child: const Text('Gửi yêu cầu')),
        ],
      ),
    );
    final text = reason.text.trim();
    reason.dispose();
    if (ok == true && context.mounted) {
      await _run(context, (repo) => repo.requestProjectCancel(project, text));
      if (context.mounted) showAppSnack(context, 'Đã gửi yêu cầu hủy, chờ ${project.vendorName} duyệt');
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = project;
    final style = _statusStyle(p.status);
    final pendingCancel = p.isOpen && p.cancelRequestedAt != null;
    return AppCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(asVendor ? p.coupleName : p.vendorName,
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                    const SizedBox(height: 4),
                    Text(p.packageName, style: const TextStyle(fontSize: 14, color: AppColors.slate500)),
                  ],
                ),
              ),
              StatusChip(label: p.status.label, color: style.color, background: style.bg),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _Pill(icon: Icons.calendar_today, text: Fmt.date(p.eventDate)),
              _Pill(icon: Icons.payments_outlined, text: Fmt.vnd(p.agreedPrice)),
              if (p.venue != null) _Pill(icon: Icons.location_on_outlined, text: p.venue!),
            ],
          ),
          if (pendingCancel) ...[
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(color: AppColors.red50, borderRadius: BorderRadius.circular(12)),
              child: Text(
                asVendor ? 'Cặp đôi xin hủy: ${p.cancelReason ?? ''}' : 'Đang chờ ${p.vendorName} duyệt yêu cầu hủy',
                style: const TextStyle(fontSize: 13, color: AppColors.red600),
              ),
            ),
          ],
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              if (asVendor && pendingCancel) ...[
                TextButton(
                  onPressed: () => _run(context, (repo) => repo.decideProjectCancel(p, approve: false)),
                  child: const Text('Từ chối hủy'),
                ),
                FilledButton(
                  onPressed: () => _run(context, (repo) => repo.decideProjectCancel(p, approve: true)),
                  child: const Text('Đồng ý hủy'),
                ),
              ] else if (asVendor && p.isOpen) ...[
                TextButton(
                  style: TextButton.styleFrom(foregroundColor: AppColors.red600),
                  onPressed: () async {
                    if (await _confirm(context, 'Hủy dự án?', 'Cặp đôi ${p.coupleName} sẽ nhận được thông báo.') && context.mounted) {
                      await _run(context, (repo) => repo.updateProjectStatus(p, ProjectStatus.cancelled));
                    }
                  },
                  child: const Text('Hủy'),
                ),
                for (final next in p.nextSteps)
                  TextButton(
                    onPressed: () => _run(context, (repo) => repo.updateProjectStatus(p, next)),
                    child: Text(_stepLabel(next)),
                  ),
              ],
              if (!asVendor && p.isOpen && !pendingCancel)
                TextButton(
                  style: TextButton.styleFrom(foregroundColor: AppColors.red600),
                  onPressed: () => _askToCancel(context),
                  child: const Text('Xin hủy'),
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: AppColors.slate50, borderRadius: BorderRadius.circular(8)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: AppColors.slate400),
          const SizedBox(width: 8),
          Flexible(child: Text(text, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: AppColors.slate600))),
        ],
      ),
    );
  }
}
