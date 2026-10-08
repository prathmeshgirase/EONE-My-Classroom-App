import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/session_controller.dart';
import '../dashboard/providers.dart';
import '../../core/preferences.dart';
import '../auth/password_screen.dart';
import '../../core/push_service.dart';
import '../../core/api_client.dart';
import '../attendance/my_attendance_screen.dart';
import '../announcements/announcements_screen.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(sessionProvider).user!;
    final d = ref.watch(dashboardProvider);
    return ListView(
        padding: const EdgeInsets.fromLTRB(18, 18, 18, 110),
        children: [
          Center(
              child: CircleAvatar(
                  radius: 42,
                  child: Text(user.firstName.substring(0, 1),
                      style: const TextStyle(
                          fontSize: 30, fontWeight: FontWeight.w800)))),
          const SizedBox(height: 12),
          Center(
              child: Text(user.name,
                  style: Theme.of(context)
                      .textTheme
                      .headlineSmall
                      ?.copyWith(fontWeight: FontWeight.w800))),
          Center(child: Text(user.email)),
          const SizedBox(height: 22),
          if (user.departmentId != null)
            ListTile(
                leading: const Icon(Icons.campaign_outlined),
                title: const Text('Department announcements'),
                onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => AnnouncementsScreen(
                            scope: user.departmentId!, target: 'DEPARTMENT')))),
          ListTile(
              leading: const Icon(Icons.lock_outline),
              title: const Text('Change password'),
              onTap: () async {
                final changed = await Navigator.push<bool>(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const PasswordScreen(change: true)));
                if (changed == true) {
                  await ref.read(sessionProvider.notifier).logout();
                }
              }),
          if (user.accountType == 'STUDENT')
            ListTile(
                leading: const Icon(Icons.fact_check_outlined),
                title: const Text('My attendance'),
                trailing: const Icon(Icons.chevron_right),
                onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const MyAttendanceScreen()))),
          Card(
              child: Padding(
                  padding: const EdgeInsets.all(18),
                  child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Account',
                            style: Theme.of(context)
                                .textTheme
                                .titleMedium
                                ?.copyWith(fontWeight: FontWeight.w800)),
                        const SizedBox(height: 10),
                        Text('Type: ${user.accountType}'),
                        Text('Department: ${user.departmentName ?? '-'}'),
                        Text('Status: ${user.status}')
                      ]))),
          const SizedBox(height: 12),
          d.when(
              data: (data) => Card(
                  child: Padding(
                      padding: const EdgeInsets.all(18),
                      child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Responsibilities',
                                style: Theme.of(context)
                                    .textTheme
                                    .titleMedium
                                    ?.copyWith(fontWeight: FontWeight.w800)),
                            const SizedBox(height: 8),
                            ...data.contexts.map((c) => Padding(
                                padding:
                                    const EdgeInsets.symmetric(vertical: 4),
                                child: Row(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Icon(Icons.check_circle_outline,
                                          size: 18),
                                      const SizedBox(width: 8),
                                      Expanded(child: Text(c.label))
                                    ])))
                          ]))),
              loading: () => const Card(
                  child: Padding(
                      padding: EdgeInsets.all(18),
                      child: LinearProgressIndicator())),
              error: (e, _) => Card(
                  child: Padding(
                      padding: const EdgeInsets.all(18), child: Text('$e')))),
          const SizedBox(height: 18),
          if (PushService.configured)
            OutlinedButton.icon(
                icon: const Icon(Icons.notifications_active_outlined),
                label: const Text('Enable push notifications'),
                onPressed: () async {
                  try {
                    await ref.read(pushServiceProvider).enable();
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                          content: Text('Push notifications enabled.')));
                    }
                  } catch (e) {
                    if (context.mounted) {
                      ScaffoldMessenger.of(context)
                          .showSnackBar(SnackBar(content: Text(apiError(e))));
                    }
                  }
                }),
          DropdownButtonFormField<ThemeMode>(
              key: ValueKey(ref.watch(themeModeProvider)),
              initialValue: ref.watch(themeModeProvider),
              decoration: const InputDecoration(labelText: 'Appearance'),
              items: ThemeMode.values
                  .map((mode) =>
                      DropdownMenuItem(value: mode, child: Text(mode.name)))
                  .toList(),
              onChanged: (mode) async {
                if (mode == null) return;
                try {
                  await ref.read(themeModeProvider.notifier).select(mode);
                } catch (_) {
                  if (context.mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                        content: Text(
                            'Theme changed, but could not save the preference.')));
                  }
                }
              }),
          const SizedBox(height: 18),
          OutlinedButton.icon(
              onPressed: () async {
                try {
                  await ref.read(pushServiceProvider).disable();
                } catch (_) {
                  /* Always allow sign out. Push payloads contain no private data. */
                }
                await ref.read(sessionProvider.notifier).logout();
              },
              icon: const Icon(Icons.logout),
              label: const Text('Sign out'))
        ]);
  }
}
