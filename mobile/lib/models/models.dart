class AppUser {
  final String id, email, firstName, lastName, accountType, status;
  final String? departmentId, departmentName, phone;
  const AppUser(
      {required this.id,
      required this.email,
      required this.firstName,
      required this.lastName,
      required this.accountType,
      required this.status,
      this.departmentId,
      this.departmentName,
      this.phone});
  String get name => '$firstName $lastName';
  factory AppUser.fromJson(Map<String, dynamic> j) => AppUser(
        id: j['id'] as String,
        email: j['email'] as String,
        firstName: j['firstName'] as String,
        lastName: j['lastName'] as String,
        accountType: j['accountType'] as String,
        status: j['status'] as String,
        departmentId: j['departmentId'] as String?,
        departmentName: j['departmentName'] as String?,
        phone: j['phone'] as String?,
      );
}

class EoneContext {
  final String kind, role;
  final String? departmentId,
      departmentName,
      classroomId,
      classroomName,
      classSubjectId,
      subjectName;
  const EoneContext(
      {required this.kind,
      required this.role,
      this.departmentId,
      this.departmentName,
      this.classroomId,
      this.classroomName,
      this.classSubjectId,
      this.subjectName});
  factory EoneContext.fromJson(Map<String, dynamic> j) => EoneContext(
      kind: j['kind'] as String,
      role: j['role'] as String,
      departmentId: j['departmentId'] as String?,
      departmentName: j['departmentName'] as String?,
      classroomId: j['classroomId'] as String?,
      classroomName: j['classroomName'] as String?,
      classSubjectId: j['classSubjectId'] as String?,
      subjectName: j['subjectName'] as String?);
  String get label => subjectName != null
      ? '$role • $subjectName'
      : classroomName != null
          ? '$role • $classroomName'
          : '$role • ${departmentName ?? ''}';
}

class Classroom {
  final String id,
      name,
      programCode,
      programName,
      yearLevel,
      division,
      termName,
      departmentId;
  const Classroom(
      {required this.id,
      required this.name,
      required this.programCode,
      required this.programName,
      required this.yearLevel,
      required this.division,
      required this.termName,
      required this.departmentId});
  factory Classroom.fromJson(Map<String, dynamic> j) => Classroom(
      id: j['id'] as String,
      name: j['name'] as String,
      programCode: j['programCode'] as String,
      programName: j['programName'] as String,
      yearLevel: j['yearLevel'] as String,
      division: j['division'] as String,
      termName: j['termName'] as String,
      departmentId: j['departmentId'] as String);
}

class SubjectItem {
  final String classSubjectId,
      subjectId,
      code,
      name,
      classroomId,
      classroomName;
  const SubjectItem(
      {required this.classSubjectId,
      required this.subjectId,
      required this.code,
      required this.name,
      required this.classroomId,
      required this.classroomName});
  factory SubjectItem.fromJson(Map<String, dynamic> j) => SubjectItem(
      classSubjectId: j['classSubjectId'] as String,
      subjectId: j['subjectId'] as String,
      code: j['code'] as String,
      name: j['name'] as String,
      classroomId: j['classroomId'] as String,
      classroomName: j['classroomName'] as String);
}

class SubmissionItem {
  final String id, studentId, studentName, status;
  final String? answerText, attachmentUrl, feedback;
  final DateTime? submittedAt;
  final double? marks;
  const SubmissionItem(
      {required this.id,
      required this.studentId,
      required this.studentName,
      required this.status,
      this.answerText,
      this.attachmentUrl,
      this.feedback,
      this.submittedAt,
      this.marks});
  factory SubmissionItem.fromJson(Map<String, dynamic> j) => SubmissionItem(
      id: j['id'] as String,
      studentId: j['studentId'] as String,
      studentName: j['studentName'] as String,
      status: j['status'] as String,
      answerText: j['answerText'] as String?,
      attachmentUrl: j['attachmentUrl'] as String?,
      feedback: j['feedback'] as String?,
      submittedAt: j['submittedAt'] == null
          ? null
          : DateTime.parse(j['submittedAt'] as String),
      marks: (j['marks'] as num?)?.toDouble());
}

class AssignmentItem {
  final String id,
      classSubjectId,
      classroomId,
      classroomName,
      subjectName,
      title,
      description;
  final DateTime dueAt;
  final double maxMarks;
  final bool published;
  final DateTime? closedAt;
  final String? lifecycleState;
  String get state =>
      lifecycleState ??
      (!published
          ? 'DRAFT'
          : closedAt != null
              ? 'CLOSED'
              : 'OPEN');
  final SubmissionItem? mySubmission;
  const AssignmentItem(
      {required this.id,
      required this.classSubjectId,
      required this.classroomId,
      required this.classroomName,
      required this.subjectName,
      required this.title,
      required this.description,
      required this.dueAt,
      required this.maxMarks,
      required this.published,
      this.mySubmission,
      this.closedAt,
      this.lifecycleState});
  factory AssignmentItem.fromJson(Map<String, dynamic> j) => AssignmentItem(
      id: j['id'] as String,
      classSubjectId: j['classSubjectId'] as String,
      classroomId: j['classroomId'] as String,
      classroomName: j['classroomName'] as String,
      subjectName: j['subjectName'] as String,
      title: j['title'] as String,
      description: j['description'] as String,
      dueAt: DateTime.parse(j['dueAt'] as String),
      maxMarks: (j['maxMarks'] as num).toDouble(),
      published: j['published'] as bool,
      closedAt: j['closedAt'] == null
          ? null
          : DateTime.parse(j['closedAt'] as String),
      lifecycleState: j['state'] as String?,
      mySubmission: j['mySubmission'] == null
          ? null
          : SubmissionItem.fromJson(
              Map<String, dynamic>.from(j['mySubmission'] as Map)));
}

