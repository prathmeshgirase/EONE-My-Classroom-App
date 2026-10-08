import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});
  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final first = TextEditingController(),
      last = TextEditingController(),
      email = TextEditingController(),
      password = TextEditingController(),
      department = TextEditingController(text: 'CSE');
  String type = 'STUDENT';
  bool busy = false;
  @override
  void dispose() {
    for (final c in [first, last, email, password, department]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> submit() async {
    setState(() => busy = true);
    try {
      await ref.read(eoneApiProvider).register(
          email: email.text.trim(),
          password: password.text,
          firstName: first.text.trim(),
          lastName: last.text.trim(),
          accountType: type,
          departmentCode: department.text.trim());
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
            content: Text(
                'Registration submitted. Your department HOD must approve the account.')));
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(apiError(e))));
      }
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
      appBar: AppBar(title: const Text('Create account')),
      body: ListView(padding: const EdgeInsets.all(20), children: [
        Row(children: [
          Expanded(
              child: TextField(
                  controller: first,
                  decoration: const InputDecoration(labelText: 'First name'))),
          const SizedBox(width: 12),
          Expanded(
              child: TextField(
                  controller: last,
                  decoration: const InputDecoration(labelText: 'Last name')))
        ]),
        const SizedBox(height: 12),
        TextField(
            controller: email,
            decoration: const InputDecoration(labelText: 'Email')),
        const SizedBox(height: 12),
        TextField(
            controller: password,
            obscureText: true,
            decoration:
                const InputDecoration(labelText: 'Password (8+ characters)')),
        const SizedBox(height: 12),
        TextField(
            controller: department,
            textCapitalization: TextCapitalization.characters,
            decoration: const InputDecoration(labelText: 'Department code')),
        const SizedBox(height: 12),
        SegmentedButton<String>(segments: const [
          ButtonSegment(
              value: 'STUDENT',
              label: Text('Student'),
              icon: Icon(Icons.person_outline)),
          ButtonSegment(
              value: 'STAFF',
              label: Text('Staff'),
              icon: Icon(Icons.badge_outlined))
        ], selected: {
          type
        }, onSelectionChanged: (s) => setState(() => type = s.first)),
        const SizedBox(height: 20),
        FilledButton(
            onPressed: busy ? null : submit,
            child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 14),
                child: Text(busy ? 'Submitting...' : 'Register'))),
        const SizedBox(height: 12),
        const Text(
            'New accounts start as PENDING. This prevents users from entering a department/classroom until the HOD approves them.'),
      ]));
}
