# Peluqueria Isabella - Sistema de Gestion

Aplicacion full-stack para gestionar una peluqueria desde un panel web: agenda, clientes, empleados, servicios, inventario, ventas, ausencias, resultados, mensajeria interna y asistente de gestion con IA.

El proyecto esta desarrollado como aplicacion profesional de gestion para un salon realista, con backend Spring Boot, frontend Nuxt y despliegue local con Docker Compose.

## Indice rapido

- [Que hace la aplicacion](#que-hace-la-aplicacion)
- [Stack tecnico](#stack-tecnico)
- [Arquitectura](#arquitectura)
- [Asistente de gestion con IA](#asistente-de-gestion-con-ia)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Arranque rapido](#arranque-rapido)
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
- Control de concurrencia en solicitudes de calendario para evitar carreras cuando varios empleados piden vacaciones a la vez.

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

### Control de concurrencia

El caso de uso de ausencias incluye un control de concurrencia con `Semaphore(1, true)` para serializar las solicitudes que afectan al calendario, como vacaciones y permisos. Esto evita condiciones de carrera cuando dos empleados intentan pedir vacaciones al mismo tiempo: una solicitud entra, valida dias bloqueados, aplica reglas de antelacion y guarda; la siguiente espera su turno.

Las bajas medicas quedan fuera de este bloqueo porque representan una situacion inevitable y no deberian depender de disponibilidad de calendario.

Esta decision tiene valor tecnico porque demuestra gestion de hilos, seccion critica y proteccion de reglas de negocio en el backend. Para el despliegue actual con una instancia de Spring Boot es suficiente. Si el sistema escalara a varias instancias, el siguiente paso seria mover este bloqueo a base de datos o a un mecanismo distribuido.

## Asistente de gestion con IA

La aplicacion incluye un asistente interno para administradores y empleados. No es un chat generico pegado a la interfaz: esta conectado al dominio de la peluqueria y responde usando informacion real del sistema.

### Como funciona

El frontend envia la pregunta al backend mediante el modulo de chatbot. El backend autentica al usuario, detecta su rol y decide que informacion puede consultar. A partir de ahi combina dos mecanismos:

- Contexto de negocio cacheado: datos que cambian poco, como nombre del centro, horario, politicas, equipo, servicios, productos y ofertas activas. Este contexto se genera al arrancar y se regenera automaticamente cada noche.
- Consultas en tiempo real: datos que deben salir de base de datos en el momento, como clientes VIP, total de clientes, inventario, stock bajo, ganancias, productos mas vendidos, citas del empleado o vacaciones.

Para las consultas mas criticas, como clientes VIP o total de clientes, el backend responde directamente desde base de datos. Asi la respuesta no depende de la cuota del proveedor IA y no se inventan datos. Para preguntas mas abiertas, el backend usa Gemini con function calling: el modelo puede pedir una funcion concreta, el backend ejecuta esa funcion contra PostgreSQL y devuelve el resultado para construir la respuesta final.

### Seguridad y roles

El asistente respeta permisos. Un empleado puede consultar informacion propia, como sus citas o vacaciones, mientras que las metricas de negocio y clientes quedan limitadas al administrador. Esta comprobacion se hace en el backend, no solo en la pantalla.

La clave de Gemini se configura por variable de entorno (`GEMINI_API_KEY`) y no se publica en el repositorio.

### Puntos fuertes para la presentacion

- Integracion real con el negocio: el asistente consulta clientes, agenda, inventario, ventas y ausencias, no responde solo con texto estatico.
- Arquitectura limpia: el caso de uso del asistente vive en `chatbot/application`, el dominio define contratos de negocio y la infraestructura contiene Gemini, contexto y consultas PostgreSQL.
- Control por roles: admin y empleado tienen capacidades diferentes, lo que demuestra seguridad aplicada a una funcionalidad IA.
- Function calling: el modelo no accede directamente a la base de datos; pide funciones controladas y el backend decide que ejecutar.
- Fallback local: algunas respuestas importantes se resuelven sin depender del proveedor externo, mejorando fiabilidad.
- Contexto regenerable: el asistente se alimenta de datos actualizados del centro y puede regenerar su contexto.
- Buen enfoque de producto: convierte el panel en una herramienta mas rapida para preguntar "que productos tienen bajo stock", "quienes son clientes VIP" o "cuantas citas tengo hoy".

## Estructura del repositorio

```text
peluqueria/
├── backend-spring/       # API Spring Boot
├── front-nuxt/           # Aplicacion Nuxt
├── docker/               # Docker Compose y variables de entorno
└── docs/                 # Puesta en marcha publica
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

## Documentacion

- [Puesta en marcha](docs/puesta-en-marcha.md)

Las credenciales demo y notas internas se mantienen fuera del repositorio publico, dentro de `documentacion-no-github/`.

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
