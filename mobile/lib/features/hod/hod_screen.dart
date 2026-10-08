import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api_client.dart';
import '../../core/eone_api.dart';
import '../../models/models.dart';
import '../classrooms/classrooms_screen.dart';
import '../dashboard/providers.dart';
import 'audit_screen.dart';

class HodScreen extends ConsumerStatefulWidget {
  const HodScreen({super.key});
  @override
  ConsumerState<HodScreen> createState() => _HodScreenState();
}

class _HodScreenState extends ConsumerState<HodScreen> {
  int version = 0;
  @override
  Widget build(BuildContext context) {
    final async = ref.watch(dashboardProvider);
    return async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (d) {
          final dep = d.contexts
              .where((c) => c.role == 'HOD')
              .map((c) => c.departmentId)
              .whereType<String>()
              .firstOrNull;
          if (dep == null) {
            return const Center(
                child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Text(
                        'This account is staff, but it does not have an HOD role. Subject/classroom teaching actions remain available inside the assigned classrooms.')));
          }
          return _body(dep);
        });
  }

  Widget _body(String dep) => FutureBuilder<List<AppUser>>(
      key: ValueKey(version),
      future: ref.read(eoneApiProvider).pendingUsers(dep),
      builder: (context, snap) {
        final pending = snap.data ?? [];
        return ListView(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 110),
            children: [
              Text('Department management',
                  style: Theme.of(context)
                      .textTheme
                      .headlineSmall
                      ?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 4),
              const Text(
                  'HOD actions are department-scoped. Teaching permissions still require a separate subject-teacher assignment.'),
              const SizedBox(height: 18),
              ListTile(
                  leading: const Icon(Icons.history),
                  title: const Text('Audit history'),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(
                          builder: (_) => AuditScreen(department: dep)))),
              Text('Pending approvals (${pending.length})',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              if (snap.connectionState == ConnectionState.waiting)
                const LinearProgressIndicator()
              else if (pending.isEmpty)
                const Card(
                    child: Padding(
                        padding: EdgeInsets.all(16),
                        child: Text('No pending accounts.')))
              else
                ...pending.map((u) => Card(
                    child: ListTile(
                        title: Text(u.name,
                            style:
                                const TextStyle(fontWeight: FontWeight.w700)),
                        subtitle: Text('${u.email}\n${u.accountType}'),
                        isThreeLine: true,
                        trailing: FilledButton(
                            onPressed: () async {
                              await ref
                                  .read(eoneApiProvider)
                                  .approveUser(dep, u.id);
                              setState(() => version++);
                            },
                            child: const Text('Approve'))))),
              const SizedBox(height: 22),
              Text('Academic setup',
                  style: Theme.of(context)
                      .textTheme
                      .titleLarge
                      ?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 10),
              GridView.count(
                  crossAxisCount: 2,
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  mainAxisSpacing: 10,
                  crossAxisSpacing: 10,
                  childAspectRatio: 1.55,
                  children: [
                    _Action(Icons.account_tree_outlined, 'Program',
                        () => _addProgram(dep)),
                    _Action(Icons.calendar_month_outlined, 'Term',
                        () => _addTerm(dep)),
                    _Action(Icons.menu_book_outlined, 'Subject',
                        () => _addSubject(dep)),
                    _Action(Icons.meeting_room_outlined, 'Classroom',
                        () => _addClassroom(dep)),
                    _Action(
                        Icons.link, 'Link subject', () => _linkSubject(dep)),
                    _Action(Icons.person_add_alt_1, 'Enroll student',
                        () => _enroll(dep)),
                    _Action(Icons.badge_outlined, 'Assign role',
                        () => _assignRole(dep))
                  ])
            ]);
      });

  Future<void> _addProgram(String dep) async {
    final a = TextEditingController(), b = TextEditingController();
    if (await _two('Create program', 'Code', a, 'Name', b)) {
      await _run(
          () => ref.read(eoneApiProvider).createProgram(dep, a.text, b.text));
    }
  }

  Future<void> _addSubject(String dep) async {
    final a = TextEditingController(), b = TextEditingController();
    if (await _two('Create subject', 'Code', a, 'Name', b)) {
      await _run(
          () => ref.read(eoneApiProvider).createSubject(dep, a.text, b.text));
    }
  }

  Future<void> _addTerm(String dep) async {
    final name = TextEditingController(text: '2027-28');
    if (!mounted) return;
    final ok = await showDialog<bool>(
        context: context,
        builder: (c) => AlertDialog(
                title: const Text('Create academic term'),
                content: TextField(
                    controller: name,
                    decoration: const InputDecoration(labelText: 'Term name')),
                actions: [
                  TextButton(
                      onPressed: () => Navigator.pop(c, false),
                      child: const Text('Cancel')),
                  FilledButton(
                      onPressed: () => Navigator.pop(c, true),
                      child: const Text('Create'))
                ]));
    if (ok == true) {
      final now = DateTime.now();
      await _run(() => ref.read(eoneApiProvider).createTerm(dep,
          name: name.text,
          start: DateTime(now.year, 7, 1),
          end: DateTime(now.year + 1, 5, 31)));
    }
  }

  Future<void> _addClassroom(String dep) async {
    final api = ref.read(eoneApiProvider),
        programs = await api.hodPrograms(dep),
        terms = await api.hodTerms(dep);
    if (programs.isEmpty || terms.isEmpty) {
      _msg('Create a program and term first.');
      return;
    }
    String program = programs.first['id'].toString(),
        term = terms.first['id'].toString(),
        year = 'FY';
    final name = TextEditingController(text: 'FY CSE - A'),
        division = TextEditingController(text: 'A');
    if (!mounted) return;
    final ok = await showDialog<bool>(
        context: context,
        builder: (c) => StatefulBuilder(
            builder: (c, set) => AlertDialog(
                    title: const Text('Create classroom'),
                    content: SingleChildScrollView(
                        child:
                            Column(mainAxisSize: MainAxisSize.min, children: [
                      DropdownButtonFormField<String>(
                          initialValue: program,
                          items: programs
                              .map((p) => DropdownMenuItem(
                                  value: p['id'].toString(),
                                  child: Text('${p['code']} - ${p['name']}')))
                              .toList(),
                          onChanged: (v) => set(() => program = v!)),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                          initialValue: term,
                          items: terms
                              .map((t) => DropdownMenuItem(
                                  value: t['id'].toString(),
                                  child: Text(t['name'].toString())))
                              .toList(),
                          onChanged: (v) => set(() => term = v!)),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                          initialValue: year,
                          items: ['FY', 'SY', 'TY', 'FOURTH_YEAR', 'OTHER']
                              .map((y) =>
                                  DropdownMenuItem(value: y, child: Text(y)))
                              .toList(),
                          onChanged: (v) => set(() => year = v!)),
                      const SizedBox(height: 10),
                      TextField(
                          controller: division,
                          decoration:
                              const InputDecoration(labelText: 'Division')),
                      const SizedBox(height: 10),
                      TextField(
                          controller: name,
                          decoration:
                              const InputDecoration(labelText: 'Display name'))
                    ])),
                    actions: [
                      TextButton(
                          onPressed: () => Navigator.pop(c, false),
                          child: const Text('Cancel')),
                      FilledButton(
                          onPressed: () => Navigator.pop(c, true),
                          child: const Text('Create'))
                    ])));
    if (ok == true) {
      await _run(() => api.createClassroom(dep,
          programId: program,
          termId: term,
          yearLevel: year,
          division: division.text,
          name: name.text));
    }
  }

  Future<void> _linkSubject(String dep) async {
    final api = ref.read(eoneApiProvider),
        classes = await api.classrooms(),
        subjects = await api.hodSubjects(dep);
    if (classes.isEmpty || subjects.isEmpty) {
      _msg('Create a classroom and subject first.');
      return;
    }
    String c = classes.first.id, s = subjects.first['id'].toString();
    if (!mounted) return;
    final ok = await showDialog<bool>(
        context: context,
        builder: (x) => StatefulBuilder(
            builder: (x, set) => AlertDialog(
                    title: const Text('Link subject to classroom'),
                    content: Column(mainAxisSize: MainAxisSize.min, children: [
                      DropdownButtonFormField<String>(
                          initialValue: c,
                          items: classes
                              .map((v) => DropdownMenuItem(
                                  value: v.id, child: Text(v.name)))
                              .toList(),
                          onChanged: (v) => set(() => c = v!)),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                          initialValue: s,
                          items: subjects
                              .map((v) => DropdownMenuItem(
                                  value: v['id'].toString(),
                                  child: Text('${v['code']} - ${v['name']}')))
                              .toList(),
                          onChanged: (v) => set(() => s = v!))
                    ]),
                    actions: [
                      TextButton(
                          onPressed: () => Navigator.pop(x, false),
                          child: const Text('Cancel')),
                      FilledButton(
                          onPressed: () => Navigator.pop(x, true),
                          child: const Text('Link'))
                    ])));
    if (ok == true) await _run(() => api.linkSubject(dep, c, s));
  }

  Future<void> _enroll(String dep) async {
    final api = ref.read(eoneApiProvider),
        users = (await api.departmentUsers(dep))
            .where((u) => u.accountType == 'STUDENT' && u.status == 'ACTIVE')
            .toList(),
        classes = await api.classrooms();
    if (users.isEmpty || classes.isEmpty) {
      _msg('You need an active student and a classroom.');
      return;
    }
    String u = users.first.id, c = classes.first.id;
    if (!mounted) return;
    final ok = await showDialog<bool>(
        context: context,
        builder: (x) => StatefulBuilder(
            builder: (x, set) => AlertDialog(
                    title: const Text('Enroll student'),
                    content: Column(mainAxisSize: MainAxisSize.min, children: [
                      DropdownButtonFormField<String>(
                          initialValue: u,
                          items: users
                              .map((v) => DropdownMenuItem(
                                  value: v.id, child: Text(v.name)))
                              .toList(),
                          onChanged: (v) => set(() => u = v!)),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                          initialValue: c,
                          items: classes
                              .map((v) => DropdownMenuItem(
                                  value: v.id, child: Text(v.name)))
                              .toList(),
                          onChanged: (v) => set(() => c = v!))
                    ]),
                    actions: [
                      TextButton(
                          onPressed: () => Navigator.pop(x, false),
                          child: const Text('Cancel')),
                      FilledButton(
                          onPressed: () => Navigator.pop(x, true),
                          child: const Text('Enroll'))
                    ])));
    if (ok == true) await _run(() => api.enrollStudent(dep, u, c));
  }

  Future<void> _assignRole(String dep) async {
    final api = ref.read(eoneApiProvider),
        staff = (await api.departmentUsers(dep))
            .where((u) => u.accountType == 'STAFF' && u.status == 'ACTIVE')
            .toList(),
        classes = await api.classrooms();
    if (staff.isEmpty) {
      _msg('No active staff accounts.');
      return;
    }
    String u = staff.first.id,
        role = 'SUBJECT_TEACHER',
        classroom = classes.isEmpty ? '' : classes.first.id;
    List<SubjectItem> classSubjects =
        classroom.isEmpty ? [] : await api.subjects(classroom);
    String? cs =
        classSubjects.isEmpty ? null : classSubjects.first.classSubjectId;
    if (!mounted) return;
    final ok = await showDialog<bool>(
        context: context,
        builder: (x) => StatefulBuilder(
            builder: (x, set) => AlertDialog(
                    title: const Text('Assign responsibility'),
                    content: SingleChildScrollView(
                        child:
                            Column(mainAxisSize: MainAxisSize.min, children: [
                      DropdownButtonFormField<String>(
                          initialValue: u,
                          items: staff
                              .map((v) => DropdownMenuItem(
                                  value: v.id, child: Text(v.name)))
                              .toList(),
                          onChanged: (v) => set(() => u = v!)),
                      const SizedBox(height: 10),
                      DropdownButtonFormField<String>(
                          initialValue: role,
                          items: ['HOD', 'CLASS_TEACHER', 'SUBJECT_TEACHER']
                              .map((v) => DropdownMenuItem(
                                  value: v,
                                  child: Text(v.replaceAll('_', ' '))))
                              .toList(),
                          onChanged: (v) async {
                            role = v!;
                            set(() {});
                          }),
                      if (role != 'HOD' && classes.isNotEmpty) ...[
                        const SizedBox(height: 10),
                        DropdownButtonFormField<String>(
                            initialValue: classroom,
                            items: classes
                                .map((v) => DropdownMenuItem(
                                    value: v.id, child: Text(v.name)))
                                .toList(),
                            onChanged: (v) async {
                              classroom = v!;
                              classSubjects = await api.subjects(classroom);
                              cs = classSubjects.isEmpty
                                  ? null
                                  : classSubjects.first.classSubjectId;
                              set(() {});
                            })
                      ],
                      if (role == 'SUBJECT_TEACHER' &&
                          classSubjects.isNotEmpty) ...[
                        const SizedBox(height: 10),
                        DropdownButtonFormField<String>(
                            initialValue: cs,
                            items: classSubjects
                                .map((v) => DropdownMenuItem(
                                    value: v.classSubjectId,
                                    child: Text(v.name)))
                                .toList(),
                            onChanged: (v) => set(() => cs = v))
                      ]
                    ])),
                    actions: [
                      TextButton(
                          onPressed: () => Navigator.pop(x, false),
                          child: const Text('Cancel')),
                      FilledButton(
                          onPressed: () => Navigator.pop(x, true),
                          child: const Text('Assign'))
                    ])));
    if (ok == true) {
      if (role == 'CLASS_TEACHER' && classroom.isEmpty) {
        _msg('Create a classroom first.');
        return;
      }
      if (role == 'SUBJECT_TEACHER' && cs == null) {
        _msg('Link a subject to a classroom first.');
        return;
      }
      await _run(() => api.assignRole(dep,
          userId: u,
          role: role,
          classroomId: role == 'CLASS_TEACHER' ? classroom : null,
          classSubjectId: role == 'SUBJECT_TEACHER' ? cs : null));
    }
  }

  Future<bool> _two(String title, String la, TextEditingController a, String lb,
          TextEditingController b) async =>
      (await showDialog<bool>(
          context: context,
          builder: (c) => AlertDialog(
                  title: Text(title),
                  content: Column(mainAxisSize: MainAxisSize.min, children: [
                    TextField(
                        controller: a,
                        decoration: InputDecoration(labelText: la)),
                    const SizedBox(height: 10),
                    TextField(
                        controller: b,
                        decoration: InputDecoration(labelText: lb))
                  ]),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(c, false),
                        child: const Text('Cancel')),
                    FilledButton(
                        onPressed: () => Navigator.pop(c, true),
                        child: const Text('Create'))
                  ]))) ??
      false;
  Future<void> _run(Future<void> Function() work) async {
    try {
      await work();
      if (mounted) {
        setState(() => version++);
        ref.invalidate(classroomsProvider);
        _msg('Saved.');
      }
    } catch (e) {
      _msg(apiError(e));
    }
  }

  void _msg(String s) {
    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(s)));
    }
  }
}

class _Action extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  const _Action(this.icon, this.label, this.onTap);
  @override
  Widget build(BuildContext context) => Card(
      child: InkWell(
          borderRadius: BorderRadius.circular(18),
          onTap: onTap,
          child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(icon),
                    const SizedBox(height: 6),
                    Text(label,
                        textAlign: TextAlign.center,
                        style: const TextStyle(fontWeight: FontWeight.w700))
                  ]))));
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull => isEmpty ? null : first;
}
