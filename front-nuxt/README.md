# Frontend Nuxt

Frontend de Peluqueria Isabella construido con Nuxt 4, Vue 3, TypeScript, Pinia y Tailwind CSS.

## Stack

- Nuxt 4
- Vue 3
- TypeScript
- Pinia
- Tailwind CSS
- Axios
- FullCalendar
- Chart.js
- Lucide

## Arquitectura

La aplicacion usa vertical slicing por modulo.

Las rutas Nuxt viven en `app/pages`, pero son wrappers finos. La implementacion real de cada pantalla vive en `app/modules`.

Ejemplo:

```text
app/pages/admin/clientes.vue
└── importa app/modules/clientes/pages/ClientesAdminPage.vue
```

## Estructura

```text
app/
├── pages                  # Rutas Nuxt
├── modules                # Slices de negocio
├── components             # Layout y componentes globales
├── middleware             # Auth y roles
├── infrastructure/http    # Cliente API
└── assets/css             # Estilos globales
```

## Modulos

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

## Comandos

Instalar:

```bash
npm install
```

Desarrollo:

```bash
npm run dev
```

Build:

```bash
npm run build
```

Tests:

```bash
npm test
```

## API

El cliente HTTP vive en:

```text
app/infrastructure/http/api.ts
```

En local, la API se consume desde:

```text
http://localhost:8080/api
```

## Criterios UI actuales

- Cards no clicables sin hover.
- Inputs y filtros con estilo coherente.
- Edicion inline en clientes y empleados.
- Modales para crear o editar citas.
- Sidebar plegable.
- Look sobrio: blanco suave, navy y sombras ligeras.
