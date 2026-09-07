package com.app.taskmanagement.controller;

import com.app.taskmanagement.enums.Status;
import com.app.taskmanagement.requestdto.ProjectRequestDto;
import com.app.taskmanagement.responsedto.AdminDashboardDto;
import com.app.taskmanagement.responsedto.EmployeeResponseDto;
import com.app.taskmanagement.responsedto.ProjectResponseDto;
import com.app.taskmanagement.responsedto.TaskResponseDto;
import com.app.taskmanagement.services.ProjectServices;
import com.app.taskmanagement.services.TaskServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything an Admin can do with Projects: create one, edit it, delete it,
 * list all of them, and drill down into one project's team/task snapshot.
 * Every single endpoint here is Admin-only.
 */
@RestController
@RequestMapping("/api/task_management/project")
@RequiredArgsConstructor
@Validated
public class ProjectController {
    private final ProjectServices projectServices;
    private final TaskServices taskServices;

    /** Creates a new project and appoints one existing employee as its Manager. */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProjectResponseDto> addProjects(@Valid @RequestBody ProjectRequestDto dto){
        return ResponseEntity.status(HttpStatus.CREATED).body(projectServices.addProjects(dto));
    }

    /** Edits a project you own. */
    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProjectResponseDto> updateProject(@Valid @RequestBody ProjectRequestDto dto, @RequestParam String id){
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                projectServices.update(dto,id)
        );
    }

    /** Fetches one project's basic details. */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<ProjectResponseDto> get(@RequestParam("id") String id){
        return ResponseEntity.ok(projectServices.get(id));
    }

    /** Deletes a project you own. */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestParam("id") String id){
        projectServices.delete(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Successfully deleted!");
    }

    /** Lists every project belonging to your company. */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProjectResponseDto>> getAll(){
        return ResponseEntity.ok(projectServices.listOfProjects());
    }

    /**
     * The Admin Dashboard's "report card" for ONE project: manager/employee
     * counts and to-do/in-progress/completed/overdue task counts.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<AdminDashboardDto> getAdminDashboard(@PathVariable("id") String projectId) {
        return ResponseEntity.ok(taskServices.getProjectDashboard(projectId));
    }

    /**
     * Drill-down: the actual task list for one status column (TO_DO,
     * IN_PROGRESS, COMPLETED, or OVERDUE) inside one project.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskResponseDto>> getProjectTasks(
            @PathVariable("id") String projectId, @RequestParam String status) {
        List<TaskResponseDto> tasks = "OVERDUE".equalsIgnoreCase(status)
                ? taskServices.getProjectOverdueTasks(projectId)
                : taskServices.getProjectTasksByStatus(projectId, Status.valueOf(status.toUpperCase()));
        return ResponseEntity.ok(tasks);
    }

    /** Drill-down: who is the Manager running this project. */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}/manager")
    public ResponseEntity<EmployeeResponseDto> getProjectManager(@PathVariable("id") String projectId) {
        return ResponseEntity.ok(projectServices.getProjectManager(projectId));
    }

    /** Drill-down: every employee on this project (manager excluded). */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/{id}/employees")
    public ResponseEntity<List<EmployeeResponseDto>> getProjectEmployees(@PathVariable("id") String projectId) {
        return ResponseEntity.ok(projectServices.getProjectEmployees(projectId));
    }
}
