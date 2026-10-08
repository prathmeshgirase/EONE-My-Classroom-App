import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final tokenStoreProvider = Provider<TokenStore>((ref) => TokenStore());

class TokenStore {
  static const _access = 'eone_access_token';
  static const _refresh = 'eone_refresh_token';
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<String?> accessToken() => _storage.read(key: _access);
  Future<String?> refreshToken() => _storage.read(key: _refresh);
  Future<void> save(String access, String refresh) async {
    await _storage.write(key: _access, value: access);
    await _storage.write(key: _refresh, value: refresh);
  }

  Future<void> clear() async {
    await _storage.delete(key: _access);
    await _storage.delete(key: _refresh);
  }
}
