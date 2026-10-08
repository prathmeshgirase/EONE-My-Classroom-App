import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../models/models.dart';

class AssignmentEditor extends ConsumerStatefulWidget {
  const AssignmentEditor(
      {super.key, required this.classSubjectId, this.assignment});
  final String classSubjectId;
  final AssignmentItem? assignment;
  @override
  ConsumerState<AssignmentEditor> createState() => _AssignmentEditorState();
}

class _AssignmentEditorState extends ConsumerState<AssignmentEditor> {
  final form = GlobalKey<FormState>();
  late final title = TextEditingController(text: widget.assignment?.title);
  late final description =
      TextEditingController(text: widget.assignment?.description);
  late final marks = TextEditingController(
      text: widget.assignment?.maxMarks.toString() ?? '20');
  late DateTime due = widget.assignment?.dueAt.toLocal() ??
      DateTime.now().add(const Duration(days: 7));
  bool publish = false, busy = false;
  String? error;
  @override
  void dispose() {
    title.dispose();
    description.dispose();
    marks.dispose();
    super.dispose();
  }

  Future<void> save() async {
    if (busy || !form.currentState!.validate()) return;
    if (!due.isAfter(DateTime.now())) {
      setState(() => error = 'Choose a future deadline.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final api = ref.read(eoneApiProvider);
      if (widget.assignment == null) {
        await api.createAssignment(widget.classSubjectId,
            title: title.text.trim(),
            description: description.text.trim(),
            dueAt: due,
            maxMarks: double.parse(marks.text),
            published: publish);
      } else {
        await api.editAssignment(widget.assignment!.id,
            title: title.text.trim(),
            description: description.text.trim(),
            dueAt: due,
            maxMarks: double.parse(marks.text));
      }
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(
            title: Text(
                widget.assignment == null ? 'New assignment' : 'Edit draft')),
        body: SafeArea(
            child: Center(
                child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 680),
          child: Form(
              key: form,
              child: ListView(padding: const EdgeInsets.all(24), children: [
                TextFormField(
                    controller: title,
                    enabled: !busy,
                    maxLength: 180,
                    decoration: const InputDecoration(labelText: 'Title'),
                    validator: (v) =>
                        v == null || v.trim().isEmpty ? 'Enter a title' : null),
                const SizedBox(height: 16),
                TextFormField(
                    controller: description,
                    enabled: !busy,
                    minLines: 4,
                    maxLines: 10,
                    decoration:
                        const InputDecoration(labelText: 'Instructions'),
                    validator: (v) => v == null || v.trim().isEmpty
                        ? 'Enter instructions'
                        : null),
                const SizedBox(height: 16),
                TextFormField(
                    controller: marks,
                    enabled: !busy,
                    keyboardType:
                        const TextInputType.numberWithOptions(decimal: true),
                    decoration:
                        const InputDecoration(labelText: 'Maximum marks'),
                    validator: (v) {
                      final n = double.tryParse(v ?? '');
                      return n == null || !n.isFinite || n <= 0
                          ? 'Enter positive marks'
                          : null;
                    }),
                const SizedBox(height: 16),
                ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Deadline'),
                    subtitle:
                        Text(DateFormat('dd MMM yyyy, h:mm a').format(due)),
                    trailing: const Icon(Icons.calendar_month_outlined),
                    onTap: busy
                        ? null
                        : () async {
                            final now = DateTime.now();
                            final date = await showDatePicker(
                                context: context,
                                initialDate: due.isBefore(now) ? now : due,
                                firstDate: now,
                                lastDate: DateTime(now.year + 5));
                            if (date == null || !context.mounted) return;
                            final time = await showTimePicker(
                                context: context,
                                initialTime: TimeOfDay.fromDateTime(due));
                            if (time != null && mounted) {
                              setState(() => due = DateTime(
                                  date.year,
                                  date.month,
                                  date.day,
                                  time.hour,
                                  time.minute));
                            }
                          }),
                if (widget.assignment == null)
                  SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: const Text('Publish immediately'),
                      subtitle:
                          const Text('Leave off to save a private draft.'),
                      value: publish,
                      onChanged:
                          busy ? null : (v) => setState(() => publish = v)),
                if (error != null)
                  Padding(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      child: Text(error!,
                          style: TextStyle(
                              color: Theme.of(context).colorScheme.error))),
                const SizedBox(height: 16),
                FilledButton(
                    onPressed: busy ? null : save,
                    child: Text(busy
                        ? 'Saving…'
                        : publish
                            ? 'Publish assignment'
                            : 'Save draft')),
              ])),
        ))),
      );
}
