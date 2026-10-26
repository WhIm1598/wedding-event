import 'dart:async';
import 'dart:io' show Platform;

import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';

import '../config/env.dart';
import '../network/api_client.dart';

/// FN-NOTI-02: Firebase Cloud Messaging. Only active with `--dart-define=FCM_ENABLED=true` against the real API (needs
/// `google-services.json` / `GoogleService-Info.plist`, see docs/setup/google-cloud-firebase.md). Never throws: without
/// Firebase the app simply gets no pushes.
class PushService {
  PushService(this._api);

  final ApiClient _api;

  /// Bumped on a `plan_updated` data message (FN-PLAN-05): the plan screen reloads
  final ValueNotifier<int> planUpdates = ValueNotifier(0);

  /// A notification arrived while the app is open (the OS only shows it when the app is in the background)
  void Function(String title, String body)? onForegroundNotification;

  /// The user tapped a notification that opened the app
  void Function()? onOpenNotifications;

  bool _initialized = false;
  String? _token;

  bool get enabled => Env.fcmEnabled && !Env.useMock;

  /// After sign-in / session restore: asks for permission and registers this device
  Future<void> start() async {
    if (!enabled) return;
    try {
      final messaging = await _init();
      final settings = await messaging.requestPermission();
      if (settings.authorizationStatus == AuthorizationStatus.denied) return;
      // shortcut: registration tokens are what FCM HTTP v1 `message.token` documents; firebase_messaging 17 deprecates
      // them for FID registration (register()/onRegistered) — switch both sides once the backend targets FIDs
      // ignore: deprecated_member_use
      final token = await messaging.getToken();
      if (token != null) await _register(token);
    } catch (e) {
      debugPrint('FCM unavailable: $e');
    }
  }

  /// Before logout, while the access token still works: this device stops getting the user's pushes
  Future<void> stop() async {
    final token = _token;
    _token = null;
    if (!enabled || token == null) return;
    try {
      await _api.delete('/users/me/devices', query: {'token': token}, parse: (_) {});
    } catch (e) {
      debugPrint('FCM unregister failed: $e');
    }
  }

  Future<FirebaseMessaging> _init() async {
    final messaging = FirebaseMessaging.instance;
    if (_initialized) return messaging;
    await Firebase.initializeApp();
    _initialized = true;
    FirebaseMessaging.onMessage.listen(_onMessage);
    FirebaseMessaging.onMessageOpenedApp.listen((_) => onOpenNotifications?.call());
    // ignore: deprecated_member_use
    messaging.onTokenRefresh.listen((token) {
      if (_api.refreshToken != null) _register(token).catchError((Object e) => debugPrint('FCM re-register failed: $e'));
    });
    if (await messaging.getInitialMessage() != null) onOpenNotifications?.call();
    return messaging;
  }

  /// PUT /users/me/devices (FN-NOTI-02)
  Future<void> _register(String token) async {
    _token = token;
    if (kDebugMode) debugPrint('FCM token: $token'); // for "Send test message" in the Firebase console
    await _api.put('/users/me/devices', body: {'token': token, 'platform': Platform.isIOS ? 'IOS' : 'ANDROID'}, parse: (_) {});
  }

  void _onMessage(RemoteMessage m) {
    if (m.data['type'] == 'plan_updated') {
      planUpdates.value++;
    } else if (m.notification != null) {
      onForegroundNotification?.call(m.notification!.title ?? '', m.notification!.body ?? '');
    }
  }
}
