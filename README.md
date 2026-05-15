# Peluqueria Isabella - Sistema de Gestion

Aplicacion full-stack para gestionar una peluqueria desde un panel web: agenda, clientes, empleados, servicios, inventario, ventas, ausencias, resultados, mensajeria interna y asistente de gestion con IA.

El proyecto esta desarrollado como aplicacion profesional de gestion para un salon realista, con backend Spring Boot, frontend Nuxt y despliegue local con Docker Compose.

## Indice rapido

- [Que hace la aplicacion](#que-hace-la-aplicacion)
- [Stack tecnico](#stack-tecnico)
- [Arquitectura](#arquitectura)
- [Seguridad y credenciales](#seguridad-y-credenciales)
- [API y manejo de errores](#api-y-manejo-de-errores)
- [Asistente de gestion con IA](#asistente-de-gestion-con-ia)
- [Calidad y pruebas](#calidad-y-pruebas)
- [Responsive](#responsive)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Arranque rapido](#arranque-rapido)
- [Documentacion](#documentacion)
- [Comandos utiles](#comandos-utiles)

## Que hace la aplicacion

Peluqueria Isabella no es solo un chatbot ni una demo de pantallas aisladas. Es una aplicacion de gestion completa para un salon de peluqueria, con separacion entre administracion y empleados, datos reales en PostgreSQL, seguridad por roles, auditoria, agenda, ventas, inventario, mensajeria, resultados financieros y un asistente IA integrado en el negocio.

### Panel administrador

- Panel de control con KPIs, evolucion de ventas, citas, inventario y alertas.
- Agenda visual para crear, editar, cancelar y completar citas.
- Gestion de clientes con datos personales, VIP, historial, consentimiento y fotos.
- Gestion de empleados, especialidades, disponibilidad, bajas y vacaciones.
- Gestion de contrasenas de empleados desde administracion.
- Catalogo de servicios con precio, duracion, categoria y genero.
- Servicios con precio promocional y visualizacion de precio normal tachado.
- Inventario de productos con stock, stock minimo, filtros y ventas.
- Mensajeria interna y envio de emails a empleados.
- Resultados financieros: ingresos, gastos, beneficios, productos y rendimiento.
- Configuracion del negocio, horarios por franjas, sabados, dias especiales y dias bloqueados para vacaciones.
- Dias especiales con descuento en porcentaje para el usuario, convertido internamente al multiplicador que espera el backend.
- Asistente de gestion para consultar datos del negocio.
- Auditoria de actividad para revisar creaciones, modificaciones y eliminaciones del equipo.

### Portal peluquero/a

- Agenda propia.
- Clientes para consulta y alta operativa.
- Mensajes internos.
- Ventas de productos.
- Solicitud de vacaciones y bajas.
- Notificaciones de aprobacion o rechazo de ausencias.
- Asistente de gestion limitado por rol.
- Control de concurrencia en solicitudes de calendario para evitar carreras cuando varios empleados piden vacaciones a la vez.

### Modulos principales

| Modulo | Que resuelve |
|---|---|
| Agenda y calendario | Gestion diaria de citas, estados, solapamientos y vista operativa para admin y empleados |
| Clientes | Alta, consulta, historial, consentimiento, cliente VIP, archivado/reactivado y fotos |
| Empleados | Equipo, especialidad, disponibilidad, bajas, vacaciones y calendario laboral |
| Servicios | Catalogo, duracion, precio, genero, categoria y precio promocional |
| Inventario | Productos, stock, stock minimo, ventas, ranking y alertas |
| Ventas | Venta de productos y actualizacion automatica de stock |
| Finanzas y resultados | Ingresos, gastos, beneficio, balance, ticket medio y evolucion mensual |
| Configuracion | Datos del centro, horarios por franjas, sabados, dias bloqueados y dias especiales |
| Mensajeria | Mensajes internos y envio de emails a empleados con Mailtrap en local |
| Auditoria | Registro de acciones sensibles con usuario, rol, ruta, metodo, estado y fecha |
| Credenciales | Recuperacion exclusiva para admin y cambio de contrasena de empleados desde administracion |
| Asistente IA | Consultas en lenguaje natural sobre datos reales del negocio con permisos por rol |

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
| Spring Mail + Mailtrap | Emails en entorno de pruebas |
| Spring AI + OpenAI | Asistente IA con function calling |
| Springdoc OpenAPI | Swagger UI y contrato REST |
| JUnit 5 + Mockito | Tests unitarios y de casos de uso |

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
| Vitest | Tests unitarios del frontend |

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
└── infrastructure   # Web, persistencia, email, Spring AI, configuracion...
```

La arquitectura evita nombres tecnicos ruidosos como `Port`, `Adapter`, `In` u `Out`. Los contratos viven en `domain` con nombres de negocio y las implementaciones viven en `infrastructure` indicando la tecnologia cuando aporta claridad, por ejemplo `PostgresClienteRepository`.

El frontend usa el mismo criterio de vertical slicing por modulo. Las rutas de Nuxt en `app/pages` son wrappers finos y las pantallas reales viven en `app/modules`, separadas por negocio: agenda, clientes, inventario, mensajes, ventas, ausencias, chatbot, etc.

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

Esta decision tiene valor tecnico porque demuestra gestion de hilos, seccion critica, semaforos y proteccion de reglas de negocio en el backend. Para el despliegue actual con una instancia de Spring Boot es suficiente. Si el sistema escalara a varias instancias, el siguiente paso seria mover este bloqueo a base de datos o a un mecanismo distribuido.

### Roles y auditoria

La aplicacion trabaja con dos roles principales:

- `ADMIN`: acceso completo al panel, configuracion, empleados, resultados, inventario, servicios, clientes, agenda y auditoria.
- `ROLE_HAIRDRESSER` (peluquero/a): agenda, clientes operativos, ventas, productos en lectura, vacaciones, mensajes y asistente sin datos economicos.

El backend aplica permisos con Spring Security y no depende solo de ocultar opciones en el menu. Ademas, existe un registro de actividad para administracion: cada accion autenticada que crea, modifica o elimina datos queda guardada con usuario, rol, modulo, metodo HTTP, ruta, estado y fecha. Esto permite responder a preguntas como quien creo una cita, quien edito un cliente o quien cambio una solicitud.

## Seguridad y credenciales

La aplicacion separa claramente el acceso de administracion y el acceso de empleados. El backend valida permisos con Spring Security y las pantallas solo muestran las acciones que corresponden a cada rol.

### Recuperacion de contrasena

- La recuperacion desde el login esta reservada para el administrador.
- El formulario de login muestra el flujo como recuperacion admin, no como una opcion general para empleados.
- Si el email no existe o no pertenece al admin, la API responde igual para no revelar informacion.
- Si el email pertenece al admin activo, se genera un token temporal de 30 minutos y se envia un enlace a `/reset-password?token=...`.
- Los tokens anteriores sin usar se invalidan cuando se solicita uno nuevo.
- El token queda marcado como usado al restablecer la contrasena.

### Contrasenas de empleados

Los empleados no tienen "olvide mi contrasena". Si pierden acceso, el administrador les define una nueva contrasena desde:

```text
/admin/contrasenas
```

La pantalla permite buscar un empleado, seleccionar su ficha y escribir la nueva contrasena dos veces. La validacion se aplica tanto en frontend como en backend:

- minimo 8 caracteres;
- al menos una mayuscula;
- al menos una minuscula;
- al menos un numero;
- ambos campos deben coincidir.

El endpoint esta protegido con rol admin:

```text
POST /api/peluqueros/clave-empleado
```

La contrasena se guarda siempre hasheada con `PasswordEncoder`; nunca se almacena en texto plano.

### Usuarios demo

Los emails demo se normalizan en el arranque para evitar datos antiguos mezclados:

| Usuario | Email |
|---|---|
| `admin` | `admin@email.com` |
| `carmen` | `carmen@email.com` |
| `lucia` | `lucia@email.com` |
| `sofia` | `sofia@email.com` |

Si existe un empleado demo antiguo como `maria`, tambien se sincroniza a `maria@email.com`. La contrasena demo se lee desde `APP_DEMO_PASSWORD` y no debe documentarse con valor real en el repositorio.

### Emails

El envio de recuperacion usa Spring Mail con Mailtrap en entorno local. Para que el enlace llegue realmente al inbox de Mailtrap deben estar configuradas estas variables:

```env
MAILTRAP_USERNAME=...
MAILTRAP_PASSWORD=...
APP_ADMIN_EMAIL=admin@email.com
FRONTEND_BASE_URL=http://localhost:3000
```

Si Mailtrap devuelve `Authentication failed`, el flujo crea el token pero el email no se entrega hasta corregir las credenciales SMTP.

## API y manejo de errores

El backend expone una API REST protegida por JWT. Las rutas de administracion y empleado se validan en servidor mediante Spring Security, de modo que el frontend no es la unica barrera de seguridad.

### Endpoints principales

| Area | Endpoint base |
|---|---|
| Autenticacion | `/api/auth` |
| Citas | `/api/citas` |
| Clientes | `/api/v1/clientes` |
| Fotos de clientes | `/api/v1/clientes/{clienteId}/fotos` |
| Peluqueros / empleados | `/api/peluqueros` |
| Servicios | `/api/v1/servicios` |
| Productos e inventario | `/api/v1/productos` |
| Ventas de productos | `/api/v1/productos/ventas` |
| Ausencias | `/api/v1/ausencias` |
| Dias bloqueados | `/api/v1/dias-bloqueados` |
| Ofertas y dias especiales | `/api/v1/ofertas`, `/api/v1/dias-especiales` |
| Finanzas | `/api/finanzas` |
| Configuracion | `/api/configuracion` |
| Mensajes | `/api/mensajes` |
| Asistente IA | `/api/chat` |
| Auditoria | `/api/v1/auditoria` |

El contrato se puede consultar desde Swagger:

```text
http://localhost:8080/swagger-ui.html
```

### Errores controlados

La aplicacion incluye una capa global para evitar errores crudos sin contexto:

- `400`: validaciones y datos incorrectos.
- `401`: sesion no valida o caducada.
- `403`: permisos insuficientes.
- `404`: recurso no encontrado.
- `500`: error interno controlado con mensaje seguro.

El backend devuelve una respuesta uniforme con estado, codigo, mensaje, ruta y fecha. El frontend normaliza esos errores en el cliente HTTP y muestra una pagina global de aviso cuando Nuxt recibe un error de navegacion, con acciones para volver, ir al inicio o iniciar sesion.

## Asistente de gestion con IA

La aplicacion incluye un asistente interno para administradores y empleados. No es un chat generico pegado a la interfaz: esta conectado al dominio de la peluqueria y responde usando informacion real del sistema.

### Como funciona

El frontend envia la pregunta al backend mediante el modulo de chatbot. El backend autentica al usuario, detecta su rol y decide que informacion puede consultar. A partir de ahi combina dos mecanismos:

- Contexto de negocio cacheado: datos que cambian poco, como nombre del centro, horario, politicas, equipo, servicios, productos y ofertas activas. Este contexto se genera al arrancar y se regenera automaticamente cada noche.
- Consultas en tiempo real: datos que deben salir de base de datos en el momento, como clientes VIP, total de clientes, inventario, stock bajo, ganancias, productos mas vendidos, citas del empleado o vacaciones.

Para las consultas mas criticas, como clientes VIP o total de clientes, el backend responde directamente desde base de datos. Asi la respuesta no depende de la cuota del proveedor IA y no se inventan datos. Para preguntas mas abiertas, el backend usa un proveedor LLM configurable con function calling: el modelo puede pedir una funcion concreta, el backend ejecuta esa funcion contra PostgreSQL y devuelve el resultado para construir la respuesta final.

Tambien se ha mejorado el formato de respuesta del chat para que sea mas util dentro del panel: respuestas sin iconos decorativos, datos estructurados cuando pregunta por agenda, clientes, ventas o inventario, sugerencias limpias y foco mantenido en el input para poder seguir preguntando sin interrupciones.

### Seguridad y roles

El asistente respeta permisos. Un empleado puede consultar informacion propia, como sus citas o vacaciones, mientras que las metricas de negocio y clientes quedan limitadas al administrador. Esta comprobacion se hace en el backend, no solo en la pantalla.

El proveedor IA se configura con la variable de entorno `AI_PROVIDER`. Por defecto usa Spring AI sobre OpenAI (`AI_PROVIDER=spring-ai`). La clave no se publica en el repositorio.

```env
AI_PROVIDER=spring-ai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4.1-nano
OPENAI_MAX_TOKENS=200
```

### Puntos fuertes para la presentacion

- Integracion real con el negocio: el asistente consulta clientes, agenda, inventario, ventas y ausencias, no responde solo con texto estatico.
- Arquitectura limpia: el caso de uso del asistente vive en `chatbot/application`, el dominio define contratos de negocio y la infraestructura contiene Spring AI, contexto y consultas PostgreSQL.
- Control por roles: admin y empleado tienen capacidades diferentes, lo que demuestra seguridad aplicada a una funcionalidad IA.
- Function calling: el modelo no accede directamente a la base de datos; pide funciones controladas y el backend decide que ejecutar.
- Spring AI: el proyecto usa Spring AI sobre OpenAI, compatible con Spring Boot 3.5. Gestiona el ciclo de tools de forma nativa: el modelo solicita una herramienta, el backend ejecuta una consulta segura del dominio y el modelo recibe el resultado para redactar la respuesta.
- Proveedor intercambiable: el dominio depende de `ModeloLenguaje`, por lo que el proveedor IA se puede cambiar por configuracion sin tocar el frontend.
- Fallback local: algunas respuestas importantes se resuelven sin depender del proveedor externo, mejorando fiabilidad.
- Contexto regenerable: el asistente se alimenta de datos actualizados del centro y puede regenerar su contexto.
- Buen enfoque de producto: convierte el panel en una herramienta mas rapida para preguntar "que productos tienen bajo stock", "quienes son clientes VIP" o "cuantas citas tengo hoy".

## Calidad y pruebas

El proyecto incluye pruebas automatizadas en backend y frontend.

En backend se usan JUnit 5 y Mockito para validar casos de uso, reglas de negocio y permisos sin depender de infraestructura real. Esto permite probar escenarios como solicitudes de vacaciones, restricciones por rol y respuestas del asistente con repositorios o servicios simulados.

En frontend se usa Vitest para comprobar stores, servicios y composables clave. La idea es cubrir la logica que no debe romperse al cambiar la interfaz: autenticacion, cliente HTTP, estado global y comportamiento del asistente.

### Tests backend

| Test | Que valida | Para que sirve |
|---|---|---|
| `PeluqueriaApiApplicationTests` | Arranque del contexto Spring completo con perfil de test | Detectar fallos de configuracion, inyeccion de dependencias, entidades, repositorios o seguridad antes de ejecutar la app |
| `GestionarAgendaTest` | Creacion y edicion de citas, solapamientos y citas consecutivas permitidas | Evitar que dos citas del mismo peluquero ocupen el mismo tramo horario |
| `GestionarAusenciasTest` | Serializacion de solicitudes concurrentes de vacaciones con semaforo | Demostrar control de hilos y evitar carreras cuando dos empleados solicitan ausencias a la vez |
| `AutenticarUsuarioTest` | Registro seguro, rol por defecto, password hasheado y tokens | Garantizar que un registro publico no pueda crear administradores ni guardar contrasenas en plano |
| `GestionarCredencialesTest` | Recuperacion admin, invalidacion de tokens, password de empleados y hash seguro | Evitar que empleados usen recuperacion, proteger tokens y asegurar cambios de contrasena solo desde admin |
| `AESCryptoUtilTest` | Cifrado y descifrado AES, IV aleatorio, claves invalidas y payloads corruptos | Proteger la utilidad de cifrado usada por el chat interno y evitar regresiones de seguridad |
| `ResponderConsultasGestionTest` | Asistente IA, function calling, respuestas directas, permisos por rol y sugerencias | Validar que el asistente no inventa datos criticos y respeta diferencias entre admin y peluquero/a |
| `ChatFunctionExecutorTest` | Ejecucion de funciones del asistente contra repositorios simulados y control de permisos | Comprobar que las funciones de negocio del chatbot devuelven datos correctos y bloquean informacion de admin a empleados |

### Tests frontend

| Test | Que valida | Para que sirve |
|---|---|---|
| `auth.store.test.ts` | Guardado, restauracion y cierre de sesion; roles admin/peluquero; token expirado | Evitar errores de sesion, menus incorrectos o acceso visual con token invalido |
| `authService.test.ts` | Llamadas de login, solicitud de reset y cambio de contrasena | Garantizar que el frontend llama a los endpoints correctos de autenticacion |
| `useChatbot.test.ts` | Mensajes del usuario y asistente, historial, errores, loading, sugerencias y limpiar chat | Proteger el comportamiento del asistente de gestion aunque se cambie la interfaz |

Comandos principales:

```bash
cd backend-spring
./mvnw test

cd front-nuxt
npm test
```

## Responsive

La aplicacion esta pensada como panel de gestion de escritorio, pero se ha trabajado para que las pantallas clave tambien funcionen en tablet y movil. El layout general contempla menu lateral como drawer en pantallas pequenas, cabecera compacta, paddings responsive, toasts adaptativos, modales centrados y paneles que no se salen del viewport.

Breakpoints y tamanos revisados manualmente:

| Vista | Tamano de referencia |
|---|---|
| Movil estrecho | `360x800` |
| Movil medio | `414x896` |
| Tablet vertical | `768x1024` |
| Desktop base | `1366x768` |
| Desktop grande | `1920x1080` |

Pantallas revisadas con foco responsive:

- Configuracion admin.
- Panel de control admin.
- Modales y popups de cabecera.
- Cards de KPIs, graficas, estado del equipo, gastos, personal e inventario.
- Formularios de horario, dias especiales y precios.
- Mensajes.
- Servicios.
- Inventario.
- Empleados.
- Calendario laboral.

Mejoras aplicadas:

- cards con contenido ajustado en movil;
- inputs y botones sin desbordar;
- textos largos acortados en formularios de horario;
- modales centrados en movil;
- graficas y leyendas con centrado corregido;
- espaciados mas respirables en cards densas;
- tablas sustituidas por cards en movil y tablet cuando era necesario;
- filtros y acciones reorganizados para evitar scroll horizontal;
- calendario laboral con cabecera y celdas compactas en movil;
- menu lateral y enlaces protegidos tras refrescar sesion;
- login y recuperacion admin preparados para movil.

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

Crear primero `docker/.env` a partir de `docker/.env.example` y rellenar las variables necesarias. Las credenciales reales no se guardan en Git.

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

Configurar las variables de entorno equivalentes a `docker/.env` en el entorno local o en el IDE antes de arrancar Spring Boot.

```bash
cd backend-spring
./mvnw spring-boot:run
```

Frontend:

Crear `front-nuxt/.env` con:

```env
NUXT_PUBLIC_API_BASE=http://localhost:8080/api
```

```bash
cd front-nuxt
npm install
npm run dev
```

## Documentacion

- [Puesta en marcha](docs/puesta-en-marcha.md)

Las credenciales demo y notas privadas se entregan aparte y no deben subirse al repositorio publico.

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

El proyecto esta completo y funcional. Cubre todos los modulos previstos: agenda, clientes, empleados, servicios, inventario, ventas, ausencias, mensajeria, resultados, configuracion, auditoria, credenciales y asistente de gestion con IA.

Las ultimas mejoras incluyen responsive en configuracion, panel de control, mensajes, servicios, inventario, empleados y calendario laboral; recuperacion de contrasena exclusiva para admin; pantalla admin para cambiar contrasenas de empleados; emails demo normalizados; descuentos promocionales en servicios; dias especiales expresados como porcentaje para el usuario; y manejo global de errores `400`, `401`, `403`, `404` y `500`.

Resumen de mejoras recientes:

- Ajuste responsive en vistas moviles `360x800` y `414x896`, tablet vertical `768x1024`, desktop `1366x768` y desktop grande `1920x1080`.
- Correccion de tarjetas, modales, popups, tablas, filtros y graficas para evitar desbordes y scroll horizontal.
- Cambio de "multiplicador" por "descuento (%)" en dias especiales, manteniendo compatibilidad con el backend actual.
- Precio promocional en servicios, mostrando precio anterior tachado y precio rebajado.
- Uso del precio con descuento en tickets y calculos financieros.
- Recuperacion de contrasena solo para el administrador mediante email.
- Cambio de contrasena de empleados desde administracion, con doble campo y validaciones.
- Normalizacion de emails demo.
- Error page global en Nuxt y respuestas JSON uniformes desde Spring Boot.
- Limpieza de configuracion IA para usar Spring AI con OpenAI y dejar el proveedor externo desactivable en demo.

Verificaciones recientes:

```bash
cd backend-spring
./mvnw test

cd front-nuxt
npx vue-tsc --noEmit
npm test
npm run build
```

Listo para demo y presentacion. Para demostrar el email real de recuperacion hace falta configurar credenciales SMTP validas de Mailtrap en el entorno local.
