package com.eone.config;

import com.eone.domain.*;
import com.eone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="eone.seed.enabled", havingValue="true")
public class DevSeedConfig implements CommandLineRunner {
    private final DepartmentRepository departments;
    private final ProgramRepository programs;
    private final AcademicTermRepository terms;
    private final ClassroomRepository classrooms;
    private final SubjectRepository subjects;
    private final ClassSubjectRepository classSubjects;
    private final UserRepository users;
    private final RoleAssignmentRepository roles;
    private final EnrollmentRepository enrollments;
    private final ChapterRepository chapters;
    private final LearningMaterialRepository materials;
    private final AssignmentRepository assignments;
    private final AnnouncementRepository announcements;
    private final NotificationRepository notifications;
    private final PasswordEncoder passwords;

    @Override @Transactional
    public void run(String... args) {
        if (departments.count() > 0) return;

        Department cse = departments.save(new Department("CSE", "Computer Science & Engineering"));
        Program btech = programs.save(new Program(cse, "BTECH-CS", "B.Tech Computer Science"));
        AcademicTerm term = terms.save(new AcademicTerm("2026-27", LocalDate.of(2026,7,1), LocalDate.of(2027,5,31), true));

        Classroom fy = classroom(btech,term,YearLevel.FY,"A","FY CSE - A");
        Classroom sy = classroom(btech,term,YearLevel.SY,"A","SY CSE - A");
        Classroom ty = classroom(btech,term,YearLevel.TY,"A","TY CSE - A");

        Subject dsa = subject(cse,"DSA","Data Structures & Algorithms");
        Subject dbms = subject(cse,"DBMS","Database Management Systems");
        Subject web = subject(cse,"WEB","Web Engineering");

        ClassSubject syDsa = link(sy,dsa);
        ClassSubject syDbms = link(sy,dbms);
        ClassSubject fyWeb = link(fy,web);
        ClassSubject tyDsa = link(ty,dsa);

        User hod = user(cse,"hod@eone.dev","Anita","Patil",AccountType.STAFF);
        User teacher = user(cse,"teacher@eone.dev","Rohan","Shah",AccountType.STAFF);
        User student = user(cse,"student@eone.dev","Aarav","Kulkarni",AccountType.STUDENT);
        User pending = user(cse,"pending@eone.dev","Neha","Joshi",AccountType.STUDENT); pending.setStatus(UserStatus.PENDING);

        role(hod,ScopedRole.HOD,cse,null,null);
        role(hod,ScopedRole.CLASS_TEACHER,null,sy,null);
        role(hod,ScopedRole.SUBJECT_TEACHER,null,null,syDsa);
        role(teacher,ScopedRole.SUBJECT_TEACHER,null,null,syDbms);
        role(teacher,ScopedRole.SUBJECT_TEACHER,null,null,fyWeb);
        role(teacher,ScopedRole.SUBJECT_TEACHER,null,null,tyDsa);

        Enrollment e = new Enrollment();e.setStudent(student);e.setClassroom(sy);e.setStatus(EnrollmentStatus.ACTIVE);enrollments.save(e);

        Chapter ch1 = new Chapter();ch1.setClassSubject(syDsa);ch1.setTitle("Arrays and Complexity");ch1.setPosition(1);ch1=chapters.save(ch1);
        LearningMaterial note = new LearningMaterial();note.setChapter(ch1);note.setTitle("Big-O Quick Notes");note.setType(MaterialType.NOTE);note.setUrl("https://example.com/eone-demo/big-o-notes.pdf");note.setDescription("Demo material URL; replace it with your object-storage file URL.");note.setCreatedBy(hod);materials.save(note);

        Assignment a = new Assignment();a.setClassSubject(syDsa);a.setTitle("Array Rotation");a.setDescription("Implement left and right rotation and explain time/space complexity.");a.setDueAt(Instant.now().plus(Duration.ofDays(7)));a.setMaxMarks(20);a.setPublished(true);a.setCreatedBy(hod);a=assignments.save(a);

        Announcement announcement = new Announcement();announcement.setTitle("Welcome to EONE");announcement.setBody("Your SY classroom is active. Check DSA for the first assignment.");announcement.setTargetType(TargetType.CLASSROOM);announcement.setClassroom(sy);announcement.setCreatedBy(hod);announcements.save(announcement);

        Notification n = new Notification();n.setUser(student);n.setTitle("Welcome to EONE");n.setBody("Your classroom and subjects are ready.");n.setType(NotificationType.SYSTEM);notifications.save(n);
    }

    private Classroom classroom(Program p, AcademicTerm t, YearLevel y, String d, String name){Classroom c=new Classroom();c.setProgram(p);c.setAcademicTerm(t);c.setYearLevel(y);c.setDivision(d);c.setName(name);return classrooms.save(c);}
    private Subject subject(Department d,String code,String name){Subject s=new Subject();s.setDepartment(d);s.setCode(code);s.setName(name);return subjects.save(s);}
    private ClassSubject link(Classroom c,Subject s){ClassSubject cs=new ClassSubject();cs.setClassroom(c);cs.setSubject(s);return classSubjects.save(cs);}
    private User user(Department d,String email,String first,String last,AccountType type){User u=new User();u.setDepartment(d);u.setEmail(email);u.setPasswordHash(passwords.encode("Eone@123"));u.setFirstName(first);u.setLastName(last);u.setAccountType(type);u.setStatus(UserStatus.ACTIVE);return users.save(u);}
    private void role(User u,ScopedRole role,Department d,Classroom c,ClassSubject cs){RoleAssignment r=new RoleAssignment();r.setUser(u);r.setRole(role);r.setDepartment(d);r.setClassroom(c);r.setClassSubject(cs);r.setActive(true);roles.save(r);}
}
