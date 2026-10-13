package com.example.project.service;

import com.example.project.payLoad.AuthenticationResult;
import com.example.project.payLoad.UserDTO;
import com.example.project.payLoad.UserResponse;
import com.example.project.security.request.LoginRequest;
import com.example.project.security.request.SignUpRequest;
import com.example.project.security.response.MessageResponse;
import com.example.project.security.response.UserInfoResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Set;

public interface AuthService {
    AuthenticationResult login(LoginRequest loginRequest);

    ResponseEntity<MessageResponse> registerUser(@Valid SignUpRequest signUpRequest);

    UserInfoResponse getCurrentUserDetails(Authentication authentication);

    ResponseCookie logoutUser();

    UserResponse getAllSeller(Pageable pageDetails);

    /** Admin-only role assignment. Must never be exposed on a public endpoint. */
    UserDTO assignRoles(Long userId, Set<String> roleNames);
}
