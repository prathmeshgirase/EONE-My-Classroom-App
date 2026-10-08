import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eone_mobile/core/eone_api.dart';
import 'package:eone_mobile/features/files/file_widgets.dart';

void main() {
  test('upload sends replayable multipart bytes with file and answer fields',
      () async {
    final dio = Dio(BaseOptions(baseUrl: 'https://eone.invalid/api/v1/'));
    RequestOptions? sent;
    dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      sent = options;
      handler.resolve(Response(requestOptions: options, statusCode: 200));
    }));
    await EoneApi(dio).uploadFile(
        'assignments/a/submit-file',
        'answer.pdf',
        Uint8List.fromList(utf8.encode('%PDF-1.7')),
        {'answerText': 'My answer'});
    expect(sent!.data, isA<Uint8List>());
    expect(sent!.contentType, startsWith('multipart/form-data; boundary='));
    final body = utf8.decode(sent!.data as Uint8List);
    expect(body, contains('filename="answer.pdf"'));
    expect(body, contains('name="answerText"'));
    expect(body, contains('My answer'));
    expect(body, contains('%PDF-1.7'));
  });

  test('download stays on the configured API and preserves filename and bytes',
      () async {
    final dio = Dio(BaseOptions(baseUrl: 'https://eone.invalid/api/v1/'));
    dio.interceptors.add(InterceptorsWrapper(onRequest: (options, handler) {
      expect(options.uri.host, 'eone.invalid');
      expect(options.uri.path,
          '/api/v1/files/12345678-1234-1234-1234-123456789abc');
      handler.resolve(Response<List<int>>(
          requestOptions: options,
          data: [1, 2, 3],
          headers: Headers.fromMap({
            'content-disposition': ['attachment; filename="notes.pdf"']
          })));
    }));
    final (bytes, name) = await EoneApi(dio)
        .downloadFile('/api/v1/files/12345678-1234-1234-1234-123456789abc');
    expect(bytes, [1, 2, 3]);
    expect(name, 'notes.pdf');
    await expectLater(
        EoneApi(dio).downloadFile('https://other.invalid/private'),
        throwsArgumentError);
    await expectLater(EoneApi(dio).downloadFile('/api/v1/files/../../auth'),
        throwsArgumentError);
  });

  testWidgets('external links stay selectable and stored files offer download',
      (tester) async {
    await tester.pumpWidget(const ProviderScope(
        child: MaterialApp(
            home: Scaffold(
                body: Column(children: [
      AttachmentLink(url: 'https://example.org/notes'),
      AttachmentLink(url: '/api/v1/files/12345678-1234-1234-1234-123456789abc'),
    ])))));
    expect(find.text('https://example.org/notes'), findsOneWidget);
    expect(find.text('Download attachment'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
