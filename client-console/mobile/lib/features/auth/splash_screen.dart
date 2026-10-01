import 'dart:async';

import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import '../../core/router/app_router.dart';
import '../../core/theme/app_colors.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late final AnimationController _pulse = AnimationController(vsync: this, duration: const Duration(milliseconds: 900))
    ..repeat(reverse: true);
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // TODO: restore a saved session here before deciding the next route
    _timer = Timer(const Duration(seconds: 2), () {
      if (mounted) context.go(Routes.login);
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _pulse.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(gradient: AppColors.brandGradient),
        alignment: Alignment.center,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ScaleTransition(
              scale: Tween<double>(begin: 0.9, end: 1.05).animate(_pulse),
              child: const Icon(Icons.favorite, size: 64, color: AppColors.white),
            ),
            const SizedBox(height: 16),
            const Text('WedPlanner', style: TextStyle(fontSize: 36, fontWeight: FontWeight.bold, color: AppColors.white, letterSpacing: 1.5)),
            const SizedBox(height: 8),
            const Text('Hành trình hạnh phúc của bạn', style: TextStyle(color: AppColors.pink100, fontWeight: FontWeight.w500)),
          ],
        ),
      ),
    );
  }
}
