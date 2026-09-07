package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.enums.Role;
import com.app.taskmanagement.repository.UserRepository;
import com.app.taskmanagement.requestdto.LoginDto;
import com.app.taskmanagement.requestdto.UserRequestDto;
import com.app.taskmanagement.responsedto.UserResponseDto;
import com.app.taskmanagement.utils.JwtServices;
import com.app.taskmanagement.utils.Mapper;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * This class is the "front desk" logic for basic accounts: registering a
 * brand new person, and logging an existing person in.
 *
 * Think of this like the school's main enrollment office - everyone,
 * whether they'll later become an Admin, a Manager, or stay a regular
 * Employee, starts here with just a name, email, password, and phone number.
 */
@Service
public class UserServices {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationProvider authenticationProvider;
    private final JwtServices jwtServices;

    public UserServices(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationProvider authenticationProvider, JwtServices jwtServices) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProvider = authenticationProvider;
        this.jwtServices = jwtServices;
    }

    /**
     * Creates a brand new User account.
     *
     * Simple example: like filling in an enrollment form at school - the
     * office first checks "has this email already enrolled before?" before
     * accepting a new form, and your password gets scrambled (encrypted)
     * before it's ever written down anywhere.
     */
    public UserResponseDto register(UserRequestDto requestDto) {
        User exist = userRepository.findByEmail(requestDto.getEmail());
        if (exist != null) throw new RuntimeException("You already have an account!");

        User user = Mapper.toUserEntity(requestDto);
        user.setPassword(encodePassword(requestDto.getPassword()));

        user=userRepository.save(user);
        return Mapper.toUserResponseDto(
                user
        );
    }

    /**
     * Checks a username/password combination and, if correct, hands back a
     * signed JWT "wristband" proving who this person is.
     *
     * Simple example: like showing your ID and a secret password at the
     * ticket booth - if it matches, you get a wristband that lets you walk
     * into every ride (API endpoint) you're allowed on, without having to
     * show your ID again at every single ride.
     */
    public String login(LoginDto loginDto) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword()
        );

        Authentication authResult = authenticationProvider.authenticate(authentication);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        return jwtServices.generateToken((UserDetails) authResult.getPrincipal());

    }

    /** Scrambles a plain-text password so it's never stored or seen in readable form. */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
