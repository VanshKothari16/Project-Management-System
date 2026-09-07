package com.app.taskmanagement.controller;

import com.app.taskmanagement.requestdto.CompanyRequestDto;
import com.app.taskmanagement.requestdto.UserRequestDto;
import com.app.taskmanagement.responsedto.CompanyResponseDto;
import com.app.taskmanagement.services.CompanyServices;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task_management/company")
@RequiredArgsConstructor
@Validated
public class CompanyController {
    private final CompanyServices companyServices;

    @PostMapping
    public ResponseEntity<CompanyResponseDto> register(
            @Valid @ModelAttribute UserRequestDto userRequestDto,
            @Valid @ModelAttribute CompanyRequestDto companyRequestDto
    ){
        return ResponseEntity.status(HttpStatus.CREATED).body(
                companyServices.register(companyRequestDto,userRequestDto)
        );
    }
}
