package com.emat.vehicle_collector_service.vehuser

import com.emat.vehicle_collector_service.api.admin.CreateUserDto
import com.emat.vehicle_collector_service.api.admin.UpdateUserDto
import com.emat.vehicle_collector_service.infrastructure.keycloak.KeycloakClient
import com.emat.vehicle_collector_service.infrastructure.keycloak.KeycloakUserRequest
import com.emat.vehicle_collector_service.vehuser.domain.VehUser
import com.emat.vehicle_collector_service.vehuser.domain.VehUserRole
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class VehUserService(
    private val keycloakClient: KeycloakClient
) {
    private val log = LoggerFactory.getLogger(VehUserService::class.java)

    fun createRegularUser(vehUserDto: CreateUserDto): Mono<String> =
        keycloakClient.createUser(KeycloakUserRequest.fromCreateDto(vehUserDto, VehUserRole.REGULAR_USER))
            .doOnSuccess { suc -> log.info("User ${vehUserDto.username}, id: $suc created successful.") }

    fun listAllRegularUsers(): Flux<VehUser> =
        keycloakClient.listUsersByRole(VehUserRole.REGULAR_USER.name)
            .map { user -> user.toDomain(VehUserRole.REGULAR_USER) }
            .doOnError { err -> log.error("Error fetched regular users $", err) }

    fun updateRegularUser(updateDto: UpdateUserDto): Mono<VehUser> =
        keycloakClient.updateUser(updateDto.id, KeycloakUserRequest.fromUpdateDto(updateDto, VehUserRole.REGULAR_USER))
            .map { updatedUser -> updatedUser.toDomain(VehUserRole.REGULAR_USER) }
            .doOnSuccess { suc -> log.info("User ${updateDto.id}, id: $suc updated successful.") }

    fun deleteUser(id: String): Mono<Void> =
        keycloakClient.deleteUser(id)
            .doOnSuccess { log.info("User id $id succesfully removed") }

    fun changeUserState(userId: String, isEnabled: Boolean) =
        keycloakClient.changeUserStatus(userId,isEnabled)
            .doOnSuccess{log.info("User id: $userId has changed to $isEnabled")}

    fun getUserById(userId: String): Mono<VehUser> =
        keycloakClient.getUserById(userId)
            .map { user -> user.toDomain(VehUserRole.REGULAR_USER) } // todo hardcoded role. Later when user will have more roles need to be fix
            .doOnSuccess{user -> log.info("Successfully fetched data for userId: ${user.id}, name: ${user.username}")}

    fun sendUserActionsEmail(userId: String, actions: List<String>) =
        keycloakClient.sendUserActionsEmail(userId, actions)
            .doOnSuccess{log.info("Actions $actions email has been sent for userId: $userId .")}
}