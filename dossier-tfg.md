# PROYECTO DE 2º DAM
## SISTEMA INTEGRAL PARA LA GESTIÓN DE PELUQUERÍAS

**ALUMNO:** Marco Antonio Galián Raja  
**TUTOR DEL PROYECTO:** Jairo Paul Moreno Villarroel  
**CURSO:** 2025-2026  
**Ciclo Formativo:** 2º Desarrollo de Aplicaciones Multiplataforma

---

## 1. RESUMEN

El presente proyecto detalla el diseño, desarrollo e implementación de una solución tecnológica denominada "Sistema Integral de Gestión de Peluquería". Se trata de una plataforma web orientada a la gestión profesional de centros de estética y peluquería, que centraliza en una única aplicación todas las operaciones del negocio: agenda, clientes, empleados, inventario, ventas, finanzas, mensajería interna y un asistente de gestión con inteligencia artificial.

**Objetivo principal:** Desarrollar una aplicación web full-stack que digitalice la operativa diaria de un salón de belleza, optimizando los procesos de reserva, la gestión del equipo y la toma de decisiones financieras mediante datos en tiempo real y un asistente conversacional conectado al dominio del negocio.

**Tecnologías empleadas:** El ecosistema tecnológico ha sido seleccionado siguiendo los estándares actuales de la industria del software:

- **Backend:** Java 21 sobre Spring Boot 3.5, con arquitectura hexagonal y vertical slicing.
- **Persistencia:** PostgreSQL 15 gestionado a través de Spring Data JPA e Hibernate.
- **Seguridad:** Spring Security con autenticación y autorización basadas en JWT y Refresh Tokens.
- **Frontend:** Nuxt 4 con Vue 3, TypeScript, Pinia y Tailwind CSS.
- **Inteligencia Artificial:** Spring AI integrado con la API de OpenAI, con function calling para consultas en tiempo real sobre la base de datos.
- **Despliegue:** Docker Compose con tres servicios orquestados: base de datos PostgreSQL, API Spring Boot y frontend Nuxt.

**Alcance y funcionalidades clave:** El proyecto abarca la digitalización completa del flujo de trabajo del salón. Entre las funcionalidades destacan la gestión de clientes con historial clínico y consentimiento de fotos, un motor de citas con validación de solapamientos, un panel de KPIs financieros, mensajería interna cifrada con WebSocket, un módulo de auditoría de actividad del equipo y un asistente de IA que responde preguntas sobre el negocio consultando datos reales de la base de datos en tiempo real mediante function calling.

---

## 2. JUSTIFICACIÓN

### Problemática detectada

En la actualidad, una gran parte del sector de las peluquerías y centros de estética de pequeño y mediano tamaño sigue gestionando su operativa mediante métodos tradicionales: agendas de papel, hojas de cálculo o aplicaciones de mensajería genéricas. Esta falta de digitalización conlleva los siguientes problemas críticos:

1. **Conflictos de agenda:** El error humano al anotar citas provoca solapamientos, situando a dos clientes en el mismo tramo horario con el mismo empleado. Esto daña la imagen del negocio y genera pérdidas económicas directas.

2. **Gestión deficiente del equipo:** Sin una herramienta centralizada, coordinar las vacaciones, bajas y disponibilidad del personal es un proceso manual propenso a errores. La ausencia de visibilidad sobre el estado del equipo dificulta la planificación de la agenda.

3. **Inseguridad de datos:** El manejo de información personal de clientes (nombres, teléfonos, preferencias, historial de tratamientos) sin protocolos de seguridad adecuados no cumple con los estándares del Reglamento General de Protección de Datos (RGPD).

4. **Falta de análisis financiero:** Sin un registro digital estructurado, es extremadamente difícil calcular ingresos reales, gastos operativos y beneficios de forma automática, lo que impide tomar decisiones empresariales basadas en datos objetivos.

5. **Comunicación interna ineficiente:** El uso de aplicaciones de mensajería externas para la comunicación entre propietario y empleados supone una brecha de seguridad y una mezcla del ámbito personal con el profesional.

### Destinatarios del proyecto

Este software está dirigido a dos perfiles principales dentro del salón:

- **Administradores o propietarios** del centro, que necesitan una visión global del negocio: control de agenda, resultados financieros, estado del equipo, inventario y auditoría de actividad.
- **Empleados (peluqueros/as)**, que necesitan una herramienta ágil para consultar su agenda diaria, gestionar sus propias citas, solicitar vacaciones, comunicarse con el equipo y vender productos.

### Propuesta de mejora y valor añadido

Frente a las soluciones genéricas o el método manual, este proyecto plantea una propuesta de valor diferenciada basada en cuatro ejes:

1. **Excelencia en ingeniería de software:** El sistema implementa Arquitectura Hexagonal, Screaming Architecture y Vertical Slicing, siguiendo los principios SOLID. Esto garantiza que el software sea mantenible y escalable, permitiendo añadir nuevas funcionalidades sin comprometer el núcleo de negocio.

