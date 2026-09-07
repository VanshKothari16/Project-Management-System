package com.app.taskmanagement.services;

import com.app.taskmanagement.entity.User;
import com.app.taskmanagement.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * This class is a small translator between OUR OWN "User" entity and the
 * format Spring Security expects everywhere internally (called
 * "UserDetails"). Spring Security has never heard of our custom User class,
 * so this wraps it in a shape Spring understands.
 *
 * Simple example: imagine Spring Security only reads passports written in
 * English, but our own records are written in our own custom format. This
 * class is the translator standing at the door, converting our format into
 * something Spring Security can actually read.
 */
@Component
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Looks up a user by email and wraps them in Spring Security's expected
     * format. This is called automatically, behind the scenes, every time
     * someone logs in or presents a JWT wristband.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=userRepository.findByEmail(username);
        if(user!=null){
            return new UserDetailsImpl(user);
        } else throw new UsernameNotFoundException("Invalid Email or Password");
    }
}

/**
 * The actual "translated passport" - a thin wrapper that makes our User
 * entity look like whatever Spring Security expects to see.
 */
class UserDetailsImpl implements UserDetails{
    private final User user;

    UserDetailsImpl(User user) {
        this.user = user;
    }

    /**
     * Converts our Role's list of permission stickers (e.g. "ROLE_ADMIN",
     * "CRUD_TASK") into the exact object type Spring Security's
     * @PreAuthorize checks expect to compare against.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRole().getAuthorities().stream().map(
                (s)->new SimpleGrantedAuthority(s)
        ).toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}
