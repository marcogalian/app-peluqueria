# Memoria de Progreso — 2026-05-09

Documento de referencia para no perder el hilo del proyecto. Recoge tanto lo ya existente en el repo como lo avanzado en esta etapa, incluyendo trabajo previo realizado por otra IA y el remate hecho en esta sesión.

## 1. Estado general

La base del sistema está montada y funcional:

- Backend `Spring Boot` con seguridad JWT, PostgreSQL, JPA/Hibernate, WebSocket STOMP y arquitectura modular por dominios.
- Frontend `Nuxt 4 + Vue 3 + TypeScript + Tailwind + Pinia`.
- Docker operativo con `db`, `api` y `front`.
- Los 7 planes funcionales ya se consideran listos según la planificación previa, incluido el chatbot.

Además, el chatbot ya estaba integrado como bloque terminado antes de esta memoria:

- Contexto de negocio cargado desde backend.
- Servicio de chatbot y funciones auxiliares existentes.
- Tests de chatbot pasando en backend.

## 2. Módulos y bloques ya presentes en el proyecto

Según el código actual del repositorio, el sistema ya incluye:

- `security`: login JWT + refresh token.
- `clientes`: clientes, VIP, historial, fotos y consentimiento.
- `citas` y `agenda`: gestión de citas.
- `peluqueros` / empleados.
- `servicios`.
- `productos` / inventario.
- `ausencias` / vacaciones y bajas.
- `ofertas` y días especiales.
- `finanzas` / resultados.
- `chat` / mensajes.
- `chatbot`.

## 3. Trabajo previo importante heredado del otro hilo / otra IA

### 3.1. Ausencias, vacaciones y días bloqueados

Quedó implementado y validado:

- Validación de días bloqueados en `AusenciaService.solicitar`.
- Nuevo campo `vistaPorEmpleado` en solicitud de ausencia.
- Endpoint para marcar notificación de ausencia como vista.
- Al aprobar o rechazar una solicitud, vuelve a quedar como no vista para que el empleado la vea al entrar.
- CRUD de días bloqueados para vacaciones.
- Integración en admin > Configuración para gestionar días bloqueados.
- Página de vacaciones del empleado con:
  - card de notificación de aprobación/rechazo,
  - aviso de días bloqueados,
  - mini calendario / warning de solapamiento,
  - bloqueo del envío si choca con fechas bloqueadas.

Estado reportado anteriormente:

- Build frontend OK.
- Build backend OK.
- Tests backend pasando.

### 3.2. Clientes: género y UX del panel

Quedó corregido:

- Eliminado `OTRO` del género en frontend.
- El tipo `Genero` del frontend quedó reducido a `MASCULINO | FEMENINO`.
- Los formularios de cliente dejaron `FEMENINO` como valor por defecto.

## 4. Trabajo rematado en esta sesión

### 4.1. Clientes: ficha desplegable inline

Se rehizo la experiencia de detalle de cliente:

- Se eliminó la idea de panel lateral fijo a la derecha.
- Ahora al pulsar un cliente, su ficha se despliega hacia abajo desde esa propia tarjeta.
- El bloque expandido permite editar datos del cliente en línea.
- Mantiene actividad, notas, VIP, fotos e historial visible en el propio flujo de la lista.

Ajustes de UI hechos después:

- Quitado el borde duro del cliente seleccionado.
- Estado seleccionado más limpio con fondo suave.
- Los selects se han dejado menos redondos.
- Los campos editables en la ficha de cliente se han aclarado para no verse “apagados” o demasiado grises.

Archivos principales tocados en esta parte:

- [clientes.vue](C:/Users/marco/wa/peluqueria/front-nuxt/app/pages/admin/clientes.vue)
- [cliente.types.ts](C:/Users/marco/wa/peluqueria/front-nuxt/app/modules/clientes/types/cliente.types.ts)
- [main.css](C:/Users/marco/wa/peluqueria/front-nuxt/app/assets/css/main.css)

### 4.2. Ajuste global visual de selects

Se suavizó el estilo de desplegables:

- `select-field` pasó a un radio menos exagerado.
- Se quitaron varios `rounded-full` que forzaban aspecto de píldora en distintas pantallas.

