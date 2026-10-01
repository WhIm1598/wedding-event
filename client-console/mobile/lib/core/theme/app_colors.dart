import 'package:flutter/material.dart';

/// Tailwind-equivalent palette from the design system (feature spec §8).
class AppColors {
  AppColors._();

  // Brand
  static const Color pink50 = Color(0xFFFDF2F8);
  static const Color pink100 = Color(0xFFFCE7F3);
  static const Color pink200 = Color(0xFFFBCFE8);
  static const Color pink500 = Color(0xFFEC4899);
  static const Color pink600 = Color(0xFFDB2777);
  static const Color pink700 = Color(0xFFBE185D);
  static const Color rose400 = Color(0xFFFB7185);
  static const Color rose500 = Color(0xFFF43F5E);
  static const Color purple50 = Color(0xFFFAF5FF);
  static const Color purple500 = Color(0xFFA855F7);
  static const Color purple600 = Color(0xFF9333EA);

  // Neutrals
  static const Color white = Color(0xFFFFFFFF);
  static const Color slate50 = Color(0xFFF8FAFC);
  static const Color slate100 = Color(0xFFF1F5F9);
  static const Color slate200 = Color(0xFFE2E8F0);
  static const Color slate300 = Color(0xFFCBD5E1);
  static const Color slate400 = Color(0xFF94A3B8);
  static const Color slate500 = Color(0xFF64748B);
  static const Color slate600 = Color(0xFF475569);
  static const Color slate700 = Color(0xFF334155);
  static const Color slate800 = Color(0xFF1E293B);
  static const Color slate900 = Color(0xFF0F172A);

  // Status / accents
  static const Color emerald50 = Color(0xFFECFDF5);
  static const Color emerald400 = Color(0xFF34D399);
  static const Color emerald500 = Color(0xFF10B981);
  static const Color emerald600 = Color(0xFF059669);
  static const Color teal500 = Color(0xFF14B8A6);
  static const Color amber50 = Color(0xFFFFFBEB);
  static const Color amber400 = Color(0xFFFBBF24);
  static const Color amber500 = Color(0xFFF59E0B);
  static const Color amber600 = Color(0xFFD97706);
  static const Color orange50 = Color(0xFFFFF7ED);
  static const Color orange500 = Color(0xFFF97316);
  static const Color orange600 = Color(0xFFEA580C);
  static const Color red50 = Color(0xFFFEF2F2);
  static const Color red500 = Color(0xFFEF4444);
  static const Color red600 = Color(0xFFDC2626);
  static const Color blue50 = Color(0xFFEFF6FF);
  static const Color blue500 = Color(0xFF3B82F6);
  static const Color blue600 = Color(0xFF2563EB);

  // Translucent helpers (avoid runtime alpha computation)
  static const Color white20 = Color(0x33FFFFFF);
  static const Color white80 = Color(0xCCFFFFFF);
  static const Color shadow = Color(0x0D000000);

  static const LinearGradient brandGradient = LinearGradient(
    colors: [pink500, rose400],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient successGradient = LinearGradient(colors: [emerald400, teal500]);
  static const LinearGradient aiGradient = LinearGradient(colors: [pink500, purple600]);
}
