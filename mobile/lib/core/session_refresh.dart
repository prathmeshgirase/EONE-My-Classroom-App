import 'package:dio/dio.dart';
import 'token_store.dart';

/// One rotation serves all requests that failed with the same access token.
class SessionRefresh {
  SessionRefresh(this.store, this.transport);
  final TokenStore store;
  final Dio transport;
  Future<String?>? _pending;

  Future<String?> refresh(String? failedAuthorization) async {
    final current = await store.accessToken();
    if (current == null) return null;
    if ('Bearer $current' != failedAuthorization) return current;
    if (_pending != null) return _pending!;
    final pending = _rotate();
    _pending = pending;
    try {
      return await pending;
    } finally {
      _pending = null;
    }
  }

  Future<String?> _rotate() async {
    final refreshToken = await store.refreshToken();
    if (refreshToken == null) return null;
    try {
      final response = await transport.post<Map<String, dynamic>>(
        'auth/refresh',
        data: {'refreshToken': refreshToken},
      );
      // A logout or another login must not be undone by an old response.
      if (await store.refreshToken() != refreshToken) return null;
      final data = response.data!;
      final access = data['accessToken'] as String;
      await store.save(access, data['refreshToken'] as String);
      return access;
    } on DioException catch (error) {
      if ((error.response?.statusCode == 401 ||
              error.response?.statusCode == 403) &&
          await store.refreshToken() == refreshToken) {
        await store.clear();
      }
      rethrow;
    }
  }
}
