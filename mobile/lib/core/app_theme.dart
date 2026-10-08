import 'package:flutter/material.dart';

ThemeData buildEoneTheme({Brightness brightness = Brightness.light}) {
  const seed = Color(0xFF3157D5);
  final scheme = ColorScheme.fromSeed(seedColor: seed, brightness: brightness);
  return ThemeData(
    colorScheme: scheme,
    useMaterial3: true,
    scaffoldBackgroundColor: scheme.surface,
    appBarTheme:
        const AppBarTheme(centerTitle: false, scrolledUnderElevation: 0),
    inputDecorationTheme: InputDecorationTheme(
      filled: true,
      fillColor: scheme.surfaceContainerHighest,
      border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(14), borderSide: BorderSide.none),
    ),
  );
}
