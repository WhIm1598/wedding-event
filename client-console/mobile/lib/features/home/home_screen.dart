import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../../state/session_controller.dart';
import 'couple_home.dart';
import 'vendor_home.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final session = context.watch<SessionController>();
    // Keyed by hasPlan so the couple home reloads its plan after creation
    return session.isVendor ? const VendorHome() : CoupleHome(key: ValueKey(session.hasPlan));
  }
}
