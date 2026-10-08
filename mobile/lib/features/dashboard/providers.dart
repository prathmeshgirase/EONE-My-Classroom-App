import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/eone_api.dart';
import '../../models/models.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../auth/session_controller.dart';

final dashboardProvider = FutureProvider.autoDispose<DashboardData>(
    (ref) => ref.read(eoneApiProvider).dashboard());
final activeClassroomProvider =
    StateNotifierProvider<ClassroomPreference, String?>((ref) =>
        ClassroomPreference(
            ref.watch(sessionProvider.select((s) => s.user?.id))));

class ClassroomPreference extends StateNotifier<String?> {
  ClassroomPreference(this.user) : super(null) {
    restore();
  }
  final String? user;
  final storage = const FlutterSecureStorage();
  bool changed = false;
  Future<void> restore() async {
    if (user == null) return;
    try {
      final saved = await storage.read(key: 'eone.classroom.$user');
      if (mounted && !changed) state = saved;
    } catch (_) {/* Use the first currently authorized classroom. */}
  }

  Future<void> choose(String? id) async {
    changed = true;
    state = id;
    if (user != null) {
      await storage.write(key: 'eone.classroom.$user', value: id);
    }
  }
}

bool hasRole(DashboardData data, String role,
        {String? classroomId, String? classSubjectId}) =>
    data.contexts.any((c) =>
        c.role == role &&
        (classroomId == null || c.classroomId == classroomId) &&
        (classSubjectId == null || c.classSubjectId == classSubjectId));
