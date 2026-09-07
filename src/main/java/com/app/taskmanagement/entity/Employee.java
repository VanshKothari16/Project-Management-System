package com.app.taskmanagement.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
public class Employee extends User{
    @Column(nullable = false)
    private String qualification;

    @Column(nullable = false)
    private String specialization;

    @ManyToOne
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    private Projects project;

    @ManyToMany(mappedBy = "employee")
    List<Task> task;
}
