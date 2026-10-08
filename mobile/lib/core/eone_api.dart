import 'package:dio/dio.dart';
import 'dart:typed_data';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../models/models.dart';
import 'api_client.dart';

final eoneApiProvider = Provider<EoneApi>(
  (ref) => EoneApi(
    ref.read(dioProvider),
  ),
);

class EoneApi {
  final Dio _dio;

  EoneApi(this._dio);

  Future<Map<String, dynamic>> announcementPage(
          String scope, String target, int page, String search,
          {int size = 20}) async =>
      Map<String, dynamic>.from((await _dio.get('announcements/page',
              queryParameters: {
            'scope': scope,
            'target': target,
            'page': page,
            'size': size,
            'search': search
          }))
          .data as Map);

  Future<void> requestPassword(String email) async {
    await _dio.post('auth/password/request', data: {'email': email});
  }

  Future<void> resetPassword(String token, String password) async {
    await _dio.post('auth/password/reset',
        data: {'token': token, 'password': password});
  }

  Future<void> changePassword(String current, String password) async {
    await _dio.post('me/password',
        data: {'currentPassword': current, 'password': password});
  }

  Future<void> registerDevice(String token) async {
    await _dio.post('devices', data: {'token': token});
  }

  Future<void> removeDevice(String token) async {
    await _dio.delete('devices', data: {'token': token});
  }

  Future<Map<String, dynamic>> auditPage(
          String department, int page, String action) async =>
      Map<String, dynamic>.from((await _dio.get(
              'hod/departments/$department/audit',
              queryParameters: {'page': page, 'size': 20, 'action': action}))
          .data as Map);

  Future<List<Map<String, dynamic>>> myAttendanceSummary() async =>
      ((await _dio.get('attendance/me/summary')).data as List)
          .map((v) => Map<String, dynamic>.from(v as Map))
          .toList();

  Future<Map<String, dynamic>> notificationsPage(int page,
          {bool unread = false}) async =>
      Map<String, dynamic>.from((await _dio.get('notifications/page',
              queryParameters: {'page': page, 'size': 20, 'unread': unread}))
          .data as Map);

  Future<void> uploadFile(String route, String filename, Uint8List bytes,
      Map<String, String> fields,
      {ProgressCallback? onProgress}) async {
    final form = FormData.fromMap({
      ...fields,
      'file': MultipartFile.fromBytes(bytes, filename: filename),
    });
    // A bounded byte body can be replayed by the existing session interceptor.
    // A finalized FormData stream cannot be replayed after token rotation.
    final body = await form.readAsBytes();
    await _dio.post(route,
        data: body,
        options: Options(
            contentType: 'multipart/form-data; boundary=${form.boundary}',
            sendTimeout: const Duration(minutes: 2),
            receiveTimeout: const Duration(minutes: 2)),
        onSendProgress: onProgress);
  }

  Future<(Uint8List, String)> downloadFile(String url) async {
    if (!RegExp(r'^/api/v1/files/[0-9a-fA-F-]{36}$').hasMatch(url)) {
      throw ArgumentError('Invalid EONE file URL');
    }
    final response = await _dio.get<List<int>>(url.substring('/api/v1/'.length),
        options: Options(
            responseType: ResponseType.bytes,
            receiveTimeout: const Duration(minutes: 2)));
    final disposition = response.headers.value('content-disposition') ?? '';
    final name =
        RegExp(r'filename="([^"]+)"').firstMatch(disposition)?.group(1) ??
            'attachment';
    return (
      Uint8List.fromList(response.data!),
      name.replaceAll(RegExp(r'[^a-zA-Z0-9._ -]'), '_')
    );
  }

  // ---------------------------------------------------------------------------
  // Authentication
  // ---------------------------------------------------------------------------

  Future<Map<String, dynamic>> login(
    String email,
    String password,
  ) async {
    final response = await _dio.post(
      'auth/login',
      data: {
        'email': email,
        'password': password,
      },
    );

    return Map<String, dynamic>.from(
      response.data as Map,
    );
  }

