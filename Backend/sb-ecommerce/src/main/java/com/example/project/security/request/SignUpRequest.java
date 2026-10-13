package com.example.project.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public self-registration payload.
 *
 * NOTE: there is deliberately no `role` field here. Allowing a client to name its
 * own roles let anyone POST {"role":["admin"]} to /api/auth/signup and become an
 * administrator. Roles are now assigned server-side only - every account created
 * through this endpoint gets ROLE_USER, and elevation happens through the
 * ADMIN-guarded PUT /api/admin/users/{userId}/roles endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignUpRequest {

    @NotBlank
    @Size(max = 20)
    private String username;

    @NotBlank
    @Email
    @Size(max = 50)
    private String email;

    @NotBlank
    @Size(min = 6, max = 40)
    private String password;

}
