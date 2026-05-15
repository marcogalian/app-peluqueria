# SISTEMA INTEGRAL PARA LA GESTIÓN DE PELUQUERÍAS

**ISEN · Centro Universitario de Tecnología e Ingeniería — Cartagena, Murcia**

---

**2º DESARROLLO DE APLICACIONES MULTIPLATAFORMA**  
**PROYECTO FINAL DE GRADO SUPERIOR**

**Plataforma web full-stack · Spring Boot 3.5 + Nuxt 4 + PostgreSQL + Spring AI**

| | |
|---|---|
| **Alumno** | Marco Antonio Galián Raja |
| **Tutor** | Jairo Paul Moreno Villarroel |
| **Curso** | 2025-2026 |

---

## ÍNDICE

1. [Resumen](#1-resumen)
2. [Justificación](#2-justificación)
3. [Marco Teórico](#3-marco-teórico)
4. [Objetivos](#4-objetivos)
5. [Desarrollo del Proyecto](#5-desarrollo-del-proyecto)
6. [Conclusiones y Futuras Mejoras](#6-conclusiones-y-futuras-mejoras)
7. [Bibliografía](#7-bibliografía)
8. [Anexos](#8-anexos)

---

## 1. RESUMEN

El presente proyecto detalla el diseño, desarrollo e implementación de una solución tecnológica denominada **«Sistema Integral de Gestión de Peluquería»**. Se trata de una plataforma web orientada a la gestión profesional de salones de peluquería unisex, que centraliza en una única aplicación todas las operaciones del negocio: agenda de citas, gestión de clientes, empleados, inventario, ventas, finanzas, mensajería interna, notificaciones automáticas por email y un asistente de gestión con inteligencia artificial.

**Objetivo principal:** Desarrollar una aplicación web full-stack que digitalice la operativa diaria de un salón de belleza, optimizando los procesos de reserva, la gestión del equipo y la toma de decisiones financieras mediante datos en tiempo real y un asistente conversacional conectado al dominio del negocio.

**Tecnologías empleadas:**

- **Backend:** Java 21 sobre Spring Boot 3.5, con arquitectura hexagonal (Ports & Adapters) y screaming architecture.
- **Persistencia:** PostgreSQL 15 gestionado a través de Spring Data JPA e Hibernate.
- **Seguridad:** Spring Security con autenticación stateless basada en JWT y Refresh Tokens, rate limiting por IP mediante filtro anti-abuso configurable.
- **Frontend:** Nuxt 4 con Vue 3, TypeScript strict, Pinia y Tailwind CSS v4.
- **Mensajería interna:** sistema de correo interno entre empleados mediante Spring Mail (Mailtrap sandbox). Los mensajes se persisten en base de datos y se visualizan en una bandeja por contacto. Contador de mensajes no leídos en la barra de navegación.
- **Notificaciones:** Spring Mail (Mailtrap sandbox) para email ante eventos de negocio (ausencias, recordatorios, stock bajo, recuperación de contraseña).
- **Inteligencia Artificial:** Spring AI integrado con la API de OpenAI, con function calling para consultas en tiempo real sobre la base de datos.
- **Despliegue:** Docker + Docker Compose con tres servicios orquestados: PostgreSQL, API Spring Boot y frontend Nuxt.

**Alcance y funcionalidades clave:** El proyecto digitaliza completamente el flujo de trabajo del salón. Incluye gestión de clientes con historial clínico y consentimiento RGPD, motor de citas con validación de solapamientos, galería fotográfica de clientes, panel de KPIs financieros, mensajería interna cifrada con AES-256, recuperación de contraseña por email con token de un solo uso, cambio de contraseña para empleados por parte del administrador, auditoría de actividad, protección anti-abuso por IP y un asistente de IA con function calling sobre datos reales.

---

## 2. JUSTIFICACIÓN

### Problemática detectada

En la actualidad, una gran parte del sector de las peluquerías y centros de estética de pequeño y mediano tamaño sigue gestionando su operativa mediante métodos tradicionales: agendas de papel, hojas de cálculo o aplicaciones de mensajería genéricas. Esta falta de digitalización conlleva los siguientes problemas críticos:

1. **Conflictos de agenda:** el error humano al anotar citas provoca solapamientos, situando a dos clientes en el mismo tramo horario con el mismo empleado. Esto daña la imagen del negocio y genera pérdidas económicas directas.
2. **Gestión deficiente del equipo:** coordinar vacaciones, bajas y disponibilidad del personal de forma manual es propenso a errores. Sin visibilidad sobre el estado del equipo la planificación de la agenda se vuelve inviable.
3. **Inseguridad de datos:** el manejo de información personal de clientes sin protocolos adecuados no cumple con el Reglamento General de Protección de Datos (RGPD).
4. **Falta de análisis financiero:** sin registro digital estructurado es imposible calcular ingresos reales, gastos operativos y beneficios de forma automática.
5. **Comunicación interna ineficiente:** el uso de aplicaciones externas de mensajería supone una brecha de seguridad y mezcla el ámbito personal con el profesional.
6. **Ausencia de notificaciones automáticas:** sin sistema de avisos, los clientes no reciben recordatorios de cita y los administradores no son alertados de situaciones críticas como el bajo stock de productos.

### Destinatarios del proyecto

Este software está dirigido a dos perfiles principales dentro del salón:

- **Administradores o propietarios del centro:** necesitan visión global del negocio, incluyendo control de agenda, resultados financieros, estado del equipo, inventario, auditoría de actividad, gestión de ausencias y administración de contraseñas del personal.
- **Empleados (peluqueros/as):** necesitan una herramienta ágil para su agenda diaria, gestión de citas propias, solicitudes de vacaciones, comunicación interna, registro de ventas de productos y cambio seguro de su contraseña.

### Propuesta de mejora y valor añadido

Frente a soluciones genéricas como Fresha, Goldie o Booksy, que implican cesión de datos y coste recurrente por suscripción, este proyecto plantea una propuesta de valor diferenciada en cinco ejes:

1. **Excelencia en ingeniería de software:** Arquitectura Hexagonal, Screaming Architecture y principios SOLID en ambas capas garantizan mantenibilidad y escalabilidad.
2. **Seguridad de nivel profesional:** JWT + Refresh Tokens, cifrado AES-256 del chat, rate limiting por IP anti-abuso, recuperación segura de contraseña con token de un solo uso y validación de extensiones en subida de archivos.
3. **Notificaciones integradas:** el sistema envía avisos automáticos por email ante eventos de negocio sin dependencia de aplicaciones externas.
4. **Control total sobre los datos:** plataforma propia desplegable en el entorno del cliente mediante Docker Compose con un único comando.
5. **Asistente de IA integrado con el negocio:** Spring AI con function calling sobre OpenAI, conectado al dominio real de la peluquería con permisos por rol y contexto cacheado.

---

## 3. MARCO TEÓRICO

### Análisis del estado del arte

En el mercado actual de soluciones para la gestión de centros de estética y peluquería se observa una clara polarización. Por un lado, una gran parte del sector sigue utilizando métodos analógicos. Por otro, existen plataformas SaaS comerciales como Fresha, Goldie o Booksy que, aunque completas, presentan limitaciones importantes: dependencia de un proveedor externo, falta de personalización, coste recurrente por suscripción y cesión del control de los datos del negocio a terceros.

La presente solución se diferencia al ofrecer una plataforma propia desplegable en el entorno del cliente, con control total sobre los datos. La integración de inteligencia artificial como herramienta de gestión interna, la aplicación de patrones arquitectónicos de nivel empresarial y la hoja de ruta hacia un modelo SaaS multi-tenant representan diferenciaciones técnicas relevantes en el contexto de un proyecto académico de FP de grado superior.

### Tecnologías y herramientas utilizadas

#### Backend

**Java 21 y Spring Boot 3.5:** versión LTS con records, sealed classes y mejoras de rendimiento. Spring Boot 3.5 proporciona autoconfiguración, gestión de dependencias y servidor Tomcat embebido.

**Spring Data JPA e Hibernate:** repositorios genéricos que reducen código repetitivo; Hibernate como ORM para mapear entidades de negocio a tablas PostgreSQL 15.

**Spring Security + JWT:** autenticación stateless con JWT firmado de corta duración y Refresh Token de larga duración. Roles `ROLE_ADMIN` y `ROLE_HAIRDRESSER` controlados mediante anotaciones `@PreAuthorize`. El filtro `JwtAuthenticationFilter` captura `JwtException` para devolver 401 en vez de 500 ante tokens malformados.

**AntiAbuseFilter:** filtro de rate limiting por IP configurable mediante variables de entorno (`ANTI_ABUSE_GENERAL_PER_MINUTE`, `ANTI_ABUSE_AUTH_PER_MINUTE`, `ANTI_ABUSE_CHAT_PER_MINUTE`). Protege contra scraping, fuerza bruta y abuso de la API de IA sin depender de proxies externos.

**Spring Mail + mensajería interna:** los mensajes entre empleados se envían vía Spring Mail (Mailtrap sandbox) y se persisten en base de datos. El administrador puede escribir a cualquier empleado desde la bandeja de mensajes, que muestra el historial por contacto y un contador de no leídos.

**Spring Mail + Mailtrap:** envío de correo electrónico para notificaciones de resolución de ausencias, alertas de stock, recordatorios de cita y recuperación de contraseña. Mailtrap actúa como servidor SMTP sandbox en desarrollo.

**MapStruct y Lombok:** MapStruct genera automáticamente los mapeadores entre entidades de dominio y entidades JPA. Lombok elimina el código boilerplate mediante anotaciones.

**Spring AI + OpenAI:** Spring AI 1.x gestiona el ciclo completo de function calling. El modelo solicita una función de negocio, el backend la ejecuta contra PostgreSQL y devuelve el resultado al modelo para que redacte la respuesta. Hasta tres iteraciones de function calling por petición. Si `SPRING_AI_MODEL_CHAT=none`, el asistente responde con un mensaje informativo sin llamar a la API externa.

**Springdoc OpenAPI:** documentación automática de la API REST en formato OpenAPI 3.0 con Swagger UI. Desactivado por defecto en producción (`SWAGGER_ENABLED=false`).

**JUnit 5 + Mockito:** framework de pruebas unitarias con simulación de dependencias para el backend.

#### Frontend

**Nuxt 4 + Vue 3:** framework full-stack con enrutamiento automático basado en ficheros, layouts, composables y soporte SPA. Vue 3 con Composition API para componentes reactivos y mantenibles.

**TypeScript (strict):** tipado estático en modo estricto que detecta errores en tiempo de desarrollo. Todos los bloques `catch` usan `unknown` con `axios.isAxiosError()` en vez de `any`.

**Pinia:** store de estado global para Vue 3. Todos los accesos a `localStorage` y `window` están protegidos con `import.meta.client` para evitar errores en entornos SSR o edge middleware.

**Tailwind CSS v4:** framework de utilidades CSS para diseñar interfaces responsive y modernas.

**FullCalendar:** librería para la vista de agenda visual con soporte para arrastrar y soltar, y vistas de día, semana y mes.

**Chart.js + vue-chartjs:** librería de gráficas para visualizar ingresos, gastos, beneficios y tendencias en el panel de resultados financieros.

**Axios:** gestiona todas las llamadas HTTP del frontend, incluyendo el envío y consulta de mensajes internos a través de la API REST.

**Axios:** cliente HTTP con interceptores para la gestión automática del token JWT en cada petición a la API y refresco automático mediante Refresh Token.

**Vitest:** framework de pruebas unitarias compatible con el ecosistema Vite/Nuxt.

#### Infraestructura

**Docker + Docker Compose:** tres servicios orquestados (`db`, `api`, `front`) con health checks, política `restart: always` y volúmenes persistentes para datos de PostgreSQL y archivos subidos. Toda la configuración sensible se gestiona mediante variables de entorno en `docker/.env`, excluido del repositorio mediante `.gitignore`.

---

## 4. OBJETIVOS

### Objetivo General

Diseñar, desarrollar e implementar una aplicación web full-stack para la gestión profesional de peluquerías unisex que centralice la operativa diaria del negocio, garantizando la seguridad de los datos mediante arquitecturas limpias, e incorporando notificaciones automáticas por email y un asistente de inteligencia artificial conectado al dominio real del negocio para facilitar la toma de decisiones.

### Objetivos Específicos

1. Implementar una **arquitectura hexagonal (Ports & Adapters)** con screaming architecture, vertical slicing y principios SOLID, garantizando la separación entre dominio, aplicación e infraestructura en cada módulo del sistema.
2. Desarrollar un **sistema de seguridad multicapa** con Spring Security, JWT de corta duración, Refresh Tokens y rate limiting por IP, diferenciando roles de administrador y empleado tanto en el backend como en el frontend.
3. Diseñar un **motor de validación de disponibilidad** en la capa de dominio que prevenga el solapamiento de citas para el mismo empleado.
4. Implementar **control de concurrencia mediante semáforos** para serializar solicitudes simultáneas de vacaciones, garantizando que dos empleados no puedan ocupar los mismos días al mismo tiempo.
5. Desarrollar un **sistema de notificaciones automáticas por email** que informe a los usuarios ante eventos clave del negocio: resolución de ausencias, stock bajo, recordatorios de cita y recuperación de contraseña.
6. Implementar **recuperación segura de contraseña** mediante token de un solo uso enviado por email, con expiración temporal y uso único.
7. Permitir al **administrador gestionar las contraseñas** de los empleados directamente desde el panel, con notificación automática al afectado.
8. Integrar un **asistente de IA con Spring AI y OpenAI** con function calling para responder preguntas en lenguaje natural sobre datos reales del negocio, respetando permisos por rol.
9. Crear un **panel de control con KPIs financieros** en tiempo real (ingresos, gastos, ticket medio, tasa de cancelación, productos más vendidos, rendimiento por empleado).
10. Desarrollar un **módulo de mensajería interna** con envío de correo entre empleados, historial persistido en base de datos y contador de mensajes no leídos en tiempo real.
11. Implementar **auditoría automática** de todas las acciones de creación, modificación y eliminación realizadas por los usuarios.
12. **Desplegar la aplicación con Docker Compose** con un único comando en cualquier entorno con Docker instalado.
13. Cubrir la lógica de negocio crítica con **pruebas automatizadas**: solapamientos de agenda, concurrencia en ausencias, autenticación, recuperación de contraseña y comportamiento del asistente IA.

---

## 5. DESARROLLO DEL PROYECTO

### 5.1. Análisis y Requisitos

#### Requerimientos Funcionales

| Código | Módulo | Descripción |
|--------|--------|-------------|
| RF-01 | Autenticación | JWT + Refresh Token, BCrypt, recuperación de contraseña por email con token de un solo uso, cambio de contraseña por administrador |
| RF-02 | Agenda | Crear/editar/cancelar/completar citas; validación de solapamientos; calendario visual |
| RF-03 | Clientes | CRUD, tipo VIP, descuento, historial clínico, galería de fotos, consentimiento RGPD |
| RF-04 | Empleados | CRUD, especialidad, horario base, bajas médicas, disponibilidad |
| RF-05 | Servicios | Nombre, precio, duración en minutos, categoría y género |
| RF-06 | Inventario | Stock, alertas de mínimo, ventas por empleado, ajuste manual de stock |
| RF-07 | Ausencias | Solicitud, aprobación/rechazo, notificación email, semáforo de concurrencia |
| RF-08 | Resultados | KPIs por periodo, ticket medio, tasa cancelación, top servicios, gastos operativos |
| RF-09 | Mensajería interna | Envío de correo interno entre empleados vía Spring Mail, historial por contacto en BD, contador de no leídos en navegación |
| RF-10 | Notificaciones | Email: ausencias, stock bajo, recordatorios de cita, reset de contraseña, cambio de contraseña |
| RF-11 | Asistente IA | Function calling sobre OpenAI, permisos por rol, rechazo de temas ajenos al negocio |
| RF-12 | Panel de control | KPIs diarios, gráficas de evolución, alertas de stock y empleados disponibles |
| RF-13 | Auditoría | Registro automático con actor, rol, módulo, entidad, método HTTP y marca temporal |
| RF-14 | Configuración | Datos del centro, horarios, gestión de ofertas y días especiales con edición correcta por ID |
| RF-15 | Galería de fotos | Subida y gestión de imágenes de clientes con validación de tipo de archivo (whitelist) |
| RF-16 | Anti-abuso | Rate limiting por IP configurable para endpoints generales, de autenticación y del chatbot |

#### Requerimientos No Funcionales

| Código | Atributo | Descripción |
|--------|----------|-------------|
| RNF-01 | Seguridad | BCrypt, JWT firmado con captura de tokens malformados (401 en vez de 500), AES-256 para chat, validación de extensiones en subida de archivos, CORS externalizable por variable de entorno |
| RNF-02 | Rendimiento | Contexto IA cacheado; regeneración automática diaria a las 03:00; resumen de stock a las 08:00 |
| RNF-03 | Disponibilidad | Despliegue con un comando, `restart: always` en Docker Compose |
| RNF-04 | Mantenibilidad | Arquitectura hexagonal + screaming architecture; módulos independientes y modificables |
| RNF-05 | Escalabilidad | Contenedores independientes escalables por capa |
| RNF-06 | Compatibilidad | Chrome, Firefox, Edge, Safari; diseño responsive para escritorio, tablet y móvil |
| RNF-07 | Reproducibilidad | Despliegue completo en cualquier máquina con Docker Desktop instalado |
| RNF-08 | Trazabilidad | Toda modificación de datos queda registrada en el módulo de auditoría |
| RNF-09 | Resiliencia | Sin credenciales de email configuradas, las notificaciones se simulan en logs sin errores |
| RNF-10 | Protección de errores | `server.error.include-message=never` en producción para no exponer stack traces |

---

### 5.2. Diseño

#### Arquitectura del sistema

La aplicación sigue una arquitectura de tres capas desplegadas como servicios Docker independientes:

```
[ Frontend Nuxt 4 : 3000 ]  →  [ API Spring Boot : 8080 ]  →  [ PostgreSQL 15 : 5432 ]
```

El backend implementa **Arquitectura Hexagonal (Ports & Adapters)** con **Screaming Architecture**. Cada módulo de negocio tiene la siguiente estructura interna:

```
backend-spring/src/main/java/com/marcog/peluqueria/<modulo>/
├── domain/           ← Entidades de negocio puras (sin dependencias de framework)
├── application/      ← Interfaces de casos de uso e implementaciones
└── infrastructure/
    ├── web/          ← Controladores REST (adaptadores de entrada)
    └── persistence/  ← JPA: entidades, mappers, repositorios (adaptadores de salida)
```

Módulos implementados: `security`, `clientes`, `citas`, `peluqueros`, `servicios`, `productos`, `ausencias`, `fotos`, `ofertas`, `finanzas`, `chatbot`, `auditoria`, `configuracion` y `shared` (notificaciones transversales).

El frontend sigue **Screaming Architecture** organizando el código por dominio de negocio:

```
front-nuxt/app/
├── modules/            ← Organizado por dominio de negocio
│   ├── auth/           (login, reset-password, contrasenas-empleados)
│   ├── agenda/
│   ├── calendario/
│   ├── clientes/
│   ├── empleados/
│   ├── vacaciones/
│   ├── servicios/
│   ├── finanzas/
│   ├── chatbot/
│   └── mensajes/
└── infrastructure/
    └── http/api.ts     ← Cliente Axios centralizado con interceptores JWT (SSR-safe)
```

#### Modelo de datos principal

Las entidades principales del dominio son: `Usuario` (credenciales y rol), `Peluquero` (perfil profesional y disponibilidad), `Cliente` (datos personales, tipo VIP y consentimiento RGPD), `Servicio` (catálogo con duración en minutos), `Cita` (reserva entre cliente, peluquero y servicio con estado PENDIENTE/COMPLETADA/CANCELADA), `Producto` (inventario con stock mínimo), `Ausencia` (solicitud con estado PENDIENTE/APROBADA/RECHAZADA), `Mensaje` (chat cifrado AES-256), `Gasto` (operativo), `RegistroActividad` (auditoría), `Configuracion` (datos del centro), `Oferta` (promociones y días especiales) y `PasswordResetToken` (token de un solo uso para recuperación de contraseña con campo `usado` y `expiraEn`).

#### Diseño de seguridad

**Flujo de autenticación estándar:**

1. Cliente envía credenciales a `/api/auth/login`.
2. Backend valida con BCrypt y emite JWT (corta duración) + Refresh Token (larga duración).
3. Cliente adjunta JWT en `Authorization: Bearer <token>` en cada petición.
4. `JwtAuthenticationFilter` intercepta, valida firma y caducidad. Si el token es malformado captura `JwtException` → deja pasar sin autenticar (→ 401 en endpoints protegidos, no 500).
5. Al caducar el JWT, el interceptor de Axios solicita renovación automática con el Refresh Token.

**Flujo de recuperación de contraseña:**

1. Usuario solicita reset en `/api/auth/forgot-password` con su email.
2. `GestionarCredenciales` genera un token UUID de un solo uso con expiración de 1 hora y lo persiste en `PasswordResetTokenEntity`.
3. `Notificaciones` envía email con enlace `{FRONTEND_BASE_URL}/reset-password?token=<uuid>`.
4. El usuario accede al enlace, introduce nueva contraseña y envía a `/api/auth/reset-password`.
5. Backend valida que el token exista, no haya expirado y no haya sido ya usado. Actualiza la contraseña con BCrypt y marca el token como usado.

**Flujo de cambio de contraseña por administrador:**

1. Admin accede al panel de Contraseñas en `/admin/contrasenas`.
2. Selecciona empleado, introduce nueva contraseña y confirma.
3. Backend (`/api/auth/cambiar-password`) valida rol `ROLE_ADMIN` y actualiza con BCrypt.
4. `Notificaciones` envía email al empleado avisando del cambio.

Los roles se aplican mediante `@PreAuthorize` en cada endpoint, garantizando que las restricciones se cumplan independientemente de lo que muestre el frontend.

#### Protección anti-abuso

El `AntiAbuseFilter` se registra como filtro de Servlet antes del filtro JWT. Mantiene contadores por IP en un mapa en memoria con ventana deslizante de 1 minuto. Tres límites configurables:

| Variable de entorno | Por defecto | Aplica a |
|---------------------|------------|----------|
| `ANTI_ABUSE_GENERAL_PER_MINUTE` | 240 | Todos los endpoints |
| `ANTI_ABUSE_AUTH_PER_MINUTE` | 20 | `/api/auth/**` |
| `ANTI_ABUSE_CHAT_PER_MINUTE` | 8 | `/api/chat/**` |

Si se supera el límite, devuelve `429 Too Many Requests`. Se puede desactivar con `ANTI_ABUSE_ENABLED=false`.

#### Sistema de notificaciones

El módulo `shared/notification/Notificaciones` centraliza el envío de emails. Si `spring.mail.username` no está configurado, las llamadas se convierten en logs informativos sin lanzar excepciones, facilitando el desarrollo sin dependencias externas.

| Evento | Destinatario | Canal |
|--------|-------------|-------|
| Mensaje interno | Empleado destinatario | Email (Mailtrap) |
| Ausencia aprobada | Peluquero | Email |
| Ausencia rechazada | Peluquero | Email |
| Stock bajo | Administrador | Email |
| Resumen diario de stock (08:00) | Administrador | Email |
| Recordatorio de cita (24h antes) | Cliente | Email |
| Recuperación de contraseña | Usuario solicitante | Email |
| Cambio de contraseña por admin | Empleado afectado | Email |

#### Diseño del asistente IA

El asistente combina dos mecanismos:

1. **Contexto estático cacheado:** al arranque, `ArchivoContextoNegocio` genera un JSON con datos estables (horario, catálogo, equipo, ofertas, información del negocio). Se inyecta en el system prompt del modelo y se regenera automáticamente cada noche a las 03:00. La ruta del fichero es configurable mediante `CHATBOT_CONTEXT_FILE`.

2. **Function calling en tiempo real:** para datos frescos (citas del día, KPIs, stock, vacaciones aprobadas), el modelo invoca funciones de negocio que el backend ejecuta contra PostgreSQL mediante `ConsultasGestionPeluqueriaPostgres`. Hasta tres iteraciones de function calling por petición.

```
Usuario → Backend → LLM (OpenAI vía Spring AI)
                      ↓ solicita función de negocio
              Backend ejecuta función → PostgreSQL
                      ↓ resultado
              LLM → respuesta en lenguaje natural → Usuario
```

Si `SPRING_AI_MODEL_CHAT=none`, el asistente responde con un mensaje estático sin coste de API, ideal para demos sin clave de OpenAI.

---

### 5.3. Desarrollo

#### Control de concurrencia en ausencias

Uno de los retos técnicos más relevantes del proyecto es garantizar que dos empleados no puedan solicitar y obtener los mismos días de vacaciones simultáneamente.

En un servidor web, varias peticiones HTTP se procesan en paralelo en hilos distintos de Spring Boot. Sin protección, el siguiente escenario es posible: el empleado A y el empleado B solicitan las mismas fechas al mismo tiempo; ambos pasan la validación de disponibilidad porque ninguno ha persistido todavía su ausencia; ambos guardan la ausencia aprobada, y el calendario queda inconsistente.

La solución implementada utiliza un `Semaphore(1, true)` (semáforo binario justo) en el caso de uso `GestionarAusencias`. Al llegar una solicitud de vacaciones, el hilo adquiere el único permiso del semáforo antes de entrar en la sección crítica. Si otro hilo ya lo tiene, espera en cola ordenada (`fairness=true` garantiza FIFO). Dentro de la sección crítica se realizan: validación de días bloqueados, verificación de solapamientos con ausencias ya aprobadas y persistencia. Solo al liberar el semáforo puede continuar el siguiente hilo.

Las bajas médicas quedan intencionadamente fuera de este mecanismo, ya que representan una situación sobrevenida que no debe bloquearse. Para una arquitectura con múltiples instancias de Spring Boot, el siguiente paso sería migrar el bloqueo a Redis (Redisson) o a bloqueo optimista/pesimista en base de datos.

#### Cifrado del chat interno

Los mensajes del chat interno se cifran con AES-256 antes de persistirse. El vector de inicialización (IV) se genera aleatoriamente en cada operación mediante `AESCryptoUtil`, garantizando que dos mensajes con el mismo contenido produzcan textos cifrados distintos. La clave AES se configura mediante la variable de entorno `CHAT_AES_KEY` y nunca se almacena en el código fuente.

La mensajería interna entre empleados usa Spring Mail para el envío y persiste los mensajes en base de datos para que el receptor los consulte desde su bandeja.

#### Auditoría automática

El módulo de auditoría funciona mediante `AuditoriaInterceptor`, un `HandlerInterceptor` de Spring MVC que intercepta todas las peticiones `POST`, `PUT`, `PATCH` y `DELETE` tras su finalización (`afterCompletion`). Para cada operación registra: usuario autenticado, rol, módulo afectado, descripción legible con el nombre de la entidad, método HTTP, ruta, código de respuesta y marca temporal.

#### Seguridad en subida de archivos

`PeluqueroController` valida la extensión del archivo subido contra una lista blanca antes de persistirlo en disco:

```java
if (!Set.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
    return ResponseEntity.badRequest().body(Map.of("error", "Tipo de archivo no permitido. Use JPG, PNG o WEBP."));
}
```

Esto previene que un atacante pueda subir archivos ejecutables o scripts disfrazados de imágenes.

#### Gestión de errores centralizada

`ApiExceptionHandler` (`@RestControllerAdvice`) centraliza las respuestas de error del backend con códigos HTTP correctos:

| Excepción | Código HTTP |
|-----------|------------|
| `NoSuchElementException` | 404 Not Found |
| `CitaSuperpuestaException` | 409 Conflict |
| `IllegalArgumentException` | 400 Bad Request |
| `AccessDeniedException` | 403 Forbidden |
| `Exception` (genérica) | 500 Internal Server Error |

Todos los mensajes de error en producción ocultan el stack trace (`server.error.include-message=never`).

#### Metodología de desarrollo

El proyecto sigue el modelo **GitFlow** con las siguientes ramas:

- `main`: producción, solo recibe merges desde develop con versiones estables.
- `develop`: rama de integración continua donde confluyen las funcionalidades completadas.
- `feature/*`: una rama por funcionalidad.
- `fix/*`: correcciones de bugs.
- `docs/*` / `chore/*`: documentación y mantenimiento sin lógica de negocio.

#### Datos de demo

En el primer arranque, el sistema crea automáticamente datos de ejemplo. Si `randomuser.me` no está disponible, el backend usa un conjunto local de clientes de respaldo para no depender de internet.

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| admin | `APP_DEMO_PASSWORD` del `.env` | Administrador |
| sofia | `APP_DEMO_PASSWORD` del `.env` | Empleada |
| carmen | `APP_DEMO_PASSWORD` del `.env` | Empleada |
| lucia | `APP_DEMO_PASSWORD` del `.env` | Empleada |

---

### 5.4. Usabilidad y Experiencia de Usuario

La interfaz se ha diseñado como un panel de gestión profesional orientado a escritorio con adaptación responsive para tablet y móvil:

- **Jerarquía visual clara:** menú lateral agrupado por área funcional; KPIs prominentes en el panel de control.
- **Feedback inmediato:** toasts de confirmación o error en todas las operaciones de escritura.
- **Confirmación de acciones destructivas:** modal obligatorio antes de eliminar cualquier entidad.
- **Diseño adaptativo por rol:** menú y secciones visibles se adaptan al rol del usuario autenticado.
- **Calendario visual con FullCalendar:** vistas de día, semana y mes; creación de citas con clic en hueco libre.
- **Página de error personalizada:** `error.vue` captura errores de navegación con mensaje amigable y botón de vuelta al inicio.
- **Flujo de recuperación de contraseña:** página `/reset-password` accesible sin autenticación, con validación visual del token y feedback de éxito/error.

---

### 5.5. Pruebas

#### Backend — JUnit 5 + Mockito

| Test | Qué valida |
|------|-----------|
| `PeluqueriaApiApplicationTests` | Arranque del contexto Spring completo. Detecta fallos de configuración e inyección de dependencias. |
| `GestionarAgendaTest` | Solapamientos, citas consecutivas, creación y edición de citas. |
| `GestionarAusenciasTest` | Serialización concurrente de solicitudes de vacaciones con semáforo. |
| `AutenticarUsuarioTest` | Registro, rol por defecto, password hasheado y emisión de JWT + Refresh Token. |
| `GestionarCredencialesTest` | Generación de token de reset, expiración, uso único, cambio de contraseña por administrador. |
| `AESCryptoUtilTest` | Cifrado/descifrado AES-256, IV aleatorio, claves inválidas y payloads corruptos. |
| `ResponderConsultasGestionTest` | Function calling, respuestas directas, permisos por rol y sugerencias del asistente IA. |
| `ChatFunctionExecutorTest` | Ejecución de funciones del chatbot y control de permisos por rol. |

#### Frontend — Vitest

| Test | Qué valida |
|------|-----------|
| `auth.store.test.ts` | Guardado, restauración y cierre de sesión; roles admin/peluquero; token expirado. |
| `authService.test.ts` | Llamadas de login, solicitud de reset y cambio de contraseña. |
| `useChatbot.test.ts` | Mensajes del usuario y asistente, historial, errores, loading y sugerencias. |

```bash
# Backend
cd backend-spring && ./mvnw test

# Frontend
cd front-nuxt && npm test
```

---

### 5.6. Despliegue

La aplicación se despliega con Docker Compose. Requisito previo: Docker Desktop instalado (no requiere Java ni Node.js para el despliegue con Docker).

```bash
cd docker
docker compose up --build
```

| Servicio | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html (solo con `SWAGGER_ENABLED=true`) |
| PostgreSQL | localhost:5432 |

Para desarrollo local sin Docker:

```bash
# Backend (requiere Java 21)
cd backend-spring && ./mvnw spring-boot:run

# Frontend (requiere Node.js 22)
cd front-nuxt && npm install && npm run dev
```

La configuración sensible se gestiona mediante variables de entorno en `docker/.env` (excluido del repositorio mediante `.gitignore`). El fichero `docker/.env.example` documenta todas las variables requeridas con sus valores por defecto.

---

## 6. CONCLUSIONES Y FUTURAS MEJORAS

### Valoración personal

El desarrollo de este proyecto ha permitido consolidar los conocimientos adquiridos a lo largo del ciclo y aplicarlos en un contexto realista y completo. Adoptar la arquitectura hexagonal desde el inicio supuso un esfuerzo adicional en las primeras fases, pero demostró su valor al crecer el sistema: añadir nuevos módulos como la auditoría, el sistema de notificaciones o el asistente IA resultó natural sin tocar el núcleo existente.

La integración de Spring AI en sustitución de clientes HTTP directos a proveedores de IA aportó una capa de abstracción sobre el proveedor: cambiar de modelo o proveedor solo requiere modificar la configuración, no el código. La gestión de contraseñas (reset por email y cambio por administrador) añadió un ciclo de seguridad completo que acerca el proyecto a un producto real. El sistema de rate limiting protege los endpoints más sensibles sin infraestructura adicional.

### Grado de cumplimiento de objetivos

Todos los objetivos específicos definidos en el apartado 4 han sido alcanzados: arquitectura hexagonal en todos los módulos, seguridad JWT + roles + rate limiting, motor de validación de solapamientos, control de concurrencia con semáforo en ausencias, sistema de notificaciones por email, recuperación y gestión de contraseñas, asistente IA con function calling y permisos por rol, panel de KPIs en tiempo real, mensajería WebSocket STOMP con cifrado AES-256, auditoría completa, despliegue Docker reproducible y cobertura de tests sobre los casos de uso críticos.

### Problemas encontrados y soluciones aplicadas

**Solapamiento de citas:** la validación inicial no contemplaba citas consecutivas que terminan exactamente a la hora de inicio de la siguiente. Se corrigió ajustando la condición de comparación.

**Concurrencia en ausencias:** en pruebas con peticiones simultáneas, dos empleados podían obtener los mismos días. Se resolvió con `Semaphore(1, true)` que serializa el acceso con orden FIFO.

**Token JWT malformado → 500:** `JwtAuthenticationFilter` no capturaba `JwtException` ante tokens basura. Se añadió un try-catch que deja pasar la petición sin autenticar (→ 401 en los endpoints protegidos), en vez de propagar una excepción no controlada (→ 500).

**CORS hardcodeado:** los orígenes CORS estaban literales en el código fuente. Se externalizaron a `CORS_ORIGINS` en variables de entorno, manteniendo los valores de desarrollo como default.

**SSR en Nuxt con localStorage:** las stores de Pinia accedían a `localStorage` durante el renderizado en servidor, generando errores de hidratación. Se resolvió protegiendo todos los accesos con `import.meta.client`.

**Cifrado AES con IV fijo:** la primera implementación generaba siempre el mismo IV, haciendo predecible el texto cifrado. Se corrigió generando un IV aleatorio por mensaje y almacenándolo junto al texto cifrado.

**Datos de demo sin internet:** la integración con `randomuser.me` fallaba en entornos sin acceso a internet. Se implementó un conjunto local de clientes de respaldo.

**Swagger expuesto por defecto:** el valor por defecto de `SWAGGER_ENABLED` era `true`. Se cambió a `false` con override a `true` en el `docker-compose.yml` para el entorno de demo.

---

### Posibles mejoras y ampliaciones futuras

#### 1. Evolución a plataforma SaaS multi-tenant

La arquitectura actual gestiona un único salón. La evolución natural es convertir el sistema en una plataforma SaaS donde cada salón es un **tenant** independiente con sus propios datos, usuarios y configuración, gestionados desde un **backoffice global** de super administración.

**Plan de implementación técnica:**

**Capa de datos — multi-tenancy por columna discriminadora:**
- Todas las entidades añaden `tenant_id UUID NOT NULL` con índice.
- Spring Data JPA filtra automáticamente mediante `TenantContext` en cada repositorio.
- El `TenantContext` se resuelve en el filtro de seguridad a partir del subdominio (`nombreSalon.app.com`) o del claim `tenantId` en el JWT del usuario.

**Capa de seguridad — nuevo rol SUPER_ADMIN:**
- Rol independiente de `ROLE_ADMIN` y `ROLE_HAIRDRESSER`, no pertenece a ningún tenant.
- Acceso exclusivo a `/backoffice/**` protegido con `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- Panel global: lista de tenants, estado de suscripción, consumo de recursos, gestión de incidencias.

**Autenticación de dos factores (2FA) obligatoria para SUPER_ADMIN:**
- TOTP (Time-based One-Time Password) compatible con Google Authenticator / Authy.
- Al activar 2FA, el backend genera un secreto TOTP (Base32) y devuelve un código QR.
- En cada login: (1) verificación usuario/contraseña → JWT provisional con claim `mfa_pending=true`; (2) cliente envía código TOTP de 6 dígitos a `/api/auth/mfa/verify`; (3) backend valida y emite JWT definitivo.
- Cierre de sesión automático por inactividad configurable (por defecto 15 minutos para SUPER_ADMIN).

**Registro y onboarding de nuevos tenants:**
- Formulario público de registro con nombre del centro, email y plan de suscripción.
- El sistema crea automáticamente el tenant en base de datos, el usuario ADMIN inicial y envía las credenciales por email.
- Período de prueba configurable (por defecto 14 días) antes de requerir suscripción activa.

#### 2. Sistema de tickets de soporte

Los tenants podrán abrir incidencias de soporte directamente desde su panel de administración, sin salir de la aplicación.

**Modelo de datos:**

```
Ticket {
    id            UUID PK
    tenantId      UUID FK → Tenant
    asunto        VARCHAR(200)
    descripcion   TEXT
    prioridad     ENUM(BAJA, MEDIA, ALTA, CRITICA)
    estado        ENUM(ABIERTO, EN_PROCESO, RESUELTO, CERRADO)
    creadoPor     VARCHAR(100)
    creadoEn      TIMESTAMP
    actualizadoEn TIMESTAMP
}

MensajeTicket {
    id        UUID PK
    ticketId  UUID FK → Ticket
    autor     VARCHAR(100)
    esStaff   BOOLEAN       ← false = tenant, true = soporte interno
    cuerpo    TEXT
    creadoEn  TIMESTAMP
}
```

**Flujo desde el tenant:**
1. Sección **Soporte** en el panel con botón «Abrir incidencia».
2. El tenant rellena asunto, descripción y prioridad. Estado inicial: `ABIERTO`.
3. El tenant recibe confirmación por email con número de ticket.
4. Puede consultar el hilo de respuestas y añadir mensajes adicionales desde su panel.

**Flujo desde el backoffice SUPER_ADMIN:**
1. Panel global de tickets con filtros por estado, prioridad, tenant y fecha.
2. El super administrador puede responder, cambiar estado y asignar prioridad.
3. Cada cambio de estado notifica al tenant por email.
4. Métricas: tiempo medio de resolución, tickets abiertos por tenant, SLA por prioridad.

#### 3. Autenticación de dos factores (2FA) para administradores de salón

Además del 2FA obligatorio para SUPER_ADMIN, el sistema ofrecerá 2FA opcional para los usuarios `ROLE_ADMIN` de cada salón:

- Activación voluntaria desde la configuración de perfil.
- Mismo mecanismo TOTP (Google Authenticator compatible).
- Códigos de recuperación de un solo uso para acceso de emergencia (10 códigos generados al activar 2FA).
- Si el admin pierde acceso, el SUPER_ADMIN puede desactivar el 2FA desde el backoffice.

#### 4. Portal de autocita para clientes

Interfaz pública (sin autenticación de empleado) que permita al cliente reservar su propia cita seleccionando servicio, empleado disponible y franja horaria en tiempo real.

#### 5. Aplicación móvil nativa

App para iOS y Android con agenda del empleado, gestión de citas y notificaciones push mediante Firebase Cloud Messaging.

#### 6. Control de concurrencia distribuido

Migrar el semáforo de vacaciones a Redis (Redisson) o bloqueo optimista en base de datos para soportar múltiples instancias de Spring Boot en un despliegue horizontal (Kubernetes).

---

## 7. BIBLIOGRAFÍA

### Documentación oficial y referencias técnicas

- Spring Framework Team. (2025). *Spring Boot Reference Documentation (3.5.x)*. VMware. https://docs.spring.io/spring-boot/docs/current/reference/html/
- Spring Framework Team. (2025). *Spring Security Reference*. VMware. https://docs.spring.io/spring-security/reference/
- Spring AI Team. (2025). *Spring AI Reference Documentation (1.x)*. VMware. https://docs.spring.io/spring-ai/reference/
- Vue.js Team. (2025). *Vue 3 Documentation*. https://vuejs.org/guide/introduction.html
- Nuxt Team. (2025). *Nuxt 4 Documentation*. NuxtLabs. https://nuxt.com/docs
- Docker Inc. (2025). *Docker Compose Documentation*. https://docs.docker.com/compose/
- PostgreSQL Global Development Group. (2025). *PostgreSQL 15 Documentation*. https://www.postgresql.org/docs/15/
- OpenAI. (2025). *OpenAI API Reference — Function Calling*. https://platform.openai.com/docs/guides/function-calling
- RFC 6238. (2011). *TOTP: Time-Based One-Time Password Algorithm*. IETF. https://datatracker.ietf.org/doc/html/rfc6238

### Libros y recursos académicos

- Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.
- Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.
- Walls, C. (2022). *Spring in Action (6th Edition)*. Manning Publications.
- Richards, M., & Ford, N. (2020). *Fundamentals of Software Architecture*. O'Reilly Media.

### Normativa

- Reglamento (UE) 2016/679, de 27 de abril de 2016, relativo a la protección de datos personales (RGPD). *Diario Oficial de la Unión Europea*, L 119, 1-88.

---

## 8. ANEXOS

### Anexo A: Comandos de despliegue y operación

```bash
# Despliegue completo (recomendado)
cd docker && docker compose up --build

# Modo background
docker compose up --build -d

# Logs del backend / frontend
docker compose logs api --tail 120
docker compose logs front --tail 120

# Desarrollo local sin Docker
cd backend-spring && ./mvnw spring-boot:run   # requiere Java 21
cd front-nuxt && npm install && npm run dev   # requiere Node.js 22

# Tests de backend y frontend
cd backend-spring && ./mvnw test
cd front-nuxt && npm test
```

---

### Anexo B: Variables de entorno (`docker/.env`)

| Variable | Descripción | Requerida |
|----------|-------------|----------|
| `DB_USER` | Usuario de PostgreSQL | Sí |
| `DB_PASSWORD` | Contraseña de PostgreSQL | Sí |
| `DB_NAME` | Nombre de la base de datos | Sí (default: `peluqueria_db`) |
| `JWT_SECRET_KEY` | Clave secreta para firmar JWT (≥ 32 bytes hex) | Sí |
| `CHAT_AES_KEY` | Clave AES-256 para cifrado del chat (32 bytes hex) | Sí |
| `APP_DEMO_PASSWORD` | Contraseña para usuarios demo | Sí |
| `MAILTRAP_USERNAME` | Usuario SMTP de Mailtrap | Para emails |
| `MAILTRAP_PASSWORD` | Contraseña SMTP de Mailtrap | Para emails |
| `APP_ADMIN_EMAIL` | Email del administrador para alertas | Para emails |
| `FRONTEND_BASE_URL` | URL base del frontend para enlaces de reset | Para emails |
| `OPENAI_API_KEY` | Clave de la API de OpenAI | Para chatbot IA |
| `OPENAI_MODEL` | Modelo a usar | No (default: `gpt-4.1-nano`) |
| `OPENAI_MAX_TOKENS` | Tokens máximos por respuesta | No (default: `200`) |
| `SPRING_AI_MODEL_CHAT` | `none` para desactivar IA | No |
| `SWAGGER_ENABLED` | `true` para mostrar Swagger UI | No (default Docker: `true`) |
| `ANTI_ABUSE_ENABLED` | `false` para desactivar rate limiting | No (default: `true`) |
| `ANTI_ABUSE_GENERAL_PER_MINUTE` | Peticiones/min por IP (general) | No (default: `240`) |
| `ANTI_ABUSE_AUTH_PER_MINUTE` | Peticiones/min por IP (auth) | No (default: `20`) |
| `ANTI_ABUSE_CHAT_PER_MINUTE` | Peticiones/min por IP (chat) | No (default: `8`) |
| `CORS_ORIGINS` | Orígenes CORS permitidos (CSV) | No |

Generar claves seguras:
```bash
openssl rand -hex 32   # para JWT_SECRET_KEY
openssl rand -hex 32   # para CHAT_AES_KEY
```

---

### Anexo C: Módulos implementados

| Módulo | Descripción | Roles con acceso |
|--------|-------------|-----------------|
| Panel de control | KPIs, gráficas y alertas del negocio | Admin |
| Agenda | Gestión visual de citas | Admin, Empleado |
| Clientes | Gestión completa con historial, fotos y VIP | Admin, Empleado |
| Empleados | Gestión del equipo | Admin |
| Servicios | Catálogo de tratamientos con duración | Admin |
| Inventario | Productos y stock | Admin, Empleado (lectura) |
| Ventas | Registro de ventas de productos | Admin, Empleado |
| Ausencias | Vacaciones y bajas con semáforo de concurrencia | Admin, Empleado |
| Finanzas | KPIs financieros detallados | Admin |
| Mensajería | Correo interno entre empleados con historial por contacto y contador no leídos | Admin, Empleado |
| Chatbot | Asistente IA con function calling | Admin, Empleado |
| Auditoría | Registro de actividad del equipo | Admin |
| Configuración | Datos del centro, ofertas, días especiales | Admin |
| Contraseñas | Gestión de contraseñas de empleados por admin | Admin |
| Reset contraseña | Recuperación de contraseña por email con token | Público |

---

### Anexo D: Usuarios de demostración

En el primer arranque, el sistema crea automáticamente los siguientes usuarios de prueba junto con servicios, productos y clientes de muestra:

| Usuario | Contraseña | Rol |
|---------|-----------|-----|
| admin | valor de `APP_DEMO_PASSWORD` en `.env` | Administrador |
| sofia | valor de `APP_DEMO_PASSWORD` en `.env` | Empleada |
| carmen | valor de `APP_DEMO_PASSWORD` en `.env` | Empleada |
| lucia | valor de `APP_DEMO_PASSWORD` en `.env` | Empleada |

Si `randomuser.me` no está disponible, el backend usa un conjunto local de clientes de respaldo para no depender de conexión a internet.

---

### Anexo E: Estructura de ramas GitFlow

```
main
└── develop
    ├── feature/migracion-spring-ai       ← chatbot a Spring AI + OpenAI
    ├── feature/gestion-contrasenas       ← reset password + cambio por admin
    ├── feature/mejoras-generales         ← servicios, fotos, auditoría, DataInitializer
    ├── fix/seguridad-hardening           ← JWT, CORS, Swagger, whitelist, rate limiting
    ├── fix/bugs-logica                   ← OfertaController, CitaController null-checks
    ├── chore/frontend-pages              ← páginas Vue, api.ts SSR-safe, tests
    └── docs/dossier-tfg                  ← README, logotipo, documentación
```
