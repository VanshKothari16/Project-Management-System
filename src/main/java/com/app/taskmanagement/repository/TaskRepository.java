package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Task;
import com.app.taskmanagement.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, String> {

    @Query("SELECT t FROM Task t WHERE t.title = :title AND t.projects.id = :id GROUP BY t")
    Optional<Task> findByTitleAndProjects(@Param("title") String title, @Param("id") String projectId);

    @Query("SELECT t FROM Task t JOIN t.employee e WHERE e.email = :email GROUP BY t")
    List<Task> findTaskOfEmployee(@Param("email") String email);

    @Query("SELECT t FROM Task t WHERE t.status = :status AND t.projects.id = :id GROUP BY t")
    List<Task> findTaskByStatusAndProjects(@Param("status") Status status, @Param("id") String projectId);

    @Query("SELECT t FROM Task t WHERE t.projects.id = :id GROUP BY t")
    List<Task> findTaskByProjects(@Param("id") String id);

    @Query("SELECT t, COUNT(c) FROM Task t LEFT JOIN t.commitments c WHERE t.projects.id = :projectId GROUP BY t")
    List<Object[]> findTasksWithCommitmentCount(@Param("projectId") String projectId);

    @Query("SELECT t FROM Task t WHERE t.projects.id = :id AND t.dueDate < CURRENT_DATE AND t.status <> 'COMPLETED' GROUP BY t")
    List<Task> findOverdueTaskByProjects(@Param("id") String id);
}
