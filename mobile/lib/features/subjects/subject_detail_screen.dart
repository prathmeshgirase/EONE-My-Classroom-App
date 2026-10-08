import '../assignments/assignment_editor.dart';
import '../files/file_widgets.dart';
import '../files/material_upload_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';
import '../../models/models.dart';
import '../assignments/assignment_detail_screen.dart';
import '../dashboard/providers.dart';
import '../announcements/announcements_screen.dart';

class SubjectDetailScreen extends ConsumerStatefulWidget {
  final SubjectItem subject;
  const SubjectDetailScreen({super.key, required this.subject});
  @override
  ConsumerState<SubjectDetailScreen> createState() => _SubjectDetailState();
}

class _SubjectDetailState extends ConsumerState<SubjectDetailScreen> {
  int version = 0;

  Future<(List<ChapterItem>, List<AssignmentItem>, List<AttendanceSessionItem>)>
      _load() async {
    final api = ref.read(eoneApiProvider);
    return (
      await api.chapters(widget.subject.classSubjectId),
      await api.subjectAssignments(widget.subject.classSubjectId),
      await api.attendanceSessions(widget.subject.classSubjectId),
    );
  }

  bool _canTeach() {
    final data = ref.watch(dashboardProvider).valueOrNull;
    return data?.contexts.any((c) =>
            c.role == 'SUBJECT_TEACHER' &&
            c.classSubjectId == widget.subject.classSubjectId) ??
        false;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: Text(widget.subject.name)),
        floatingActionButton: _canTeach()
            ? FloatingActionButton.extended(
                onPressed: _actions,
                icon: const Icon(Icons.add),
                label: const Text('Create'))
            : null,
        body: FutureBuilder<
            (
              List<ChapterItem>,
              List<AssignmentItem>,
              List<AttendanceSessionItem>
            )>(
          key: ValueKey(version),
          future: _load(),
          builder: (context, snap) {
            if (!snap.hasData && !snap.hasError) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return Center(
                  child: Text('Could not load subject\n${snap.error}'));
            }
            final chapters = snap.data!.$1;
            final assignments = snap.data!.$2;
            final attendance = snap.data!.$3;
            return ListView(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 110),
              children: [
                Text(widget.subject.code,
                    style: Theme.of(context).textTheme.labelLarge),
                OutlinedButton.icon(
                    onPressed: () => Navigator.push(
                        context,
                        MaterialPageRoute(
                            builder: (_) => AnnouncementsScreen(
                                scope: widget.subject.classSubjectId,
                                target: 'SUBJECT'))),
                    icon: const Icon(Icons.campaign_outlined),
                    label: const Text('Subject announcements')),
                Text(widget.subject.name,
                    style: Theme.of(context)
                        .textTheme
                        .headlineSmall
                        ?.copyWith(fontWeight: FontWeight.w800)),
                const SizedBox(height: 22),
                _sectionTitle('Chapters & materials'),
                const SizedBox(height: 8),
                if (chapters.isEmpty)
                  const Card(
                      child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('No learning material yet.')))
                else
                  ...chapters.map((c) => Card(
                        child: ExpansionTile(
                          title: Text('${c.position}. ${c.title}',
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700)),
                          children: c.materials.isEmpty
                              ? [
                                  const ListTile(
                                      title:
                                          Text('No materials in this chapter.'))
                                ]
                              : c.materials
                                  .map((m) => ListTile(
                                        leading: Icon(_materialIcon(m.type)),
                                        title: Text(m.title),
                                        subtitle: Text(m.type),
                                        trailing: const Icon(Icons.open_in_new),
                                        onTap: () => _showUrl(m.url),
                                      ))
                                  .toList(),
                        ),
                      )),
                const SizedBox(height: 22),
                _sectionTitle('Assignments'),
                const SizedBox(height: 8),
                if (assignments.isEmpty)
                  const Card(
                      child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('No assignments yet.')))
                else
                  ...assignments.map((a) => Card(
                        child: ListTile(
                          title: Text(a.title,
                              style:
                                  const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: Text(
                              'Due ${DateFormat('dd MMM yyyy, h:mm a').format(a.dueAt.toLocal())} • ${a.maxMarks.toStringAsFixed(0)} marks'),
                          trailing: const Icon(Icons.chevron_right),
                          onTap: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                  builder: (_) => AssignmentDetailScreen(
                                      assignmentId: a.id))),
                        ),
                      )),
                const SizedBox(height: 22),
                _sectionTitle('Attendance'),
                const SizedBox(height: 8),
                if (attendance.isEmpty)
                  const Card(
                      child: Padding(
                          padding: EdgeInsets.all(16),
                          child: Text('No attendance sessions yet.')))
                else
                  ...attendance.take(12).map((s) {
                    final summary = s.records.isEmpty
                        ? 'No records visible'
                        : s.records.length == 1
                            ? s.records.first.status
                            : '${s.records.where((r) => r.status == 'PRESENT').length}/${s.records.length} present';
                    return Card(
                      child: ListTile(
                        leading: const Icon(Icons.fact_check_outlined),
                        title: Text(s.topic?.isNotEmpty == true
                            ? s.topic!
                            : 'Class session'),
                        subtitle: Text(
                            '${DateFormat('dd MMM yyyy').format(s.sessionDate)} • $summary'),
                      ),
                    );
                  }),
              ],
            );
          },
        ),
      );

  Widget _sectionTitle(String text) => Text(text,
      style: Theme.of(context)
          .textTheme
          .titleLarge
          ?.copyWith(fontWeight: FontWeight.w800));

  IconData _materialIcon(String t) => switch (t) {
        'VIDEO' => Icons.play_circle_outline,
        'FILE' => Icons.attach_file,
        'LINK' => Icons.link,
        _ => Icons.description_outlined,
      };

  void _showUrl(String url) => showDialog(
        context: context,
        builder: (_) => AlertDialog(
          title: const Text('Material URL'),
          content: AttachmentLink(url: url),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(context),
                child: const Text('Close'))
          ],
        ),
      );

  Future<void> _actions() async {
    final choice = await showModalBottomSheet<String>(
      context: context,
      builder: (c) => SafeArea(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          ListTile(
              leading: const Icon(Icons.menu_book),
              title: const Text('Add chapter'),
              onTap: () => Navigator.pop(c, 'chapter')),
          ListTile(
              leading: const Icon(Icons.attach_file),
              title: const Text('Add material link'),
              onTap: () => Navigator.pop(c, 'material')),
          ListTile(
              leading: const Icon(Icons.upload_file),
              title: const Text('Upload material file'),
              onTap: () => Navigator.pop(c, 'upload')),
          ListTile(
              leading: const Icon(Icons.assignment_outlined),
              title: const Text('Create assignment'),
              onTap: () => Navigator.pop(c, 'assignment')),
          ListTile(
              leading: const Icon(Icons.fact_check_outlined),
              title: const Text('Take attendance'),
              onTap: () => Navigator.pop(c, 'attendance')),
        ]),
      ),
    );
    if (choice == 'chapter') await _createChapter();
    if (choice == 'material') await _createMaterial();
    if (choice == 'upload') await _uploadMaterial();
    if (choice == 'assignment') await _createAssignment();
    if (choice == 'attendance') await _takeAttendance();
  }

  Future<void> _createChapter() async {
    final title = TextEditingController();
    final pos = TextEditingController(text: '1');
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('Add chapter'),
        content: Column(mainAxisSize: MainAxisSize.min, children: [
          TextField(
              controller: title,
              decoration: const InputDecoration(labelText: 'Title')),
          const SizedBox(height: 10),
          TextField(
              controller: pos,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Position')),
        ]),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(c, false),
              child: const Text('Cancel')),
          FilledButton(
              onPressed: () => Navigator.pop(c, true),
              child: const Text('Create')),
        ],
      ),
    );
    if (ok == true) {
      await _run(() => ref.read(eoneApiProvider).createChapter(
          widget.subject.classSubjectId,
          title.text,
          int.tryParse(pos.text) ?? 1));
    }
  }

  Future<void> _uploadMaterial() async {
    try {
      final chapters = await ref
          .read(eoneApiProvider)
          .chapters(widget.subject.classSubjectId);
      if (!mounted) return;
      if (chapters.isEmpty) {
        _msg('Create a chapter first.');
        return;
      }
      final saved = await Navigator.push<bool>(
          context,
          MaterialPageRoute(
              builder: (_) => MaterialUploadScreen(chapters: chapters)));
      if (saved == true && mounted) setState(() => version++);
    } catch (e) {
      _msg(apiError(e));
    }
  }

  Future<void> _createMaterial() async {
    final api = ref.read(eoneApiProvider);
    final chapters = await api.chapters(widget.subject.classSubjectId);
    if (chapters.isEmpty) {
      _msg('Create a chapter first.');
      return;
    }
    if (!mounted) return;
    String chapterId = chapters.first.id;
    String type = 'NOTE';
    final title = TextEditingController();
    final url = TextEditingController();
    final description = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => StatefulBuilder(
        builder: (c, set) => AlertDialog(
          title: const Text('Add learning material'),
          content: SingleChildScrollView(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              DropdownButtonFormField<String>(
                initialValue: chapterId,
                items: chapters
                    .map((x) =>
                        DropdownMenuItem(value: x.id, child: Text(x.title)))
                    .toList(),
                onChanged: (v) => set(() => chapterId = v!),
                decoration: const InputDecoration(labelText: 'Chapter'),
              ),
              const SizedBox(height: 10),
              TextField(
                  controller: title,
                  decoration: const InputDecoration(labelText: 'Title')),
              const SizedBox(height: 10),
              DropdownButtonFormField<String>(
                initialValue: type,
                items: ['NOTE', 'VIDEO', 'LINK', 'FILE']
                    .map((x) => DropdownMenuItem(value: x, child: Text(x)))
                    .toList(),
                onChanged: (v) => set(() => type = v!),
                decoration: const InputDecoration(labelText: 'Type'),
              ),
              const SizedBox(height: 10),
              TextField(
                  controller: url,
                  decoration: const InputDecoration(labelText: 'URL')),
              const SizedBox(height: 10),
              TextField(
                  controller: description,
                  maxLines: 2,
                  decoration: const InputDecoration(labelText: 'Description')),
            ]),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(c, false),
                child: const Text('Cancel')),
            FilledButton(
                onPressed: () => Navigator.pop(c, true),
                child: const Text('Publish')),
          ],
        ),
      ),
    );
    if (ok == true) {
      await _run(() => api.createMaterial(chapterId,
          title: title.text,
          type: type,
          url: url.text,
          description: description.text));
    }
  }

  Future<void> _createAssignment() async {
    final saved = await Navigator.push<bool>(
        context,
        MaterialPageRoute(
            builder: (_) => AssignmentEditor(
                classSubjectId: widget.subject.classSubjectId)));
    if (saved == true && mounted) {
      ref.invalidate(dashboardProvider);
      setState(() => version++);
    }
  }

  Future<void> _takeAttendance() async {
    final api = ref.read(eoneApiProvider);
    final students = await api.students(widget.subject.classroomId);
    if (students.isEmpty) {
      _msg('No active students are enrolled in this classroom.');
      return;
    }
    if (!mounted) return;
    final marks = <String, String>{for (final s in students) s.id: 'PRESENT'};
    final topic = TextEditingController();
    final ok = await showDialog<bool>(
      context: context,
      builder: (c) => StatefulBuilder(
        builder: (c, set) => AlertDialog(
          title: const Text('Take attendance'),
          content: SizedBox(
            width: 520,
            height: 430,
            child: Column(children: [
              TextField(
                  controller: topic,
                  decoration:
                      const InputDecoration(labelText: 'Topic (optional)')),
              const SizedBox(height: 10),
              Expanded(
                child: ListView(
                  children: students
                      .map((student) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            title: Text(student.name),
                            trailing: DropdownButton<String>(
                              value: marks[student.id],
                              items: ['PRESENT', 'ABSENT', 'LATE', 'EXCUSED']
                                  .map((x) => DropdownMenuItem(
                                      value: x,
                                      child: Text(x.substring(0, 1) +
                                          x.substring(1).toLowerCase())))
                                  .toList(),
                              onChanged: (v) =>
                                  set(() => marks[student.id] = v!),
                            ),
                          ))
                      .toList(),
                ),
              ),
            ]),
          ),
          actions: [
            TextButton(
                onPressed: () => Navigator.pop(c, false),
                child: const Text('Cancel')),
            FilledButton(
                onPressed: () => Navigator.pop(c, true),
                child: const Text('Save attendance')),
          ],
        ),
      ),
    );
    if (ok == true) {
      await _run(() => api.createAttendance(
            widget.subject.classSubjectId,
            date: DateTime.now(),
            topic: topic.text,
            records: students
                .map((s) => {'studentId': s.id, 'status': marks[s.id]})
                .toList(),
          ));
    }
  }

  Future<void> _run(Future<void> Function() work) async {
    try {
      await work();
      if (mounted) setState(() => version++);
    } catch (e) {
      _msg(apiError(e));
    }
  }

  void _msg(String s) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(s)));
    }
  }
}
