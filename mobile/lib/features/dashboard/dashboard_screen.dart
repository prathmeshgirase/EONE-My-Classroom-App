import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../models/models.dart';
import '../assignments/assignment_detail_screen.dart';
import '../classrooms/classroom_detail_screen.dart';
import 'providers.dart';

class DashboardScreen extends ConsumerWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final async = ref.watch(dashboardProvider);
    final selected = ref.watch(activeClassroomProvider);

    return RefreshIndicator(
      onRefresh: () => ref.refresh(dashboardProvider.future),
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
          padding: const EdgeInsets.all(24),
          children: [
            Text('Could not load dashboard\n$e'),
            const SizedBox(height: 12),
            FilledButton(
              onPressed: () => ref.invalidate(dashboardProvider),
              child: const Text('Retry'),
            ),
          ],
        ),
        data: (data) {
          final active = data.classrooms.any((c) => c.id == selected)
              ? selected
              : (data.classrooms.isEmpty ? null : data.classrooms.first.id);

          return _Dashboard(
            data: data,
            activeClassroomId: active,
            onClassroomChanged: (id) async {
              try {
                await ref.read(activeClassroomProvider.notifier).choose(id);
              } catch (_) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
                      content: Text(
                          'Classroom changed, but could not save the preference.')));
                }
              }
            },
          );
        },
      ),
    );
  }
}

class _Dashboard extends StatelessWidget {
  final DashboardData data;
  final String? activeClassroomId;
  final ValueChanged<String?> onClassroomChanged;

  const _Dashboard({
    required this.data,
    required this.activeClassroomId,
    required this.onClassroomChanged,
  });

  @override
  Widget build(BuildContext context) {
    final roles = data.contexts.map((e) => e.role).toSet().toList();

    final activeClassroom = activeClassroomId == null
        ? null
        : data.classrooms.where((c) => c.id == activeClassroomId).firstOrNull;

    final activeAssignments = activeClassroomId == null
        ? <AssignmentItem>[]
        : data.upcomingAssignments
            .where((a) => a.classroomId == activeClassroomId)
            .toList();

    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 110),
      children: [
        Text(
          'Hi, ${data.user.firstName}',
          style: Theme.of(context)
              .textTheme
              .headlineMedium
              ?.copyWith(fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 4),
        Text(
          data.user.departmentName ?? 'EONE',
          style: Theme.of(context).textTheme.bodyLarge,
        ),
        const SizedBox(height: 16),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: roles
              .map(
                (role) => Chip(
                  avatar: Icon(
                    _roleIcon(role),
                    size: 18,
                  ),
                  label: Text(
                    role.replaceAll('_', ' '),
                  ),
                ),
              )
              .toList(),
        ),
        const SizedBox(height: 18),
        if (data.classrooms.isNotEmpty)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Active classroom',
                    style: TextStyle(
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<String>(
                    initialValue: activeClassroomId,
                    items: data.classrooms
                        .map(
                          (c) => DropdownMenuItem<String>(
                            value: c.id,
                            child: Text(c.name),
                          ),
                        )
                        .toList(),
                    onChanged: onClassroomChanged,
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'Classroom-specific assignments and activity stay '
                    'isolated when you switch FY/SY/TY or streams.',
                  ),
                ],
              ),
            ),
          ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: _Metric(
                icon: Icons.meeting_room_outlined,
                value: '${data.classrooms.length}',
                label: 'Classrooms',
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: _Metric(
                icon: Icons.notifications_none,
                value: '${data.unreadNotifications}',
                label: 'Unread',
              ),
            ),
          ],
        ),
        const SizedBox(height: 26),
        _Title(
          activeClassroom == null ? 'Current classroom' : activeClassroom.name,
        ),
        const SizedBox(height: 10),
        if (activeClassroom == null)
          const _Empty(
            'No classroom is assigned yet.',
          )
        else
          Card(
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 16,
                vertical: 8,
              ),
              leading: CircleAvatar(
                child: Text(
                  activeClassroom.yearLevel.substring(0, 1),
                ),
              ),
              title: Text(
                activeClassroom.name,
                style: const TextStyle(
                  fontWeight: FontWeight.w700,
                ),
              ),
              subtitle: Text(
                '${activeClassroom.programCode} • '
                '${activeClassroom.termName}',
              ),
              trailing: const Icon(
                Icons.chevron_right,
              ),
              onTap: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => ClassroomDetailScreen(
                    classroom: activeClassroom,
                  ),
                ),
              ),
            ),
          ),
        const SizedBox(height: 22),
        const _Title(
          'Upcoming assignments',
        ),
        const SizedBox(height: 10),
        if (activeAssignments.isEmpty)
          const _Empty(
            'Nothing due in this classroom right now.',
          )
        else
          ...activeAssignments.take(6).map(
                (assignment) => Card(
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 8,
                    ),
                    title: Text(
                      assignment.title,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    subtitle: Text(
                      '${assignment.subjectName} • due '
                      '${DateFormat('dd MMM, h:mm a').format(
                        assignment.dueAt.toLocal(),
                      )}',
                    ),
                    trailing: assignment.mySubmission == null
                        ? const Icon(
                            Icons.chevron_right,
                          )
                        : const Icon(
                            Icons.check_circle_outline,
                          ),
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => AssignmentDetailScreen(
                          assignmentId: assignment.id,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
      ],
    );
  }

  IconData _roleIcon(String role) {
    return switch (role) {
      'HOD' => Icons.admin_panel_settings_outlined,
      'CLASS_TEACHER' => Icons.groups_outlined,
      'SUBJECT_TEACHER' => Icons.menu_book_outlined,
      _ => Icons.school_outlined,
    };
  }
}

class _Metric extends StatelessWidget {
  final IconData icon;
  final String value;
  final String label;

  const _Metric({
    required this.icon,
    required this.value,
    required this.label,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon),
            const SizedBox(height: 12),
            Text(
              value,
              style: Theme.of(context)
                  .textTheme
                  .headlineMedium
                  ?.copyWith(fontWeight: FontWeight.w800),
            ),
            Text(label),
          ],
        ),
      ),
    );
  }
}

class _Title extends StatelessWidget {
  final String text;

  const _Title(this.text);

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: Theme.of(context)
          .textTheme
          .titleLarge
          ?.copyWith(fontWeight: FontWeight.w800),
    );
  }
}

class _Empty extends StatelessWidget {
  final String text;

  const _Empty(this.text);

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Text(text),
      ),
    );
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
