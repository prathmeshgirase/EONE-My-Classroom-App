import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/eone_api.dart';
import '../../core/api_client.dart';
import '../files/file_widgets.dart';

class SubmissionHistoryScreen extends ConsumerStatefulWidget {
  const SubmissionHistoryScreen({super.key, required this.submissionId});
  final String submissionId;
  @override
  ConsumerState<SubmissionHistoryScreen> createState() => _HistoryState();
}

class _HistoryState extends ConsumerState<SubmissionHistoryScreen> {
  int page = 0;
  late Future<Map<String, dynamic>> history;
  @override
  void initState() {
    super.initState();
    load();
  }

  void load() {
    history =
        ref.read(eoneApiProvider).submissionHistory(widget.submissionId, page);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Previous submissions')),
        body: FutureBuilder<Map<String, dynamic>>(
            future: history,
            builder: (context, snapshot) {
              if (snapshot.hasError) {
                return Center(
                    child: Column(mainAxisSize: MainAxisSize.min, children: [
                  Text(apiError(snapshot.error!)),
                  TextButton(
                      onPressed: () => setState(load),
                      child: const Text('Retry'))
                ]));
              }
              if (!snapshot.hasData) {
                return const Center(child: CircularProgressIndicator());
              }
              final data = snapshot.data!;
              final items = data['content'] as List;
              return ListView(padding: const EdgeInsets.all(24), children: [
                if (items.isEmpty)
                  const Text(
                      'No earlier versions. Your current submission is shown on the assignment.'),
                for (final item in items)
                  Card(
                      child: Padding(
                          padding: const EdgeInsets.all(16),
                          child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(DateFormat('dd MMM yyyy, h:mm a').format(
                                    DateTime.parse(
                                            item['submittedAt'] as String)
                                        .toLocal())),
                                const SizedBox(height: 8),
                                SelectableText(
                                    item['answerText'] as String? ?? ''),
                                if (item['attachmentUrl'] != null)
                                  AttachmentLink(
                                      url: item['attachmentUrl'] as String),
                              ]))),
                Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      TextButton(
                          onPressed: page == 0
                              ? null
                              : () => setState(() {
                                    page--;
                                    load();
                                  }),
                          child: const Text('Previous')),
                      TextButton(
                          onPressed: page + 1 >= (data['totalPages'] as int)
                              ? null
                              : () => setState(() {
                                    page++;
                                    load();
                                  }),
                          child: const Text('Next')),
                    ]),
              ]);
            }),
      );
}
