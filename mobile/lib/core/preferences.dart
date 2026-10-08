import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

final themeModeProvider = StateNotifierProvider<ThemePreference, ThemeMode>(
    (ref) => ThemePreference());

class ThemePreference extends StateNotifier<ThemeMode> {
  ThemePreference() : super(ThemeMode.system) {
    restore();
  }
  final storage = const FlutterSecureStorage();
  bool changed = false;
  Future<void> restore() async {
    try {
      final value = await storage.read(key: 'eone.theme');
      if (mounted && !changed) {
        state = ThemeMode.values
            .firstWhere((v) => v.name == value, orElse: () => ThemeMode.system);
      }
    } catch (_) {
      /* Device preferences are optional; retain the system theme. */
    }
  }

  Future<void> select(ThemeMode mode) async {
    changed = true;
    state = mode;
    await storage.write(key: 'eone.theme', value: mode.name);
  }
}