2. **Seguridad de nivel profesional:** JWT con Refresh Tokens garantiza que solo el personal autorizado acceda a la información. El chat interno utiliza cifrado AES para proteger los mensajes. La autenticación diferencia claramente entre el rol de administrador y el rol de empleado, aplicando restricciones tanto en el frontend como en el backend.

3. **Despliegue reproducible con Docker:** La aplicación se despliega en cualquier máquina con un único comando (`docker compose up --build`), eliminando problemas de configuración de entorno y facilitando la puesta en producción.

4. **Asistente de IA integrado con el negocio:** El asistente conversacional no es un chatbot genérico, sino que está conectado al dominio de la peluquería. Utiliza Spring AI con function calling sobre OpenAI para responder preguntas en lenguaje natural consultando datos reales de la base de datos: citas del día, clientes VIP, inventario bajo stock, resultados financieros, etc.

---

## 3. MARCO TEÓRICO

### Análisis del estado del arte

En el mercado actual de soluciones para la gestión de centros de estética y peluquería, se observa una clara polarización. Por un lado, una gran parte del sector sigue utilizando métodos analógicos. Por otro, existen plataformas SaaS comerciales como Fresha, Goldie o Booksy que, aunque completas, presentan limitaciones importantes: dependencia de un proveedor externo, falta de personalización, coste recurrente por suscripción y cesión del control de los datos del negocio a terceros.

La presente solución se diferencia de las existentes al ofrecer una plataforma propia, desplegable en el entorno del cliente, con control total sobre los datos. La integración de inteligencia artificial como herramienta de gestión interna (no como producto externo) representa una diferenciación técnica relevante respecto a los sistemas comparables.

Desde el punto de vista técnico, el proyecto aplica patrones de arquitectura que habitualmente se reservan para sistemas empresariales de mayor escala, demostrando su viabilidad en el contexto de un proyecto académico de FP de grado superior.

### Tecnologías y herramientas utilizadas

#### Backend

**Java 21 y Spring Boot 3.5:** Java 21 es la versión LTS (Long Term Support) más reciente en el momento del desarrollo, con mejoras de rendimiento y características modernas del lenguaje como los records y los sealed classes. Spring Boot 3.5 facilita la creación de aplicaciones autónomas de nivel de producción, con configuración automática, gestión de dependencias y un servidor embebido Tomcat.

**Spring Data JPA e Hibernate:** El acceso a los datos se realiza a través de Spring Data JPA, que proporciona repositorios genéricos reduciendo el código repetitivo. Hibernate actúa como implementación ORM (Object-Relational Mapping), mapeando las entidades de dominio a tablas de PostgreSQL.

**PostgreSQL 15:** Motor de base de datos relacional de código abierto, seleccionado por su robustez, soporte de tipos avanzados y compatibilidad con Spring Data JPA. Se ejecuta como servicio Docker con persistencia en volumen nombrado.

**Spring Security y JWT:** La seguridad del sistema se implementa con Spring Security. La autenticación es stateless: el servidor emite un JWT (JSON Web Token) firmado al hacer login, que el cliente envía en cada petición. El sistema diferencia dos roles: ADMIN y ROLE_HAIRDRESSER (empleado), aplicando restricciones de acceso en cada endpoint mediante anotaciones de seguridad.

**MapStruct y Lombok:** MapStruct genera automáticamente los mapeadores entre objetos de dominio y entidades JPA, evitando código manual propenso a errores. Lombok reduce el código boilerplate (getters, setters, constructores) mediante anotaciones.

**Spring Mail:** Integración de envío de correo electrónico para notificaciones de aprobación o rechazo de ausencias, usando Mailtrap como servidor SMTP de pruebas.

**Spring AI y OpenAI:** Spring AI 1.1.x, compatible con Spring Boot 3.5, actúa como capa de integración con la API de OpenAI. Gestiona el ciclo completo de function calling: el modelo solicita una función de negocio, el backend la ejecuta de forma segura contra PostgreSQL y devuelve el resultado para que el modelo redacte la respuesta final.

**Springdoc OpenAPI:** Generación automática de documentación de la API REST en formato OpenAPI 3.0, con interfaz Swagger UI accesible en desarrollo.

**JUnit 5 y Mockito:** Framework de pruebas unitarias para el backend, con Mockito para la simulación de dependencias.

#### Frontend

**Nuxt 4 y Vue 3:** Nuxt 4 es el framework full-stack sobre Vue 3 que proporciona enrutamiento automático, layouts, composables y una estructura de proyecto organizada. Vue 3 con Composition API permite crear componentes reactivos con código limpio y mantenible.

**TypeScript:** Tipado estático que mejora la robustez del código frontend y facilita el mantenimiento, detectando errores en tiempo de desarrollo.

**Pinia:** Store de estado global para Vue 3, utilizado para gestionar la sesión del usuario, el estado de la agenda y la comunicación entre componentes. Las stores están protegidas contra el acceso a localStorage en el lado del servidor (SSR) mediante `import.meta.client`.

