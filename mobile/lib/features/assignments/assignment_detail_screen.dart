import 'assignment_controls.dart';
import 'submission_panel.dart';
import 'package:flutter/material.dart';
import '../files/file_widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';
import '../../models/models.dart';
import '../auth/session_controller.dart';
import '../dashboard/providers.dart';

class AssignmentDetailScreen extends ConsumerStatefulWidget {
  final String assignmentId;
  const AssignmentDetailScreen({super.key, required this.assignmentId});
  @override
  ConsumerState<AssignmentDetailScreen> createState() =>
      _AssignmentDetailState();
}

class _AssignmentDetailState extends ConsumerState<AssignmentDetailScreen> {
  int version = 0;

  void reload() {
    ref.invalidate(dashboardProvider);
    setState(() => version++);
  }

  bool canTeach(String classSubjectId) {
    final d = ref.watch(dashboardProvider).valueOrNull;
    return d?.contexts.any((c) =>
            c.role == 'SUBJECT_TEACHER' &&
            c.classSubjectId == classSubjectId) ??
        false;
  }

  @override
  Widget build(BuildContext context) => Scaffold(
      appBar: AppBar(title: const Text('Assignment')),
      body: FutureBuilder<AssignmentItem>(
          key: ValueKey(version),
          future: ref.read(eoneApiProvider).assignment(widget.assignmentId),
          builder: (context, snap) {
            if (!snap.hasData && !snap.hasError) {
              return const Center(child: CircularProgressIndicator());
            }
            if (snap.hasError) {
              return Center(
                  child: Text('Could not load assignment\n${snap.error}'));
            }
            final a = snap.data!;
            final staff =
                ref.read(sessionProvider).user?.accountType == 'STAFF';
            return ListView(padding: const EdgeInsets.all(18), children: [
              Text(a.subjectName,
                  style: Theme.of(context).textTheme.labelLarge),
              Text(a.title,
                  style: Theme.of(context)
                      .textTheme
                      .headlineSmall
                      ?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 10),
              Text(a.description),
              const SizedBox(height: 16),
              Wrap(spacing: 8, children: [
                Chip(label: Text(a.state)),
                Chip(label: Text('${a.maxMarks.toStringAsFixed(0)} marks')),
                Chip(
                    label: Text(
                        'Due ${DateFormat('dd MMM, h:mm a').format(a.dueAt.toLocal())}'))
              ]),
              const SizedBox(height: 20),
              if (staff && canTeach(a.classSubjectId)) ...[
                AssignmentControls(
                    key: ValueKey(version), assignment: a, onChanged: reload),
                _TeacherPanel(assignment: a, onChanged: reload)
              ] else if (!staff)
                SubmissionPanel(
                    key: ValueKey(version), assignment: a, onSubmitted: reload)
            ]);
          }));
}

class _TeacherPanel extends ConsumerWidget {
  final AssignmentItem assignment;
  final VoidCallback onChanged;
  const _TeacherPanel({required this.assignment, required this.onChanged});
  @override
  Widget build(BuildContext context, WidgetRef ref) => FutureBuilder<
          List<SubmissionItem>>(
      future: ref.read(eoneApiProvider).submissions(assignment.id),
      builder: (context, snap) {
        if (!snap.hasData && !snap.hasError) {
          return const Center(
              child: Padding(
                  padding: EdgeInsets.all(20),
                  child: CircularProgressIndicator()));
        }
        if (snap.hasError) {
          return Text('Could not load submissions: ${snap.error}');
        }
        final items = snap.data!;
        return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Text('Submissions (${items.length})',
              style: Theme.of(context)
                  .textTheme
                  .titleLarge
                  ?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          if (items.isEmpty)
            const Card(
                child: Padding(
                    padding: EdgeInsets.all(18),
                    child: Text('No submissions yet.')))
          else
            ...items.map((s) => Card(
                child: ListTile(
                    title: Text(s.studentName,
                        style: const TextStyle(fontWeight: FontWeight.w700)),
                    subtitle: Text(
                        '${s.status}${s.marks == null ? '' : ' • ${s.marks}/${assignment.maxMarks}'}'),
                    trailing: const Icon(Icons.rate_review_outlined),
                    onTap: () => _grade(context, ref, s))))
        ]);
      });
  Future<void> _grade(
      BuildContext context, WidgetRef ref, SubmissionItem s) async {
    final form = GlobalKey<FormState>();
    final marks = TextEditingController(text: s.marks?.toString() ?? ''),
        feedback = TextEditingController(text: s.feedback ?? '');
    final ok = await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
                title: Text('Grade ${s.studentName}'),
                content: SingleChildScrollView(
                    child: Form(
                        key: form,
                        child:
                            Column(mainAxisSize: MainAxisSize.min, children: [
                          if (s.answerText?.isNotEmpty == true) ...[
                            const Text('Submitted answer',
                                style: TextStyle(fontWeight: FontWeight.w700)),
                            const SizedBox(height: 8),
                            SelectableText(s.answerText!),
                            const SizedBox(height: 16),
                          ],
                          if (s.attachmentUrl?.isNotEmpty == true) ...[
                            const Text('Attachment'),
                            AttachmentLink(url: s.attachmentUrl!),
                            const SizedBox(height: 16),
                          ],
                          TextFormField(
                              controller: marks,
                              validator: (value) {
                                final score = double.tryParse(value ?? '');
                                return score == null ||
                                        !score.isFinite ||
                                        score < 0 ||
                                        score > assignment.maxMarks
                                    ? 'Enter marks between 0 and ${assignment.maxMarks}'
                                    : null;
                              },
                              keyboardType: TextInputType.number,
                              decoration: InputDecoration(
                                  labelText:
                                      'Marks / ${assignment.maxMarks.toStringAsFixed(0)}')),
                          const SizedBox(height: 10),
                          TextField(
                              controller: feedback,
                              maxLines: 3,
                              decoration:
                                  const InputDecoration(labelText: 'Feedback'))
                        ]))),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.pop(c, false),
                      child: const Text('Cancel')),
                  FilledButton(
                      onPressed: () {
                        if (form.currentState!.validate()) {
                          Navigator.pop(c, true);
                        }
                      },
                      child: const Text('Save'))
                ]));
    if (ok == true) {
      try {
        await ref
            .read(eoneApiProvider)
            .grade(s.id, double.parse(marks.text), feedback.text);
        onChanged();
      } catch (e) {
        if (context.mounted) {
          ScaffoldMessenger.of(context)
              .showSnackBar(SnackBar(content: Text(apiError(e))));
        }
      }
    }
    marks.dispose();
    feedback.dispose();
  }
}
