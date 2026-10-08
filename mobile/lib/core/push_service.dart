import 'dart:async';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'eone_api.dart';

final pushServiceProvider = Provider((ref) {
  final service = PushService(ref.read(eoneApiProvider));
  ref.onDispose(service.dispose);
  return service;
});

class PushService {
  PushService(this.api);
  final EoneApi api;
  StreamSubscription<String>? refresh;
  static const configured = bool.fromEnvironment('EONE_PUSH_ENABLED');
  Future<void> enable() async {
    if (!configured) throw StateError('Push is not configured in this build.');
    if (Firebase.apps.isEmpty) {
      await Firebase.initializeApp(
          options: const FirebaseOptions(
              apiKey: String.fromEnvironment('FIREBASE_API_KEY'),
              appId: String.fromEnvironment('FIREBASE_APP_ID'),
              messagingSenderId: String.fromEnvironment('FIREBASE_SENDER_ID'),
              projectId: String.fromEnvironment('FIREBASE_PROJECT_ID')));
    }
    final messaging = FirebaseMessaging.instance;
    final permission = await messaging.requestPermission();
    if (permission.authorizationStatus == AuthorizationStatus.denied) {
      throw StateError('Notification permission was denied.');
    }
    await messaging.setAutoInitEnabled(true);
    final token = await messaging.getToken();
    if (token == null) {
      throw StateError('No device token available. Try again.');
    }
    await api.registerDevice(token);
    await refresh?.cancel();
    refresh = messaging.onTokenRefresh.listen((token) async {
      try {
        await api.registerDevice(token);
      } catch (_) {/* Enable again to retry registration. */}
    });
  }

  Future<void> disable() async {
    await refresh?.cancel();
    refresh = null;
    if (Firebase.apps.isEmpty) return;
    final messaging = FirebaseMessaging.instance;
    await messaging.setAutoInitEnabled(false);
    final token = await messaging.getToken();
    try {
      if (token != null) await api.removeDevice(token);
    } finally {
      await messaging.deleteToken();
    }
  }

  void dispose() {
    refresh?.cancel();
  }
}