**Tailwind CSS:** Framework de utilidades CSS que permite diseñar interfaces responsive y modernas directamente en el HTML, sin necesidad de escribir CSS personalizado en la mayoría de los casos.

**FullCalendar:** Librería para la vista de agenda visual, con soporte para eventos, arrastrar y soltar, y vistas de día, semana y mes.

**Chart.js:** Librería de gráficas utilizada en el panel de resultados financieros para visualizar ingresos, gastos, beneficios y tendencias.

**Axios:** Cliente HTTP para las llamadas a la API REST del backend, con interceptores para la gestión automática del token JWT.

**WebSocket:** Comunicación en tiempo real para el módulo de mensajería interna, permitiendo la recepción de mensajes sin necesidad de polling.

**Vitest:** Framework de pruebas unitarias para el frontend, compatible con el ecosistema Vite y Nuxt.

#### Infraestructura

**Docker y Docker Compose:** La aplicación se conteneriza en tres servicios: `db` (PostgreSQL 15), `api` (Spring Boot) y `front` (Nuxt). Docker Compose orquesta el arranque en el orden correcto, con health checks y volúmenes persistentes para los datos y los archivos subidos.

---

## 4. OBJETIVOS

### Objetivo General

Diseñar, desarrollar e implementar una aplicación web full-stack para la gestión profesional de peluquerías que centralice la operativa diaria del negocio, garantizando la seguridad de los datos mediante arquitecturas limpias, y que incorpore un asistente de inteligencia artificial conectado al dominio real del negocio para facilitar la toma de decisiones.

### Objetivos Específicos

1. **Implementar una arquitectura de software robusta** basada en los principios de Arquitectura Hexagonal, Screaming Architecture y Vertical Slicing, garantizando la separación entre dominio, aplicación e infraestructura en cada módulo del sistema.

2. **Desarrollar un sistema de seguridad multicapa** con Spring Security, JWT y Refresh Tokens, diferenciando los roles de administrador y empleado tanto en el backend (restricciones por endpoint) como en el frontend (rutas protegidas y menús adaptativos).

3. **Diseñar un motor de validación de disponibilidad** en la capa de dominio que prevenga automáticamente el solapamiento de citas, detectando conflictos de horario para el mismo empleado antes de confirmar cualquier reserva.

4. **Implementar un mecanismo de control de concurrencia** mediante semáforos para serializar las solicitudes simultáneas de vacaciones y ausencias, garantizando la consistencia del calendario cuando varios empleados acceden al sistema al mismo tiempo.

5. **Integrar un asistente de inteligencia artificial** con Spring AI y OpenAI que utilice function calling para responder preguntas en lenguaje natural sobre datos reales del negocio: agenda, clientes, inventario, ventas, ausencias y resultados financieros. El asistente debe respetar los permisos por rol.

6. **Crear un panel de control con KPIs financieros** que ofrezca al administrador una visión en tiempo real de ingresos, gastos, beneficios, ticket medio, tasa de cancelación, productos más vendidos y rendimiento por empleado.

7. **Desarrollar un módulo de mensajería interna** con comunicación en tiempo real mediante WebSocket y cifrado AES de los mensajes, proporcionando un canal de comunicación seguro dentro de la propia plataforma.

8. **Implementar un sistema de auditoría de actividad** que registre automáticamente todas las acciones de creación, modificación y eliminación realizadas por los usuarios, identificando al actor, el módulo afectado, la entidad modificada y la fecha y hora de la operación.

9. **Desplegar la aplicación de forma reproducible** mediante Docker Compose, permitiendo levantar todos los servicios (base de datos, API y frontend) con un único comando en cualquier entorno.

10. **Cubrir la lógica de negocio crítica con pruebas automatizadas**, incluyendo la validación de solapamientos en la agenda, el control de concurrencia en ausencias, la seguridad de la autenticación y el comportamiento del asistente de IA con distintos roles.

---

## 5. DESARROLLO DEL PROYECTO

### 5.1. Análisis y Requisitos

#### Requerimientos Funcionales

El sistema debe satisfacer los siguientes requerimientos funcionales, organizados por módulo:

**RF-01. Autenticación y gestión de sesión**
- El sistema debe permitir el inicio de sesión mediante usuario y contraseña.
- Las contraseñas deben almacenarse cifradas con BCrypt.
- El sistema debe emitir un JWT de acceso y un Refresh Token al autenticar al usuario.
- El Refresh Token permite renovar el JWT sin necesidad de volver a introducir credenciales.
- Los usuarios deben poder cerrar sesión, invalidando el token de acceso.
- El sistema debe soportar recuperación de contraseña mediante enlace enviado por correo electrónico.

**RF-02. Gestión de citas (Agenda)**
- El administrador debe poder crear, editar, cancelar y completar citas.
- El sistema debe impedir la creación de citas que se solapen en horario para el mismo empleado.
- La agenda debe mostrarse en formato visual de calendario (vistas de día, semana y mes).
- Cada cita debe asociar cliente, empleado, servicio, fecha, hora de inicio y duración.
- El empleado debe poder ver únicamente su propia agenda.

