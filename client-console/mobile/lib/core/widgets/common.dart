import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../theme/app_colors.dart';

/// White rounded card with a soft border (Tailwind: bg-white rounded-2xl shadow-sm border-slate-100)
class AppCard extends StatelessWidget {
  const AppCard({super.key, required this.child, this.padding = const EdgeInsets.all(20), this.onTap, this.borderColor});

  final Widget child;
  final EdgeInsetsGeometry padding;
  final VoidCallback? onTap;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.white,
      borderRadius: BorderRadius.circular(16),
      child: InkWell(
        borderRadius: BorderRadius.circular(16),
        onTap: onTap,
        child: Ink(
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: borderColor ?? AppColors.slate100),
            boxShadow: const [BoxShadow(color: AppColors.shadow, blurRadius: 6, offset: Offset(0, 2))],
          ),
          child: Padding(padding: padding, child: child),
        ),
      ),
    );
  }
}

/// Sticky white header with a back button, used by full-screen pages.
class ScreenHeader extends StatelessWidget {
  const ScreenHeader({super.key, required this.title, this.subtitle, this.actions = const [], this.onBack, this.bottom});

  final String title;
  final Widget? subtitle;
  final List<Widget> actions;
  final VoidCallback? onBack;
  final Widget? bottom;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      padding: EdgeInsets.fromLTRB(12, MediaQuery.paddingOf(context).top + 8, 16, bottom == null ? 12 : 0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back, color: AppColors.slate500),
                onPressed: onBack ?? () => context.canPop() ? context.pop() : context.go('/home'),
              ),
              const SizedBox(width: 4),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.slate800)),
                    if (subtitle != null) subtitle!,
                  ],
                ),
              ),
              ...actions,
            ],
          ),
          if (bottom != null) bottom!,
        ],
      ),
    );
  }
}

/// Large title header for bottom-nav tab pages.
class TabHeader extends StatelessWidget {
  const TabHeader({super.key, required this.title, this.trailing, this.bottom});

  final String title;
  final Widget? trailing;
  final Widget? bottom;

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.white,
      padding: EdgeInsets.fromLTRB(24, MediaQuery.paddingOf(context).top + 16, 24, 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(child: Text(title, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: AppColors.slate800))),
              if (trailing != null) trailing!,
            ],
          ),
          if (bottom != null) ...[const SizedBox(height: 16), bottom!],
        ],
      ),
    );
  }
}

/// Round colored icon container (Tailwind: w-12 h-12 rounded-full bg-x-50 text-x-500)
class IconBubble extends StatelessWidget {
  const IconBubble({super.key, required this.icon, required this.color, required this.background, this.size = 48, this.radius});

  final IconData icon;
  final Color color;
  final Color background;
  final double size;
  final double? radius;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(radius ?? size / 2),
      ),
      child: Icon(icon, color: color, size: size * 0.45),
    );
  }
}

/// Small rounded status label
class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.label, required this.color, required this.background});

  final String label;
  final Color color;
  final Color background;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(8)),
      child: Text(label.toUpperCase(), style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold)),
    );
  }
}

/// Primary pink button with loading state
class PrimaryButton extends StatelessWidget {
  const PrimaryButton({super.key, required this.label, required this.onPressed, this.loading = false, this.icon, this.color});

  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final IconData? icon;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      style: color == null ? null : ElevatedButton.styleFrom(backgroundColor: color),
      onPressed: loading ? null : onPressed,
      child: loading
          ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2.5, color: AppColors.white))
          : Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                if (icon != null) ...[Icon(icon, size: 20), const SizedBox(width: 8)],
                Text(label),
              ],
            ),
    );
  }
}

/// Bottom action bar pinned above the safe area
class BottomActionBar extends StatelessWidget {
  const BottomActionBar({super.key, required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: EdgeInsets.fromLTRB(16, 12, 16, 12 + MediaQuery.paddingOf(context).bottom),
      decoration: const BoxDecoration(
        color: AppColors.white,
        border: Border(top: BorderSide(color: AppColors.slate100)),
        boxShadow: [BoxShadow(color: AppColors.shadow, blurRadius: 20, offset: Offset(0, -10))],
      ),
      child: child,
    );
  }
}

class FieldLabel extends StatelessWidget {
  const FieldLabel(this.text, {super.key});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(text, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.slate700)),
    );
  }
}

/// FutureBuilder with standard loading / error states
class AsyncView<T> extends StatelessWidget {
  const AsyncView({super.key, required this.future, required this.builder, this.onRetry});

  final Future<T> future;
  final Widget Function(BuildContext context, T data) builder;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<T>(
      future: future,
      builder: (context, snapshot) {
        if (snapshot.hasError) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.error_outline, color: AppColors.red500, size: 40),
                  const SizedBox(height: 8),
                  Text(snapshot.error.toString(), textAlign: TextAlign.center, style: const TextStyle(color: AppColors.slate500)),
                  if (onRetry != null) TextButton(onPressed: onRetry, child: const Text('Thử lại')),
                ],
              ),
            ),
          );
        }
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: Padding(padding: EdgeInsets.all(24), child: CircularProgressIndicator()));
        }
        return builder(context, snapshot.data as T);
      },
    );
  }
}

void showAppSnack(BuildContext context, String message, {bool error = false}) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message), backgroundColor: error ? AppColors.red600 : AppColors.slate800));
}
