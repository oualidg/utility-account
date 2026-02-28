/*
 * Copyright (c) 2026 Oualid Gharach. All rights reserved.
 *
 * Aiming for production-grade standards through clean code and best practices
 * for educational and informational purposes.
 *
 * Created on: 2/25/2026 at 9:40 PM
 *
 * Feel free to use or contribute. Contact: oualid.gharach@gmail.com
 */
package com.mycompany.api.account.mapper;

import com.mycompany.api.account.dto.CreateUserRequest;
import com.mycompany.api.account.dto.UpdateUserRequest;
import com.mycompany.api.account.dto.UserResponse;
import com.mycompany.api.account.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

/**
 * MapStruct mapper for {@link User} entity.
 * Handles normalisation of string fields on inbound requests.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps a User entity to a UserResponse DTO.
     */
    UserResponse toResponse(User user);

    /**
     * Maps a CreateUserRequest to a User entity.
     * Normalises string fields — trims whitespace, lowercases email and username.
     * Password is intentionally excluded — encoded separately in the service.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "username", source = "username", qualifiedByName = "normalizeUsername")
    @Mapping(target = "firstName", source = "firstName", qualifiedByName = "normalizeString")
    @Mapping(target = "lastName", source = "lastName", qualifiedByName = "normalizeString")
    @Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
    User toEntity(CreateUserRequest request);

    /**
     * Updates an existing User entity from an UpdateUserRequest.
     * Normalises string fields in place.
     * Username is intentionally excluded — usernames are immutable after creation.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "firstName", source = "firstName", qualifiedByName = "normalizeString")
    @Mapping(target = "lastName", source = "lastName", qualifiedByName = "normalizeString")
    @Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);

    @Named("normalizeString")
    default String normalizeString(String value) {
        return value == null ? null : value.trim();
    }

    @Named("normalizeEmail")
    default String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Named("normalizeUsername")
    default String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }
}