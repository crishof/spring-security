package com.crishof.springsecurity.security;

import com.crishof.springsecurity.model.SecurityAccount;
import com.crishof.springsecurity.model.User;
import com.crishof.springsecurity.repository.SecurityAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final SecurityAccountRepository securityAccountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be blank");
        }

        SecurityAccount account = securityAccountRepository.findByUserEmailIgnoreCase(email.trim()).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        User user = account.getUser();
        return new SecurityUser(user, account);
    }
}