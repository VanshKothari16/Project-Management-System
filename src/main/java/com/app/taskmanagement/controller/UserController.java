package com.app.taskmanagement.controller;

import com.app.taskmanagement.requestdto.LoginDto;
import com.app.taskmanagement.requestdto.UserRequestDto;
import com.app.taskmanagement.responsedto.UserResponseDto;
import com.app.taskmanagement.services.UserServices;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The "front desk" for basic sign-up and sign-in. Both endpoints here are
 * PUBLIC (no wristband/JWT needed) - see SecurityConfiguration - because
 * you obviously can't be asked for a ticket before you've even entered the
 * building.
 */
@RestController
@RequestMapping("/api/task_management/user")
@Validated
public class UserController {
    private final UserServices userServices;

    public UserController(UserServices userServices) {
        this.userServices = userServices;
    }

    /** Creates a brand new basic User account. */
    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody UserRequestDto requestDto){
        UserResponseDto user=userServices.register(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /** Checks credentials and returns a signed JWT token as plain text. */
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginDto loginDto){
        String jwtToken=userServices.login(loginDto);
        return ResponseEntity.ok(jwtToken);
    }
}
