package com.ecommerce.project.util;

import com.ecommerce.project.entities.User;
import com.ecommerce.project.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthUtil {

    private final UserRepository userRepository;

    public String loggedInEmail() {
        return loggedInUser().getEmail();
    }

    public Long loggedInUserId() {
        return loggedInUser().getUserId();
    }

    public User loggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found"));
    }
}
