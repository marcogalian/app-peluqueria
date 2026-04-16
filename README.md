# Sistema de Gestión de Peluquería

Aplicación web full-stack para la gestión integral de un salón de peluquería.
Desarrollada como Trabajo de Fin de Grado (DAM/DAW).

---

## Tecnologías

### Backend (`backend-spring/`)
| Tecnología | Versión | Uso |
|---|---|---|
| Spring Boot | 3.5.9 | Framework principal |
| Java | 21 | Lenguaje |
| Spring Security + JWT | — | Autenticación stateless |
| Spring Data JPA + Hibernate | — | Persistencia |
| PostgreSQL | 15 | Base de datos |
| MapStruct | — | Mapeo entidad↔dominio |
| Lombok | — | Reducción de boilerplate |
| WebSocket STOMP | — | Chat en tiempo real |
| Twilio | — | Notificaciones WhatsApp |

### Frontend (`front-nuxt/`)
| Tecnología | Versión | Uso |
|---|---|---|
| Nuxt | 4 | Framework Vue SSR/SPA |
| Vue 3 | — | Composition API |
| TypeScript | strict | Tipado estático |
| Tailwind CSS | 4 | Estilos |
| Pinia | — | Estado global |
| FullCalendar | — | Calendario de citas |
| Chart.js + vue-chartjs | — | Gráficas de finanzas |
| STOMP.js | — | Cliente WebSocket |
| Axios | — | Cliente HTTP |

### Infraestructura (`docker/`)
| Tecnología | Uso |
|---|---|
| Docker + Docker Compose | Contenedores |
| PostgreSQL 15 (imagen oficial) | Base de datos |

---

## Arquitectura

### Backend — Arquitectura Hexagonal (Ports & Adapters)

```
backend-spring/src/main/java/com/marcog/peluqueria/
│
├── [módulo]/
│   ├── domain/
│   │   ├── model/          ← Entidades de negocio puras
│   │   └── port/
│   │       ├── in/         ← Casos de uso (interfaces)
│   │       └── out/        ← Puertos de salida (interfaces)
│   ├── application/
│   │   └── service/        ← Implementación de casos de uso
│   └── infrastructure/
│       ├── in/web/         ← Controladores REST
│       └── out/persistence/ ← JPA: entidades, mappers, repositorios
```

**Módulos implementados:**
- `security` — Autenticación JWT + refresh token
- `clientes` — Gestión de clientes con historial clínico, VIP, consentimiento fotos
- `citas` — Agenda de citas con estados, notificaciones y recordatorios
- `peluqueros` — Gestión de empleados, disponibilidad, especialidades
- `servicios` — Catálogo de servicios por género y precio
- `productos` — Inventario de productos con control de stock
- `ausencias` — Solicitudes de vacaciones/bajas con flujo de aprobación
- `fotos` — Galería fotográfica de clientes (almacenamiento local)
- `ofertas` — Promociones temporales y días especiales
- `finanzas` — Dashboard de ingresos, gastos y KPIs
- `chat` — Mensajería interna en tiempo real (WebSocket + AES-256)

### Frontend — Screaming Architecture

```
front-nuxt/app/
│
├── modules/                ← Organizado por dominio de negocio
│   ├── auth/
│   ├── agenda/
│   ├── calendario/
│   ├── clientes/
│   ├── empleados/
│   ├── vacaciones/
│   ├── productos/
│   ├── servicios/
│   ├── finanzas/
│   └── mensajes/
│
├── shared/                 ← Componentes y utilidades transversales
│   ├── components/ui/
│   ├── components/layout/
│   └── composables/
│
└── infrastructure/
    └── http/api.ts         ← Cliente Axios centralizado con interceptors JWT
```

---

## Estructura del repositorio

```
app-peluqueria/
├── backend-spring/     ← API REST Spring Boot
├── front-nuxt/         ← SPA Nuxt 4
└── docker/             ← Docker Compose para desarrollo y producción
```

---

## Instalación y ejecución

### Requisitos previos
- Docker Desktop
- Java 21 (para desarrollo local del backend)
- Node.js 22 (para desarrollo local del frontend)

### Con Docker (recomendado)

```bash
cd docker
docker compose up --build
```

| Servicio | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

### Desarrollo local

**Backend:**
```bash
cd backend-spring
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd front-nuxt
npm install
npm run dev
```

---

## Variables de entorno

Crea un archivo `.env` en `docker/` con las siguientes variables opcionales:

```env
MAILTRAP_USERNAME=tu_usuario
MAILTRAP_PASSWORD=tu_password
TWILIO_ACCOUNT_SID=tu_sid
TWILIO_AUTH_TOKEN=tu_token
TWILIO_WHATSAPP_FROM=whatsapp:+14155238886
```

---

## Metodología de desarrollo

- **GitFlow**: ramas `main`, `develop` y `feature/*` por funcionalidad
- **Arquitectura**: Hexagonal (backend) + Screaming Architecture (frontend)
- **Principios**: SOLID aplicados en ambas capas
