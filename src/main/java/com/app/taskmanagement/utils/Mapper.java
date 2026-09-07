package com.app.taskmanagement.utils;

import com.app.taskmanagement.entity.*;
import com.app.taskmanagement.enums.Role;
import com.app.taskmanagement.enums.Status;
import com.app.taskmanagement.requestdto.*;
import com.app.taskmanagement.responsedto.*;

import java.util.ArrayList;

public class Mapper {

    public static User toUserEntity(UserRequestDto userRequestDto){
        return User.builder()
                .name(userRequestDto.getName())
                .email(userRequestDto.getEmail())
                .password(userRequestDto.getPassword())
                .mobileNo(userRequestDto.getPhone())
                .role(Role.EMPLOYEE)
                .build();
    }

    public static UserResponseDto toUserResponseDto(User user){
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getMobileNo())
                .role(user.getRole())
                .build();
    }

    public static Company toCompanyEntity(CompanyRequestDto companyRequestDto){
        return Company.builder()
                .name(companyRequestDto.getCompanyName())
                .occupation(companyRequestDto.getOccupation())
                .address(companyRequestDto.getAddress())
                .build();
    }

    public static CompanyResponseDto toCompanyResponseDto(Company company){
        return CompanyResponseDto.builder()
                .id(company.getId())
                .name(company.getName())
                .occupation(company.getOccupation())
                .createdAt(company.getCreatedAt())
                .admin(toUserResponseDto(company.getAdmin()))
                .address(company.getAddress())
                .build();
    }


    public static Employee toEmployeeEntity(EmployeeRequestDto dto){
        return (Employee) Employee.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(dto.getPassword())
                .mobileNo(dto.getPhone())
                .qualification(dto.getQualification())
                .specialization(dto.getSpecialization())
                .role(Role.EMPLOYEE)
                .build();
    }

    public static EmployeeResponseDto toEmployeeResponseDto(Employee employee){
        return EmployeeResponseDto.builder()
                .id(employee.getId())
                .name(employee.getName())
                .email(employee.getEmail())
                .phone(employee.getMobileNo())
                .role(employee.getRole())
                .qualification(employee.getQualification())
                .specialization(employee.getSpecialization())
                .company(employee.getCompany().getName())
                .project(employee.getProject() != null ? employee.getProject().getName() : null)
                .build();
    }

    public static Projects toProjectEntity(ProjectRequestDto dto){
        return Projects.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .deadline(dto.getDeadline())
                .status(Status.TO_DO)
                .build();
    }

    public static ProjectResponseDto toProjectResponseDto(Projects projects,Employee manager){
        return ProjectResponseDto.builder()
                .id(projects.getId())
                .name(projects.getName())
                .description(projects.getDescription())
                .deadline(projects.getDeadline())
                .status(projects.getStatus())
                .companyName(projects.getCompany().getName())
                .manager(manager.getName())
                .build();
    }

    public static Task toTaskEntity(TaskRequestDto dto){
        return Task.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .priority(dto.getPriority())
                .status(Status.TO_DO)
                .employee(new ArrayList<>())
                .build();

    }

    public static TaskResponseDto toTaskReponseDto(Task task){
        return TaskResponseDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .dueDate(task.getDueDate())
                .priority(task.getPriority())
                .status(task.getStatus())
                .projectName(task.getProjects().getName())
                .employee(task.getEmployee().stream().map((e)->e.getName()).toList())
                .build();
    }

    public static Commitments toCommitmentEntity(CommitmentRequestDto dto){
        return Commitments.builder()
                .commitments(dto.getCommitment())
                .build();
    }

    public static CommitmentResponseDto toCommitmentResponseDto(Commitments commitments){
        return CommitmentResponseDto.builder()
                .id(commitments.getId())
                .commitment(commitments.getCommitments())
                .createdAt(commitments.getCreatedAt())
                .taskName(commitments.getTask().getTitle())
                .employeeName(commitments.getCreatedBy().getName())
                .employeeEmail(commitments.getCreatedBy().getEmail())
                .build();
    }
}
