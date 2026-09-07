package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, String> {

    @Query("FROM Company c where c.name=:name AND c.admin.email=:email")
    public Company findByNameAndEmail(@Param("name") String name, @Param("email") String email);
}
