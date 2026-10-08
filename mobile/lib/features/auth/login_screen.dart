import 'package:flutter/material.dart';
import 'password_screen.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'register_screen.dart';
import 'session_controller.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});
  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final form = GlobalKey<FormState>();
  final email = TextEditingController();
  final password = TextEditingController();
  bool busy = false;
  bool obscure = true;

  @override
  void dispose() {
    email.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (busy || !form.currentState!.validate()) return;
    setState(() => busy = true);
    final ok = await ref
        .read(sessionProvider.notifier)
        .login(email.text.trim(), password.text);
    if (!mounted) return;
    setState(() => busy = false);
    if (!ok) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(ref.read(sessionProvider).error ??
            'Sign-in failed. Please try again.'),
      ));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: SafeArea(
            child: Center(
                child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: AutofillGroup(
                child: Form(
                    key: form,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Align(
                            alignment: Alignment.centerLeft,
                            child: Container(
                              padding: const EdgeInsets.all(18),
                              decoration: BoxDecoration(
                                  color: Theme.of(context)
                                      .colorScheme
                                      .primaryContainer,
                                  borderRadius: BorderRadius.circular(20)),
                              child: Icon(Icons.school_rounded,
                                  size: 36,
                                  color: Theme.of(context)
                                      .colorScheme
                                      .onPrimaryContainer),
                            )),
                        const SizedBox(height: 24),
                        Text('Welcome to EONE',
                            style: Theme.of(context)
                                .textTheme
                                .headlineMedium
                                ?.copyWith(fontWeight: FontWeight.w800)),
                        const SizedBox(height: 8),
                        Text(
                            'Your classes, coursework and academic updates in one place.',
                            style: Theme.of(context).textTheme.bodyLarge),
                        const SizedBox(height: 32),
                        TextFormField(
                            controller: email,
                            enabled: !busy,
                            keyboardType: TextInputType.emailAddress,
                            autofillHints: const [AutofillHints.username],
                            textInputAction: TextInputAction.next,
                            autocorrect: false,
                            validator: (value) => value != null &&
                                    RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$')
                                        .hasMatch(value.trim())
                                ? null
                                : 'Enter a valid email address',
                            decoration: const InputDecoration(
                                labelText: 'Email',
                                prefixIcon: Icon(Icons.alternate_email))),
                        const SizedBox(height: 16),
                        TextFormField(
                            controller: password,
                            enabled: !busy,
                            obscureText: obscure,
                            autofillHints: const [AutofillHints.password],
                            onFieldSubmitted: (_) => submit(),
                            validator: (value) => value == null || value.isEmpty
                                ? 'Enter your password'
                                : null,
                            decoration: InputDecoration(
                                labelText: 'Password',
                                prefixIcon: const Icon(Icons.lock_outline),
                                suffixIcon: IconButton(
                                    tooltip: obscure
                                        ? 'Show password'
                                        : 'Hide password',
                                    onPressed: () =>
                                        setState(() => obscure = !obscure),
                                    icon: Icon(obscure
                                        ? Icons.visibility_outlined
                                        : Icons.visibility_off_outlined)))),
                        const SizedBox(height: 24),
                        FilledButton(
                            onPressed: busy ? null : submit,
                            child: Padding(
                                padding:
                                    const EdgeInsets.symmetric(vertical: 14),
                                child: busy
                                    ? const SizedBox(
                                        width: 20,
                                        height: 20,
                                        child: CircularProgressIndicator(
                                            strokeWidth: 2))
                                    : const Text('Sign in'))),
                        const SizedBox(height: 12),
                        TextButton(
                            onPressed: busy
                                ? null
                                : () => Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (_) =>
                                            const PasswordScreen())),
                            child: const Text('Forgot password?')),
                        OutlinedButton(
                            onPressed: busy
                                ? null
                                : () => Navigator.push(
                                    context,
                                    MaterialPageRoute(
                                        builder: (_) =>
                                            const RegisterScreen())),
                            child: const Text('Create an account')),
                        if (const bool.fromEnvironment(
                            'EONE_SHOW_DEMO_ACCOUNTS')) ...[
                          const SizedBox(height: 24),
                          const Text(
                              'Local demo: student@eone.dev, teacher@eone.dev or hod@eone.dev. Password: Eone@123'),
                        ],
                      ],
                    ))),
          ),
        ))),
      );
}
