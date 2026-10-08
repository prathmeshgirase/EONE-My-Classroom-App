import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../core/paged_records.dart';
import '../../models/models.dart';

// Kept for existing session invalidation and dashboard consumers.
final notificationsProvider =
    FutureProvider.autoDispose<List<NotificationItem>>(
  (ref) => ref.read(eoneApiProvider).notifications(),
);

class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});
  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsState();
}

class _NotificationsState extends ConsumerState<NotificationsScreen> {
  bool unread = false, busy = false;
  int version = 0;
  Future<void> mark(Future<void> Function() action, VoidCallback reload) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await action();
      ref.invalidate(notificationsProvider);
      if (mounted) reload();
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
  Widget build(BuildContext context) => Column(children: [
        Padding(
            padding: const EdgeInsets.all(16),
            child: Wrap(
                spacing: 16,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text('Notifications',
                      style: Theme.of(context).textTheme.headlineSmall),
                  FilterChip(
                      label: const Text('Unread only'),
                      selected: unread,
                      onSelected: (v) => setState(() {
                            unread = v;
                            version++;
                          })),
                  TextButton(
                      onPressed: busy
                          ? null
                          : () => mark(
                              () => ref.read(eoneApiProvider).markAllRead(),
                              () => setState(() => version++)),
                      child: const Text('Mark all read')),
                ])),
        Expanded(
            child: PagedRecords(
                key: ValueKey(version),
                load: (page, _) => ref
                    .read(eoneApiProvider)
                    .notificationsPage(page, unread: unread),
                itemBuilder: (item, reload) => Card(
                        child: ListTile(
                      leading: Icon(item['readAt'] == null
                          ? Icons.notifications_active_outlined
                          : Icons.notifications_none),
                      title: Text(item['title'] as String),
                      subtitle: Text(
                          '${item['body']}\n${DateFormat('dd MMM, h:mm a').format(DateTime.parse(item['createdAt'] as String).toLocal())}'),
                      onTap: busy
                          ? null
                          : () => mark(
                              () => ref
                                  .read(eoneApiProvider)
                                  .markRead(item['id'] as String),
                              reload),
                    )))),
      ]);
}
