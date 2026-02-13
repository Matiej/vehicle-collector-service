package com.emat.vehicle_collector_service.api.admin

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateUserDto(
    @NotBlank(message = "Field username can't be empty!")
    @Size(max = 255, message = "To long username Maximum size of testId filed is 255 characters.")
    val username: String,

    @NotBlank(message = "Field firstName can't be empty!")
    @Size(max = 255, message = "To long firstName Maximum size of testId filed is 255 characters.")
    val firstName: String,

    @NotBlank(message = "Field lastName can't be empty!")
    @Size(max = 255, message = "To long lastName Maximum size of testId filed is 255 characters.")
    val lastName: String,

    @NotBlank(message = "email is required")
    @Email(message = "Must be a valid email address")
    @Size(max = 320, message = "Email must be at most 320 characters long")
    val email: String,
    val enabled: Boolean,
    val emailVerified: Boolean
)