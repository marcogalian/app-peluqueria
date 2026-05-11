# Guia tecnica

Esta guia resume como esta hecha la aplicacion por dentro.

## Vision general

La aplicacion se divide en tres partes:

```text
Frontend Nuxt -> Backend Spring Boot -> PostgreSQL
```

El frontend consume una API REST protegida con JWT. El backend aplica reglas de negocio, persiste datos con JPA y expone documentacion OpenAPI con Swagger UI.

## Backend

Ruta principal:

```text
backend-spring/src/main/java/com/marcog/peluqueria
```

La estructura sigue Vertical Slicing + Screaming Architecture + Hexagonal implicita.

Cada modulo intenta expresar negocio desde la carpeta:

```text
clientes/
citas/
productos/
servicios/
peluqueros/
ausencias/
finanzas/
chat/
chatbot/
security/
```

Y por dentro:

```text
<modulo>/
├── application
├── domain
└── infrastructure
```

### `application`

Contiene casos de uso con nombres funcionales:

- `GestionarAgenda`
- `RegistrarClienteEnSistema`
- `GestionarInventario`
- `GestionarAusencias`
- `ResponderConsultasGestion`
- `AutenticarUsuario`

La capa de aplicacion coordina reglas de negocio y dependencias, pero no deberia conocer detalles de HTTP ni de JPA.

### `domain`

Contiene:

- Entidades de negocio.
- Enums.
- Excepciones de dominio.
- Contratos de repositorio o servicios externos.

Ejemplos:

- `Cliente`
- `Producto`
- `Cita`
- `ClienteRepository`
- `ProductoRepository`
- `NotificadorCitas`
- `ModeloLenguaje`

### `infrastructure`

Contiene detalles tecnicos:

- Controladores REST en `web`.
- Entidades JPA, mappers y repositorios en `persistence`.
- Integraciones como email, Gemini, contexto del chatbot o configuracion.

Ejemplos:

- `ClienteController`
- `PostgresClienteRepository`
- `JpaClienteRepository`
- `GeminiModeloLenguaje`
- `EmailNotificadorCitas`

## Frontend

Ruta principal:

```text
front-nuxt/app
```

### Rutas

Nuxt mantiene las rutas en `app/pages`, pero esas rutas son wrappers finos:

```text
app/pages/admin/clientes.vue -> app/modules/clientes/pages/ClientesAdminPage.vue
```

Esto permite respetar el enrutado de Nuxt sin mezclar todas las pantallas en una carpeta gigante.

### Modulos

Las pantallas reales viven en:

```text
front-nuxt/app/modules
```

Modulos actuales:

- `agenda`
- `ausencias`
- `auth`
- `calendario`
- `chat`
- `chatbot`
- `clientes`
- `configuracion`
- `dashboard`
- `empleados`
- `inventario`
- `mensajes`
- `resultados`
- `servicios`
- `ventas`

Cada modulo puede tener:

- `pages`
- `components`
- `services`
- `composables`
- `types`
- `store`

## Seguridad

El login genera:

- Access token JWT.
- Refresh token.

El frontend guarda sesion mediante el store de autenticacion. El cliente Axios centralizado anade el token y renueva sesion cuando procede.

El backend protege rutas con Spring Security y permisos por rol.

## API REST

La documentacion OpenAPI esta disponible en:

```text
http://localhost:8080/swagger-ui.html
```

Endpoints principales:

- `/api/auth`
- `/api/clientes`
- `/api/citas`
- `/api/peluqueros`
- `/api/servicios`
- `/api/productos`
- `/api/ausencias`
- `/api/dias-bloqueados`
- `/api/ofertas`
- `/api/finanzas`
- `/api/mensajes`
- `/api/chat`

## Chat y asistente

Hay dos piezas distintas:

### Mensajeria interna

Modulo `chat`:

- Mensajes internos.
- Emails manuales.
- Archivo y eliminacion.
- WebSocket STOMP para comunicacion en tiempo real.

### Asistente de gestion

Modulo `chatbot`:

- Consulta datos del negocio.
- Usa Gemini para preguntas abiertas.
- Usa funciones directas contra BD para datos sensibles o concretos.

Consultas basicas como clientes VIP o total de clientes no dependen de Gemini.

## Persistencia

La base de datos principal es PostgreSQL.

El backend usa:

- Entidades JPA en `infrastructure/persistence`.
- Repositorios Spring Data `Jpa*Repository`.
- Adaptadores con nombre de tecnologia `Postgres*Repository`.
- Modelos de dominio fuera de JPA.

## Documentacion generada

Documentacion disponible:

- Swagger UI para API REST.
- Javadoc para codigo Java.
- Markdown manual en `docs/` y `specs/`.

## Decisiones importantes

- No usar nombres `Port`, `Adapter`, `In`, `Out` en clases.
- Mantener los nombres de aplicacion orientados a caso de uso.
- Mantener el frontend organizado por dominio, no por tipo tecnico.
- Evitar paneles laterales en secciones donde el usuario prefiere desplegables inline.
- Dejar consultas criticas del asistente resueltas por backend cuando sea posible.
