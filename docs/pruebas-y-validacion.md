# Pruebas y validacion

## Tests automaticos

### Backend

```bash
cd backend-spring
./mvnw test
```

En Windows PowerShell:

```powershell
cd backend-spring
.\mvnw.cmd test
```

Los tests usan H2 en memoria.

Cobertura actual destacada:

- Autenticacion.
- Utilidad AES del chat.
- Agenda.
- Asistente de gestion.
- Funciones directas del chatbot.
- Carga basica del contexto Spring.

### Frontend

```bash
cd front-nuxt
npm test
```

Build de produccion:

```bash
cd front-nuxt
npm run build
```

## Checklist manual recomendado

Esta lista sirve para una revision completa antes de entregar o presentar.

### Login y roles

- Entrar como `admin`.
- Confirmar redireccion a panel de control.
- Cerrar sesion.
- Entrar como `sofia`.
- Confirmar que no accede a rutas admin.

### Panel de control

- Revisar que las cards no clicables no tengan hover.
- Revisar KPIs.
- Revisar graficas.
- Revisar alertas de inventario.
- Probar scroll y sidebar.

### Agenda

- Crear cita.
- Editar cita desde modal.
- Cancelar cita.
- Completar cita.
- Confirmar que el bloque de cita muestra hora, cliente y servicio.
- Comprobar validacion de solapes.

### Clientes

- Buscar cliente.
- Abrir desplegable inline.
- Editar telefono o email.
- Marcar/desmarcar VIP.
- Confirmar que solo existen generos masculino y femenino.
- Revisar que los inputs tienen estilo coherente.

### Servicios

- Crear servicio.
- Editar servicio.
- Filtrar o buscar si aplica.
- Revisar que cards no clicables no tengan hover.

### Inventario

- Buscar producto.
- Filtrar por categoria.
- Filtrar bajo stock.
- Filtrar sin stock.
- Crear producto.
- Actualizar stock.
- Confirmar alertas de stock bajo.

### Ventas

- Buscar producto.
- Anadir productos al carrito.
- Confirmar venta.
- Confirmar descuento de stock.
- Revisar impacto en resultados.

### Empleados

- Abrir desplegable de empleado.
- Editar datos.
- Cambiar disponibilidad.
- Revisar estado de vacaciones o baja.

### Ausencias

- Como empleado, solicitar vacaciones.
- Como admin, aprobar.
- Volver como empleado y confirmar card de notificacion.
- Cerrar card y confirmar que no vuelve a aparecer.
- Crear dia bloqueado.
- Intentar solicitar vacaciones sobre dia bloqueado.

### Mensajes

- Seleccionar peluquero.
- Enviar mensaje.
- Confirmar toast.
- Confirmar que aparece como task bajo la card.
- Archivar mensaje.
- Eliminar mensaje.
- Cambiar peluquero y confirmar filtrado.

### Resultados

- Revisar KPIs.
- Revisar graficas.
- Confirmar datos tras ventas.

### Asistente de gestion

- Preguntar: `cuantos clientes tenemos en total`.
- Preguntar: `quienes son los clientes vip`.
- Preguntar: `que productos tienen bajo stock`.
- Probar pregunta abierta si Gemini tiene clave y cuota.

## Validacion antes de subir

Recomendado antes de cada push:

```bash
cd backend-spring
./mvnw test
```

```bash
cd front-nuxt
npm run build
```

```bash
git status --short --branch
```

## Warnings conocidos

Pueden aparecer warnings no bloqueantes:

- MapStruct: propiedad no mapeada `peluqueroNombre` en ausencias.
- RestTemplateBuilder: metodos de timeout marcados como deprecated.
- Mockito: aviso sobre agente dinamico en Java futuro.
- Nuxt/Vite: sourcemap warning de module preload.

Ninguno impide ejecutar la aplicacion actualmente.
