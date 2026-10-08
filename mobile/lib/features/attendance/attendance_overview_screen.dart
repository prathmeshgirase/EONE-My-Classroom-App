import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';

class AttendanceOverviewScreen extends ConsumerStatefulWidget {
  const AttendanceOverviewScreen(
      {super.key, required this.classroomId, required this.classroomName});
  final String classroomId, classroomName;
  @override
  ConsumerState<AttendanceOverviewScreen> createState() => _OverviewState();
}

class _OverviewState extends ConsumerState<AttendanceOverviewScreen> {
  int page = 0;
  bool lowOnly = false;
  late Future<Map<String, dynamic>> summary;
  @override
  void initState() {
    super.initState();
    load();
  }

  void load() {
    summary = ref.read(eoneApiProvider).classroomAttendance(
        widget.classroomId, page,
        below: lowOnly ? 75 : 101);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Attendance overview')),
        body: SafeArea(
            child: Column(children: [
          Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(widget.classroomName,
                        style: Theme.of(context).textTheme.titleLarge),
                    const SizedBox(height: 8),
                    const Text(
                        'Present and late count as attended. Excused records are excluded. Unmarked sessions do not count.'),
                    const SizedBox(height: 8),
                    FilterChip(
                        label: const Text('Below 75%'),
                        selected: lowOnly,
                        onSelected: (value) => setState(() {
                              lowOnly = value;
                              page = 0;
                              load();
                            })),
                  ])),
          Expanded(
              child: FutureBuilder<Map<String, dynamic>>(
                  future: summary,
                  builder: (context, snapshot) {
                    if (snapshot.hasError) {
                      return Center(
                          child:
                              Column(mainAxisSize: MainAxisSize.min, children: [
                        Text(apiError(snapshot.error!)),
                        TextButton(
                            onPressed: () => setState(load),
                            child: const Text('Retry'))
                      ]));
                    }
                    if (!snapshot.hasData) {
                      return const Center(child: CircularProgressIndicator());
                    }
                    final data = snapshot.data!,
                        items = data['content'] as List;
                    return ListView(
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        children: [
                          if (items.isEmpty)
                            Padding(
                                padding: const EdgeInsets.all(24),
                                child: Text(lowOnly
                                    ? 'No recorded attendance below 75%.'
                                    : 'No active students in this classroom.')),
                          for (final row in items)
                            Card(
                                child: ListTile(
                              title: Text(row['studentName'] as String),
                              subtitle: Text(
                                  'Present ${row['present']} · Absent ${row['absent']} · Late ${row['late']} · Excused ${row['excused']}'),
                              trailing: Text(row['percentage'] == null
                                  ? 'No records'
                                  : '${(row['percentage'] as num).toStringAsFixed(1)}%'),
                            )),
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
                                Text('${data['totalElements']} students'),
                                TextButton(
                                    onPressed:
                                        page + 1 >= (data['totalPages'] as int)
                                            ? null
                                            : () => setState(() {
                                                  page++;
                                                  load();
                                                }),
                                    child: const Text('Next')),
                              ]),
                        ]);
                  })),
        ])),
      );
}
