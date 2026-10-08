import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/paged_records.dart';
import 'package:intl/intl.dart';

class AuditScreen extends ConsumerWidget {
  const AuditScreen({super.key, required this.department});
  final String department;
  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
      appBar: AppBar(title: const Text('Department audit history')),
      body: PagedRecords(
          searchLabel: 'Action (for example APPROVE_USER)',
          load: (page, search) =>
              ref.read(eoneApiProvider).auditPage(department, page, search),
          itemBuilder: (item, reload) => Card(
              child: ListTile(
                  title: Text((item['action'] as String).replaceAll('_', ' ')),
                  subtitle: Text(
                      '${item['details'] ?? ''}\n${DateFormat('dd MMM yyyy, h:mm a').format(DateTime.parse(item['createdAt'] as String).toLocal())}\nActor: ${item['actor'] ?? 'System'}')))));
}
