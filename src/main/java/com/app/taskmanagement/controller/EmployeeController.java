package com.app.taskmanagement.controller;

import com.app.taskmanagement.requestdto.EmployeeRequestDto;
import com.app.taskmanagement.responsedto.EmployeeResponseDto;
import com.app.taskmanagement.services.EmployeeServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Handles everything about an Employee's own account: joining a company,
 * viewing/editing your own profile, and (for a Manager) finding teammates
 * to assign a task to.
 */
@RestController
@RequestMapping("/api/task_management/employee")
@RequiredArgsConstructor
@Validated
public class EmployeeController {
    private final EmployeeServices services;

    /** Public - anyone with a valid Company ID can register as an Employee. */
    @PostMapping
    public ResponseEntity<EmployeeResponseDto> register(@Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(services.addEmployee(dto));
    }

    /**
     * Updates your OWN profile. Open to every logged-in role - including a
     * plain Employee - since everyone should be able to edit their own
     * details, not just Managers/Admins.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    @PutMapping
    public ResponseEntity<EmployeeResponseDto> update(@Valid @RequestBody EmployeeRequestDto dto) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(services.update(dto));
    }

    /** Returns your OWN profile - open to every logged-in role. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<EmployeeResponseDto> get() {
        return ResponseEntity.ok(services.getEmployee());
    }

    /**
     * Used by a Manager while assigning a task: returns emails of teammates
     * who are NOT already assigned to the given task.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_MANAGER','ROLE_ADMIN')")
    @GetMapping("/emails")
    public ResponseEntity<List<String>> getAllEmployeeEmails(@RequestParam String taskId){
        return ResponseEntity.ok(services.getAllEmployeeEmails(taskId));
    }
}