**RF-03. Gestión de clientes**
- El sistema debe permitir registrar, editar y archivar clientes.
- Cada cliente debe disponer de: datos personales, tipo (VIP o estándar), descuento personalizado, historial de citas, galería de fotos y consentimiento de tratamiento de imágenes.
- Los clientes archivados deben poder reactivarse.
- El administrador debe poder filtrar clientes por nombre, tipo VIP y estado (activo/archivado).

**RF-04. Gestión de empleados**
- El administrador debe poder registrar, editar y dar de baja empleados.
- Cada empleado debe tener: nombre, especialidad, horario base, estado de disponibilidad, indicadores de baja y vacaciones, y cuenta de usuario asociada.
- El sistema debe permitir registrar bajas médicas y reactivar empleados.

**RF-05. Catálogo de servicios**
- El administrador debe poder crear, editar y eliminar servicios.
- Cada servicio debe tener: nombre, precio, duración en minutos, categoría (señora/caballero) y género.

**RF-06. Inventario de productos**
- El sistema debe gestionar un catálogo de productos con nombre, precio, stock actual, stock mínimo, categoría y género.
- El sistema debe alertar cuando el stock de un producto esté por debajo del mínimo.
- Los empleados deben poder registrar ventas de productos desde su panel.
- El administrador debe poder ajustar el stock manualmente.

**RF-07. Gestión de ausencias y vacaciones**
- Los empleados deben poder solicitar vacaciones, bajas y permisos desde su panel.
- El administrador debe poder aprobar, rechazar o cancelar las solicitudes.
- El empleado debe recibir una notificación por correo electrónico con la resolución.
- El sistema debe serializar las solicitudes concurrentes para evitar condiciones de carrera en el calendario.
- El administrador debe poder bloquear días del calendario para impedir la solicitud de vacaciones en fechas concretas.

**RF-08. Resultados financieros**
- El administrador debe poder consultar ingresos, gastos y beneficios por periodo (hoy, semana, mes, trimestre, año).
- El sistema debe calcular el ticket medio, la tasa de cancelación y el top de servicios más rentables.
- El administrador debe poder registrar y consultar gastos operativos del negocio.

**RF-09. Mensajería interna**
- El sistema debe permitir enviar mensajes entre usuarios autenticados.
- Los mensajes deben entregarse en tiempo real mediante WebSocket.
- Los mensajes deben almacenarse cifrados con AES en la base de datos.
- El administrador debe poder enviar mensajes a todos los empleados simultáneamente.
- El sistema debe notificar el número de mensajes no leídos en la interfaz.

**RF-10. Asistente de inteligencia artificial**
- El sistema debe incorporar un asistente conversacional accesible desde el panel.
- El asistente debe responder preguntas en lenguaje natural sobre el negocio.
- El asistente debe utilizar function calling para consultar datos en tiempo real de la base de datos.
- El asistente debe respetar los permisos por rol: el administrador accede a todos los datos; el empleado solo a los suyos.
- Las preguntas ajenas al negocio deben rechazarse con un mensaje estándar.

**RF-11. Panel de control**
- El panel de control debe mostrar KPIs del día: citas del día, ingresos, productos con bajo stock, empleados disponibles y mensajes pendientes.
- Debe incluir gráficas de evolución de ingresos, ventas y estado de la agenda.

**RF-12. Auditoría de actividad**
- El sistema debe registrar automáticamente cada acción de creación, modificación y eliminación.
- Cada registro debe incluir: usuario, rol, módulo, descripción de la acción (con nombre de la entidad), método HTTP, ruta, código de respuesta y fecha y hora.
- El administrador debe poder consultar el historial de actividad reciente.

**RF-13. Configuración del negocio**
- El administrador debe poder editar los datos del centro: nombre, dirección, teléfono, email y horario de apertura y cierre.
- El sistema debe permitir gestionar ofertas y promociones activas.

#### Requerimientos No Funcionales

**RNF-01. Seguridad:** Todas las contraseñas deben almacenarse con hash BCrypt. La comunicación entre cliente y servidor debe estar protegida mediante tokens JWT firmados. Los mensajes del chat interno deben cifrarse con AES antes de persistirse. Los permisos deben validarse en el backend independientemente de lo que muestre el frontend.

**RNF-02. Rendimiento:** El contexto de negocio del asistente IA se cachea en memoria y se regenera automáticamente cada noche a las 03:00, evitando consultas innecesarias a la base de datos en cada petición del chat.

**RNF-03. Disponibilidad:** La aplicación debe poder desplegarse con un único comando y permanecer operativa de forma continua mediante las políticas de reinicio de Docker Compose (`restart: always`).

**RNF-04. Mantenibilidad:** La arquitectura hexagonal y el vertical slicing garantizan que cada módulo sea independiente y modificable sin afectar al resto del sistema. Los contratos de dominio (interfaces de repositorio) desacoplan la lógica de negocio de la tecnología de persistencia.

**RNF-05. Escalabilidad:** La separación entre frontend, backend y base de datos en contenedores independientes permite escalar cada capa de forma independiente en el futuro.

