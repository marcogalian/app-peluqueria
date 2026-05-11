# Guia funcional

Esta guia explica la aplicacion desde el punto de vista de usuario: que se puede hacer, que rol lo usa y como encajan las secciones.

## Roles

### Administrador

El administrador gestiona toda la peluqueria:

- Clientes.
- Citas.
- Empleados.
- Servicios.
- Inventario.
- Ventas.
- Resultados.
- Mensajes.
- Configuracion.
- Ausencias.
- Asistente de gestion.

### Empleado

El empleado tiene acceso operativo:

- Ver su agenda.
- Gestionar ventas.
- Enviar y recibir mensajes.
- Solicitar vacaciones o bajas.
- Consultar el asistente con permisos limitados.

## Login

La aplicacion empieza en `/login`.

Credenciales demo:

| Usuario | Contrasena | Rol |
|---|---|---|
| `admin` | `1234` | Administrador |
| `sofia` | `1234` | Empleada |
| `carmen` | `1234` | Empleada |
| `lucia` | `1234` | Empleada |

Tras el login:

- Admin entra en `/admin/dashboard`.
- Empleado entra en `/agenda`.

## Panel de control

El panel de control muestra el estado general del salon:

- Resumen de citas.
- Ventas e ingresos.
- Alertas de inventario.
- Productos con bajo stock.
- Graficas de evolucion.
- Datos operativos para revisar rapidamente el negocio.

La idea es que sea un panel de supervision, no una pagina de entrada de datos.

## Agenda

La agenda permite:

- Ver citas por dia.
- Crear una cita.
- Editar una cita desde modal.
- Cancelar o completar una cita.
- Ver hora, cliente y servicio de forma rapida en cada bloque.

El objetivo UX es que modificar una cita sea rapido y visual, sin navegar a otra pantalla.

## Clientes

La seccion de clientes permite:

- Buscar clientes.
- Filtrar por tipo.
- Ver tarjetas de cliente.
- Desplegar cada cliente hacia abajo para editar sus datos.
- Marcar cliente VIP.
- Guardar telefono, email, genero, notas e historial.
- Gestionar consentimiento de fotos.

El patron elegido es desplegable inline para evitar paneles laterales y mantener el contexto visual.

## Servicios

Permite gestionar el catalogo del salon:

- Nombre.
- Categoria.
- Genero.
- Precio.
- Duracion.
- Descripcion.

Los servicios son usados al crear citas y al calcular importes.

## Inventario

Permite controlar productos:

- Buscar producto.
- Filtrar por categoria.
- Filtrar por todos, bajo stock o sin stock.
- Crear producto.
- Actualizar stock.
- Revisar precios y stock minimo.

Los productos con bajo stock se usan tambien en el panel de control y en el asistente.

## Ventas

Permite vender productos:

- Buscar productos.
- Filtrar por categoria.
- Anadir al carrito.
- Confirmar venta.
- Descontar stock.
- Registrar metodo de pago.

Las ventas alimentan las estadisticas de resultados.

## Empleados

Permite:

- Ver empleados.
- Editar datos desde desplegable inline.
- Gestionar disponibilidad.
- Registrar especialidad.
- Ver estado de vacaciones o baja.

Se evita el panel lateral para mantener una experiencia parecida a clientes.

## Ausencias y vacaciones

El empleado puede solicitar:

- Vacaciones.
- Bajas.
- Otros tipos de ausencia.

El administrador puede aprobar o rechazar. Cuando se aprueba o rechaza:

- La solicitud queda marcada como no vista para el empleado.
- El empleado ve una card de notificacion al entrar.
- Al cerrar la card se marca como vista.

Tambien existen dias bloqueados para impedir solicitudes de vacaciones en fechas configuradas por administracion.

## Configuracion

Permite ajustar datos generales del negocio:

- Datos de la peluqueria.
- Horarios.
- Dias especiales.
- Dias bloqueados para vacaciones.

## Mensajes

Permite enviar mensajes internos a empleados:

- Seleccionar contacto.
- Redactar mensaje.
- Guardarlo como task/mensaje bajo la card de envio.
- Archivar mensajes.
- Eliminar mensajes.
- Filtrar mensajes por peluquero seleccionado.

## Resultados

Muestra informacion de negocio:

- Ingresos.
- Gastos.
- Beneficio.
- Citas completadas.
- Graficas.
- Distribuciones y datos relevantes para entender el rendimiento.

## Asistente de gestion

Chat interno para consultar datos del negocio.

Puede responder, entre otras cosas:

- Total de clientes.
- Clientes VIP.
- Servicios disponibles.
- Inventario.
- Productos con bajo stock.
- Citas o vacaciones del empleado.

Las consultas basicas de clientes se resuelven directamente desde la base de datos para no depender de Gemini. Las consultas abiertas siguen usando la API de Gemini si la clave y la cuota estan disponibles.
