package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    @Query(value = "FROM Employee e where e.email=:email")
    Optional<Employee> findByEmail(@Param("email") String email);

    // NOTE: field on Employee is named "project" (singular) - keep this spelling exact,
    // "projects" here would throw a QuerySyntaxException at call time.
    @Query("Select COUNT(e) from Employee e where e.project.id=:id")
    Object findTotalEmployeeInProject(@Param("id") String id);

    @Query("Select e.email from Employee e where e.project.id=:id")
    List<String> findAllEmails(@Param("id") String projectId);

    /**
     * Used by a Manager assigning a task: returns every employee in the SAME
     * COMPANY who is NOT already assigned to this specific task, so the
     * "assign" dropdown never offers someone who's already on it.
     */
    @Query("SELECT e.email FROM Employee e " +
            "WHERE e.company.id = :companyId " +
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM Task t " +
            "   JOIN t.employee emp " +
            "   WHERE t.id = :taskId AND emp.id = e.id" +
            ")")
    List<String> findEmployeeEmailsNotAssignedToTask(
            @Param("companyId") String companyId,
            @Param("taskId") String taskId);

    /** Used by the Admin's "Employees" drill-down - excludes the project's manager. */
    @Query("FROM Employee e WHERE e.project.id = :projectId AND e.id <> :managerId")
    List<Employee> findEmployeesInProjectExcludingManager(
            @Param("projectId") String projectId, @Param("managerId") String managerId);
}
