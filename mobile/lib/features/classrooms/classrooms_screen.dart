import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/eone_api.dart';
import '../../models/models.dart';
import 'classroom_detail_screen.dart';

final classroomsProvider = FutureProvider.autoDispose<List<Classroom>>(
  (ref) => ref.read(eoneApiProvider).classrooms(),
);

class ClassroomsScreen extends ConsumerWidget {
  const ClassroomsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(classroomsProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(classroomsProvider.future),
      child: async.when(
        loading: () => ListView(
          children: const [
            SizedBox(height: 300),
            Center(
              child: CircularProgressIndicator(),
            ),
          ],
        ),
        error: (e, _) => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text('Unable to load classrooms\n$e'),
          ],
        ),
        data: (items) => ListView(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 110),
          children: [
            Text(
              'Classrooms',
              style: Theme.of(context)
                  .textTheme
                  .headlineSmall
                  ?.copyWith(fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 12),
            if (items.isEmpty)
              const Card(
                child: Padding(
                  padding: EdgeInsets.all(18),
                  child: Text('No classroom access yet.'),
                ),
              )
            else
              ...items.map(
                (c) => Card(
                  child: ListTile(
                    contentPadding: const EdgeInsets.all(16),
                    leading: CircleAvatar(
                      child: Text(c.yearLevel.substring(0, 1)),
                    ),
                    title: Text(
                      c.name,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    subtitle: Text(
                      '${c.programName}\n'
                      '${c.termName} • Division ${c.division}',
                    ),
                    isThreeLine: true,
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => ClassroomDetailScreen(
                          classroom: c,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}
