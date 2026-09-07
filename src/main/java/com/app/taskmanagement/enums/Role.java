package com.app.taskmanagement.enums;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Role {
    ADMIN("ROLE_ADMIN","CRUD_PROJECTS","CRUD_MANAGER","CRUD_EMPLOYEE"),
    MANAGER("ROLE_MANAGER","CRUD_PROJECTS","CRUD_TASK"),
    EMPLOYEE("ROLE_EMPLOYEE");

    public Set<String> authorities;

    Role(String... authority){
        authorities=new HashSet<>();
        authorities.addAll(Arrays.stream(authority).toList());
    }

    public Set<String> getAuthorities(){
        return authorities;
    }

}
