package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.Company;
import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.enums.Role;
import com.app.taskmanagement.repository.CompanyRepository;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.requestdto.CompanyRequestDto;
import com.app.taskmanagement.requestdto.UserRequestDto;
import com.app.taskmanagement.responsedto.CompanyResponseDto;
import com.app.taskmanagement.responsedto.UserResponseDto;
import com.app.taskmanagement.utils.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class is the "brain" for registering a brand new Company - the very
 * first step for anyone who wants to become an Admin.
 *
 * Think of this like opening a brand new school: the very first person who
 * signs up automatically becomes its Principal (Admin), and the school
 * itself gets its own unique ID that future teachers/students will use to
 * join.
 */
@Service
@RequiredArgsConstructor
public class CompanyServices {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserServices userServices;

    /**
     * Registers a brand new Company AND promotes the person registering it
     * to the ADMIN role, all in one go.
     *
     * Simple example: imagine filling out one single form that BOTH creates
     * your personal membership card AND stamps "PRINCIPAL" on it the moment
     * you also tell the office "I'm opening a new school called ___".
     *
     * Steps: 1) register this person as a normal User first (reusing
     * UserServices, so we don't duplicate that logic), 2) make sure there
     * isn't already a company with this exact name under this exact admin
     * (no duplicate schools), 3) promote their role to ADMIN, 4) link them
     * as the company's owner, 5) save.
     */
    @Transactional
    public CompanyResponseDto register(CompanyRequestDto companyRequestDto, UserRequestDto userRequestDto){
        UserResponseDto userResponseDto=userServices.register(userRequestDto);

        Company existCompany=companyRepository.findByNameAndEmail(companyRequestDto.getCompanyName(),userResponseDto.getEmail());
        if(existCompany!=null) throw new RuntimeException("Duplicate Company!");

        Company company=Mapper.toCompanyEntity(companyRequestDto);

        User admin=userRepository.findByEmail(userRequestDto.getEmail());
        admin.setRole(Role.ADMIN);
        company.setAdmin(admin);


        company=companyRepository.save(company);
        companyRepository.flush();
        return Mapper.toCompanyResponseDto(company);
    }
}
