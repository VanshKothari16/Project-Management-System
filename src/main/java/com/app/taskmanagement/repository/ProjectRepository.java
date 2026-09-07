package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Projects;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Projects,String> {

    @Query("From Projects p where p.name=:name AND p.company.admin.email=:email")
    public Optional<Projects> findByNameAndEmail(@Param("name") String name,@Param("email") String email);

    @Query("From Projects p where p.company.admin.email=:email")
    public List<Projects> findAllByEmail(@Param("email") String email);
}