  Future<AppUser> register({
    required String email,
    required String password,
    required String firstName,
    required String lastName,
    required String accountType,
    required String departmentCode,
  }) async {
    final response = await _dio.post(
      'auth/register',
      data: {
        'email': email,
        'password': password,
        'firstName': firstName,
        'lastName': lastName,
        'accountType': accountType,
        'departmentCode': departmentCode,
      },
    );

    return AppUser.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  Future<void> logout(
    String refreshToken,
  ) async {
    await _dio.post(
      'auth/logout',
      data: {
        'refreshToken': refreshToken,
      },
    );
  }

  // ---------------------------------------------------------------------------
  // Current user
  // ---------------------------------------------------------------------------

  Future<AppUser> me() async {
    final response = await _dio.get(
      'me',
    );

    return AppUser.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  Future<DashboardData> dashboard() async {
    final response = await _dio.get(
      'me/dashboard',
    );

    return DashboardData.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Classrooms
  // ---------------------------------------------------------------------------

  Future<List<Classroom>> classrooms() async {
    final response = await _dio.get(
      'classrooms',
    );

    return (response.data as List)
        .map(
          (item) => Classroom.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<List<SubjectItem>> subjects(
    String classroomId,
  ) async {
    final response = await _dio.get(
      'classrooms/$classroomId/subjects',
    );

    return (response.data as List)
        .map(
          (item) => SubjectItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<List<AppUser>> students(
    String classroomId,
  ) async {
    final response = await _dio.get(
      'classrooms/$classroomId/students',
    );

    return (response.data as List)
        .map(
          (item) => AppUser.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<List<AnnouncementItem>> announcements(
    String classroomId,
  ) async {
    final response = await _dio.get(
      'classrooms/$classroomId/announcements',
    );

    return (response.data as List)
        .map(
          (item) => AnnouncementItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<void> createClassroomAnnouncement(
    String classroomId,
    String title,
    String body,
  ) async {
    await _dio.post(
      'announcements',
      data: {
        'title': title,
        'body': body,
        'targetType': 'CLASSROOM',
        'classroomId': classroomId,
      },
    );
  }

  // ---------------------------------------------------------------------------
  // Attendance
  // ---------------------------------------------------------------------------

  Future<Map<String, dynamic>> classroomAttendance(String id, int page,
      {double below = 101}) async {
    return Map<String, dynamic>.from((await _dio.get(
            'attendance/classrooms/$id/summary',
            queryParameters: {'page': page, 'size': 20, 'below': below}))
        .data as Map);
  }

  Future<List<AttendanceSessionItem>> attendanceSessions(
    String classSubjectId,
  ) async {
    final response = await _dio.get(
      'attendance/class-subjects/$classSubjectId/sessions',
    );

    return (response.data as List)
        .map(
          (item) => AttendanceSessionItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<void> createAttendance(
    String classSubjectId, {
    required DateTime date,
    String? topic,
    required List<Map<String, dynamic>> records,
  }) async {
    await _dio.post(
      'attendance/class-subjects/$classSubjectId/sessions',
      data: {
        'sessionDate': date.toIso8601String().substring(0, 10),
        'topic': topic,
        'records': records,
      },
    );
  }

  // ---------------------------------------------------------------------------
  // Chapters / Subjects
  // ---------------------------------------------------------------------------

  Future<List<ChapterItem>> chapters(
    String classSubjectId,
  ) async {
    final response = await _dio.get(
      'class-subjects/$classSubjectId/chapters',
    );

    return (response.data as List)
        .map(
          (item) => ChapterItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<void> createChapter(
    String classSubjectId,
    String title,
    int position,
  ) async {
    await _dio.post(
      'class-subjects/$classSubjectId/chapters',
      data: {
        'title': title,
        'position': position,
      },
    );
  }

  Future<void> createMaterial(
    String chapterId, {
    required String title,
    required String type,
    required String url,
    String? description,
  }) async {
    await _dio.post(
      'class-subjects/chapters/$chapterId/materials',
      data: {
        'title': title,
        'type': type,
        'url': url,
        'description': description,
      },
    );
  }

  // ---------------------------------------------------------------------------
  // Assignments
  // ---------------------------------------------------------------------------

  Future<List<AssignmentItem>> subjectAssignments(
    String classSubjectId,
  ) async {
    final response = await _dio.get(
      'class-subjects/$classSubjectId/assignments',
    );

    return (response.data as List)
        .map(
          (item) => AssignmentItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<AssignmentItem> assignment(
    String id,
  ) async {
    final response = await _dio.get(
      'assignments/$id',
    );

    return AssignmentItem.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  Future<void> createAssignment(
    String classSubjectId, {
    required String title,
    required String description,
    required DateTime dueAt,
    required double maxMarks,
    bool published = true,
  }) async {
    await _dio.post(
      'class-subjects/$classSubjectId/assignments',
      data: {
        'title': title,
        'description': description,
        'dueAt': dueAt.toUtc().toIso8601String(),
        'maxMarks': maxMarks,
        'published': published,
      },
    );
  }

  Future<void> publishAssignment(String id) async {
    await _dio.post('assignments/$id/publish');
  }

  Future<void> closeAssignment(String id) async {
    await _dio.post('assignments/$id/close');
  }

  Future<void> editAssignment(String id,
      {required String title,
      required String description,
      required DateTime dueAt,
      required double maxMarks}) async {
    await _dio.patch('assignments/$id', data: {
      'title': title,
      'description': description,
      'dueAt': dueAt.toUtc().toIso8601String(),
      'maxMarks': maxMarks
    });
  }

  Future<Map<String, dynamic>> assignmentStats(String id) async {
    return Map<String, dynamic>.from(
        (await _dio.get('assignments/$id/statistics')).data as Map);
  }

  Future<Map<String, dynamic>> submissionHistory(String id, int page) async {
    return Map<String, dynamic>.from((await _dio.get(
            'assignments/submissions/$id/history',
            queryParameters: {'page': page, 'size': 20}))
        .data as Map);
  }

  Future<SubmissionItem> submitAssignment(
    String id, {
    String? answerText,
    String? attachmentUrl,
  }) async {
    final response = await _dio.post(
      'assignments/$id/submit',
      data: {
        'answerText': answerText,
        'attachmentUrl': attachmentUrl,
      },
    );

    return SubmissionItem.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  Future<List<SubmissionItem>> submissions(
    String assignmentId,
  ) async {
    final response = await _dio.get(
      'assignments/$assignmentId/submissions',
    );

    return (response.data as List)
        .map(
          (item) => SubmissionItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<SubmissionItem> grade(
    String submissionId,
    double marks,
    String feedback,
  ) async {
    final response = await _dio.patch(
      'assignments/submissions/$submissionId/grade',
      data: {
        'marks': marks,
        'feedback': feedback,
      },
    );

    return SubmissionItem.fromJson(
      Map<String, dynamic>.from(
        response.data as Map,
      ),
    );
  }

  // ---------------------------------------------------------------------------
  // Notifications
  // ---------------------------------------------------------------------------

  Future<List<NotificationItem>> notifications() async {
    final response = await _dio.get(
      'notifications',
    );

    return (response.data as List)
        .map(
          (item) => NotificationItem.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<void> markRead(
    String id,
  ) async {
    await _dio.post(
      'notifications/$id/read',
    );
  }

  Future<void> markAllRead() async {
    await _dio.post(
      'notifications/read-all',
    );
  }

  // ---------------------------------------------------------------------------
  // HOD administration
  // ---------------------------------------------------------------------------

  Future<List<Map<String, dynamic>>> hodPrograms(
    String departmentId,
  ) async {
    final response = await _dio.get(
      'hod/departments/$departmentId/programs',
    );

    return (response.data as List)
        .map(
          (item) => Map<String, dynamic>.from(
            item as Map,
          ),
        )
        .toList();
  }

  Future<List<Map<String, dynamic>>> hodTerms(
    String departmentId,
  ) async {
    final response = await _dio.get(
      'hod/departments/$departmentId/terms',
    );

    return (response.data as List)
        .map(
          (item) => Map<String, dynamic>.from(
            item as Map,
          ),
        )
        .toList();
  }

  Future<List<Map<String, dynamic>>> hodSubjects(
    String departmentId,
  ) async {
    final response = await _dio.get(
      'hod/departments/$departmentId/subjects',
    );

    return (response.data as List)
        .map(
          (item) => Map<String, dynamic>.from(
            item as Map,
          ),
        )
        .toList();
  }

  Future<List<AppUser>> departmentUsers(
    String departmentId,
  ) async {
    final response = await _dio.get(
      'hod/departments/$departmentId/users',
    );

    return (response.data as List)
        .map(
          (item) => AppUser.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<List<AppUser>> pendingUsers(
    String departmentId,
  ) async {
    final response = await _dio.get(
      'hod/departments/$departmentId/pending-users',
    );

    return (response.data as List)
        .map(
          (item) => AppUser.fromJson(
            Map<String, dynamic>.from(
              item as Map,
            ),
          ),
        )
        .toList();
  }

  Future<void> createProgram(
    String departmentId,
    String code,
    String name,
  ) async {
    await _dio.post(
      'hod/departments/$departmentId/programs',
      data: {
        'code': code,
        'name': name,
      },
    );
  }

  Future<void> createTerm(
    String departmentId, {
    required String name,
    required DateTime start,
    required DateTime end,
    bool active = true,
  }) async {
    await _dio.post(
      'hod/departments/$departmentId/terms',
      data: {
        'name': name,
        'startDate': start.toIso8601String().substring(0, 10),
        'endDate': end.toIso8601String().substring(0, 10),
        'active': active,
      },
    );
  }

  Future<void> createClassroom(
    String departmentId, {
    required String programId,
    required String termId,
    required String yearLevel,
    required String division,
    required String name,
  }) async {
    await _dio.post(
      'hod/departments/$departmentId/classrooms',
      data: {
        'programId': programId,
        'termId': termId,
        'yearLevel': yearLevel,
        'division': division,
        'name': name,
      },
    );
  }

  Future<void> createSubject(
    String departmentId,
    String code,
    String name,
  ) async {
    await _dio.post(
      'hod/departments/$departmentId/subjects',
      data: {
        'code': code,
        'name': name,
      },
    );
  }

  Future<void> linkSubject(
    String departmentId,
    String classroomId,
    String subjectId,
  ) async {
    await _dio.post(
      'hod/departments/$departmentId/class-subjects',
      data: {
        'classroomId': classroomId,
        'subjectId': subjectId,
      },
    );
  }

  Future<void> approveUser(
    String departmentId,
    String userId,
  ) async {
    await _dio.patch(
      'hod/departments/$departmentId/users/$userId/approve',
    );
  }

  Future<void> suspendUser(
    String departmentId,
    String userId,
  ) async {
    await _dio.patch(
      'hod/departments/$departmentId/users/$userId/suspend',
    );
  }

  Future<void> enrollStudent(
    String departmentId,
    String studentId,
    String classroomId,
  ) async {
    await _dio.post(
      'hod/departments/$departmentId/enrollments',
      data: {
        'studentId': studentId,
        'classroomId': classroomId,
      },
    );
  }

  Future<void> assignRole(
    String departmentId, {
    required String userId,
    required String role,
    String? classroomId,
    String? classSubjectId,
  }) async {
    await _dio.post(
      'hod/departments/$departmentId/roles',
      data: {
        'userId': userId,
        'role': role,
        'departmentId': role == 'HOD' ? departmentId : null,
        'classroomId': classroomId,
        'classSubjectId': classSubjectId,
      },
    );
  }
}
