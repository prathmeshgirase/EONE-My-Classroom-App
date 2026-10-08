import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';

final myAttendanceProvider = FutureProvider.autoDispose(
    (ref) => ref.read(eoneApiProvider).myAttendanceSummary());

class MyAttendanceScreen extends ConsumerWidget {
  const MyAttendanceScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
      appBar: AppBar(title: const Text('My attendance')),
      body: ref.watch(myAttendanceProvider).when(
            loading: () => const Center(child: CircularProgressIndicator()),
            error: (e, _) => Center(
                child: Column(mainAxisSize: MainAxisSize.min, children: [
              Text(apiError(e)),
              TextButton(
                  onPressed: () => ref.invalidate(myAttendanceProvider),
                  child: const Text('Retry'))
            ])),
            data: (rows) => RefreshIndicator(
                onRefresh: () => ref.refresh(myAttendanceProvider.future),
                child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    padding: const EdgeInsets.all(24),
                    children: [
                      const Text(
                          'Present and late count as attended. Excused and unmarked sessions do not affect the percentage.'),
                      const SizedBox(height: 16),
                      if (rows.isEmpty) const Text('No active subjects.'),
                      for (final row in rows)
                        Card(
                            child: ListTile(
                                title: Text(row['subject'] as String),
                                subtitle: Text(
                                    'Present: ${row['present']} · Absent: ${row['absent']}\nLate: ${row['late']} · Excused: ${row['excused']}'),
                                trailing: Text(row['percentage'] == null
                                    ? '—'
                                    : '${(row['percentage'] as num).toStringAsFixed(1)}%'))),
                    ])),
          ));
}
