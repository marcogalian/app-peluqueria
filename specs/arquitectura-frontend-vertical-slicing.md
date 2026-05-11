# Arquitectura frontend

El frontend usa Nuxt/Vue con Vertical Slicing y Screaming Architecture adaptadas a una aplicacion de interfaz.

No copia la hexagonal del backend porque en frontend seria artificial. Aqui la arquitectura grita negocio separando rutas, pantallas, componentes, composables, servicios y tipos.

## Regla principal

Las rutas Nuxt viven en `app/pages`, pero deben ser finas. La pantalla real vive en `app/modules/<modulo>/pages`.

```text
app/pages/admin/clientes.vue
  -> importa modules/clientes/pages/ClientesAdminPage.vue
```

Asi se conserva el routing de Nuxt sin llenar `pages` de logica.

## Como leer un modulo

```text
modules/clientes/
  pages/        Pantallas completas del modulo
  components/   Componentes propios de clientes
  composables/  Estado y logica reutilizable
  services/     Llamadas HTTP del modulo
  types/        Tipos TypeScript del modulo
```

No todos los modulos necesitan todas las carpetas desde el primer dia. Se crean cuando aportan claridad.

## Estructura actual

```text
modules/
  agenda/
    pages/AgendaPage.vue
    services/
    types/

  auth/
    pages/LoginPage.vue
    composables/
    services/
    store/
    types/

  calendario/
    pages/CalendarioPage.vue
    pages/CalendarioLaboralAdminPage.vue
    services/
    types/

  clientes/
    pages/ClientesAdminPage.vue
    types/

  empleados/
    pages/EmpleadosAdminPage.vue

  inventario/
    pages/InventarioAdminPage.vue

  ventas/
    pages/VentasPage.vue

  servicios/
    pages/ServiciosAdminPage.vue

  resultados/
    pages/ResultadosAdminPage.vue

  configuracion/
    pages/ConfiguracionAdminPage.vue

  dashboard/
    pages/PanelControlAdminPage.vue

  mensajes/
    pages/MensajesPage.vue

  ausencias/
    pages/VacacionesPage.vue

  chatbot/
    pages/AsistenteGestionPage.vue
    composables/
```

## Carpetas compartidas

```text
components/
  layout/       Layout global: sidebar, header
  AppSelect.vue

infrastructure/
  http/api.ts   Cliente HTTP comun

shared/
  types/

modules/shared/
  components/   Toast, piezas reutilizables
  composables/  useToast, useSidebarCollapsed
```

## Regla mental

- Si quieres tocar una ruta, mira primero `app/pages`.
- Si quieres entender una pantalla, salta al modulo que importa esa ruta.
- Si quieres tocar negocio visual, entra en `modules/<modulo>`.
- Si quieres tocar transporte HTTP, entra en `infrastructure/http`.
- Si algo se usa en muchas pantallas, vive en `components`, `shared` o `modules/shared`.

## Siguiente mejora natural

Las pantallas ya estan colocadas por slices. El siguiente paso no es mover mas carpetas, sino partir pantallas grandes en piezas del mismo modulo:

```text
modules/clientes/components/ClienteCard.vue
modules/clientes/components/ClienteDetalleDesplegable.vue
modules/clientes/composables/useClientes.ts
modules/clientes/services/clientesApi.ts
```

Ese paso se puede hacer modulo por modulo sin romper rutas.
