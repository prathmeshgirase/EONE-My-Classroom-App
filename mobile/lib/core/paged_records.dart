import 'package:flutter/material.dart';
import 'api_client.dart';

class PagedRecords extends StatefulWidget {
  const PagedRecords(
      {super.key,
      required this.load,
      required this.itemBuilder,
      this.searchLabel});
  final Future<Map<String, dynamic>> Function(int page, String search) load;
  final Widget Function(Map<String, dynamic> item, VoidCallback reload)
      itemBuilder;
  final String? searchLabel;
  @override
  State<PagedRecords> createState() => _PagedRecordsState();
}

class _PagedRecordsState extends State<PagedRecords> {
  int page = 0;
  String search = '';
  late Future<Map<String, dynamic>> result;
  @override
  void initState() {
    super.initState();
    reload();
  }

  void reload() {
    result = widget.load(page, search);
  }

  @override
  Widget build(BuildContext context) => Column(children: [
        if (widget.searchLabel != null)
          Padding(
              padding: const EdgeInsets.all(16),
              child: TextField(
                  decoration: InputDecoration(
                      labelText: widget.searchLabel,
                      suffixIcon: const Icon(Icons.search)),
                  textInputAction: TextInputAction.search,
                  onSubmitted: (value) => setState(() {
                        search = value.trim();
                        page = 0;
                        reload();
                      }))),
        Expanded(
            child: FutureBuilder<Map<String, dynamic>>(
                future: result,
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (snapshot.hasError) {
                    return Center(
                        child:
                            Column(mainAxisSize: MainAxisSize.min, children: [
                      Text(apiError(snapshot.error!)),
                      TextButton(
                          onPressed: () => setState(reload),
                          child: const Text('Retry'))
                    ]));
                  }
                  final data = snapshot.data!;
                  final rows = data['content'] as List;
                  return RefreshIndicator(
                      onRefresh: () async {
                        setState(reload);
                        await result;
                      },
                      child: ListView(
                          physics: const AlwaysScrollableScrollPhysics(),
                          padding: const EdgeInsets.all(16),
                          children: [
                            if (rows.isEmpty)
                              const Padding(
                                  padding: EdgeInsets.all(24),
                                  child: Text('No matching records.')),
                            for (final row in rows)
                              widget.itemBuilder(
                                  Map<String, dynamic>.from(row as Map),
                                  () => setState(reload)),
                            Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  TextButton(
                                      onPressed: page == 0
                                          ? null
                                          : () => setState(() {
                                                page--;
                                                reload();
                                              }),
                                      child: const Text('Previous')),
                                  Text(
                                      '${page + 1} / ${(data['totalPages'] as int).clamp(1, 100001)}'),
                                  TextButton(
                                      onPressed: page + 1 >=
                                              (data['totalPages'] as int)
                                          ? null
                                          : () => setState(() {
                                                page++;
                                                reload();
                                              }),
                                      child: const Text('Next')),
                                ]),
                          ]));
                })),
      ]);
}
