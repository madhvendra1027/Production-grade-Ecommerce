package com.example.project.service;

import com.example.project.globalExceptionHandler.APIexception;
import com.example.project.globalExceptionHandler.ResourceNotFoundException;
import com.example.project.model.AppRole;
import com.example.project.model.Role;
import com.example.project.model.User;
import com.example.project.payLoad.AuthenticationResult;
import com.example.project.payLoad.UserDTO;
import com.example.project.payLoad.UserResponse;
import com.example.project.repositories.RoleRepository;
import com.example.project.repositories.UserRepository;
import com.example.project.security.jwt.JwtUtils;
import com.example.project.security.request.LoginRequest;
import com.example.project.security.request.SignUpRequest;
import com.example.project.security.response.MessageResponse;
import com.example.project.security.response.UserInfoResponse;
import com.example.project.security.services.UserDetailsImplementation;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class AuthServiceImplementation implements AuthService{

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public AuthenticationResult login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    ));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();
        assert userDetails != null;
        ResponseCookie jwtCookie = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .toList();

        // jwtToken is deliberately null. It used to carry jwtCookie.toString() - the
        // entire Set-Cookie header, raw JWT included - which the frontend then wrote
        // into localStorage, putting the token within reach of any XSS. The token now
        // travels only in the HttpOnly cookie set on the response.
        UserInfoResponse response = new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, userDetails.getEmail(), null);
        return new AuthenticationResult(response, jwtCookie);
    }

    @Override
    public ResponseEntity<MessageResponse> registerUser(SignUpRequest signUpRequest) {
        if (userRepository.existsByUserName(signUpRequest.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error : Entered username is already taken!"));
        }

        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("Error : Entered email is already used!"));
        }

        User user = new User(
                signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword())
        );

        // Public self-registration ALWAYS yields ROLE_USER. The request object no longer
        // carries a role field, so there is no path from an unauthenticated caller to
        // ROLE_SELLER or ROLE_ADMIN. Use assignRoles (admin-only) to elevate an account.
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseThrow(() -> new APIexception(
                        "ROLE_USER is not configured. Role bootstrapping did not run."));

        user.setRoles(Set.of(userRole));
        userRepository.save(user);
        return ResponseEntity.ok(new MessageResponse("USER registered successfully!"));
    }

    /**
     * Replaces a user's role set. Only reachable from an ADMIN-guarded controller
     * method, so this is the single legitimate way an account gains elevated roles.
     */
    @Override
    public UserDTO assignRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        if (roleNames == null || roleNames.isEmpty()) {
            throw new APIexception("At least one role must be supplied.");
        }

        Set<Role> roles = new HashSet<>();
        for (String roleName : roleNames) {
            AppRole appRole = parseRole(roleName);
            Role role = roleRepository.findByRoleName(appRole)
                    .orElseThrow(() -> new APIexception("Role " + appRole + " is not configured."));
            roles.add(role);
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        UserDTO dto = modelMapper.map(savedUser, UserDTO.class);
        dto.setPassword(null);
        return dto;
    }

    /**
     * Accepts "admin", "ADMIN", "ROLE_ADMIN" etc. and rejects anything that is not a
     * real AppRole, rather than silently falling through to a default like the old
     * switch statement did.
     */
    private AppRole parseRole(String roleName) {
        String normalized = roleName == null ? "" : roleName.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        try {
            return AppRole.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new APIexception("Unknown role : " + roleName);
        }
    }

    @Override
    public UserInfoResponse getCurrentUserDetails(Authentication authentication) {
        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // No longer mints a fresh JWT here. This is a read-only "who am I" endpoint;
        // issuing a new token on every call both wasted work and leaked the token
        // into the response body.
        return new UserInfoResponse(userDetails.getId(),
                userDetails.getUsername(), roles, userDetails.getEmail(), null);
    }

    @Override
    public ResponseCookie logoutUser() {
        return jwtUtils.getCleanJwtCookie();
    }

    @Override
    public UserResponse getAllSeller(Pageable pageable) {
        Page<User> allUsers = userRepository.findByRoleName(AppRole.ROLE_SELLER, pageable);
        List<UserDTO> userDTOs = allUsers.getContent()
                .stream()
                .map(p -> {
                    UserDTO dto = modelMapper.map(p, UserDTO.class);
                    // UserDTO carries a password field; never let the bcrypt hash
                    // leave the service layer.
                    dto.setPassword(null);
                    return dto;
                })
                .toList();
        UserResponse response = new UserResponse();
        response.setContent(userDTOs);
        response.setPageNumber(allUsers.getNumber());
        response.setPageSize(allUsers.getSize());
        response.setTotalElements(allUsers.getTotalElements());
        response.setTotalPages(allUsers.getTotalPages());
        response.setLastPage(allUsers.isLast());
        return response;
    }
}