Páginas ajustadas en esta parte:

- `admin/clientes`
- `admin/servicios`
- `admin/inventario`
- `admin/resultados`
- `ventas`

### 4.3. Mensajes: problema detectado, corregido y rehecho a nivel UX

Problema reportado:

- Al enviar un mensaje/correo a Sofía Martínez desde `/mensajes`, no aparecía toast.
- Tampoco se veía el mensaje en ninguna parte.

Causa real detectada:

- La pantalla de mensajes estaba enviando correo por Mailtrap, pero no persistía conversación.
- El endpoint de historial devolvía siempre lista vacía.
- El frontend no usaba el sistema global de toasts en esa página.

Primera solución implementada:

- Se añadió persistencia de mensajes internos en backend:
  - entidad `MensajeInternoEntity`,
  - repositorio `JpaMensajeInternoRepository`.
- El historial entre usuario y contacto ya devuelve mensajes reales.
- Enviar desde `/mensajes/email/{contactoId}` ahora:
  - envía el correo,
  - guarda un mensaje interno,
  - devuelve el mensaje creado al frontend.
- El frontend de mensajes ahora:
  - muestra toast de éxito o error,
  - inserta el mensaje enviado en la conversación al momento,
  - deja visible el mensaje enviado sin necesidad de recargar.
- Se ajustó la identificación del emisor para que el mensaje propio salga correctamente como mensaje “mío”.

Remate posterior en UX:

- La parte inferior estilo “burbuja/chat” se eliminó.
- Los mensajes pasaron a mostrarse como lista de tareas alargadas debajo de la card de redactar email.
- Cada mensaje queda asociado al peluquero/contacto seleccionado en la columna izquierda.
- Se añadieron acciones por mensaje:
  - `Archivar`, para ocultarlo de la bandeja activa,
  - `Eliminar`, para borrarlo completamente.
- Se eliminó el texto auxiliar que decía que el correo se vería en Mailtrap.
- Se suavizó la UI de la bandeja para que se parezca más a una lista operativa que a un chat:
  - asunto más compacto,
  - acciones menos invasivas,
  - jerarquía visual más limpia.

Archivos principales tocados:

- [MensajesController.java](C:/Users/marco/wa/peluqueria/backend-spring/src/main/java/com/marcog/peluqueria/chat/infrastructure/in/web/MensajesController.java)
- [JpaMensajeInternoRepository.java](C:/Users/marco/wa/peluqueria/backend-spring/src/main/java/com/marcog/peluqueria/chat/infrastructure/out/persistence/JpaMensajeInternoRepository.java)
- [MensajeInternoEntity.java](C:/Users/marco/wa/peluqueria/backend-spring/src/main/java/com/marcog/peluqueria/chat/infrastructure/out/persistence/MensajeInternoEntity.java)
- [mensajes.vue](C:/Users/marco/wa/peluqueria/front-nuxt/app/pages/mensajes.vue)

### 4.4. Branding: nombre visible de la aplicación

Se corrigió el título global de la aplicación:

- El `title` servido por Nuxt dejó de usar `Atelier Sapphire — Gestión`.
- Ahora la pestaña y el documento HTML salen como `Peluquería Isabella`.
- Se validó reconstruyendo el frontend y comprobando el `<title>` real servido por `http://localhost:3000`.

Archivo tocado:

- [nuxt.config.ts](C:/Users/marco/wa/peluqueria/front-nuxt/nuxt.config.ts)

## 5. Validaciones hechas en esta etapa

### Frontend

- `npm run build` OK.
- `npm test` OK (`20` tests frontend).
- `vue-tsc --noEmit` OK cuando se validó la parte de clientes.
- `npm run build` OK también tras los últimos cambios en `/mensajes` y tras el cambio de branding del título.

### Backend

- `.\mvnw.cmd test` OK (`26` tests backend).

### Docker

Se reconstruyó y levantó:

- `api`
- `front`

Y en la parte final del día:

- se volvió a reconstruir `front` y `api` con `docker compose up --build -d`,
- se verificó que `localhost:3000` ya servía el `<title>Peluquería Isabella</title>`.

Detalle operativo importante:

