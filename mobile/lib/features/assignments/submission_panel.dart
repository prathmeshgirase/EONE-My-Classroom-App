import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../models/models.dart';
import 'submission_history_screen.dart';
import '../files/file_widgets.dart';

class SubmissionPanel extends ConsumerStatefulWidget {
  const SubmissionPanel(
      {super.key, required this.assignment, required this.onSubmitted});
  final AssignmentItem assignment;
  final VoidCallback onSubmitted;
  @override
  ConsumerState<SubmissionPanel> createState() => _SubmissionPanelState();
}

class _SubmissionPanelState extends ConsumerState<SubmissionPanel> {
  late final answer =
      TextEditingController(text: widget.assignment.mySubmission?.answerText);
  late final attachment = TextEditingController(
      text: widget.assignment.mySubmission?.attachmentUrl);
  bool editing = false, busy = false;
  String? error;
  PickedUpload? file;
  double progress = 0;
  @override
  void dispose() {
    answer.dispose();
    attachment.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if (busy) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      if (file != null) {
        await ref.read(eoneApiProvider).uploadFile(
            'assignments/${widget.assignment.id}/submit-file',
            file!.name,
            file!.bytes,
            {'answerText': answer.text}, onProgress: (sent, total) {
          if (mounted && total > 0) setState(() => progress = sent / total);
        });
      } else {
        await ref.read(eoneApiProvider).submitAssignment(widget.assignment.id,
            answerText: answer.text,
            attachmentUrl:
                attachment.text.trim().isEmpty ? null : attachment.text.trim());
      }
      if (mounted) widget.onSubmitted();
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final assignment = widget.assignment;
    final current = assignment.mySubmission;
    final open = assignment.published && assignment.closedAt == null;
    final canRevise = open &&
        current != null &&
        current.status != 'GRADED' &&
        assignment.dueAt.isAfter(DateTime.now());
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      if (current != null) ...[
        Text('Your submission', style: Theme.of(context).textTheme.titleLarge),
        const SizedBox(height: 8),
        Text(current.status),
        if (current.submittedAt != null)
          Text(DateFormat('dd MMM yyyy, h:mm a')
              .format(current.submittedAt!.toLocal())),
        const SizedBox(height: 12),
        SelectableText(current.answerText ?? ''),
        if (current.attachmentUrl != null)
          AttachmentLink(url: current.attachmentUrl!),
        if (current.marks != null)
          Text('Score: ${current.marks} / ${assignment.maxMarks}'),
        if (current.feedback != null) Text('Feedback: ${current.feedback}'),
        Wrap(spacing: 8, children: [
          TextButton(
              onPressed: () => Navigator.push(
                  context,
                  MaterialPageRoute(
                      builder: (_) =>
                          SubmissionHistoryScreen(submissionId: current.id))),
              child: const Text('Previous versions')),
          if (canRevise)
            TextButton(
                onPressed: () => setState(() => editing = !editing),
                child: Text(editing ? 'Cancel edit' : 'Edit submission')),
        ]),
      ],
      if (!open) const Text('Submissions are closed.'),
      if (open && (current == null || (editing && canRevise))) ...[
        const SizedBox(height: 16),
        TextField(
            controller: answer,
            enabled: !busy,
            minLines: 4,
            maxLines: 10,
            decoration: const InputDecoration(labelText: 'Answer / notes')),
        const SizedBox(height: 16),
        TextField(
            controller: attachment,
            enabled: !busy && file == null,
            decoration:
                const InputDecoration(labelText: 'Attachment URL (optional)')),
        const SizedBox(height: 16),
        UploadPicker(
            enabled: !busy, onChanged: (v) => setState(() => file = v)),
        if (file != null)
          const Text('The selected file replaces the attachment link.'),
        if (busy && file != null)
          LinearProgressIndicator(value: progress < 1 ? progress : null),
        const SizedBox(height: 16),
        if (assignment.dueAt.isBefore(DateTime.now()))
          const Text(
              'The deadline has passed. This submission will be marked late.'),
        if (error != null)
          Text(error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error)),
        FilledButton(
            onPressed: busy ? null : submit,
            child: Text(busy
                ? 'Submitting…'
                : current == null
                    ? 'Submit work'
                    : 'Save new version')),
      ],
    ]);
  }
}
