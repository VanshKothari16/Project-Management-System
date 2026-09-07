package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.Employee;
import com.app.taskmanagement.entity.Projects;
import com.app.taskmanagement.entity.Task;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.enums.Role;
import com.app.taskmanagement.enums.Status;
import com.app.taskmanagement.repository.EmployeeRepository;
import com.app.taskmanagement.repository.ProjectRepository;
import com.app.taskmanagement.repository.TaskRepository;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.requestdto.ProjectRequestDto;
import com.app.taskmanagement.responsedto.EmployeeResponseDto;
import com.app.taskmanagement.responsedto.ProjectResponseDto;
import com.app.taskmanagement.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is the "brain" for everything to do with Projects: creating
 * them, editing them, appointing a Manager, and figuring out whether a
 * Project's overall status should flip to Completed.
 *
 * Think of a Project like a Classroom: it has one Class Teacher (the
 * Manager) and many Homework Assignments (Tasks) inside it. Only an Admin
 * (the Principal) is allowed to create or manage classrooms.
 */
@Service
@RequiredArgsConstructor
public class ProjectServices {
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    /**
     * Creates a brand new Project for the logged-in Admin's company, and
     * appoints one existing Employee as its Manager.
     *
     * Simple example: like a Principal opening a new classroom, picking one
     * teacher from the staff list, and handing them the keys.
     *
     * Steps: 1) find out who the logged-in admin is, 2) make sure they don't
     * already have a project with this exact name (no duplicate classrooms),
     * 3) make sure the deadline isn't already in the past, 4) find the
     * chosen employee and PROMOTE them to the Manager role, 5) save.
     */
    @Transactional
    public ProjectResponseDto addProjects(ProjectRequestDto dto){
        User admin=getCurrentUser();
        projectRepository.findByNameAndEmail(dto.getName(),admin.getEmail()).ifPresent(
                (e)->{
                    throw new RuntimeException("Duplicate Project Found!");
                }
        );
        if(dto.getDeadline().isBefore(LocalDate.now()))
            throw new RuntimeException("Deadline of Project must be after "+LocalDate.now());
        Projects projects= Mapper.toProjectEntity(dto);
        projects.setStatus(Status.TO_DO);

        Employee manager=employeeRepository.findById(dto.getManager_Id()).orElseThrow(
                ()->new RuntimeException("Enter Valid Manager Id!")
        );
        projects.setManager(manager);
        manager.setRole(Role.MANAGER);
        projects.setCompany(manager.getCompany());

        projects=projectRepository.save(projects);
        manager.setProject(projects);
        return Mapper.toProjectResponseDto(projects,manager);
    }

    /**
     * Updates a project's name/description/deadline - but ONLY if the
     * logged-in Admin is actually the owner of the company this project
     * belongs to.
     *
     * Simple example: a Principal can only rename/redecorate a classroom
     * that belongs to THEIR OWN school, not a school across town.
     *
     * IMPORTANT FIX: this comparison uses ".equals()" to compare the two
     * email addresses. Using "==" here (an old bug) compares whether they
     * are the literal same object in memory, which is almost always false
     * even for the exact same email, and would incorrectly block the real
     * owner from editing their own project.
     */
    @Transactional
    public ProjectResponseDto update(ProjectRequestDto dto,String id){
        User admin=getCurrentUser();
        Projects p1=projectRepository.findById(id).orElseThrow(
                ()->{throw new RuntimeException("Project doesn't exist");}
        );

        if(!p1.getCompany().getAdmin().getEmail().equals(admin.getEmail()))
            throw new RuntimeException("Company doesn't exist!");

        if(dto.getDeadline().isBefore(LocalDate.now())) throw new RuntimeException("Deadline of Project must be after "+LocalDate.now());

        p1.setName(dto.getName());
        p1.setDeadline(dto.getDeadline());
        p1.setDescription(dto.getDescription());

        projectRepository.flush();
        return Mapper.toProjectResponseDto(p1,p1.getManager());
    }