- Hubo un conflicto con el puerto `8080` porque existía un `java.exe` local ocupándolo.
- Se detuvo ese proceso local y después arrancó correctamente el contenedor `peluqueria-api`.
- Los logs del backend confirmaron arranque correcto tras los cambios.

## 6. Estado actual razonable del proyecto

Lo que podemos dar por cerrado a día de hoy:

- Chatbot integrado.
- Flujo de ausencias y vacaciones con días bloqueados.
- Configuración admin para días bloqueados.
- Notificaciones al empleado de vacaciones aprobadas/rechazadas.
- Clientes con ficha desplegable inline.
- Género limitado a femenino/masculino en clientes.
- Ajuste visual de selects.
- Mensajes con persistencia mínima de historial + toast + reflejo visual del envío.
- Bandeja de mensajes rehecha como lista de tasks por peluquero, con archivar y eliminar.
- Título global de la app ya unificado como `Peluquería Isabella`.

## 7. Riesgos o puntos a revisar después

No parecen bloqueantes ahora mismo, pero conviene tenerlos en radar:

- La pantalla `/mensajes` mezcla concepto de “chat interno” y “envío email Mailtrap”; en el futuro puede convenir separar visualmente ambos flujos.
- Aunque la UX de `/mensajes` ha mejorado bastante, conceptualmente sigue siendo una bandeja híbrida entre email enviado y mensaje interno persistido.
- El historial persistido actual cubre bien el caso de mensajes enviados desde esa pantalla, pero no sustituye todavía un chat interno completo bidireccional persistente por WebSocket.
- Sería buena idea añadir tests específicos de `MensajesController` para blindar historial y envío.
- Quedan textos sueltos con “Atelier” dentro de copys y comentarios internos; el título global ya está cambiado, pero la limpieza de marca completa aún no está terminada.
- Si queremos una memoria más formal de TFG, esta base se puede convertir luego en un documento por sprints o por hitos funcionales.

## 8. Línea futura: SDD formal del proyecto

Queda decidido como siguiente capa de profesionalización del proyecto:

- Preparar un `Software Design Document` completo del sistema.
- Hacerlo como un SDD normal y serio, como si fuera documentación de diseño del proyecto, con la ventaja de que el sistema ya está hecho.
- No enfocarlo como documentación “a posteriori” improvisada, sino como formalización del diseño real del sistema ya consolidado.

Enfoque acordado:

- Aprovechar que el proyecto existe para redactar un SDD más preciso y coherente que uno hecho al inicio a ciegas.
- Usar el comportamiento y la implementación actual como fuente de verdad para documentar:
  - arquitectura,
  - módulos,
  - reglas de negocio,
  - flujos,
  - validaciones,
  - decisiones de diseño.

Cómo debería hacerse:

1. Crear un índice formal del SDD.
2. Dividir el sistema por módulos funcionales reales.
3. Describir cada bloque como diseño del sistema, no como changelog.
4. Relacionar frontend, backend, roles y reglas de negocio.
5. Añadir decisiones técnicas y límites conocidos.
6. Mantenerlo alineado con la implementación real.

Secciones recomendadas para ese SDD:

- Visión general del sistema.
- Arquitectura general.
- Módulos funcionales.
- Flujos principales de usuario.
- Reglas de negocio.
- Modelo de datos.
- Integraciones externas.
- Seguridad y control de acceso.
- Validaciones.
- Decisiones de diseño y límites.

Objetivo de este trabajo:

- Dar una presentación más profesional al proyecto.
- Explicar mejor el sistema en memoria o defensa.
- Tener una visión clara y ordenada del proyecto completo.

## 9. Siguiente punto de apoyo

Si retomamos más adelante, el mejor punto de reentrada es este:

1. Revisar [mensajes.vue](C:/Users/marco/wa/peluqueria/front-nuxt/app/pages/mensajes.vue) si se quiere seguir afinando la bandeja tipo task.
2. Hacer una pasada de limpieza de branding para sustituir los textos visibles que aún mencionan `Atelier`.
3. Revisar [clientes.vue](C:/Users/marco/wa/peluqueria/front-nuxt/app/pages/admin/clientes.vue) si se quiere seguir puliendo UX.
4. Mantener esta memoria como documento vivo e ir actualizándola cuando cerremos cada bloque nuevo.
