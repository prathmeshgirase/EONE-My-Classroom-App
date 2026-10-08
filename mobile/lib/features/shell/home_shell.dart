import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../auth/session_controller.dart';
import '../classrooms/classrooms_screen.dart';
import '../dashboard/dashboard_screen.dart';
import '../dashboard/providers.dart';
import '../hod/hod_screen.dart';
import '../notifications/notifications_screen.dart';
import '../profile/profile_screen.dart';

class HomeShell extends ConsumerStatefulWidget {
  const HomeShell({super.key});
  @override
  ConsumerState<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends ConsumerState<HomeShell> {
  int index = 0;
  @override
  Widget build(BuildContext context) {
    final user = ref.watch(sessionProvider).user!;
    final tabs = <Widget>[
      const DashboardScreen(),
      const ClassroomsScreen(),
      const NotificationsScreen()
    ];
    final items = <NavigationDestination>[
      const NavigationDestination(
          icon: Icon(Icons.home_outlined),
          selectedIcon: Icon(Icons.home),
          label: 'Home'),
      const NavigationDestination(
          icon: Icon(Icons.meeting_room_outlined),
          selectedIcon: Icon(Icons.meeting_room),
          label: 'Classes'),
      const NavigationDestination(
          icon: Icon(Icons.notifications_none),
          selectedIcon: Icon(Icons.notifications),
          label: 'Alerts')
    ];
    final isHod = ref
            .watch(dashboardProvider)
            .valueOrNull
            ?.contexts
            .any((context) => context.role == 'HOD') ??
        false;
    if (isHod) {
      tabs.add(const HodScreen());
      items.add(const NavigationDestination(
          icon: Icon(Icons.admin_panel_settings_outlined),
          selectedIcon: Icon(Icons.admin_panel_settings),
          label: 'Manage'));
    }
    tabs.add(const ProfileScreen());
    items.add(const NavigationDestination(
        icon: Icon(Icons.person_outline),
        selectedIcon: Icon(Icons.person),
        label: 'Profile'));
    if (index >= tabs.length) index = 0;
    return Scaffold(
        appBar: AppBar(
            title: const Text('EONE',
                style: TextStyle(fontWeight: FontWeight.w900)),
            actions: [
              Padding(
                  padding: const EdgeInsets.only(right: 12),
                  child: CircleAvatar(
                      child:
                          Text(user.firstName.substring(0, 1).toUpperCase())))
            ]),
        body: LayoutBuilder(builder: (context, constraints) {
          final content = IndexedStack(index: index, children: tabs);
          if (constraints.maxWidth < 760) return content;
          return Row(children: [
            NavigationRail(
                selectedIndex: index,
                labelType: NavigationRailLabelType.all,
                onDestinationSelected: (i) => setState(() => index = i),
                destinations: items
                    .map((item) => NavigationRailDestination(
                        icon: item.icon,
                        selectedIcon: item.selectedIcon,
                        label: Text(item.label)))
                    .toList()),
            const VerticalDivider(width: 1),
            Expanded(
                child: Center(
                    child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 1200),
                        child: content))),
          ]);
        }),
        bottomNavigationBar: MediaQuery.sizeOf(context).width >= 760
            ? null
            : NavigationBar(
                selectedIndex: index,
                onDestinationSelected: (i) => setState(() => index = i),
                destinations: items));
  }
}
