package com.marcog.peluqueria;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marcog.peluqueria.clientes.domain.Genero;
import com.marcog.peluqueria.clientes.infrastructure.persistence.ClienteEntity;
import com.marcog.peluqueria.clientes.infrastructure.persistence.JpaClienteRepository;
import com.marcog.peluqueria.ofertas.domain.TipoOferta;
import com.marcog.peluqueria.ofertas.infrastructure.persistence.JpaOfertaRepository;
import com.marcog.peluqueria.ofertas.infrastructure.persistence.OfertaEntity;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.JpaPeluqueroRepository;
import com.marcog.peluqueria.peluqueros.infrastructure.persistence.PeluqueroEntity;
import com.marcog.peluqueria.productos.domain.CategoriaProducto;
import com.marcog.peluqueria.productos.domain.GeneroProducto;
import com.marcog.peluqueria.productos.infrastructure.persistence.JpaProductoRepository;
import com.marcog.peluqueria.productos.infrastructure.persistence.ProductoEntity;
import com.marcog.peluqueria.security.domain.Role;
import com.marcog.peluqueria.security.infrastructure.persistence.JpaUserRepository;
import com.marcog.peluqueria.security.infrastructure.persistence.UserEntity;
import com.marcog.peluqueria.servicios.domain.CategoriaServicio;
import com.marcog.peluqueria.servicios.domain.TipoGenero;
import com.marcog.peluqueria.servicios.infrastructure.persistence.JpaServicioRepository;
import com.marcog.peluqueria.servicios.infrastructure.persistence.ServicioEntity;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@email.com";

    private final JpaUserRepository      userRepository;
    private final JpaPeluqueroRepository peluqueroRepository;
    private final JpaClienteRepository   clienteRepository;
    private final JpaServicioRepository  servicioRepository;
    private final JpaProductoRepository  productoRepository;
    private final JpaOfertaRepository    ofertaRepository;
    private final PasswordEncoder        passwordEncoder;
    @Value("${app.seed.remote-clients.enabled:true}")
    private boolean remoteClientsEnabled;
    @Value("${app.seed.demo-password:}")
    private String demoPassword;
    @Value("${app.seed.sync-demo-passwords:false}")
    private boolean syncDemoPasswords;

    @Override
    public void run(String... args) {
        crearAdminSiNoExiste();
        sincronizarEmailsDemo();
        if (peluqueroRepository.count() == 0) {
            crearPeluqueros();
        }
        if (clienteRepository.count() == 0) {
            crearClientes();
        }
        crearServicios();
        if (productoRepository.count() == 0) {
            crearProductos();
        }
        if (ofertaRepository.count() == 0) {
            crearOfertas();
        }
    }

    private void crearAdminSiNoExiste() {
        var adminExistente = userRepository.findByUsername("admin");
        if (adminExistente.isPresent()) {
            UserEntity admin = adminExistente.get();
            boolean modificado = false;
            if (!ADMIN_EMAIL.equalsIgnoreCase(admin.getEmail())) {
                admin.setEmail(ADMIN_EMAIL);
                modificado = true;
            }
            if (syncDemoPasswords && !passwordEncoder.matches(requireDemoPassword(), admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(requireDemoPassword()));
                modificado = true;
            }
            if (modificado) {
                userRepository.save(admin);
                log.info("DataInitializer: administrador demo sincronizado.");
            }
            return;
        }

        userRepository.save(UserEntity.builder()
                .username("admin")
                .password(passwordEncoder.encode(requireDemoPassword()))
                .role(Role.ROLE_ADMIN)
                .email(ADMIN_EMAIL)
                .active(true)
                .build());

        log.info("DataInitializer: usuario administrador demo creado.");
    }

    private void sincronizarEmailsDemo() {
        record UsuarioDemo(String username, String email) {}

        List<UsuarioDemo> usuarios = List.of(
                new UsuarioDemo("sofia", "sofia@email.com"),
                new UsuarioDemo("carmen", "carmen@email.com"),
                new UsuarioDemo("lucia", "lucia@email.com"),
                new UsuarioDemo("maria", "maria@email.com")
        );

        int actualizados = 0;
        for (UsuarioDemo usuarioDemo : usuarios) {
            var usuario = userRepository.findByUsername(usuarioDemo.username());
            if (usuario.isPresent()) {
                UserEntity user = usuario.get();
                boolean modificado = false;
                if (!usuarioDemo.email().equalsIgnoreCase(user.getEmail())) {
                    user.setEmail(usuarioDemo.email());
                    modificado = true;
                }
                if (syncDemoPasswords && !passwordEncoder.matches(requireDemoPassword(), user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(requireDemoPassword()));
                    modificado = true;
                }
                if (modificado) {
                    userRepository.save(user);
                    actualizados++;
                }
            }
        }

        if (actualizados > 0) {
            log.info("DataInitializer: {} usuarios demo sincronizados.", actualizados);
        }
    }

    private void crearPeluqueros() {
        record P(String user, String nombre, String especialidad, String horario) {}

        List<P> datos = List.of(
            new P("sofia",  "Sofía Martínez",  "Colorimetría y mechas",    "L-V 09:00-17:00"),
            new P("carmen", "Carmen López",    "Corte femenino y alisados", "L-V 10:00-18:00"),
            new P("lucia",  "Lucía Fernández", "Tratamientos capilares",    "M-S 09:00-17:00")
        );

        for (P p : datos) {
            UserEntity u = userRepository.save(UserEntity.builder()
                    .username(p.user())
                    .password(passwordEncoder.encode(requireDemoPassword()))
                    .role(Role.ROLE_HAIRDRESSER)
                    .email(p.user() + "@email.com")
                    .active(true)
                    .build());

            peluqueroRepository.save(PeluqueroEntity.builder()
                    .user(u)
                    .nombre(p.nombre())
                    .especialidad(p.especialidad())
                    .especialidades(p.especialidad())
                    .horarioBase(p.horario())
                    .disponible(true)
                    .enBaja(false)
                    .enVacaciones(false)
                    .build());
        }
        log.info("DataInitializer: 3 peluqueras demo creadas.");
    }

    private String requireDemoPassword() {
        if (demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalStateException(
                    "APP_DEMO_PASSWORD debe estar configurada para crear usuarios demo.");
        }
        return demoPassword;
    }

    private void crearServicios() {
        record S(String nombre, String descripcion, double precio, int minutos,
                 TipoGenero genero, CategoriaServicio categoria) {}

        List<S> datos = List.of(
            new S("Corte de cabello mujer",  "Corte personalizado con acabado profesional",           25.00, 45,  TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Corte de cabello hombre", "Corte clásico o moderno con lavado incluido",           15.00, 30,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Mechas californianas",    "Técnica de iluminación degradada sin raíz marcada",     75.00, 120, TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Coloración completa",     "Tinte de raíz a puntas con fórmula personalizada",     55.00, 90,  TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Balayage",                "Coloración a mano alzada para efecto natural",          85.00, 150, TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Alisado brasileño",       "Tratamiento de alisado duradero con keratina",         90.00, 180, TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Tratamiento hidratación", "Mascarilla nutritiva profesional con vapor",            35.00, 60,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Peinado de novia",        "Recogido o semirecogido para eventos especiales",       80.00, 90,  TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Lavado y secado",         "Lavado con champú profesional y secado con moldeado",  18.00, 30,  TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Permanente",              "Ondulación duradera con fijación en frío o calor",     60.00, 120, TipoGenero.FEMENINO,  CategoriaServicio.SENORA),
            new S("Depilación facial",       "Perfilado de cejas, orejas o pómulos con acabado limpio", 12.00, 20, TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Tinte de barba",          "Coloración y perfilado profesional de barba",          20.00, 30,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Corte degradado premium", "Fade con contornos definidos y acabado de precisión",  19.00, 35,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Arreglo de barba",        "Diseño, perfilado y acabado con toalla caliente",      14.00, 25,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Lavado detox hombre",     "Lavado profundo con masaje capilar y producto detox",  16.00, 20,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO),
            new S("Cobertura de canas",      "Aplicación express de color para cubrir canas",        24.00, 35,  TipoGenero.MASCULINO, CategoriaServicio.CABALLERO)
        );

        int creados = 0;
        for (S s : datos) {
            ServicioEntity servicio = servicioRepository.findByNombreIgnoreCase(s.nombre())
                    .orElseGet(() -> {
                        ServicioEntity nuevo = new ServicioEntity();
                        nuevo.setNombre(s.nombre());
                        return nuevo;
                    });

            boolean esNuevo = servicio.getId() == null;
            servicio.setDescripcion(s.descripcion());
            servicio.setPrecio(BigDecimal.valueOf(s.precio()));
            servicio.setDuracionMinutos(s.minutos());
            servicio.setGenero(s.genero());
            servicio.setCategoria(s.categoria());
            servicioRepository.save(servicio);

            if (esNuevo) {
                creados++;
            }
        }
        log.info("DataInitializer: catálogo de servicios sincronizado. {} nuevos, {} total esperado.", creados, datos.size());
    }

    private void crearProductos() {
        record P(String nombre, String descripcion, double precio, int stock, int stockMin,
                 CategoriaProducto cat, GeneroProducto genero) {}

        List<P> datos = List.of(
            new P("Champú hidratante Wella",        "Champú profesional para cabello seco y dañado",          12.50, 30, 5,  CategoriaProducto.CHAMPU,        GeneroProducto.FEMENINO),
            new P("Champú anticaída Kerastase",     "Fórmula reforzante con aminoácidos y cafeína",            18.00, 20, 4,  CategoriaProducto.CHAMPU,        GeneroProducto.MASCULINO),
            new P("Champú equilibrante Unisex",     "Para uso diario con pH neutro y extractos naturales",    10.00, 25, 5,  CategoriaProducto.CHAMPU,        GeneroProducto.UNISEX),
            new P("Acondicionador nutritivo L'Oréal","Mascarilla acondicionadora con aceite de argán",         14.00, 25, 5,  CategoriaProducto.ACONDICIONADOR, GeneroProducto.FEMENINO),
            new P("Mascarilla reparadora Redken",   "Tratamiento intensivo para puntas abiertas",              22.00, 15, 3,  CategoriaProducto.ACONDICIONADOR, GeneroProducto.UNISEX),
            new P("Tinte rubio miel Schwarzkopf",   "Coloración permanente tono 7.3 rubio miel",               9.50, 40, 8,  CategoriaProducto.COLORACION,    GeneroProducto.FEMENINO),
            new P("Tinte castaño oscuro L'Oréal",  "Coloración permanente tono 3.0 castaño oscuro",           9.00, 35, 8,  CategoriaProducto.COLORACION,    GeneroProducto.UNISEX),
            new P("Decolorante en polvo Wella",     "Polvo decolorante de alta potencia hasta 7 tonos",       11.00, 20, 5,  CategoriaProducto.COLORACION,    GeneroProducto.UNISEX),
            new P("Cera modeladora Gatsby",         "Cera de fijación fuerte con acabado mate",                8.00, 30, 6,  CategoriaProducto.CERA,          GeneroProducto.MASCULINO),
            new P("Pomada acabado brillo American Crew","Fijación media con brillo intenso para hombre",      11.50, 20, 4,  CategoriaProducto.CERA,          GeneroProducto.MASCULINO),
            new P("Laca fijadora Elnett",           "Laca de fijación fuerte y larga duración",                7.50, 35, 7,  CategoriaProducto.OTRO,          GeneroProducto.FEMENINO),
            new P("Plancha cerámica Babyliss",      "Plancha profesional 230°C con placas flotantes",         89.00,  5, 1,  CategoriaProducto.HERRAMIENTA,   GeneroProducto.UNISEX),
            new P("Rizador automático Remington",   "Rizador de barril intercambiable, 180-210°C",            65.00,  4, 1,  CategoriaProducto.HERRAMIENTA,   GeneroProducto.FEMENINO),
            new P("Sérum antifrizz Moroccanoil",    "Aceite de argán puro para control del encrespamiento",   28.00, 18, 4,  CategoriaProducto.OTRO,          GeneroProducto.UNISEX),
            new P("Spray termoprotector Tresemmé",  "Protección hasta 230°C antes de aplicar calor",           9.00, 25, 5,  CategoriaProducto.OTRO,          GeneroProducto.UNISEX)
        );

        for (P p : datos) {
            productoRepository.save(ProductoEntity.builder()
                    .nombre(p.nombre())
                    .descripcion(p.descripcion())
                    .precio(BigDecimal.valueOf(p.precio()))
                    .stock(p.stock())
                    .stockMinimo(p.stockMin())
                    .categoria(p.cat())
                    .genero(p.genero())
                    .activo(true)
                    .build());
        }
        log.info("DataInitializer: 15 productos creados.");
    }

    private void crearClientes() {
        if (!remoteClientsEnabled) {
            log.info("DataInitializer: seed remoto desactivado, usando clientes locales.");
            crearClientesFallback();
            return;
        }

        try {
            RestTemplate rest = new RestTemplate();
            RandomUserResponse response = rest.getForObject(
                    "https://randomuser.me/api/?results=30&nat=es&inc=name,email,phone,gender",
                    RandomUserResponse.class
            );

            if (response == null || response.getResults() == null) {
                log.warn("randomuser.me no disponible, usando clientes locales de respaldo.");
                crearClientesFallback();
                return;
            }

            for (RandomUserResult r : response.getResults()) {
                String nombre   = capitalize(r.getName().getFirst());
                String apellido = capitalize(r.getName().getLast());
                String telefono = r.getPhone().replaceAll("[^0-9]", "");
                telefono = telefono.substring(0, Math.min(9, telefono.length()));
                Genero genero   = "female".equals(r.getGender()) ? Genero.FEMENINO : Genero.MASCULINO;

                clienteRepository.save(ClienteEntity.builder()
                        .nombre(nombre)
                        .apellidos(apellido)
                        .telefono(telefono)
                        .email(r.getEmail())
                        .genero(genero)
                        .consentimientoFotos(true)
                        .build());
            }
            log.info("DataInitializer: 30 clientes creados desde randomuser.me.");
        } catch (Exception e) {
            log.warn("No se pudieron obtener clientes de randomuser.me: {}. Usando clientes locales.", e.getMessage());
            crearClientesFallback();
        }
    }

    private void crearClientesFallback() {
        record C(String nombre, String apellidos, String telefono, String email, Genero genero) {}

        List<C> datos = List.of(
                new C("Laura", "Navarro Ruiz", "600123111", "laura.navarro@correo.test", Genero.FEMENINO),
                new C("Diego", "Sánchez Mora", "600123112", "diego.sanchez@correo.test", Genero.MASCULINO),
                new C("Elena", "Romero Vidal", "600123113", "elena.romero@correo.test", Genero.FEMENINO),
                new C("Pablo", "Ortega León", "600123114", "pablo.ortega@correo.test", Genero.MASCULINO),
                new C("Sara", "Gil Campos", "600123115", "sara.gil@correo.test", Genero.FEMENINO),
                new C("Álvaro", "Marín Soto", "600123116", "alvaro.marin@correo.test", Genero.MASCULINO),
                new C("Lucía", "Reyes Peña", "600123117", "lucia.reyes@correo.test", Genero.FEMENINO),
                new C("Adrián", "Castro Nieto", "600123118", "adrian.castro@correo.test", Genero.MASCULINO)
        );

        for (C c : datos) {
            clienteRepository.save(ClienteEntity.builder()
                    .nombre(c.nombre())
                    .apellidos(c.apellidos())
                    .telefono(c.telefono())
                    .email(c.email())
                    .genero(c.genero())
                    .consentimientoFotos(true)
                    .build());
        }

        log.info("DataInitializer: {} clientes locales de respaldo creados.", datos.size());
    }

    private void crearOfertas() {
        record O(String nombre, String descripcion, int pct, LocalDate inicio, LocalDate fin,
                 TipoOferta tipo, boolean activa) {}

        List<O> datos = List.of(
            new O("Martes de Color",     "20% de descuento en coloraciones cada martes",          20, LocalDate.of(2026, 4, 1),  LocalDate.of(2026, 4, 30), TipoOferta.SERVICIO, true),
            new O("Pack Primavera",      "15% en cortes y tratamientos durante toda la primavera", 15, LocalDate.of(2026, 3, 20), LocalDate.of(2026, 6, 20), TipoOferta.SERVICIO, true),
            new O("Descuento Fidelidad", "10% de descuento en productos para clientes habituales", 10, LocalDate.of(2026, 1, 1),  LocalDate.of(2026, 12, 31), TipoOferta.PRODUCTO, true),
            new O("Promo San Valentín",  "25% en peinados y recogidos para la ocasión especial",  25, LocalDate.of(2026, 2, 1),  LocalDate.of(2026, 2, 14), TipoOferta.SERVICIO, false)
        );

        for (O o : datos) {
            ofertaRepository.save(OfertaEntity.builder()
                    .nombre(o.nombre())
                    .descripcion(o.descripcion())
                    .descuentoPorcentaje(o.pct())
                    .fechaInicio(o.inicio())
                    .fechaFin(o.fin())
                    .tipo(o.tipo())
                    .activa(o.activa())
                    .build());
        }
        log.info("DataInitializer: 4 ofertas demo creadas.");
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RandomUserResponse {
        private List<RandomUserResult> results;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RandomUserResult {
        private String gender;
        private RandomUserName name;
        private String email;
        private String phone;
    }

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RandomUserName {
        private String first;
        private String last;
    }
}
