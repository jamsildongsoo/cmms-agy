package com.cmms.security;

import com.cmms.model.User;
import com.cmms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!username.contains(":")) {
            throw new UsernameNotFoundException("Invalid username format. Expected 'companyId:userId'");
        }
        String[] parts = username.split(":", 2);
        String companyId = parts[0];
        String id = parts[1];

        User user = userRepository.findByCompanyIdAndIdAndDeleteYnAndUseYn(companyId, id, "N", "Y")
                .orElseThrow(() -> new UsernameNotFoundException("User not found with companyId: " + companyId + ", id: " + id));

        return UserPrincipal.create(user);
    }
}
