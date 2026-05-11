package com.marcog.peluqueria.security.infrastructure.persistence;

import com.marcog.peluqueria.security.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);
}
