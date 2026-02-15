package com.emat.vehicle_collector_service.api.admin

import com.emat.vehicle_collector_service.vehuser.VehUserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/admin")
class VehUserController(
    private val vehUserService: VehUserService
) {
    private val log = LoggerFactory.getLogger(VehUserController::class.java)

    @Operation(
        summary = "Admin GET endpoint to list all regular users.",
        description = "Fetches all available regular users."
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Regular users successful retrieved",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @GetMapping("/users/regular")
    fun listAllRegularUsers(): Flux<VehUserResponse> {
        log.info("Received GET request '/api/admin/regular/users' ")
        return vehUserService.listAllRegularUsers()
            .map { vehAppUser -> VehUserResponse.fromDomain(vehAppUser) }
    }

    @Operation(
        summary = "Admin POST endpoint to create regular user",
        description = "Admin POST endpoint, creates REGULAR user in keycloak app"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "201",
            description = "Regular user successful created",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PostMapping("/users/regular")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRegularUser(@Valid @RequestBody createUserDto: CreateUserDto): Mono<String> {
        log.info("Received POST request '/api/admin/regular/users' creating new regular user")
        return vehUserService.createRegularUser(createUserDto)
    }

    @Operation(
        summary = "Admin Put endpoint to update regular user",
        description = "Admin POST endpoint, creates REGULAR user in keycloak app"
    )
    @ApiResponses(
        value = [ApiResponse(
            responseCode = "200",
            description = "Regular user successful updated",
        ), ApiResponse(responseCode = "500", description = "Internal server error")]
    )
    @PutMapping("/users/regular")
    @ResponseStatus(HttpStatus.CREATED)
    fun updateRegularUser(@Valid @RequestBody updateDto: UpdateUserDto): Mono<VehUserResponse> {
        log.info("Received PUT request '/api/admin/regular/users' updating regular user")
        return vehUserService.updateRegularUser(updateDto)
            .map { updatedUser -> VehUserResponse.fromDomain(updatedUser) }
    }

    @Operation(
        summary = "Admin GET endpoint to get user by ID",
        description = "Fetches user by ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "User successfully retrieved"),
            ApiResponse(responseCode = "404", description = "User not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @GetMapping("/users/regular/{userId}")
    fun getUserById(@PathVariable userId: String): Mono<VehUserResponse> {
        log.info("Received GET request '/api/admin/regular/users/$userId'")
        return vehUserService.getUserById(userId)
            .map { user -> VehUserResponse.fromDomain(user) }
    }

    @Operation(
        summary = "Admin DELETE endpoint to delete user",
        description = "Deletes user by ID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "User successfully deleted"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteUser(@PathVariable userId: String): Mono<Void> {
        log.info("Received DELETE request '/api/admin/regular/users/$userId'")
        return vehUserService.deleteUser(userId)
    }

    @Operation(
        summary = "Admin PATCH endpoint to change user status",
        description = "Enables or disables user"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "User status changed"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PatchMapping("/users/{userId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeUserStatus(
        @PathVariable userId: String,
        @RequestParam isEnabled: Boolean
    ): Mono<Void> {
        log.info("Received PATCH request '/api/admin/regular/users/$userId/status' isEnabled=$isEnabled")
        return vehUserService.changeUserState(userId, isEnabled)
    }

    @Operation(
        summary = "Admin POST endpoint to send action email",
        description = "Sends action email (e.g. VERIFY_EMAIL, UPDATE_PASSWORD) to user"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Email sent"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    @PostMapping("/users/{userId}/actions-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendUserActionsEmail(
        @PathVariable userId: String,
        @RequestBody actions: List<String>
    ): Mono<Void> {
        log.info("Received POST request '/api/admin/regular/users/$userId/actions-email' actions=$actions")
        return vehUserService.sendUserActionsEmail(userId, actions)
    }
}
