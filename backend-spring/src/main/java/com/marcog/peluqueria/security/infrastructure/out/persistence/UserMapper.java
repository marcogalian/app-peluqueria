package com.marcog.peluqueria.security.infrastructure.out.persistence;

import com.marcog.peluqueria.security.domain.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);
}