**RNF-06. Compatibilidad:** La interfaz debe ser funcional en los navegadores modernos principales (Chrome, Firefox, Edge, Safari). El diseño debe adaptarse a distintos tamaños de pantalla: escritorio, tablet y móvil.

**RNF-07. Reproducibilidad del entorno:** El despliegue completo (base de datos, API y frontend) debe reproducirse en cualquier máquina con Docker instalado, sin configuración manual adicional más allá de las variables de entorno.

**RNF-08. Trazabilidad:** Toda acción que modifique datos del sistema debe quedar registrada en el módulo de auditoría, permitiendo reconstruir el historial de cambios de cualquier entidad.

---

### 5.2. Diseño

#### Arquitectura del sistema

La aplicación sigue una arquitectura de tres capas bien diferenciadas, desplegadas como servicios Docker independientes:

```
[ Frontend Nuxt 4 : 3000 ]  →  [ API Spring Boot : 8080 ]  →  [ PostgreSQL : 5432 ]
```

El backend aplica una combinación de tres patrones arquitectónicos:

- **Vertical Slicing:** El código se organiza por módulo de negocio, no por capa técnica. Cada módulo (`citas`, `clientes`, `peluqueros`, `chatbot`, etc.) contiene su propia capa de aplicación, dominio e infraestructura.
- **Screaming Architecture:** Los nombres de los paquetes reflejan el negocio, no la tecnología. Se habla de `GestionarAgenda`, `RegistrarCliente` o `ResponderConsultasGestion`, no de `Controller`, `Service` o `Repository`.
- **Arquitectura Hexagonal implícita:** Los contratos de dominio (interfaces de repositorio y puertos) viven en el paquete `domain`. Las implementaciones concretas (JPA, Spring AI, SMTP) viven en `infrastructure`. La lógica de negocio en `application` no depende de ninguna tecnología concreta.

```
backend-spring/src/main/java/com/marcog/peluqueria/<modulo>/
├── application      # Casos de uso: GestionarAgenda, RegistrarCliente
├── domain           # Modelo de negocio e interfaces: Cliente, ClienteRepository
└── infrastructure   # Implementaciones: web (controllers), persistencia (JPA), email
```

El frontend replica el mismo criterio de vertical slicing:

```
front-nuxt/app/
├── pages                 # Rutas Nuxt (wrappers finos)
├── modules               # Agenda, clientes, inventario, mensajes, chatbot...
├── components            # Componentes de layout globales
└── infrastructure/http   # Cliente Axios con interceptores JWT
```

#### Modelo de datos principal

El modelo de datos se organiza en torno a las siguientes entidades principales:

- **Usuario:** Cuenta de acceso con rol (ADMIN / ROLE_HAIRDRESSER), email, contraseña hash y tokens de refresco.
- **Peluquero:** Perfil profesional vinculado a un usuario. Contiene nombre, especialidad, horario base, estado de disponibilidad, baja y vacaciones.
- **Cliente:** Datos personales, tipo VIP, descuento, consentimiento de fotos y relación con sus citas e imágenes.
- **Servicio:** Catálogo de tratamientos con nombre, precio, duración, categoría y género.
- **Cita:** Relación entre cliente, peluquero y servicio, con fecha, hora de inicio, hora de fin y estado (PENDIENTE, COMPLETADA, CANCELADA).
- **Producto:** Artículo de inventario con nombre, precio, stock, stock mínimo, categoría y género.
- **Ausencia:** Solicitud de vacaciones o baja de un empleado, con tipo, fechas, estado (PENDIENTE, APROBADA, RECHAZADA) y justificación.
- **Mensaje:** Comunicación interna cifrada entre usuarios, con remitente, destinatario, contenido AES y marca temporal.
- **Gasto:** Gasto operativo del negocio, con concepto, importe y fecha.
- **RegistroActividad:** Entrada de auditoría con usuario, rol, módulo, descripción, método HTTP, ruta y estado.
- **Configuracion:** Datos del centro (nombre, dirección, horario, contacto).
- **Oferta:** Promoción activa con nombre, descripción, descuento, fechas de vigencia y tipo.

#### Diseño de seguridad

El flujo de autenticación funciona de la siguiente manera:

1. El cliente envía credenciales al endpoint `/api/auth/login`.
2. El backend valida las credenciales con BCrypt y, si son correctas, emite un JWT de corta duración y un Refresh Token.
3. El cliente almacena ambos tokens y adjunta el JWT en la cabecera `Authorization: Bearer <token>` de cada petición.
4. Spring Security intercepta cada petición, valida la firma y la caducidad del JWT y establece el contexto de seguridad.
5. Cuando el JWT caduca, el cliente utiliza el Refresh Token para obtener uno nuevo sin interrumpir la sesión.

Los roles se aplican mediante anotaciones de Spring Security en los controllers (`@PreAuthorize`), garantizando que las restricciones se cumplan incluso si el frontend no oculta una opción.

#### Diseño del asistente IA

