# Backend Spring Boot

API REST de Peluqueria Isabella.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security
- JWT + refresh token
- Spring Data JPA
- PostgreSQL
- H2 para tests
- MapStruct
- Lombok
- Springdoc OpenAPI
- WebSocket STOMP
- Mailtrap, Twilio y Gemini

## Arquitectura

El backend esta organizado por modulos de negocio. Cada modulo contiene tres capas:

```text
<modulo>/
├── application
├── domain
└── infrastructure
```

### `application`

Casos de uso y orquestacion de negocio.

Ejemplos:

- `GestionarAgenda`
- `GestionarInventario`
- `GestionarAusencias`
- `ResponderConsultasGestion`
- `AutenticarUsuario`

### `domain`

Modelo de negocio y contratos.

Ejemplos:

- `Cliente`
- `Cita`
- `Producto`
- `ClienteRepository`
- `NotificadorCitas`

### `infrastructure`

Detalles tecnicos:

- REST controllers.
- Entidades JPA.
- Repositorios Spring Data.
- Integraciones externas.
- Configuracion.

Ejemplos:

- `ClienteController`
- `PostgresClienteRepository`
- `JpaClienteRepository`
- `GeminiModeloLenguaje`

## Modulos principales

- `security`: login, JWT, refresh token y roles.
- `clientes`: clientes, VIP, historial y consentimiento.
- `citas`: agenda, solapes, estados, tickets y recordatorios.
- `peluqueros`: empleados, especialidades y disponibilidad.
- `ausencias`: vacaciones, bajas, aprobacion y dias bloqueados.
- `servicios`: catalogo de servicios.
- `productos`: inventario, stock y ventas.
- `ofertas`: promociones y dias especiales.
- `finanzas`: panel financiero y resultados.
- `chat`: mensajeria interna.
- `chatbot`: asistente de gestion.
- `fotos`: fotos de clientes.
- `configuracion`: datos generales del negocio.

## Ejecutar

```bash
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

## Tests

```bash
./mvnw test
```

En Windows PowerShell:

```powershell
.\mvnw.cmd test
```

## Javadoc

```bash
./mvnw javadoc:javadoc
```

Salida:

```text
target/site/apidocs/index.html
```

Guia de criterio: [`../docs/javadoc.md`](../docs/javadoc.md).

## Swagger

Con la aplicacion levantada:

```text
http://localhost:8080/swagger-ui.html
```

## Datos demo

Se crean al arrancar si la base esta vacia:

| Usuario | Contrasena | Rol |
|---|---|---|
| `admin` | `1234` | Administrador |
| `sofia` | `1234` | Empleada |
| `carmen` | `1234` | Empleada |
| `lucia` | `1234` | Empleada |
