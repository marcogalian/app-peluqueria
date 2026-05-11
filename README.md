# Peluqueria Isabella - Sistema de Gestion

Aplicacion full-stack para gestionar una peluqueria desde un panel web: agenda, clientes, empleados, servicios, inventario, ventas, ausencias, resultados, mensajeria interna y asistente de gestion con IA.

El proyecto esta desarrollado como aplicacion profesional de gestion para un salon realista, con backend Spring Boot, frontend Nuxt y despliegue local con Docker Compose.

## Indice rapido

- [Que hace la aplicacion](#que-hace-la-aplicacion)
- [Stack tecnico](#stack-tecnico)
- [Arquitectura](#arquitectura)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Arranque rapido](#arranque-rapido)
- [Usuarios demo](#usuarios-demo)
- [Documentacion](#documentacion)
- [Comandos utiles](#comandos-utiles)

## Que hace la aplicacion

### Panel administrador

- Panel de control con KPIs, evolucion de ventas, citas, inventario y alertas.
- Agenda visual para crear, editar, cancelar y completar citas.
- Gestion de clientes con datos personales, VIP, historial, consentimiento y fotos.
- Gestion de empleados, especialidades, disponibilidad, bajas y vacaciones.
- Catalogo de servicios con precio, duracion, categoria y genero.
- Inventario de productos con stock, stock minimo, filtros y ventas.
- Mensajeria interna y envio de emails a empleados.
- Resultados financieros: ingresos, gastos, beneficios, productos y rendimiento.
- Configuracion del negocio, dias especiales y dias bloqueados para vacaciones.
- Asistente de gestion para consultar datos del negocio.

### Portal empleado

- Agenda propia.
- Mensajes internos.
- Ventas de productos.
- Solicitud de vacaciones y bajas.
- Notificaciones de aprobacion o rechazo de ausencias.
- Asistente de gestion limitado por rol.

## Stack tecnico

### Backend

| Tecnologia | Uso |
|---|---|
| Java 21 | Lenguaje principal |
| Spring Boot 3.5 | API REST y aplicacion backend |
| Spring Security | Autenticacion y autorizacion |
| JWT + Refresh Token | Sesiones stateless |
| Spring Data JPA + Hibernate | Persistencia |
| PostgreSQL 15 | Base de datos principal |
| H2 | Base de datos para tests |
| MapStruct | Mapeo entre dominio y entidades |
| Lombok | Reduccion de boilerplate |
| WebSocket STOMP | Chat interno en tiempo real |
| Spring Mail + Mailtrap | Emails en entorno de pruebas |
| Gemini API | Asistente IA |
| Springdoc OpenAPI | Swagger UI y contrato REST |

### Frontend

| Tecnologia | Uso |
|---|---|
| Nuxt 4 | Framework frontend |
| Vue 3 | UI con Composition API |
| TypeScript | Tipado |
| Pinia | Estado global |
| Tailwind CSS | Estilos |
| Axios | Cliente HTTP |
| FullCalendar | Agenda visual |
| Chart.js | Graficas |
| Lucide | Iconografia |

### Infraestructura

| Tecnologia | Uso |
|---|---|
| Docker Compose | Orquestacion local |
| PostgreSQL Docker | Base de datos persistente |
| Volumen `uploads_data` | Fotos y ficheros subidos |
| Volumen `postgres_data` | Datos de PostgreSQL |

## Arquitectura

El backend sigue una combinacion de Vertical Slicing, Screaming Architecture y arquitectura hexagonal implicita.

Cada modulo de negocio tiene su propio corte vertical:

```text
backend-spring/src/main/java/com/marcog/peluqueria/<modulo>/
├── application      # Casos de uso: GestionarAgenda, RegistrarCliente...
├── domain           # Modelo de negocio y contratos: Cliente, ClienteRepository...
└── infrastructure   # Web, persistencia, email, Gemini, configuracion...
```

La arquitectura evita nombres tecnicos ruidosos como `Port`, `Adapter`, `In` u `Out`. Los contratos viven en `domain` con nombres de negocio y las implementaciones viven en `infrastructure` indicando la tecnologia cuando aporta claridad, por ejemplo `PostgresClienteRepository`.

El frontend usa vertical slicing por modulo. Las rutas de Nuxt en `app/pages` son wrappers finos y las pantallas reales viven en `app/modules`.

```text
front-nuxt/app/
├── pages                 # Rutas Nuxt muy finas
├── modules               # Agenda, clientes, inventario, mensajes...
├── components            # Layout y componentes globales
└── infrastructure/http   # Cliente API
```

## Estructura del repositorio

```text
peluqueria/
├── backend-spring/       # API Spring Boot
├── front-nuxt/           # Aplicacion Nuxt
├── docker/               # Docker Compose y variables de entorno
├── docs/                 # Documentacion funcional y tecnica
├── specs/                # Especificaciones y decisiones de arquitectura
└── plan-*.md             # Planes de mejora y release
```

## Arranque rapido

### Requisitos

- Docker Desktop
- Java 21, si se ejecuta backend fuera de Docker
- Node.js 22, si se ejecuta frontend fuera de Docker

### Con Docker

```bash
cd docker
docker compose up --build -d
```

Servicios:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

### Sin Docker

Backend:

```bash
cd backend-spring
./mvnw spring-boot:run
```

Frontend:

```bash
cd front-nuxt
npm install
npm run dev
```

## Usuarios demo

| Usuario | Contrasena | Rol |
|---|---|---|
| `admin` | `1234` | Administrador |
| `sofia` | `1234` | Empleada |
| `carmen` | `1234` | Empleada |
| `lucia` | `1234` | Empleada |

## Documentacion

- [Guia funcional](docs/guia-funcional.md)
- [Guia tecnica](docs/guia-tecnica.md)
- [Arranque y configuracion](docs/arranque-y-configuracion.md)
- [Pruebas y validacion](docs/pruebas-y-validacion.md)
- [Javadoc y documentacion de codigo](docs/javadoc.md)
- [Arquitectura backend](specs/arquitectura-backend-hexagonal.md)
- [Arquitectura frontend](specs/arquitectura-frontend-vertical-slicing.md)
- [Memoria de progreso](specs/memoria_progreso_2026-05-09.md)

## Comandos utiles

Backend:

```bash
cd backend-spring
./mvnw test
./mvnw clean test
./mvnw javadoc:javadoc
```

Frontend:

```bash
cd front-nuxt
npm install
npm run dev
npm run build
npm test
```

Docker:

```bash
cd docker
docker compose ps
docker compose logs api --tail 120
docker compose logs front --tail 120
docker compose up --build -d
```

## Estado actual

El proyecto esta en fase de cierre funcional. Queda recomendado realizar una ronda completa de pruebas manuales, revisar textos finales de documentacion y preparar capturas o demo guiada para presentacion.
