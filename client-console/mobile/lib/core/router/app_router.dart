import 'package:go_router/go_router.dart';

import '../../features/ai_chat/ai_chat_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/otp_screen.dart';
import '../../features/auth/role_select_screen.dart';
import '../../features/auth/splash_screen.dart';
import '../../features/guests/guest_list_screen.dart';
import '../../features/home/home_screen.dart';
import '../../features/notifications/notifications_screen.dart';
import '../../features/plan/create_plan_screen.dart';
import '../../features/plan/plan_management_screen.dart';
import '../../features/profile/account_settings_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/services/search_screen.dart';
import '../../features/services/service_detail_screen.dart';
import '../../features/shell/main_shell.dart';
import '../../features/sync/share_plan_screen.dart';
import '../../features/vendor/service_management_screen.dart';
import '../../features/vendor/vendor_messages_screen.dart';
import '../../features/vendor/vendor_projects_screen.dart';
import '../../state/session_controller.dart';

/// Route paths (mirrors `currentScreen` ids from the template)
class Routes {
  Routes._();

  static const splash = '/splash';
  static const login = '/login';
  static const otp = '/otp';
  static const roleSelect = '/role';

  // Bottom-nav tabs
  static const home = '/home';
  static const search = '/search';
  static const plan = '/plan';
  static const profile = '/profile';
  static const vendorProjects = '/vendor/projects';
  static const vendorMessages = '/vendor/messages';

  // Full-screen pages
  static const createPlan = '/plan/create';
  static const aiChat = '/ai-chat';
  static const guests = '/guests';
  static const sharePlan = '/sync';
  static const notifications = '/notifications';
  static const serviceDetail = '/services/:id';
  static const accountSettings = '/profile/settings';
  static const serviceManagement = '/vendor/services';

  static String serviceDetailFor(String id) => '/services/$id';
}

const _authFlow = {Routes.splash, Routes.login, Routes.otp};

GoRouter createRouter(SessionController session) {
  return GoRouter(
    initialLocation: Routes.splash,
    refreshListenable: session,
    redirect: (context, state) {
      final loc = state.matchedLocation;
      if (!session.isLoggedIn) return _authFlow.contains(loc) ? null : Routes.login;
      if (session.needsRole) return loc == Routes.roleSelect ? null : Routes.roleSelect;
      if (_authFlow.contains(loc) || loc == Routes.roleSelect) return Routes.home;
      return null;
    },
    routes: [
      GoRoute(path: Routes.splash, builder: (_, __) => const SplashScreen()),
      GoRoute(path: Routes.login, builder: (_, __) => const LoginScreen()),
      GoRoute(path: Routes.otp, builder: (_, __) => const OtpScreen()),
      GoRoute(path: Routes.roleSelect, builder: (_, __) => const RoleSelectScreen()),
      ShellRoute(
        builder: (context, state, child) => MainShell(location: state.matchedLocation, child: child),
        routes: [
          GoRoute(path: Routes.home, builder: (_, __) => const HomeScreen()),
          GoRoute(path: Routes.search, builder: (_, __) => const SearchScreen()),
          GoRoute(path: Routes.plan, builder: (_, __) => const PlanManagementScreen()),
          GoRoute(path: Routes.profile, builder: (_, __) => const ProfileScreen()),
          GoRoute(path: Routes.vendorProjects, builder: (_, __) => const VendorProjectsScreen()),
          GoRoute(path: Routes.vendorMessages, builder: (_, __) => const VendorMessagesScreen()),
        ],
      ),
      GoRoute(path: Routes.createPlan, builder: (_, __) => const CreatePlanScreen()),
      GoRoute(path: Routes.aiChat, builder: (_, __) => const AiChatScreen()),
      GoRoute(path: Routes.guests, builder: (_, __) => const GuestListScreen()),
      GoRoute(path: Routes.sharePlan, builder: (_, __) => const SharePlanScreen()),
      GoRoute(path: Routes.notifications, builder: (_, __) => const NotificationsScreen()),
      GoRoute(path: Routes.serviceDetail, builder: (_, state) => ServiceDetailScreen(serviceId: state.pathParameters['id']!)),
      GoRoute(path: Routes.accountSettings, builder: (_, __) => const AccountSettingsScreen()),
      GoRoute(path: Routes.serviceManagement, builder: (_, __) => const ServiceManagementScreen()),
    ],
  );
}