El asistente combina dos mecanismos para responder preguntas:

1. **Contexto estático cacheado:** Al arrancar la aplicación, `ArchivoContextoNegocio` consulta la base de datos y genera un JSON con los datos que cambian poco: nombre del centro, horario, políticas, equipo, catálogo de servicios, productos y ofertas activas. Este JSON se inyecta en el system prompt del modelo y se regenera automáticamente cada noche.

2. **Function calling en tiempo real:** Para datos que deben ser frescos (citas del día, resultados financieros, clientes VIP, stock actual, vacaciones), el modelo puede invocar funciones de negocio. El backend ejecuta la función contra PostgreSQL y devuelve el resultado al modelo para que redacte la respuesta final. El bucle admite hasta tres iteraciones de function calling por petición.

```
Usuario → Backend → LLM (OpenAI)
                      ↓ solicita función
              Backend ejecuta función → PostgreSQL
                      ↓ resultado
              LLM → respuesta en lenguaje natural → Usuario
```

---

### 5.3. Desarrollo

#### Control de concurrencia en ausencias

El caso de uso de gestión de ausencias implementa un control de concurrencia con `Semaphore(1, true)` para serializar las solicitudes que afectan al calendario. Cuando dos empleados envían una solicitud de vacaciones simultáneamente, el semáforo garantiza que solo una de ellas entre en la sección crítica a la vez. La primera valida los días bloqueados, aplica las reglas de negocio y guarda; la segunda espera su turno.

Esta decisión tiene valor técnico porque demuestra gestión de hilos, sección crítica y protección de reglas de negocio en el backend. Para el despliegue actual con una instancia de Spring Boot es suficiente. Si el sistema escalara a múltiples instancias, el siguiente paso natural sería mover el bloqueo a la base de datos o a un mecanismo distribuido como Redis.

Las bajas médicas quedan fuera de este bloqueo, ya que representan una situación inevitable que no debe depender de la disponibilidad del semáforo.

#### Cifrado del chat interno

Los mensajes del chat interno se cifran con AES-256 antes de persistirse en la base de datos. El vector de inicialización (IV) se genera aleatoriamente en cada cifrado, garantizando que dos mensajes con el mismo contenido produzcan distintos textos cifrados. La clave AES se configura mediante variable de entorno y nunca se almacena en el código fuente.

#### Auditoría automática

El módulo de auditoría funciona mediante un `HandlerInterceptor` de Spring MVC que intercepta todas las peticiones `POST`, `PUT`, `PATCH` y `DELETE` tras su finalización (`afterCompletion`). Para cada operación registra el usuario autenticado, el rol, el módulo afectado, una descripción legible de la acción (resolviendo el nombre de la entidad modificada a partir de su ID consultando el repositorio correspondiente), el método HTTP, la ruta, el código de respuesta y la fecha y hora.

#### Estructura del repositorio

```
peluqueria/
├── backend-spring/       # API Spring Boot (Java 21)
├── front-nuxt/           # Aplicación Nuxt 4 (Vue 3 + TypeScript)
├── docker/               # Docker Compose y variables de entorno
└── docs/                 # Documentación de puesta en marcha
```

---

### 5.4. Usabilidad y Experiencia de Usuario

La interfaz se ha diseñado como un panel de gestión profesional orientado a escritorio, con adaptación responsive para tablet y móvil. Los principios de diseño aplicados son:

- **Jerarquía visual clara:** El menú lateral agrupa las secciones por área funcional. El panel de control presenta los KPIs más importantes de forma prominente al acceder.
- **Feedback inmediato:** Todas las operaciones de creación, edición y eliminación muestran un toast de confirmación o error que desaparece automáticamente.
- **Confirmación de acciones destructivas:** Las eliminaciones requieren confirmación mediante un modal, previniendo eliminaciones accidentales.
- **Diseño adaptativo por rol:** El menú lateral y las secciones disponibles se adaptan automáticamente según el rol del usuario autenticado, mostrando únicamente las opciones pertinentes a su perfil.
- **Calendario visual:** El módulo de agenda utiliza FullCalendar con vistas de día, semana y mes, permitiendo crear citas directamente haciendo clic en un hueco disponible.
- **Asistente accesible:** El asistente de IA está disponible desde cualquier sección del panel mediante un icono fijo, sin necesidad de navegar a una página separada.

---

### 5.5. Pruebas

#### Pruebas de backend (JUnit 5 + Mockito)

| Test | Qué valida |
|---|---|
| `PeluqueriaApiApplicationTests` | Arranque del contexto Spring completo con perfil de test. Detecta fallos de configuración, inyección de dependencias o seguridad antes de ejecutar. |
| `GestionarAgendaTest` | Creación y edición de citas, solapamientos y citas consecutivas permitidas. |
| `GestionarAusenciasTest` | Serialización de solicitudes concurrentes de vacaciones con semáforo. |
| `AutenticarUsuarioTest` | Registro seguro, rol por defecto, password hasheado y emisión de tokens. |
| `AESCryptoUtilTest` | Cifrado y descifrado AES, IV aleatorio, claves inválidas y payloads corruptos. |
| `ResponderConsultasGestionTest` | Asistente IA: function calling, respuestas directas, permisos por rol y sugerencias. |
| `ChatFunctionExecutorTest` | Ejecución de funciones del chatbot y control de permisos por rol. |

