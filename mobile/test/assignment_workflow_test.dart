import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eone_mobile/core/eone_api.dart';
import 'package:eone_mobile/features/assignments/assignment_editor.dart';
import 'package:eone_mobile/features/assignments/submission_panel.dart';
import 'package:eone_mobile/models/models.dart';

class DraftApi extends EoneApi {
  DraftApi() : super(Dio());
  bool? wasPublished;
  @override
  Future<void> createAssignment(String classSubjectId,
      {required String title,
      required String description,
      required DateTime dueAt,
      required double maxMarks,
      bool published = true}) async {
    wasPublished = published;
  }
}

AssignmentItem assignment({DateTime? closed, SubmissionItem? submission}) =>
    AssignmentItem(
        id: 'a',
        classSubjectId: 'cs',
        classroomId: 'class',
        classroomName: 'SY',
        subjectName: 'DBMS',
        title: 'Task',
        description: 'Instructions',
        dueAt: DateTime.now().add(const Duration(days: 1)),
        maxMarks: 20,
        published: true,
        closedAt: closed,
        mySubmission: submission);

void main() {
  testWidgets('new assignments default to private drafts and validate marks',
      (tester) async {
    final api = DraftApi();
    await tester.pumpWidget(ProviderScope(
        overrides: [eoneApiProvider.overrideWithValue(api)],
        child: MaterialApp(
            home: Builder(
                builder: (context) => Scaffold(
                    body: TextButton(
                        onPressed: () => Navigator.push(
                            context,
                            MaterialPageRoute(
                                builder: (_) => const AssignmentEditor(
                                    classSubjectId: 'cs'))),
                        child: const Text('Create')))))));
    await tester.tap(find.text('Create'));
    await tester.pumpAndSettle();
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Title'), 'Test assignment');
    await tester.enterText(find.widgetWithText(TextFormField, 'Instructions'),
        'Complete the exercise');
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Maximum marks'), '-1');
    await tester.ensureVisible(find.text('Save draft'));
    await tester.tap(find.text('Save draft'));
    await tester.pump();
    expect(find.text('Enter positive marks'), findsOneWidget);
    expect(api.wasPublished, isNull);
    await tester.enterText(
        find.widgetWithText(TextFormField, 'Maximum marks'), '20');
    await tester.ensureVisible(find.text('Save draft'));
    await tester.tap(find.text('Save draft'));
    await tester.pumpAndSettle();
    expect(api.wasPublished, isFalse);
  });
  testWidgets('closed assignment has no submission action', (tester) async {
    await tester.pumpWidget(ProviderScope(
        child: MaterialApp(
            home: Scaffold(
                body: SubmissionPanel(
                    assignment: assignment(closed: DateTime.now()),
                    onSubmitted: () {})))));
    expect(find.text('Submissions are closed.'), findsOneWidget);
    expect(find.text('Submit work'), findsNothing);
  });
  testWidgets('graded submission displays feedback and cannot be edited',
      (tester) async {
    await tester.pumpWidget(ProviderScope(
        child: MaterialApp(
            home: Scaffold(
                body: SubmissionPanel(
                    assignment: assignment(
                        submission: const SubmissionItem(
                            id: 's',
                            studentId: 'student',
                            studentName: 'Test',
                            status: 'GRADED',
                            answerText: 'Submitted answer',
                            marks: 18,
                            feedback: 'Well explained')),
                    onSubmitted: () {})))));
    expect(find.text('Feedback: Well explained'), findsOneWidget);
    expect(find.text('Edit submission'), findsNothing);
    expect(find.text('Save new version'), findsNothing);
  });
}
