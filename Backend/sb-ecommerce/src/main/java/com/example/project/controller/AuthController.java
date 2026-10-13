package com.example.project.controller;

import com.example.project.configuration.AppConstants;
import com.example.project.payLoad.AuthenticationResult;
import com.example.project.payLoad.UserDTO;
import com.example.project.security.request.LoginRequest;
import com.example.project.security.request.RoleAssignmentRequest;
import com.example.project.security.request.SignUpRequest;
import com.example.project.security.response.MessageResponse;
import com.example.project.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/auth/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest){
        AuthenticationResult result = authService.login(loginRequest);

        //SET the cookie into the user's response
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                          result.getJwtCookie().toString())
                          .body(result.getResponse());
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        return authService.registerUser(signUpRequest);
    }

    @GetMapping("/auth/username")
    public String currentUserName(Authentication authentication){
        // This endpoint now requires authentication, so authentication is non-null.
        return authentication.getName();
    }

    @GetMapping("/auth/user")
    public ResponseEntity<?> getUserDetails(Authentication authentication){
        return ResponseEntity.ok().body(authService.getCurrentUserDetails(authentication));
    }

    @PostMapping("/auth/signout")
    public ResponseEntity<?> signoutUser(){
        ResponseCookie cookie = authService.logoutUser();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                cookie.toString())
                        .body(new MessageResponse("You have been signed out!"));
    }

    /**
     * Moved from /api/auth/sellers to /api/admin/sellers.
     *
     * Everything under /api/auth/** used to be permitAll, which made the full list
     * of seller accounts - usernames and email addresses - readable by anyone on
     * the internet without logging in. /api/admin/** is ADMIN gated.
     */
    @GetMapping("/admin/sellers")
    public ResponseEntity<?> getAllSellers(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber
    ) {
        Sort sortByAndOrder = Sort.by(AppConstants.SORT_USERS_BY).descending();
        Pageable pageDetails = PageRequest.of(pageNumber, Integer.parseInt(AppConstants.PAGE_SIZE), sortByAndOrder);
        return ResponseEntity.ok(authService.getAllSeller(pageDetails));
    }

    /**
     * The only legitimate way to grant ROLE_SELLER or ROLE_ADMIN. Sits under
     * /api/admin/** so Spring Security requires ROLE_ADMIN to reach it.
     */
    @PutMapping("/admin/users/{userId}/roles")
    public ResponseEntity<UserDTO> assignRoles(@PathVariable Long userId,
                                               @Valid @RequestBody RoleAssignmentRequest request) {
        return ResponseEntity.ok(authService.assignRoles(userId, request.getRoles()));
    }
}
