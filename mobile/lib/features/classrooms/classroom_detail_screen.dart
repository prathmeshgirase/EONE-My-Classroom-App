import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';
import '../../models/models.dart';
import '../dashboard/providers.dart';
import '../subjects/subject_detail_screen.dart';
import '../attendance/attendance_overview_screen.dart';
import '../announcements/announcements_screen.dart';

class ClassroomDetailScreen extends ConsumerStatefulWidget {
  final Classroom classroom;
  const ClassroomDetailScreen({super.key, required this.classroom});
  @override
  ConsumerState<ClassroomDetailScreen> createState() =>
      _ClassroomDetailScreenState();
}

class _ClassroomDetailScreenState extends ConsumerState<ClassroomDetailScreen> {
  int version = 0;

  Future<(List<SubjectItem>, List<AnnouncementItem>)> _load() async {
    final api = ref.read(eoneApiProvider);
    return (
      await api.subjects(widget.classroom.id),
      ((await api.announcementPage(widget.classroom.id, 'CLASSROOM', 0, '',
              size: 5))['content'] as List)
          .map((item) =>
              AnnouncementItem.fromJson(Map<String, dynamic>.from(item as Map)))
          .toList()
    );
  }

  bool _canManage() {
    final d = ref.read(dashboardProvider).valueOrNull;
    if (d == null) return false;
    return d.contexts.any((c) =>
        (c.role == 'HOD' && c.departmentId == widget.classroom.departmentId) ||
        (c.role == 'CLASS_TEACHER' && c.classroomId == widget.classroom.id));
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(widget.classroom.name)),
        floatingActionButton: _canManage()
            ? FloatingActionButton.extended(
                onPressed: _createAnnouncement,
                icon: const Icon(Icons.campaign_outlined),
                label: const Text('Announce'),
              )
            : null,
        body: FutureBuilder<(List<SubjectItem>, List<AnnouncementItem>)>(
          key: ValueKey(version),
          future: _load(),
          builder: (context, snap) {
            if (!snap.hasData && !snap.hasError) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return Center(
                  child: Text('Could not load classroom\n${snap.error}'));
            }
            final subjects = snap.data!.$1;
            final announcements = snap.data!.$2;
            return ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
              children: [
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(18),
                    child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(widget.classroom.programName,
                              style: Theme.of(context)
                                  .textTheme
                                  .titleLarge
                                  ?.copyWith(fontWeight: FontWeight.w800)),
                          const SizedBox(height: 6),
                          Text(
                              '${widget.classroom.yearLevel} • Division ${widget.classroom.division} • ${widget.classroom.termName}'),
                        ]),
                  ),
                ),
                if (_canManage()) ...[
                  const SizedBox(height: 10),
                  OutlinedButton.icon(
                    onPressed: _showRoster,
                    icon: const Icon(Icons.groups_outlined),
                    label: const Text('View class roster'),
                  ),
                  OutlinedButton.icon(
                    onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => AttendanceOverviewScreen(
                                classroomId: widget.classroom.id,
                                classroomName: widget.classroom.name))),
                    icon: const Icon(Icons.analytics_outlined),
                    label: const Text('Attendance overview'),
                  ),
                ],
                const SizedBox(height: 18),
                Text('Announcements',
                    style: Theme.of(context)
                        .textTheme
                        .titleLarge
                        ?.copyWith(fontWeight: FontWeight.w800)),
                TextButton(
                    onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => AnnouncementsScreen(
                                scope: widget.classroom.id,
                                target: 'CLASSROOM'))),
                    child: const Text('Browse all announcements')),
                const SizedBox(height: 8),
                if (announcements.isEmpty)
                  const Card(
                      child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('No classroom announcements yet.')))
                else
                  ...announcements.take(5).map((a) => Card(
                        child: ListTile(
                          leading: const Icon(Icons.campaign_outlined),
                          title: Text(a.title,
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: Text(
                              '${a.body}\n${a.authorName} • ${DateFormat('dd MMM').format(a.createdAt.toLocal())}'),
                          isThreeLine: true,
                        ),
                      )),
                const SizedBox(height: 18),
                Text('Subjects',
                    style: Theme.of(context)
                        .textTheme
                        .titleLarge
                        ?.copyWith(fontWeight: FontWeight.w800)),
                const SizedBox(height: 10),
                if (subjects.isEmpty)
                  const Card(
                      child: Padding(
                          padding: EdgeInsets.all(18),
                          child: Text('No subjects have been linked yet.')))
                else
                  ...subjects.map((s) => Card(
                        child: ListTile(
                          contentPadding: const EdgeInsets.all(14),
                          leading:
                              CircleAvatar(child: Text(s.code.substring(0, 1))),
                          title: Text(s.name,
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: Text(s.code),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                  builder: (_) =>
                                      SubjectDetailScreen(subject: s))),
                        ),
                      )),
              ],
            );
          },
        ),
      );

  Future<void> _showRoster() async {
    try {
      final students =
          await ref.read(eoneApiProvider).students(widget.classroom.id);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (c) => AlertDialog(
          title: Text('Class roster (${students.length})'),
          content: SizedBox(
            width: 480,
            height: 400,
            child: students.isEmpty
                ? const Center(child: Text('No active students enrolled.'))
                : ListView(
                    children: students
                        .map((s) => ListTile(
                            leading: const Icon(Icons.person_outline),
                            title: Text(s.name),
                            subtitle: Text(s.email)))
                        .toList()),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(c), child: const Text('Close'))
          ],
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(apiError(e))));
      }
    }
  }

  Future<void> _createAnnouncement() async {
    final title = TextEditingController();
    final body = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Classroom announcement'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(
              controller: title,
              decoration: const InputDecoration(labelText: 'Title')),
          const SizedBox(height: 10),
          TextField(
              controller: body,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Message')),
        ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(c, false),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () => Navigator.pop(c, true),
              child: const Text('Publish')),
        ],
      ),
    );
    if (ok == true) {
      try {
        await ref.read(eoneApiProvider).createClassroomAnnouncement(
            widget.classroom.id, title.text, body.text);
        if (mounted) setState(() => version++);
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(apiError(e))));
        }
      }
    }
  }
}
