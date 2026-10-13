package com.example.project.security.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String userName;
    private String email;
    private String jwtToken;
    private List<String> roles;

    public UserInfoResponse(Long id, String userName, List<String> roles, String email, String jwtToken) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.roles = roles;
        this.jwtToken = jwtToken;
    }
}