class MaterialItem {
  final String id, title, type, url;
  final String? description;
  final DateTime createdAt;
  const MaterialItem(
      {required this.id,
      required this.title,
      required this.type,
      required this.url,
      required this.createdAt,
      this.description});
  factory MaterialItem.fromJson(Map<String, dynamic> j) => MaterialItem(
      id: j['id'] as String,
      title: j['title'] as String,
      type: j['type'] as String,
      url: j['url'] as String,
      description: j['description'] as String?,
      createdAt: DateTime.parse(j['createdAt'] as String));
}

class ChapterItem {
  final String id, title;
  final int position;
  final List<MaterialItem> materials;
  const ChapterItem(
      {required this.id,
      required this.title,
      required this.position,
      required this.materials});
  factory ChapterItem.fromJson(Map<String, dynamic> j) => ChapterItem(
      id: j['id'] as String,
      title: j['title'] as String,
      position: j['position'] as int,
      materials: (j['materials'] as List? ?? [])
          .map(
              (e) => MaterialItem.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList());
}

class NotificationItem {
  final String id, title, body, type;
  final String? relatedType, relatedId;
  final DateTime createdAt;
  final DateTime? readAt;
  const NotificationItem(
      {required this.id,
      required this.title,
      required this.body,
      required this.type,
      required this.createdAt,
      this.relatedType,
      this.relatedId,
      this.readAt});
  factory NotificationItem.fromJson(Map<String, dynamic> j) => NotificationItem(
      id: j['id'] as String,
      title: j['title'] as String,
      body: j['body'] as String,
      type: j['type'] as String,
      relatedType: j['relatedType'] as String?,
      relatedId: j['relatedId'] as String?,
      createdAt: DateTime.parse(j['createdAt'] as String),
      readAt:
          j['readAt'] == null ? null : DateTime.parse(j['readAt'] as String));
}

class DashboardData {
  final AppUser user;
  final List<EoneContext> contexts;
  final List<Classroom> classrooms;
  final int unreadNotifications;
  final List<AssignmentItem> upcomingAssignments;
  const DashboardData(
      {required this.user,
      required this.contexts,
      required this.classrooms,
      required this.unreadNotifications,
      required this.upcomingAssignments});
  factory DashboardData.fromJson(Map<String, dynamic> j) => DashboardData(
      user: AppUser.fromJson(Map<String, dynamic>.from(j['user'] as Map)),
      contexts: (j['contexts'] as List)
          .map((e) => EoneContext.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
      classrooms: (j['classrooms'] as List)
          .map((e) => Classroom.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList(),
      unreadNotifications: j['unreadNotifications'] as int,
      upcomingAssignments: (j['upcomingAssignments'] as List)
          .map((e) =>
              AssignmentItem.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList());
}

class AnnouncementItem {
  final String id, title, body, targetType, authorName;
  final DateTime createdAt;
  const AnnouncementItem(
      {required this.id,
      required this.title,
      required this.body,
      required this.targetType,
      required this.authorName,
      required this.createdAt});
  factory AnnouncementItem.fromJson(Map<String, dynamic> j) => AnnouncementItem(
      id: j['id'] as String,
      title: j['title'] as String,
      body: j['body'] as String,
      targetType: j['targetType'] as String,
      authorName: j['authorName'] as String,
      createdAt: DateTime.parse(j['createdAt'] as String));
}

class AttendanceRecordItem {
  final String studentId, studentName, status;
  const AttendanceRecordItem(
      {required this.studentId,
      required this.studentName,
      required this.status});
  factory AttendanceRecordItem.fromJson(Map<String, dynamic> j) =>
      AttendanceRecordItem(
          studentId: j['studentId'] as String,
          studentName: j['studentName'] as String,
          status: j['status'] as String);
}

class AttendanceSessionItem {
  final String id, classSubjectId;
  final DateTime sessionDate;
  final String? topic;
  final List<AttendanceRecordItem> records;
  const AttendanceSessionItem(
      {required this.id,
      required this.classSubjectId,
      required this.sessionDate,
      required this.records,
      this.topic});
  factory AttendanceSessionItem.fromJson(Map<String, dynamic> j) =>
      AttendanceSessionItem(
          id: j['id'] as String,
          classSubjectId: j['classSubjectId'] as String,
          sessionDate: DateTime.parse(j['sessionDate'] as String),
          topic: j['topic'] as String?,
          records: (j['records'] as List? ?? [])
              .map((e) => AttendanceRecordItem.fromJson(
                  Map<String, dynamic>.from(e as Map)))
              .toList());
}
