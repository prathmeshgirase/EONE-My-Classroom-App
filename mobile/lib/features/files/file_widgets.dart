import 'dart:typed_data';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';

class PickedUpload {
  const PickedUpload(this.name, this.bytes);
  final String name;
  final Uint8List bytes;
}

class UploadPicker extends StatefulWidget {
  const UploadPicker({super.key, required this.onChanged, this.enabled = true});
  final ValueChanged<PickedUpload?> onChanged;
  final bool enabled;
  @override
  State<UploadPicker> createState() => _UploadPickerState();
}

class _UploadPickerState extends State<UploadPicker> {
  String? name, error;
  bool picking = false;
  Future<void> pick() async {
    setState(() {
      picking = true;
      error = null;
    });
    try {
      final result = await FilePicker.pickFiles(
          type: FileType.custom,
          allowedExtensions: ['pdf', 'png', 'jpg', 'jpeg']);
      if (result == null || !mounted) return;
      final file = result.files.single;
      if (file.size <= 0 || file.size > 10 * 1024 * 1024) {
        setState(() => error = 'Choose a non-empty file up to 10 MB.');
        return;
      }
      final bytes = await file.xFile.readAsBytes();
      if (!mounted) return;
      if (bytes.length > 10 * 1024 * 1024) {
        setState(() => error = 'Files must be 10 MB or smaller.');
        return;
      }
      setState(() => name = file.name);
      widget.onChanged(PickedUpload(file.name, bytes));
    } catch (_) {
      if (mounted) {
        setState(() => error = 'Could not read this file. Choose it again.');
      }
    } finally {
      if (mounted) setState(() => picking = false);
    }
  }

  @override
  Widget build(BuildContext context) =>
      Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        OutlinedButton.icon(
            onPressed: widget.enabled && !picking ? pick : null,
            icon: const Icon(Icons.attach_file),
            label: Text(picking ? 'Reading file…' : 'Choose PDF or image')),
        const Text('PDF, PNG or JPEG · up to 10 MB'),
        if (name != null)
          Row(children: [
            Expanded(child: Text(name!)),
            IconButton(
                tooltip: 'Remove selected file',
                onPressed: widget.enabled && !picking
                    ? () {
                        setState(() => name = null);
                        widget.onChanged(null);
                      }
                    : null,
                icon: const Icon(Icons.close))
          ]),
        if (error != null)
          Text(error!,
              style: TextStyle(color: Theme.of(context).colorScheme.error)),
      ]);
}

class AttachmentLink extends ConsumerStatefulWidget {
  const AttachmentLink({super.key, required this.url});
  final String url;
  @override
  ConsumerState<AttachmentLink> createState() => _AttachmentLinkState();
}

class _AttachmentLinkState extends ConsumerState<AttachmentLink> {
  bool busy = false;
  String? message;
  Future<void> download() async {
    setState(() {
      busy = true;
      message = null;
    });
    try {
      final (bytes, name) =
          await ref.read(eoneApiProvider).downloadFile(widget.url);
      if (!mounted) return;
      final saved = await FilePicker.saveFile(
          fileName: name, bytes: bytes, dialogTitle: 'Save attachment');
      if (mounted && saved != null) {
        setState(() => message = 'File saved. Open it from your files app.');
      }
    } catch (e) {
      if (mounted) setState(() => message = apiError(e));
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!widget.url.startsWith('/api/v1/files/')) {
      return SelectableText(widget.url);
    }
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      TextButton.icon(
          onPressed: busy ? null : download,
          icon: const Icon(Icons.download),
          label: Text(busy ? 'Downloading…' : 'Download attachment')),
      if (message != null) Text(message!),
    ]);
  }
}
