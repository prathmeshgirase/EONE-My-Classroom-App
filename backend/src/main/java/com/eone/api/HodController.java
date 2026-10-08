package com.eone.api;

import com.eone.domain.*;
import com.eone.repository.*;
import com.eone.security.CurrentUser;
import com.eone.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController @RequestMapping("/api/v1/hod/departments/{departmentId}") @RequiredArgsConstructor @Transactional
public class HodController {
    private final AccessService access;
    private final UserRepository users;
    private final DepartmentRepository departments;
    private final ProgramRepository programs;
    private final AcademicTermRepository terms;
    private final ClassroomRepository classrooms;
    private final SubjectRepository subjects;
    private final ClassSubjectRepository classSubjects;
    private final RoleAssignmentRepository roles;
    private final EnrollmentRepository enrollments;
    private final AuditService audit;


    @GetMapping("/programs")
    public List<Map<String,Object>> programs(@PathVariable UUID departmentId,Authentication auth){
        require(auth,departmentId);return programs.findByDepartmentId(departmentId).stream().map(p->Map.<String,Object>of("id",p.getId(),"code",p.getCode(),"name",p.getName())).toList();
    }

    @GetMapping("/terms")
    public List<Map<String,Object>> terms(@PathVariable UUID departmentId,Authentication auth){
        require(auth,departmentId);return terms.findAll().stream().map(t->Map.<String,Object>of("id",t.getId(),"name",t.getName(),"startDate",t.getStartDate(),"endDate",t.getEndDate(),"active",t.isActive())).toList();
    }

    @GetMapping("/subjects")
    public List<Map<String,Object>> subjects(@PathVariable UUID departmentId,Authentication auth){
        require(auth,departmentId);return subjects.findByDepartmentId(departmentId).stream().map(s->Map.<String,Object>of("id",s.getId(),"code",s.getCode(),"name",s.getName())).toList();
    }

    @GetMapping("/classrooms")
    public List<Dtos.ClassroomView> classrooms(@PathVariable UUID departmentId,Authentication auth){
        require(auth,departmentId);return classrooms.findByProgramDepartmentId(departmentId).stream().map(DtoMapper::classroom).toList();
    }

    @GetMapping("/pending-users")
    public List<Dtos.UserView> pending(@PathVariable UUID departmentId, Authentication auth){
        require(auth,departmentId);
        return users.findByDepartmentIdAndStatus(departmentId,UserStatus.PENDING).stream().map(DtoMapper::user).toList();
    }

    @PatchMapping("/users/{userId}/approve")
    public Dtos.UserView approve(@PathVariable UUID departmentId,@PathVariable UUID userId,Authentication auth){
        UUID actor=require(auth,departmentId); User u=userInDepartment(userId,departmentId);u.setStatus(UserStatus.ACTIVE);
        audit.record(departmentId,actor,"APPROVE_USER","User",u.getId(),u.getEmail()); return DtoMapper.user(u);
    }

    @PatchMapping("/users/{userId}/suspend")
    public Dtos.UserView suspend(@PathVariable UUID departmentId,@PathVariable UUID userId,Authentication auth){
        UUID actor=require(auth,departmentId); User u=userInDepartment(userId,departmentId);u.setStatus(UserStatus.SUSPENDED);
        audit.record(departmentId,actor,"SUSPEND_USER","User",u.getId(),u.getEmail()); return DtoMapper.user(u);
    }

    @GetMapping("/users")
    public List<Dtos.UserView> users(@PathVariable UUID departmentId,Authentication auth){
        require(auth,departmentId); return users.findByDepartmentId(departmentId).stream().map(DtoMapper::user).toList();
    }

