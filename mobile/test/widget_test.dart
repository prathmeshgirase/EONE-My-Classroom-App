import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eone_mobile/core/app_theme.dart';
import 'package:eone_mobile/features/auth/login_screen.dart';

void main() {
  testWidgets('login validates empty credentials without making a request',
      (tester) async {
    await tester.pumpWidget(ProviderScope(
        child:
            MaterialApp(theme: buildEoneTheme(), home: const LoginScreen())));
    await tester.tap(find.text('Sign in'));
    await tester.pump();
    expect(find.text('Enter a valid email address'), findsOneWidget);
    expect(find.text('Enter your password'), findsOneWidget);
    expect(find.textContaining('Eone@123'), findsNothing);
  });

  testWidgets(
      'login fits a small phone with large text and password visibility is accessible',
      (tester) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(ProviderScope(
        child: MaterialApp(
      theme: buildEoneTheme(),
      builder: (context, child) => MediaQuery(
          data: MediaQuery.of(context)
              .copyWith(textScaler: const TextScaler.linear(2)),
          child: child!),
      home: const LoginScreen(),
    )));
    expect(tester.takeException(), isNull);
    await tester.ensureVisible(find.byTooltip('Show password'));
    await tester.tap(find.byTooltip('Show password'));
    await tester.pump();
    expect(find.byTooltip('Hide password'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
