package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.Company;
import com.app.taskmanagement.entity.Employee;
import com.app.taskmanagement.repository.CompanyRepository;
import com.app.taskmanagement.repository.EmployeeRepository;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.requestdto.EmployeeRequestDto;
import com.app.taskmanagement.responsedto.EmployeeResponseDto;
import com.app.taskmanagement.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This class is the "brain" for everything to do with Employees: joining a
 * company, viewing/editing your own profile, and helping a Manager find
 * teammates to assign work to.
 *
 * Think of this like the school's student records office: it knows every
 * student's name, contact details, and which classroom they belong to.
 */
@Service
@RequiredArgsConstructor
public class EmployeeServices {
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    /**
     * Registers a brand new Employee under an existing Company.
     *
     * Simple example: like a new student enrolling at a school - they need
     * to provide the school's ID code (companyCode) so the office knows
     * exactly which school to add them to.
     *
     * Steps: 1) make sure this email hasn't already registered before,
     * 2) look up the company using the code they provided, 3) scramble
     * (encrypt) their password before ever saving it, 4) save.
     */
    public EmployeeResponseDto addEmployee(EmployeeRequestDto dto) {
        employeeRepository.findByEmail(dto.getEmail()).ifPresent(
                (e) -> {
                    throw new RuntimeException("You already have an Account");
                }
        );

        Company company = companyRepository.findById(dto.getCompanyCode()).orElseThrow(
                () -> new RuntimeException("Enter valid company code!")
        );

        Employee employee = Mapper.toEmployeeEntity(dto);
        employee.setCompany(company);
        employee.setPassword(encodePassword(employee.getPassword()));

        employee = employeeRepository.save(employee);
        return Mapper.toEmployeeResponseDto(employee);
    }

    /**
     * Lets an Employee update their OWN name/phone/qualification/specialization.
     * Email can never be changed here (it's the permanent ID for their account).
     *
     * Simple example: a student can update their own contact card at the
     * front office, but they can't walk in and edit someone ELSE's card,
     * and they can't change the student ID number written on it.
     */
    @Transactional
    public EmployeeResponseDto update(EmployeeRequestDto dto) {
        Employee emp = employeeRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> {
                    throw new RuntimeException("Invalid Email Id");
                }
        );

        Employee login = getCurrentUser();

        if (!login.getEmail().equals(emp.getEmail())) throw new AccessDeniedException("UnRegistered Employee");

        emp.setName(dto.getName());
        emp.setMobileNo(dto.getPhone());
        emp.setQualification(dto.getQualification());
        emp.setSpecialization(dto.getSpecialization());

        return Mapper.toEmployeeResponseDto(emp);
    }

    /**
     * Returns the currently logged-in Employee's own profile - useful for a
     * "My Profile" page.
     *
     * Simple example: a student swiping their own ID card at a kiosk and
     * seeing their own contact details pop up on screen.
     */
    public EmployeeResponseDto getEmployee() {
        Employee employee = getCurrentUser();
        return Mapper.toEmployeeResponseDto(
                employeeRepository.findByEmail(employee.getEmail()).orElseThrow(
                        () -> new RuntimeException("Invalid Email id")
                )
        );
    }

    /**
     * Used by a Manager while assigning a task: returns the email of every
     * employee in the SAME COMPANY who is NOT already assigned to this
     * specific task, so the "assign" dropdown never suggests someone who's
     * already working on it.
     *
     * Simple example: a teacher picking a student to help with a specific
     * chore - the sign-up sheet only shows students who AREN'T already
     * doing that exact chore.
     */
    public List<String> getAllEmployeeEmails(String taskId) {
        Employee currentUser = getCurrentUser();
        return employeeRepository.findEmployeeEmailsNotAssignedToTask(
                currentUser.getCompany().getId(), taskId
        );
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

    /** Scrambles a plain-text password so it's never stored or seen in readable form. */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

}
