package com.bank.mortgage.util;

import com.bank.mortgage.domain.User;
import com.bank.mortgage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public String getCurrentUserRole() {
        User user = getCurrentUser();
        return user.getRole();
    }

    public boolean isCurrentUserOwner(User applicant) {
        User currentUser = getCurrentUser();
        return currentUser.getId().equals(applicant.getId());
    }

    public boolean isCreditOfficer() {
        return "CREDIT_OFFICER".equals(getCurrentUserRole());
    }

    public boolean isApplicant() {
        return "APPLICANT".equals(getCurrentUserRole());
    }
}