#### Pruebas de frontend (Vitest)

| Test | Qué valida |
|---|---|
| `auth.store.test.ts` | Guardado, restauración y cierre de sesión; roles admin/peluquero; token expirado. |
| `authService.test.ts` | Llamadas de login, solicitud de reset y cambio de contraseña. |
| `useChatbot.test.ts` | Mensajes del usuario y asistente, historial, errores, loading y sugerencias. |

Los tests se ejecutan con:

```bash
# Backend
cd backend-spring && ./mvnw test

# Frontend
cd front-nuxt && npm test
```

---

### 5.6. Despliegue

La aplicación se despliega mediante Docker Compose con tres servicios:

- **`db`:** PostgreSQL 15 con healthcheck. Los datos persisten en el volumen `postgres_data`.
- **`api`:** Imagen construida desde `backend-spring/Dockerfile`. Espera a que `db` esté sano antes de arrancar. Los archivos subidos (fotos) persisten en el volumen `uploads_data`.
- **`front`:** Imagen construida desde `front-nuxt/Dockerfile`. Depende del servicio `api`.

El comando de despliegue completo es:

```bash
cd docker
docker compose up --build -d
```

Una vez levantados los servicios, la aplicación está disponible en:

| Servicio | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

La configuración sensible (claves JWT, clave AES, credenciales de base de datos, clave de API de OpenAI) se gestiona mediante variables de entorno en el archivo `docker/.env`, que no se incluye en el repositorio.

---

## 6. CONCLUSIONES Y FUTURAS MEJORAS

### Valoración personal

El desarrollo de este proyecto ha supuesto un reto técnico significativo que ha permitido consolidar conocimientos adquiridos a lo largo del ciclo y aplicarlos en un contexto realista. La decisión de adoptar una arquitectura hexagonal desde el inicio, aunque supuso un esfuerzo adicional en las primeras fases, demostró su valor a medida que el sistema creció: añadir nuevos módulos como la auditoría o el asistente IA resultó natural sin necesidad de modificar el núcleo existente.

La integración de Docker Compose como entorno de despliegue resolvió uno de los problemas clásicos del desarrollo: la reproducibilidad del entorno. Poder levantar la aplicación completa con un único comando facilita enormemente las pruebas y la presentación del proyecto.

### Grado de cumplimiento de objetivos

Todos los objetivos específicos definidos en el apartado 4 han sido alcanzados:

- La arquitectura hexagonal con vertical slicing está implementada en todos los módulos del backend.
- El sistema de seguridad con JWT y roles funciona correctamente, con restricciones aplicadas en backend y frontend.
- El motor de validación de solapamientos previene conflictos de agenda.
- El control de concurrencia con semáforo está operativo en el módulo de ausencias.
- El asistente IA responde preguntas en lenguaje natural con function calling y respeta los permisos por rol.
- El panel de control muestra KPIs en tiempo real.
- La mensajería interna funciona en tiempo real con WebSocket y cifrado AES.
- El módulo de auditoría registra todas las operaciones con nombre de entidad legible.
- El despliegue con Docker Compose es completamente reproducible.
- Los tests automatizados cubren los casos de uso críticos del backend y la lógica del frontend.

### Problemas encontrados y soluciones aplicadas

**Solapamiento de citas:** La validación inicial de solapamientos no contemplaba citas consecutivas que terminaban exactamente a la hora de inicio de la siguiente. Se corrigió ajustando la condición de comparación para permitir citas consecutivas sin margen.

**Concurrencia en ausencias:** En pruebas con peticiones simultáneas, el sistema permitía que dos empleados obtuvieran los mismos días si ambas solicitudes pasaban la validación antes de que alguna persistiera. Se resolvió con un semáforo que serializa el acceso a la sección crítica.

**SSR en Nuxt con localStorage:** Las stores de Pinia accedían a `localStorage` durante el renderizado en servidor, generando errores de hidratación. Se resolvió protegiendo todos los accesos a `localStorage` con `import.meta.client`.

**Cifrado de mensajes con IV aleatorio:** La primera implementación del cifrado AES generaba siempre el mismo IV, haciendo predecible el texto cifrado. Se corrigió generando un IV aleatorio por mensaje y almacenándolo junto al texto cifrado.

### Posibles mejoras y ampliaciones futuras

1. **Panel de Super Administrador (modo SaaS):** Implementar un rol de super administrador con acceso a un backoffice global que permita gestionar múltiples centros (tenants) desde una única instalación. Cada salón operaría de forma aislada con sus propios datos, convirtiendo la plataforma en un producto SaaS completo.

2. **Sistema de tickets de soporte:** Añadir un módulo de gestión de incidencias que permita a los usuarios del sistema abrir tickets de soporte, seguir su estado (abierto, en proceso, cerrado) y recibir respuestas desde el panel de administración.

