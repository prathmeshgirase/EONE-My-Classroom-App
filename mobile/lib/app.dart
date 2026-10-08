import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'core/app_theme.dart';
import 'core/preferences.dart';
import 'features/auth/login_screen.dart';
import 'features/auth/session_controller.dart';
import 'features/shell/home_shell.dart';

class EoneApp extends ConsumerWidget {
  const EoneApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(sessionProvider);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'EONE',
      theme: buildEoneTheme(),
      darkTheme: buildEoneTheme(brightness: Brightness.dark),
      themeMode: ref.watch(themeModeProvider),
      home: session.loading
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : session.restoreFailed
              ? Scaffold(
                  body: SafeArea(
                      child: Center(
                          child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(mainAxisSize: MainAxisSize.min, children: [
                    const Icon(Icons.cloud_off_outlined, size: 48),
                    const SizedBox(height: 16),
                    Text(session.error ?? 'Cannot connect to EONE.'),
                    const SizedBox(height: 16),
                    FilledButton(
                        onPressed: () =>
                            ref.read(sessionProvider.notifier).restore(),
                        child: const Text('Retry')),
                    TextButton(
                        onPressed: () =>
                            ref.read(sessionProvider.notifier).logout(),
                        child: const Text('Sign out')),
                  ]),
                ))))
              : session.user == null
                  ? const LoginScreen()
                  : const HomeShell(),
    );
  }
}
