package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Commitments;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommitmentsRepository extends JpaRepository<Commitments, String> {

    @Query("SELECT c FROM Commitments c WHERE c.task.id = :taskId")
    Page<Commitments> findCommitmentsByTaskId(@Param("taskId") String taskId, Pageable pageable);
}

