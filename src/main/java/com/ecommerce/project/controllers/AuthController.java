package com.ecommerce.project.controllers;

import com.ecommerce.project.entities.AppRole;
import com.ecommerce.project.entities.Role;
import com.ecommerce.project.entities.User;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import com.ecommerce.project.security.jwt.JwtUtils;
import com.ecommerce.project.security.request.LoginRequest;
import com.ecommerce.project.security.request.SignupRequest;
import com.ecommerce.project.security.response.MessageResponse;
import com.ecommerce.project.security.response.UserInfoResponse;
import com.ecommerce.project.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        log.debug("AuthController.authenticateUser call started with: {}", loginRequest);
        Authentication authentication;
        try {
            authentication = authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword()));
        } catch (AuthenticationException e) {
            log.error("AuthController.authenticateUser call failed", e);
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(map);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        ResponseCookie responseCookie = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UserInfoResponse loginResponse =
                new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        roles);
        log.debug("AuthController.authenticateUser call completed with: {}", loginResponse);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        log.debug("AuthController.registerUser call started with: {}", signupRequest);
        if (userRepository.existsByUsername(signupRequest.getUsername())) {
            log.error("AuthController.registerUser call failed with duplicate username: {}", signupRequest.getUsername());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Error: Username is already taken!"));
        }
        if (userRepository.existsByEmail(signupRequest.getEmail())) {
            log.error("AuthController.registerUser call failed with duplicate email: {}", signupRequest.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse("Error: Email is already taken!"));
        }

        User user = new User();
        user.setUsername(signupRequest.getUsername());
        user.setEmail(signupRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));

        Set<String> strRoles = signupRequest.getRoles();
        Set<Role> roles = new HashSet<>();
        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        } else {
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
                        roles.add(adminRole);
                        break;
                    case "seller":
                        Role sellerRole = roleRepository.findByRoleName(AppRole.ROLE_SELLER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
                        roles.add(sellerRole);
                        break;
                    default:
                        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("Error: Role is not found"));
                        roles.add(userRole);
                }
            });
        }
        user.setRoles(roles);
        userRepository.save(user);
        log.debug("AuthController.registerUser call completed with: {}", user);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/username")
    public ResponseEntity<String> currentUsername(Authentication authentication) {
        log.debug("AuthController.currentUsername call started with authentication: {}", authentication);
        if (authentication != null) {
            log.debug("AuthController.currentUsername call completed with authentication: {}", authentication.getName());
            return ResponseEntity.status(HttpStatus.OK).body(authentication.getName());
        } else {
            log.error("AuthController.currentUsername call failed with null authentication");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
    }

    @GetMapping("/user")
    public ResponseEntity<?> currentUser(Authentication authentication) {
        log.debug("AuthController.currentUser call started with authentication: {}", authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        UserInfoResponse loginResponse =
                new UserInfoResponse(
                        userDetails.getId(),
                        userDetails.getUsername(),
                        roles);
        log.debug("AuthController.currentUser call completed with: {}", loginResponse);
        return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signoutUser() {
        log.debug("AuthController.signoutUser call started.");
        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        log.debug("AuthController.signoutUser call completed with: {}", cookie);
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new MessageResponse("You've been logged out successfully!"));
    }
}