3. **Portal de autocita para clientes:** Desarrollar una interfaz pública (sin autenticación de empleado) que permita a los clientes del salón reservar sus propias citas seleccionando servicio, empleado disponible y franja horaria.

4. **Autenticación de dos factores (2FA):** Añadir TOTP (Time-based One-Time Password) como segundo factor de autenticación para cuentas de administrador, aumentando la seguridad del acceso al sistema.

5. **Aplicación móvil nativa:** Desarrollar una aplicación móvil para iOS y Android que permita a los empleados gestionar su agenda y recibir notificaciones push de nuevas citas o mensajes.

6. **Notificaciones push y recordatorios:** Implementar un sistema de recordatorios automáticos por SMS o WhatsApp a los clientes antes de su cita, reduciendo las no-presentaciones.

---

## 7. BIBLIOGRAFÍA

**Documentación oficial y referencias técnicas:**

- Spring Framework Team. (2024). *Spring Boot Reference Documentation (3.5.x)*. VMware. https://docs.spring.io/spring-boot/docs/current/reference/html/

- Spring Framework Team. (2024). *Spring Security Reference*. VMware. https://docs.spring.io/spring-security/reference/

- Spring AI Team. (2024). *Spring AI Reference Documentation (1.1.x)*. VMware. https://docs.spring.io/spring-ai/reference/

- Vue.js Team. (2024). *Vue 3 Documentation*. Evan You. https://vuejs.org/guide/introduction.html

- Nuxt Team. (2024). *Nuxt 4 Documentation*. NuxtLabs. https://nuxt.com/docs

- Docker Inc. (2024). *Docker Compose Documentation*. Docker. https://docs.docker.com/compose/

- PostgreSQL Global Development Group. (2024). *PostgreSQL 15 Documentation*. https://www.postgresql.org/docs/15/

- OpenAI. (2024). *OpenAI API Reference - Function Calling*. https://platform.openai.com/docs/guides/function-calling

**Libros y recursos académicos:**

- Evans, E. (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.

- Martin, R. C. (2017). *Clean Architecture: A Craftsman's Guide to Software Structure and Design*. Prentice Hall.

- Walls, C. (2022). *Spring in Action (6th Edition)*. Manning Publications.

- Richards, M., & Ford, N. (2020). *Fundamentals of Software Architecture*. O'Reilly Media.

**Normativa:**

- Reglamento (UE) 2016/679 del Parlamento Europeo y del Consejo, de 27 de abril de 2016, relativo a la protección de las personas físicas en lo que respecta al tratamiento de datos personales (RGPD). *Diario Oficial de la Unión Europea*, L 119, 1-88.

---

## 8. ANEXOS

### Anexo A: Comandos de despliegue y operación

```bash
# Arranque completo
cd docker
docker compose up --build -d

# Ver logs del backend
docker compose logs api --tail 120

# Ver logs del frontend
docker compose logs front --tail 120

# Ejecutar tests de backend
cd backend-spring
./mvnw test

# Ejecutar tests de frontend
cd front-nuxt
npm test
```

### Anexo B: Variables de entorno requeridas

| Variable | Descripción | Obligatoria |
|---|---|---|
| `DB_USER` | Usuario de PostgreSQL | Sí |
| `DB_PASSWORD` | Contraseña de PostgreSQL | Sí |
| `DB_NAME` | Nombre de la base de datos | Sí |
| `JWT_SECRET_KEY` | Clave secreta para firmar JWT (≥ 32 bytes) | Sí |
| `CHAT_AES_KEY` | Clave AES para cifrado del chat | Sí |
| `OPENAI_API_KEY` | Clave de API de OpenAI | Para el chatbot |
| `OPENAI_MODEL` | Modelo de OpenAI a usar | No (default: gpt-4.1-nano) |
| `MAILTRAP_USERNAME` | Usuario SMTP de Mailtrap | Para emails |
| `MAILTRAP_PASSWORD` | Contraseña SMTP de Mailtrap | Para emails |

### Anexo C: Módulos implementados

| Módulo | Descripción | Roles con acceso |
|---|---|---|
| Panel de control | KPIs, gráficas y alertas del negocio | Admin |
| Agenda | Gestión visual de citas | Admin, Empleado |
| Clientes | Gestión completa con historial y fotos | Admin, Empleado |
| Empleados | Gestión del equipo | Admin |
| Servicios | Catálogo de tratamientos | Admin |
| Inventario | Productos y stock | Admin, Empleado (lectura) |
| Ventas | Registro de ventas de productos | Admin, Empleado |
| Ausencias | Vacaciones y bajas | Admin, Empleado |
| Resultados | KPIs financieros detallados | Admin |
| Mensajería | Chat interno en tiempo real | Admin, Empleado |
| Chatbot | Asistente IA de gestión | Admin, Empleado |
| Auditoría | Registro de actividad del equipo | Admin |
| Configuración | Datos y parámetros del centro | Admin |
