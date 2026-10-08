import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';

class PasswordScreen extends ConsumerStatefulWidget {
  const PasswordScreen({super.key, this.change = false});
  final bool change;
  @override
  ConsumerState<PasswordScreen> createState() => _PasswordState();
}

class _PasswordState extends ConsumerState<PasswordScreen> {
  final email = TextEditingController(),
      code = TextEditingController(),
      current = TextEditingController(),
      password = TextEditingController(),
      confirm = TextEditingController();
  bool busy = false;
  String? message;
  @override
  void dispose() {
    for (final c in [email, code, current, password, confirm]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> run(bool request) async {
    if (busy) return;
    if (!request &&
        (password.text.length < 12 || password.text != confirm.text)) {
      setState(
          () => message = 'Use at least 12 characters and matching passwords.');
      return;
    }
    setState(() {
      busy = true;
      message = null;
    });
    try {
      final api = ref.read(eoneApiProvider);
      if (request) {
        await api.requestPassword(email.text.trim());
        if (mounted) {
          setState(() => message =
              'If the account is eligible, recovery instructions will arrive by email.');
        }
      } else {
        if (widget.change) {
          await api.changePassword(current.text, password.text);
        } else {
          await api.resetPassword(code.text.trim(), password.text);
        }
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
              content:
                  Text('Password changed. Sign in with your new password.')));
          Navigator.pop(context, true);
        }
      }
    } catch (e) {
      if (mounted) setState(() => message = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
      appBar: AppBar(
          title: Text(widget.change ? 'Change password' : 'Password recovery')),
      body: Center(
          child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 560),
              child: ListView(padding: const EdgeInsets.all(24), children: [
                if (!widget.change) ...[
                  TextField(
                      controller: email,
                      enabled: !busy,
                      keyboardType: TextInputType.emailAddress,
                      decoration:
                          const InputDecoration(labelText: 'Account email')),
                  TextButton(
                      onPressed: busy ? null : () => run(true),
                      child: const Text('Email a recovery code')),
                  const SizedBox(height: 16),
                  TextField(
                      controller: code,
                      enabled: !busy,
                      autocorrect: false,
                      decoration: const InputDecoration(
                          labelText: 'Recovery code from email')),
                ] else
                  TextField(
                      controller: current,
                      enabled: !busy,
                      obscureText: true,
                      decoration:
                          const InputDecoration(labelText: 'Current password')),
                const SizedBox(height: 16),
                TextField(
                    controller: password,
                    enabled: !busy,
                    obscureText: true,
                    autofillHints: const [AutofillHints.newPassword],
                    decoration: const InputDecoration(
                        labelText: 'New password',
                        helperText:
                            'At least 12 characters; at most 72 UTF-8 bytes')),
                const SizedBox(height: 16),
                TextField(
                    controller: confirm,
                    enabled: !busy,
                    obscureText: true,
                    decoration: const InputDecoration(
                        labelText: 'Confirm new password')),
                const SizedBox(height: 16),
                if (message != null) Text(message!),
                FilledButton(
                    onPressed: busy ? null : () => run(false),
                    child: Text(busy ? 'Please wait…' : 'Save new password')),
              ]))));
}
