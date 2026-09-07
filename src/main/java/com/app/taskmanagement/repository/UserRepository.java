package com.app.taskmanagement.repository;

import com.app.taskmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, String>{
    @Query(value = "FROM User u where u.email=:email")
    public User findByEmail(@Param("email") String email);
}
