package com.app.taskmanagement.controller;

import com.app.taskmanagement.enums.Status;
import com.app.taskmanagement.requestdto.TaskRequestDto;
import com.app.taskmanagement.responsedto.ManagerTaskResponseDto;
import com.app.taskmanagement.responsedto.TaskResponseDto;
import com.app.taskmanagement.services.TaskServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything about Tasks: a Manager's CRUD operations on their own
 * project's tasks, assigning tasks to employees, marking tasks complete,
 * and every dashboard board (Manager, Admin, Employee).
 */
@RestController
@RequestMapping("/api/task_management/task")
@RequiredArgsConstructor
public class TaskController {
    private final TaskServices taskServices;

    /** Creates a new task inside the logged-in Manager's own project. */
    @PreAuthorize("hasAuthority('CRUD_TASK')")
    @PostMapping
    public ResponseEntity<TaskResponseDto> addTask(@Valid @RequestBody TaskRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskServices.addTask(dto));
    }

    /** Assigns one task to one employee. */
    @PreAuthorize("hasAuthority('CRUD_TASK')")
    @PostMapping("/assign")
    public ResponseEntity<String> assignTaskToEmployee(@RequestParam String task, @RequestParam String email) {
        return ResponseEntity.ok(taskServices.assignTaskToEmployee(task, email));
    }

    /**
     * Fetches one task's full details. Widened to also allow ROLE_ADMIN
     * (read-only) so the Admin Dashboard's drill-down can show task detail
     * + activity log, not just Managers viewing their own tasks.
     */
    @PreAuthorize("hasAnyAuthority('CRUD_TASK','ROLE_ADMIN','ROLE_EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDto> getTask(@PathVariable("id") String id) {
        return ResponseEntity.ok(taskServices.getTask(id));
    }


    /** Every task in the logged-in Manager's own project. */
    @PreAuthorize("hasAuthority('CRUD_TASK')")
    @GetMapping("/all")
    public ResponseEntity<List<ManagerTaskResponseDto>> getAllTask() {
        return ResponseEntity.ok(taskServices.findAll());
    }

    /** Edits a task in the logged-in Manager's own project. */
    @PreAuthorize("hasAuthority('CRUD_TASK')")
    @PutMapping
    public ResponseEntity<TaskResponseDto> updateTask(@Valid @RequestBody TaskRequestDto dto, @RequestParam String id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(taskServices.updateTask(dto, id));
    }

    /** Deletes a task in the logged-in Manager's own project. */
    @PreAuthorize("hasAuthority('CRUD_TASK')")
    @DeleteMapping
    public ResponseEntity<?> deleteTask(@RequestParam String id) {
        taskServices.deleteTask(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                "Successfully deleted!"
        );
    }

    /** Marks a task Completed - Manager only. */
    @PreAuthorize("hasAuthority('ROLE_MANAGER')")
    @PostMapping("/{id}")
    public ResponseEntity<?> markTaskCompleted(@PathVariable("id") String id) {
        taskServices.markItAsCompleted(id);
        return ResponseEntity.ok("Task Status changed to COMPLETED!");
    }

    //*********** Manager and ADMIN Dashboard (the logged-in manager's OWN project) **************

    /** Overdue tasks in the logged-in manager's own project. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/overdue")
    public ResponseEntity<List<ManagerTaskResponseDto>> getAllOverDueTask() {
        return ResponseEntity.ok(taskServices.findAllOverdueTask());
    }

    /** To-Do tasks in the logged-in manager's own project. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/todo")
    public ResponseEntity<List<ManagerTaskResponseDto>> getAllToDoTask() {
        return ResponseEntity.ok(taskServices.findAllTaskByStatus(Status.TO_DO));
    }

    /** In-Progress tasks in the logged-in manager's own project. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/inprogress")
    public ResponseEntity<List<ManagerTaskResponseDto>> getAllInProgressTask() {
        return ResponseEntity.ok(taskServices.findAllTaskByStatus(Status.IN_PROGRESS));
    }

    /** Completed tasks in the logged-in manager's own project. */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_MANAGER')")
    @GetMapping("/completed")
    public ResponseEntity<List<ManagerTaskResponseDto>> getAllCompletedTask() {
        return ResponseEntity.ok(taskServices.findAllTaskByStatus(Status.COMPLETED));
    }

    // NOTE: this used to incorrectly share the exact same "/completed" path as the
    // method above, which crashed the whole application on startup (Spring cannot
    // map two different methods to one identical route). It now lives at its own
    // "/dashboard" path instead - see backend.md Bug #1 for the full story.

    //******** Only for Employee Dashboard (tasks assigned TO the logged-in person) *********

    /** Every task assigned to the logged-in employee (or manager). */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @GetMapping("/employee/all")
    public ResponseEntity<List<TaskResponseDto>> getAllTaskOfEmployee() {
        return ResponseEntity.ok(taskServices.getTaskOfEmployee());
    }

    /** The logged-in employee's own To-Do tasks. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @GetMapping("/employee/todo")
    public ResponseEntity<List<TaskResponseDto>> getToDoTaskOfEmployee() {
        return ResponseEntity.ok(taskServices.getTaskOfEmployeeByStatus(Status.TO_DO));
    }

    /** The logged-in employee's own In-Progress tasks. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @GetMapping("/employee/inprogress")
    public ResponseEntity<List<TaskResponseDto>> getInProgressTaskOfEmployee() {
        return ResponseEntity.ok(taskServices.getTaskOfEmployeeByStatus(Status.IN_PROGRESS));
    }

    /** The logged-in employee's own Completed tasks. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @GetMapping("/employee/completed")
    public ResponseEntity<List<TaskResponseDto>> getCompletedTaskOfEmployee() {
        return ResponseEntity.ok(taskServices.getTaskOfEmployeeByStatus(Status.COMPLETED));
    }

}
