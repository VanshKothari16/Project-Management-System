package com.app.taskmanagement.controller;

import com.app.taskmanagement.requestdto.CommitmentRequestDto;
import com.app.taskmanagement.responsedto.CommitmentResponseDto;
import com.app.taskmanagement.services.CommitmentServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Handles the progress-note "diary entries" (Commitments) an Employee
 * writes on a Task. Reading the activity log is also opened up to Admin
 * (read-only) so the Admin Dashboard's drill-down can show it too.
 */
@RestController
@RequestMapping("api/task_management/commitments")
@RequiredArgsConstructor
@Validated
public class CommitmentController {
    private final CommitmentServices commitmentServices;

    /** Adds a new progress note on a task. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @PostMapping
    public ResponseEntity<CommitmentResponseDto> addCommits(@Valid @RequestBody CommitmentRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                commitmentServices.addCommits(dto)
        );
    }

    /** Edits the text of an existing commitment. */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @PutMapping
    public ResponseEntity<CommitmentResponseDto> updateCommits(@Valid @RequestBody CommitmentRequestDto dto, @RequestParam String id) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                commitmentServices.editCommits(dto, id)
        );
    }

    /**
     * Returns one PAGE of commitments for a task (not all at once).
     * Widened to also allow ROLE_ADMIN (read-only) for the Admin
     * Dashboard's task drill-down.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER','ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<CommitmentResponseDto>> getAllCommitsOfTask(
            @RequestParam String taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {

        return ResponseEntity.ok(
                commitmentServices.getAllCommitmentsOfTask(taskId, page, size)
        );
    }


    /**
     * Deletes a commitment. This now actually calls the service - the
     * original version returned a "Successfully Deleted!" message without
     * ever touching the database, so nothing was really deleted.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_EMPLOYEE','ROLE_MANAGER')")
    @DeleteMapping
    public ResponseEntity<?> deleteCommits(@RequestParam String commitId) {
        commitmentServices.deleteCommits(commitId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                "Successfully Deleted!"
        );
    }
}
