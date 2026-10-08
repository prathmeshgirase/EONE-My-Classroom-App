import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../../models/models.dart';
import 'file_widgets.dart';

class MaterialUploadScreen extends ConsumerStatefulWidget {
  const MaterialUploadScreen({super.key, required this.chapters});
  final List<ChapterItem> chapters;
  @override
  ConsumerState<MaterialUploadScreen> createState() => _MaterialUploadState();
}

class _MaterialUploadState extends ConsumerState<MaterialUploadScreen> {
  final form = GlobalKey<FormState>();
  final title = TextEditingController(), description = TextEditingController();
  late String chapter = widget.chapters.first.id;
  PickedUpload? file;
  bool busy = false;
  double progress = 0;
  String? error;
  @override
  void dispose() {
    title.dispose();
    description.dispose();
    super.dispose();
  }

  Future<void> save() async {
    if (!form.currentState!.validate()) return;
    if (file == null) {
      setState(() => error = 'Choose a file first.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
      progress = 0;
    });
    try {
      await ref.read(eoneApiProvider).uploadFile(
          'class-subjects/chapters/$chapter/materials/upload',
          file!.name,
          file!.bytes,
          {'title': title.text.trim(), 'description': description.text.trim()},
          onProgress: (sent, total) {
        if (mounted && total > 0) setState(() => progress = sent / total);
      });
      if (mounted) Navigator.pop(context, true);
    } catch (e) {
      if (mounted) setState(() => error = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => PopScope(
      canPop: !busy,
      child: Scaffold(
          appBar: AppBar(title: const Text('Upload learning material')),
          body: Center(
            child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 680),
                child: Form(
                    key: form,
                    child:
                        ListView(padding: const EdgeInsets.all(24), children: [
                      DropdownButtonFormField<String>(
                          initialValue: chapter,
                          isExpanded: true,
                          decoration:
                              const InputDecoration(labelText: 'Chapter'),
                          items: widget.chapters
                              .map((c) => DropdownMenuItem(
                                  value: c.id, child: Text(c.title)))
                              .toList(),
                          onChanged: busy
                              ? null
                              : (v) => setState(() => chapter = v!)),
                      const SizedBox(height: 16),
                      TextFormField(
                          controller: title,
                          enabled: !busy,
                          maxLength: 180,
                          decoration: const InputDecoration(labelText: 'Title'),
                          validator: (v) => v == null || v.trim().isEmpty
                              ? 'Enter a title'
                              : null),
                      const SizedBox(height: 16),
                      TextField(
                          controller: description,
                          enabled: !busy,
                          maxLines: 3,
                          decoration: const InputDecoration(
                              labelText: 'Description (optional)')),
                      const SizedBox(height: 16),
                      UploadPicker(
                          enabled: !busy,
                          onChanged: (v) => setState(() => file = v)),
                      const SizedBox(height: 16),
                      if (busy)
                        LinearProgressIndicator(
                            value: progress < 1 ? progress : null),
                      if (error != null)
                        Text(error!,
                            style: TextStyle(
                                color: Theme.of(context).colorScheme.error)),
                      FilledButton(
                          onPressed: busy ? null : save,
                          child:
                              Text(busy ? 'Uploading…' : 'Publish material')),
                    ]))),
          )));
}