    @PostMapping("/programs") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createProgram(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.ProgramCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId); Program p=new Program(departments.getReferenceById(departmentId),req.code().trim().toUpperCase(),req.name().trim());p=programs.save(p);
        audit.record(departmentId,actor,"CREATE_PROGRAM","Program",p.getId(),p.getCode()); return Map.of("id",p.getId(),"code",p.getCode(),"name",p.getName());
    }

    @PostMapping("/terms") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createTerm(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.TermCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId); if(!req.endDate().isAfter(req.startDate())) throw new ApiException(HttpStatus.BAD_REQUEST,"Term end date must be after start date");
        AcademicTerm t=new AcademicTerm(req.name(),req.startDate(),req.endDate(),req.active());t=terms.save(t);audit.record(departmentId,actor,"CREATE_TERM","AcademicTerm",t.getId(),t.getName());return Map.of("id",t.getId(),"name",t.getName());
    }

    @PostMapping("/classrooms") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.ClassroomView createClassroom(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.ClassroomCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId);Program p=programs.findById(req.programId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Program not found"));
        if(!p.getDepartment().getId().equals(departmentId)) throw new ApiException(HttpStatus.BAD_REQUEST,"Program belongs to another department");
        AcademicTerm term=terms.findById(req.termId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Term not found"));
        Classroom c=new Classroom();c.setProgram(p);c.setAcademicTerm(term);c.setYearLevel(req.yearLevel());c.setDivision(req.division().trim());c.setName(req.name().trim());c=classrooms.save(c);
        audit.record(departmentId,actor,"CREATE_CLASSROOM","Classroom",c.getId(),c.getName());return DtoMapper.classroom(c);
    }

    @PostMapping("/subjects") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> createSubject(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.SubjectCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId);Subject s=new Subject();s.setDepartment(departments.getReferenceById(departmentId));s.setCode(req.code().trim().toUpperCase());s.setName(req.name().trim());s=subjects.save(s);
        audit.record(departmentId,actor,"CREATE_SUBJECT","Subject",s.getId(),s.getCode());return Map.of("id",s.getId(),"code",s.getCode(),"name",s.getName());
    }

    @PostMapping("/class-subjects") @ResponseStatus(HttpStatus.CREATED)
    public Dtos.SubjectView linkSubject(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.ClassSubjectCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId);Classroom c=classroomInDepartment(req.classroomId(),departmentId);Subject s=subjects.findById(req.subjectId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Subject not found"));
        if(!s.getDepartment().getId().equals(departmentId)) throw new ApiException(HttpStatus.BAD_REQUEST,"Subject belongs to another department");
        if(classSubjects.findByClassroomIdAndSubjectId(c.getId(),s.getId()).isPresent()) throw new ApiException(HttpStatus.CONFLICT,"Subject already linked to classroom");
        ClassSubject cs=new ClassSubject();cs.setClassroom(c);cs.setSubject(s);cs=classSubjects.save(cs);audit.record(departmentId,actor,"LINK_SUBJECT","ClassSubject",cs.getId(),s.getCode());return DtoMapper.subject(cs);
    }

    @PostMapping("/enrollments") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> enroll(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.EnrollmentCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId);User student=userInDepartment(req.studentId(),departmentId);if(student.getAccountType()!=AccountType.STUDENT) throw new ApiException(HttpStatus.BAD_REQUEST,"Only student accounts can be enrolled");
        Classroom c=classroomInDepartment(req.classroomId(),departmentId);Enrollment e=enrollments.findByStudentIdAndClassroomId(student.getId(),c.getId()).orElseGet(Enrollment::new);e.setStudent(student);e.setClassroom(c);e.setStatus(EnrollmentStatus.ACTIVE);e=enrollments.save(e);
        audit.record(departmentId,actor,"ENROLL_STUDENT","Enrollment",e.getId(),c.getName());return Map.of("id",e.getId(),"studentId",student.getId(),"classroomId",c.getId());
    }

    @PostMapping("/roles") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> assignRole(@PathVariable UUID departmentId,@Valid @RequestBody Dtos.RoleCreateRequest req,Authentication auth){
        UUID actor=require(auth,departmentId);User user=userInDepartment(req.userId(),departmentId);if(user.getAccountType()!=AccountType.STAFF) throw new ApiException(HttpStatus.BAD_REQUEST,"Scoped staff roles require a STAFF account");
        RoleAssignment r=new RoleAssignment();r.setUser(user);r.setRole(req.role());r.setActive(true);
        switch(req.role()){
            case HOD -> {
                UUID target=req.departmentId()==null?departmentId:req.departmentId(); if(!target.equals(departmentId)) throw new ApiException(HttpStatus.BAD_REQUEST,"Cannot assign HOD outside your department");
                if(roles.existsByUserIdAndRoleAndDepartmentIdAndActiveTrue(user.getId(),ScopedRole.HOD,target)) throw new ApiException(HttpStatus.CONFLICT,"HOD role already assigned");
                r.setDepartment(departments.getReferenceById(target));
            }
            case CLASS_TEACHER -> {
                if(req.classroomId()==null) throw new ApiException(HttpStatus.BAD_REQUEST,"classroomId is required");Classroom c=classroomInDepartment(req.classroomId(),departmentId);
                if(roles.existsByUserIdAndRoleAndClassroomIdAndActiveTrue(user.getId(),ScopedRole.CLASS_TEACHER,c.getId())) throw new ApiException(HttpStatus.CONFLICT,"Class teacher role already assigned");r.setClassroom(c);
            }
            case SUBJECT_TEACHER -> {
                if(req.classSubjectId()==null) throw new ApiException(HttpStatus.BAD_REQUEST,"classSubjectId is required");ClassSubject cs=classSubjects.findById(req.classSubjectId()).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Class subject not found"));
                if(!cs.getClassroom().getProgram().getDepartment().getId().equals(departmentId)) throw new ApiException(HttpStatus.BAD_REQUEST,"Class subject belongs to another department");
                if(roles.existsByUserIdAndRoleAndClassSubjectIdAndActiveTrue(user.getId(),ScopedRole.SUBJECT_TEACHER,cs.getId())) throw new ApiException(HttpStatus.CONFLICT,"Subject teacher role already assigned");r.setClassSubject(cs);
            }
        }
        r=roles.save(r);audit.record(departmentId,actor,"ASSIGN_ROLE","RoleAssignment",r.getId(),req.role().name()+" to "+user.getEmail());return Map.of("id",r.getId(),"role",r.getRole().name(),"userId",user.getId());
    }

    @DeleteMapping("/roles/{roleId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateRole(@PathVariable UUID departmentId,@PathVariable UUID roleId,Authentication auth){
        UUID actor=require(auth,departmentId);RoleAssignment r=roles.findById(roleId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Role assignment not found"));
        UUID roleDepartment=departmentFor(r);if(!departmentId.equals(roleDepartment)) throw new ApiException(HttpStatus.BAD_REQUEST,"Role belongs to another department");r.setActive(false);audit.record(departmentId,actor,"DEACTIVATE_ROLE","RoleAssignment",r.getId(),r.getRole().name());
    }

    private UUID require(Authentication auth,UUID departmentId){UUID actor=CurrentUser.id(auth);access.requireHod(actor,departmentId);return actor;}
    private User userInDepartment(UUID userId,UUID departmentId){User u=users.findById(userId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"User not found"));if(u.getDepartment()==null||!u.getDepartment().getId().equals(departmentId))throw new ApiException(HttpStatus.BAD_REQUEST,"User belongs to another department");return u;}
    private Classroom classroomInDepartment(UUID classroomId,UUID departmentId){Classroom c=classrooms.findById(classroomId).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Classroom not found"));if(!c.getProgram().getDepartment().getId().equals(departmentId))throw new ApiException(HttpStatus.BAD_REQUEST,"Classroom belongs to another department");return c;}
    private UUID departmentFor(RoleAssignment r){if(r.getDepartment()!=null)return r.getDepartment().getId();if(r.getClassroom()!=null)return r.getClassroom().getProgram().getDepartment().getId();if(r.getClassSubject()!=null)return r.getClassSubject().getClassroom().getProgram().getDepartment().getId();return null;}
}