    /**
     * Fetches one Project by ID - but only if the logged-in Admin actually
     * owns it.
     *
     * Simple example: like asking "show me classroom 5B's details" - the
     * front office only shows it to the Principal of THAT school.
     */
    public ProjectResponseDto get(String id){
        Projects p1=projectRepository.findById(id).orElseThrow(
                ()->new RuntimeException("Invalid Project Id")
        );

        User admin=getCurrentUser();
        if(!p1.getCompany().getAdmin().getEmail().equals(admin.getEmail()))
            throw new RuntimeException("Project doesn't exist!");

        return Mapper.toProjectResponseDto(p1,p1.getManager());
    }

    /**
     * Deletes a Project - only if the logged-in Admin owns it.
     *
     * Simple example: only the Principal of a school can decide to shut
     * down one of ITS OWN classrooms.
     */
    @Transactional
    public void delete(String id){
        Projects p1=projectRepository.findById(id).orElseThrow(
                ()->new RuntimeException("Invalid Project Id")
        );

        User admin=getCurrentUser();
        if(!p1.getCompany().getAdmin().getEmail().equals(admin.getEmail()))
            throw new RuntimeException("Project doesn't exist!");

        projectRepository.delete(p1);
    }

    /**
     * Returns every Project that belongs to the logged-in Admin's company.
     *
     * Simple example: like the Principal's own list of every classroom in
     * their school - never another school's classrooms.
     */
    public List<ProjectResponseDto> listOfProjects(){
        User admin=getCurrentUser();
        return projectRepository.findAllByEmail(admin.getEmail()).stream().map(
                (e)->Mapper.toProjectResponseDto(e,e.getManager())
        ).toList();
    }

    /**
     * The Admin's drill-down: "who is the Manager running this project?"
     *
     * Simple example: like asking the front office "who's the teacher for
     * classroom 5B?" and getting back their full staff profile.
     */
    public EmployeeResponseDto getProjectManager(String projectId) {
        Projects project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project doesn't exist!"));
        verifyOwnership(project);
        return Mapper.toEmployeeResponseDto(project.getManager());
    }

    /**
     * The Admin's drill-down: "who are the employees working on this
     * project?" - the Manager themself is deliberately left out of this
     * list, since they're already shown separately as "the Manager".
     *
     * Simple example: the class register showing every STUDENT in 5B,
     * without the teacher's own name mixed into the student list.
     */
    public List<EmployeeResponseDto> getProjectEmployees(String projectId) {
        Projects project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project doesn't exist!"));
        verifyOwnership(project);
        return employeeRepository
                .findEmployeesInProjectExcludingManager(projectId, project.getManager().getId())
                .stream().map(Mapper::toEmployeeResponseDto).collect(Collectors.toList());
    }

    private void verifyOwnership(Projects project) {
        User admin = getCurrentUser();
        if (!project.getCompany().getAdmin().getEmail().equals(admin.getEmail()))
            throw new RuntimeException("Project doesn't exist!");
    }

    /**
     * A small helper that looks at "who is currently logged in" (from the
     * validated JWT) and fetches their full User record.
     */
    private User getCurrentUser(){
        UserDetails userDetails=(UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(userDetails.getUsername());
    }

    /**
     * Re-checks a Project's overall status based on the status of every
     * Task inside it. This is never called directly from a Controller - it
     * is silently triggered by TaskServices whenever a task's own status
     * changes (e.g. completed, or moved to In Progress).
     *
     * Simple example: the school report card for a whole classroom doesn't
     * need a teacher to manually mark "term finished" - the moment every
     * single homework assignment is done, the report card flips itself to
     * "Completed" automatically. If ANY assignment is still in progress,
     * the whole classroom shows as "In Progress" too.
     */
    public void changeStatus(String projectId){
        Projects p1=projectRepository.findById(projectId).orElseThrow(
                ()->new RuntimeException("Invalid Project Id")
        );

        List<Task> inProgress=taskRepository.findTaskByStatusAndProjects(Status.IN_PROGRESS,projectId);
        List<Task> toDo=taskRepository.findTaskByStatusAndProjects(Status.TO_DO,projectId);
        if(inProgress.isEmpty() && toDo.isEmpty()) {
            p1.setStatus(Status.COMPLETED);
            return;
        }

        if(!inProgress.isEmpty()) p1.setStatus(Status.IN_PROGRESS);
    }
}
