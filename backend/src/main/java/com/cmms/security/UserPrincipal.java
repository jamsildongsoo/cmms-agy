package com.cmms.security;

import com.cmms.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private final String companyId;
    private final String username;
    private final String password;
    private final String name;
    private final String roleId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String companyId, String username, String password, String name, String roleId, Collection<? extends GrantedAuthority> authorities) {
        this.companyId = companyId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.roleId = roleId;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        String authorityName = "ROLE_" + user.getRoleId().toUpperCase();
        Collection<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(authorityName));

        return new UserPrincipal(
                user.getCompanyId(),
                user.getId(),
                user.getPasswordHash(),
                user.getName(),
                user.getRoleId(),
                authorities
        );
    }

    public String getCompanyId() {
        return companyId;
    }

    public String getName() {
        return name;
    }

    public String getRoleId() {
        return roleId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
