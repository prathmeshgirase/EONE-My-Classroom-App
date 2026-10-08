import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'session_refresh.dart';
import 'token_store.dart';

const apiBaseUrl = String.fromEnvironment(
  'EONE_API_URL',
  defaultValue: 'http://10.0.2.2:8081/api/v1',
);

final sessionExpiredProvider = StateProvider<bool>((ref) => false);

final dioProvider = Provider<Dio>((ref) {
  final store = ref.read(tokenStoreProvider);
  final uri = Uri.parse(apiBaseUrl);
  if (kReleaseMode && (uri.scheme != 'https' || uri.host.isEmpty)) {
    throw StateError('Release builds require an HTTPS EONE_API_URL.');
  }
  final base = apiBaseUrl.endsWith('/') ? apiBaseUrl : '$apiBaseUrl/';
  BaseOptions options() => BaseOptions(
        baseUrl: base,
        connectTimeout: const Duration(seconds: 15),
        receiveTimeout: const Duration(seconds: 20),
        contentType: Headers.jsonContentType,
      );
  final dio = Dio(options());
  final refreshTransport = Dio(options());
  final refresh = SessionRefresh(store, refreshTransport);
  ref.onDispose(() {
    dio.close();
    refreshTransport.close();
  });
  dio.interceptors.add(InterceptorsWrapper(
    onRequest: (options, handler) async {
      if (!options.path.startsWith('auth/')) {
        final token = await store.accessToken();
        if (token != null) options.headers['Authorization'] = 'Bearer $token';
      }
      handler.next(options);
    },
    onError: (error, handler) async {
      final request = error.requestOptions;
      if (error.response?.statusCode != 401 ||
          request.path.startsWith('auth/') ||
          request.extra['retried'] == true) {
        return handler.next(error);
      }
      try {
        final token =
            await refresh.refresh(request.headers['Authorization'] as String?);
        if (token != null) {
          request.extra['retried'] = true;
          request.headers['Authorization'] = 'Bearer $token';
          return handler.resolve(await dio.fetch(request));
        }
      } on DioException catch (refreshError) {
        if (await store.accessToken() == null) {
          ref.read(sessionExpiredProvider.notifier).state = true;
        }
        return handler.next(refreshError);
      } catch (_) {
        return handler.next(error);
      }
      ref.read(sessionExpiredProvider.notifier).state = true;
      handler.next(error);
    },
  ));
  return dio;
});

String apiError(Object error) {
  if (error is DioException) {
    final data = error.response?.data;
    if (data is Map && data['message'] != null) {
      return data['message'].toString();
    }
    if (error.response?.statusCode != null) {
      return 'Request failed. Please try again (${error.response!.statusCode}).';
    }
    return 'Cannot reach EONE. Check your connection and try again.';
  }
  return 'Something went wrong. Please try again.';
}
