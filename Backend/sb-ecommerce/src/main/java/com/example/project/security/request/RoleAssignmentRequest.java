package com.example.project.security.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Payload for the admin-only role assignment endpoint.
 * Accepts either bare names ("admin") or fully qualified ones ("ROLE_ADMIN").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {

    @NotEmpty(message = "At least one role must be supplied")
    private Set<String> roles;
}
