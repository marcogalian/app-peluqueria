package com.marcog.peluqueria;

import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.domain.model.User;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            // Crear administrador por defecto
            UserEntity admin = UserEntity.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("1234"))
                    .role(Role.ROLE_ADMIN)
                    .email("admin@peluqueria.com")
                    .active(true)
                    .build();
            userRepository.save(admin);

            // Crear peluquero por defecto
            UserEntity peluquero = UserEntity.builder()
                    .username("maria")
                    .password(passwordEncoder.encode("1234"))
                    .role(Role.ROLE_HAIRDRESSER)
                    .email("maria@peluqueria.com")
                    .active(true)
                    .build();
            userRepository.save(peluquero);

            System.out.println("Usuarios de prueba inicializados: admin/1234 y maria/1234");
        }
    }
}
