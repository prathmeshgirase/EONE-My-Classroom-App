import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../core/eone_api.dart';
import '../../core/paged_records.dart';

class AnnouncementsScreen extends ConsumerWidget {
  const AnnouncementsScreen(
      {super.key, required this.scope, required this.target});
  final String scope;
  final String target;

  @override
  Widget build(BuildContext context, WidgetRef ref) => Scaffold(
        appBar: AppBar(title: const Text('Announcements')),
        body: PagedRecords(
          searchLabel: 'Search announcements',
          load: (page, search) => ref
              .read(eoneApiProvider)
              .announcementPage(scope, target, page, search),
          itemBuilder: (item, reload) => Card(
              child: Padding(
            padding: const EdgeInsets.all(16),
            child:
                Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(item['title'] as String,
                  style: Theme.of(context).textTheme.titleMedium),
              const SizedBox(height: 8),
              Text(item['body'] as String),
              const SizedBox(height: 8),
              Text(
                  '${item['authorName']} • ${DateFormat.yMMMd().format(DateTime.parse(item['createdAt'] as String).toLocal())}',
                  style: Theme.of(context).textTheme.bodySmall),
            ]),
          )),
        ),
      );
}
