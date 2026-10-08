import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../models/models.dart';
import 'assignment_editor.dart';

class AssignmentControls extends ConsumerStatefulWidget {
  const AssignmentControls(
      {super.key, required this.assignment, required this.onChanged});
  final AssignmentItem assignment;
  final VoidCallback onChanged;
  @override
  ConsumerState<AssignmentControls> createState() => _ControlsState();
}

class _ControlsState extends ConsumerState<AssignmentControls> {
  bool busy = false;
  String? error;
  late final stats =
      ref.read(eoneApiProvider).assignmentStats(widget.assignment.id);
  Future<void> change(bool publish) async {
    if (busy) return;
    if (!publish) {
      final confirmed = await showDialog<bool>(
          context: context,
          builder: (c) => AlertDialog(
                  title: const Text('Close submissions?'),
                  content: const Text(
                      'Students will no longer be able to submit or revise work. You can still grade submissions.'),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(c, false),
                        child: const Text('Cancel')),
                    FilledButton(
                        onPressed: () => Navigator.pop(c, true),
                        child: const Text('Close submissions'))
                  ]));
      if (confirmed != true || !mounted) return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final api = ref.read(eoneApiProvider);
      if (publish) {
        await api.publishAssignment(widget.assignment.id);
      } else {
        await api.closeAssignment(widget.assignment.id);
      }
      if (mounted) widget.onChanged();
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) =>
      Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Wrap(spacing: 8, children: [
          if (!widget.assignment.published) ...[
            OutlinedButton(
                onPressed: busy
                    ? null
                    : () async {
                        final saved = await Navigator.push<bool>(
                            context,
                            MaterialPageRoute(
                                builder: (_) => AssignmentEditor(
                                    classSubjectId:
                                        widget.assignment.classSubjectId,
                                    assignment: widget.assignment)));
                        if (saved == true && mounted) widget.onChanged();
                      },
                child: const Text('Edit draft')),
            FilledButton(
                onPressed: busy ? null : () => change(true),
                child: const Text('Publish')),
          ] else if (widget.assignment.closedAt == null)
            OutlinedButton(
                onPressed: busy ? null : () => change(false),
                child: const Text('Close submissions')),
        ]),
        if (error != null)
          Text(error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error)),
        const SizedBox(height: 16),
        FutureBuilder<Map<String, dynamic>>(
            future: stats,
            builder: (context, snapshot) {
              if (snapshot.hasError) return Text(apiError(snapshot.error!));
              if (!snapshot.hasData) return const LinearProgressIndicator();
              final data = snapshot.data!;
              return Wrap(spacing: 8, runSpacing: 8, children: [
                for (final entry in {
                  'students': 'Students',
                  'submitted': 'Submitted',
                  'notSubmitted': 'Missing',
                  'graded': 'Graded',
                  'pendingGrading': 'To grade',
                  'late': 'Late'
                }.entries)
                  Chip(label: Text('${entry.value}: ${data[entry.key]}')),
              ]);
            }),
        const SizedBox(height: 20),
      ]);
}
