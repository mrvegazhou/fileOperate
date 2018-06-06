package com.vega.nfs.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class JwtUser implements UserDetails {

    private String username;
    private String password;
    private String basepath;
    private Collection<? extends GrantedAuthority> authorities;

    public JwtUser(String username, String password, String basepath, Collection<? extends GrantedAuthority> authorities) {
        this.username = username;
        this.password = password;
        this.basepath = basepath;
        this.authorities = authorities;
    }

    public static JwtUser getUserDetail() {
        return (JwtUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public String getBasepath() {
        return basepath;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }

}