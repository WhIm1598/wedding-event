import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:provider/provider.dart';

import 'core/network/api_client.dart';
import 'core/router/app_router.dart';
import 'core/theme/app_theme.dart';
import 'data/repositories/auth_repository.dart';
import 'data/repositories/guest_repository.dart';
import 'data/repositories/notification_repository.dart';
import 'data/repositories/plan_repository.dart';
import 'data/repositories/service_repository.dart';
import 'data/repositories/vendor_repository.dart';
import 'state/session_controller.dart';

class WedPlannerApp extends StatefulWidget {
  const WedPlannerApp({super.key});

  @override
  State<WedPlannerApp> createState() => _WedPlannerAppState();
}

class _WedPlannerAppState extends State<WedPlannerApp> {
  final ApiClient _api = ApiClient();
  late final SessionController _session = SessionController(AuthRepository(_api));
  late final GoRouter _router = createRouter(_session);

  @override
  void dispose() {
    _router.dispose();
    _session.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        Provider<ApiClient>.value(value: _api),
        ChangeNotifierProvider<SessionController>.value(value: _session),
        Provider<PlanRepository>(create: (_) => PlanRepository(_api)),
        Provider<GuestRepository>(create: (_) => GuestRepository(_api)),
        Provider<ServiceRepository>(create: (_) => ServiceRepository(_api)),
        Provider<VendorRepository>(create: (_) => VendorRepository(_api)),
        Provider<NotificationRepository>(create: (_) => NotificationRepository(_api)),
      ],
      child: MaterialApp.router(
        title: 'WedPlanner',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.light,
        routerConfig: _router,
      ),
    );
  }
}
