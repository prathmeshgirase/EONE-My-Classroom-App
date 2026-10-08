import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../core/token_store.dart';
import '../../models/models.dart';
import '../dashboard/providers.dart';
import '../classrooms/classrooms_screen.dart';
import '../notifications/notifications_screen.dart';

class SessionState {
  final bool loading;
  final bool restoreFailed;
  final AppUser? user;
  final String? error;
  const SessionState(
      {this.loading = false,
      this.restoreFailed = false,
      this.user,
      this.error});
}

final sessionProvider =
    StateNotifierProvider<SessionController, SessionState>((ref) {
  final controller = SessionController(ref);
  ref.listen<bool>(sessionExpiredProvider, (_, expired) {
    if (expired) controller.expire();
  });
  controller.restore();
  return controller;
});

class SessionController extends StateNotifier<SessionState> {
  final Ref ref;
  SessionController(this.ref) : super(const SessionState(loading: true));

  Future<void> restore() async {
    state = const SessionState(loading: true);
    try {
      final token = await ref.read(tokenStoreProvider).accessToken();
      if (token == null) {
        state = const SessionState();
        return;
      }
      final user = await ref.read(eoneApiProvider).me();
      if (mounted) state = SessionState(user: user);
    } catch (error) {
      if (!mounted) return;
      if (error is DioException &&
          (error.response?.statusCode == 401 ||
              error.response?.statusCode == 403)) {
        await ref.read(tokenStoreProvider).clear();
        expire();
      } else {
        state = SessionState(restoreFailed: true, error: apiError(error));
      }
    }
  }

  Future<bool> login(String email, String password) async {
    try {
      final data = await ref.read(eoneApiProvider).login(email, password);
      await ref
          .read(tokenStoreProvider)
          .save(data['accessToken'] as String, data['refreshToken'] as String);
      _clearPrivateState();
      ref.read(sessionExpiredProvider.notifier).state = false;
      state = SessionState(
          user:
              AppUser.fromJson(Map<String, dynamic>.from(data['user'] as Map)));
      return true;
    } catch (error) {
      state = SessionState(error: apiError(error));
      return false;
    }
  }

  Future<void> refreshUser() async {
    final user = await ref.read(eoneApiProvider).me();
    state = SessionState(user: user);
  }

  void _clearPrivateState() {
    ref.invalidate(dashboardProvider);
    ref.invalidate(classroomsProvider);
    ref.invalidate(notificationsProvider);
    ref.invalidate(activeClassroomProvider);
  }

  void expire() {
    _clearPrivateState();
    state = const SessionState();
  }

  Future<void> logout() async {
    final store = ref.read(tokenStoreProvider);
    final token = await store.refreshToken();
    await store.clear();
    expire();
    if (token != null) {
      try {
        await ref.read(eoneApiProvider).logout(token);
      } catch (_) {/* Local sign-out remains effective offline. */}
    }
  }
}
