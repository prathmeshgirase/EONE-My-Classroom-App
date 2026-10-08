import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:eone_mobile/core/eone_api.dart';
import 'package:eone_mobile/features/attendance/attendance_overview_screen.dart';

class AttendanceApi extends EoneApi {
  AttendanceApi() : super(Dio());
  double? requestedThreshold;
  @override
  Future<Map<String, dynamic>> classroomAttendance(String id, int page,
      {double below = 101}) async {
    requestedThreshold = below;
    return {
      'page': page,
      'size': 20,
      'totalElements': below == 75 ? 0 : 1,
      'totalPages': below == 75 ? 0 : 1,
      'content': below == 75
          ? []
          : [
              {
                'studentId': 's',
                'studentName': 'Test Student',
                'present': 8,
                'absent': 2,
                'late': 0,
                'excused': 0,
                'percentage': 80.0
              }
            ]
    };
  }
}

void main() {
  testWidgets(
      'low attendance selection requests a server-side filter and shows empty state',
      (tester) async {
    final api = AttendanceApi();
    await tester.pumpWidget(ProviderScope(
        overrides: [eoneApiProvider.overrideWithValue(api)],
        child: const MaterialApp(
            home: AttendanceOverviewScreen(
                classroomId: 'class', classroomName: 'SY CS'))));
    await tester.pumpAndSettle();
    expect(find.text('80.0%'), findsOneWidget);
    await tester.tap(find.text('Below 75%'));
    await tester.pumpAndSettle();
    expect(api.requestedThreshold, 75);
    expect(find.text('No recorded attendance below 75%.'), findsOneWidget);
    expect(find.text('Test Student'), findsNothing);
  });
}
