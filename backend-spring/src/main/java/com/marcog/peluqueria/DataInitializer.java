package com.marcog.peluqueria;

import com.marcog.peluqueria.clientes.domain.model.Cliente;
import com.marcog.peluqueria.clientes.domain.model.Genero;
import com.marcog.peluqueria.clientes.domain.port.in.RegistrarClienteUseCase;
import com.marcog.peluqueria.clientes.domain.port.out.ClienteRepository;
import com.marcog.peluqueria.security.domain.model.Role;
import com.marcog.peluqueria.security.infrastructure.out.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.out.persistence.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JpaUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteRepository clienteRepository;
    private final RegistrarClienteUseCase registrarClienteUseCase;

    // DTOs internos para deserializar la respuesta de randomuser.me
    private record RandomUserResponse(List<RandomUserResult> results) {}
    private record RandomUserResult(RandomUserName name, String email, String phone, String gender) {}
    private record RandomUserName(String first, String last) {}

    @Override
    public void run(String... args) throws Exception {
        inicializarUsuarios();
        importarClientesDemo();
    }

    private void inicializarUsuarios() {
        if (userRepository.count() == 0) {
            UserEntity admin = UserEntity.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("1234"))
                    .role(Role.ROLE_ADMIN)
                    .email("admin@peluqueria.com")
                    .active(true)
                    .build();
            userRepository.save(admin);

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

    private void importarClientesDemo() {
        if (!clienteRepository.findAll().isEmpty()) return;

        try {
            RestClient restClient = RestClient.create();
            RandomUserResponse response = restClient.get()
                    .uri("https://randomuser.me/api/?results=25&nat=es")
                    .retrieve()
                    .body(RandomUserResponse.class);

            if (response == null || response.results() == null) return;

            for (RandomUserResult result : response.results()) {
                Cliente cliente = Cliente.builder()
                        .nombre(capitalizar(result.name().first()))
                        .apellidos(capitalizar(result.name().last()))
                        .telefono(result.phone())
                        .email(result.email())
                        .genero("female".equals(result.gender()) ? Genero.FEMENINO : Genero.MASCULINO)
                        .esVip(false)
                        .consentimientoFotos(false)
                        .fechaRegistro(LocalDateTime.now())
                        .build();
                registrarClienteUseCase.ejecutar(cliente);
            }

            System.out.println("Importados " + response.results().size() + " clientes demo desde randomuser.me");
        } catch (Exception e) {
            System.out.println("Aviso: no se pudieron importar clientes demo (" + e.getMessage() + ")");
        }
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return texto;
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1).toLowerCase();
    }
}
