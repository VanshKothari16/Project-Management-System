package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.Commitments;
import com.app.taskmanagement.entity.Employee;
import com.app.taskmanagement.entity.Task;
import com.app.taskmanagement.repository.CommitmentsRepository;
import com.app.taskmanagement.repository.EmployeeRepository;
import com.app.taskmanagement.repository.TaskRepository;
import com.app.taskmanagement.requestdto.CommitmentRequestDto;
import com.app.taskmanagement.responsedto.CommitmentResponseDto;
import com.app.taskmanagement.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is the "brain" for Commitments - the little diary entries an
 * Employee writes on a Task to show progress, like "Today I finished the
 * login page."
 *
 * Writing the very FIRST commitment on a task also automatically flips that
 * task from "To-Do" to "In Progress" - nobody has to flip a switch by hand.
 */
@Service
@RequiredArgsConstructor
public class CommitmentServices {
    private final CommitmentsRepository commitmentsRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskServices taskServices;

    /**
     * Saves a new progress note on a Task, written by the logged-in
     * Employee, and then tells TaskServices to check whether this task
     * should move from To-Do into In Progress.
     *
     * Simple example: a student writing "I started my homework" in their
     * diary - the moment that first entry is written, the homework's status
     * on the class board automatically flips from "not started" to
     * "in progress," without the student or teacher doing anything else.
     */
    @Transactional
    public CommitmentResponseDto addCommits(CommitmentRequestDto commitmentRequestDto) {
        Employee employee = getCurrentUser();
        Task t1 = taskRepository.findById(commitmentRequestDto.getTaskId()).orElseThrow(
                () -> new RuntimeException("Invalid Task Id!")
        );
        Commitments commit = Commitments.builder()
                .commitments(commitmentRequestDto.getCommitment())
                .task(t1)
                .createdBy(employee)
                .build();

        commit = commitmentsRepository.save(commit);
        taskServices.changeStatusToIn_Progress(t1.getId());

        return Mapper.toCommitmentResponseDto(commit);
    }

    /**
     * Edits the text of an existing commitment.
     *
     * Simple example: like crossing out a line in your diary and writing a
     * corrected version underneath.
     */
    @Transactional
    public CommitmentResponseDto editCommits(CommitmentRequestDto dto, String commitId) {
        Task t1 = taskRepository.findById(dto.getTaskId()).orElseThrow(
                () -> new RuntimeException("Invalid Task Id!")
        );

        Commitments commit = commitmentsRepository.findById(commitId).orElseThrow(
                () -> new RuntimeException("Invalid commitment Id!")
        );

        if(!commit.getCreatedBy().getEmail().equals(getCurrentUser().getEmail()))
            throw new RuntimeException("You can't edit this commitment!");

        commit.setCommitments(dto.getCommitment());

        commitmentsRepository.flush();
        return Mapper.toCommitmentResponseDto(commit);
    }

    /**
     * Deletes a commitment - but only if the logged-in Employee is the same
     * person who originally wrote it.
     *
     * Simple example: you can tear a page out of YOUR OWN diary, but you
     * can't walk over and tear a page out of a classmate's diary.
     */
    @Transactional
    public void deleteCommits(String commitId) {
        Employee emp = getCurrentUser();
        Commitments commitments = commitmentsRepository.findById(commitId).orElseThrow(
                () -> new RuntimeException("Invalid Commitments Id!")
        );

        if (!emp.getEmail().equals(commitments.getCreatedBy().getEmail()))
            throw new AccessDeniedException("You can only delete your own commitments!");
        commitmentsRepository.delete(commitments);

        if(getAllCommitmentsOfTask(commitments.getTask().getId(),0,5).isEmpty())
         taskServices.changeStatusTo_ToDO(commitments.getTask().getId());
    }

    /**
     * Returns ONE PAGE of commitments for a given task (not all of them at
     * once), sorted oldest-first, like reading a diary in the order it was
     * written.
     *
     * Simple example: instead of handing someone an entire diary book at
     * once, you hand them one page at a time, in order, so it's easy to
     * read and doesn't overwhelm them.
     */
    public Page<CommitmentResponseDto> getAllCommitmentsOfTask(String taskId, int page, int size) {
        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Commitments> commitmentsPage = commitmentsRepository.findCommitmentsByTaskId(taskId, pageable);
        return commitmentsPage.map(Mapper::toCommitmentResponseDto);
    }


    /**
     * A small helper that looks at "who is currently logged in" (from the
     * validated JWT) and fetches their full Employee record.
     */
    private Employee getCurrentUser() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return employeeRepository.findByEmail(userDetails.getUsername()).orElseThrow(
                () -> new RuntimeException("Employee Couldn't found!")
        );
    }
}
