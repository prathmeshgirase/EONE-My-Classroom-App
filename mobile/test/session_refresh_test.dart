import 'dart:async';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eone_mobile/core/session_refresh.dart';
import 'package:eone_mobile/core/token_store.dart';

class MemoryTokenStore extends TokenStore {
  String? access = 'old-access';
  String? refresh = 'old-refresh';
  @override
  Future<String?> accessToken() async => access;
  @override
  Future<String?> refreshToken() async => refresh;
  @override
  Future<void> save(String a, String r) async {
    access = a;
    refresh = r;
  }

  @override
  Future<void> clear() async {
    access = null;
    refresh = null;
  }
}

void main() {
  test('concurrent 401 responses rotate once and reuse the new token',
      () async {
    final store = MemoryTokenStore();
    final dio = Dio();
    final gate = Completer<void>();
    var calls = 0;
    dio.interceptors
        .add(InterceptorsWrapper(onRequest: (options, handler) async {
      calls++;
      await gate.future;
      handler.resolve(Response(requestOptions: options, data: {
        'accessToken': 'new-access',
        'refreshToken': 'new-refresh',
      }));
    }));
    final service = SessionRefresh(store, dio);
    final first = service.refresh('Bearer old-access');
    final second = service.refresh('Bearer old-access');
    gate.complete();
    expect(await Future.wait([first, second]), ['new-access', 'new-access']);
    expect(calls, 1);
    expect(await service.refresh('Bearer old-access'), 'new-access');
    expect(calls, 1);
  });

  test('network failure preserves the session for retry', () async {
    final store = MemoryTokenStore();
    final dio = Dio();
    dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      handler.reject(DioException(
          requestOptions: options, type: DioExceptionType.connectionTimeout));
    }));
    await expectLater(SessionRefresh(store, dio).refresh('Bearer old-access'),
        throwsA(isA<DioException>()));
    expect(store.refresh, 'old-refresh');
    expect(store.access, 'old-access');
  });

  test('invalid refresh token clears the session', () async {
    final store = MemoryTokenStore();
    final dio = Dio();
    dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      handler.reject(DioException(
          requestOptions: options,
          response: Response(requestOptions: options, statusCode: 401)));
    }));
    await expectLater(SessionRefresh(store, dio).refresh('Bearer old-access'),
        throwsA(isA<DioException>()));
    expect(store.refresh, isNull);
    expect(store.access, isNull);
  });

  test('logout during refresh cannot restore the old session', () async {
    final store = MemoryTokenStore();
    final dio = Dio();
    dio.interceptors
        .add(InterceptorsWrapper(onRequest: (options, handler) async {
      await store.clear();
      handler.resolve(Response(requestOptions: options, data: {
        'accessToken': 'new-access',
        'refreshToken': 'new-refresh',
      }));
    }));
    expect(
        await SessionRefresh(store, dio).refresh('Bearer old-access'), isNull);
    expect(store.access, isNull);
  });
}
