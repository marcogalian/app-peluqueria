<script setup lang="ts">
/**
 * Sidebar de navegación — diseño Atelier Sapphire.
 *
 * Fondo: #F4F3F7 (gris claro)
 * Ítem activo: borde DERECHO con #1A365D + fondo blanco semitransparente
 * Logo: "Atelier Sapphire" + "Management Suite" en la parte superior
 * Pie: iniciales + nombre de app + versión
 *
 * Menú ADMIN: Panel de control, Agenda, Clientes, Servicios, Inventario, Empleados, Resultados, Configuración
 * Menú EMPLEADO: Agenda, Mensajes, Vacaciones
 * Los empleados NO ven: Clientes, Inventario, Empleados, Resultados
 */
import {
  LayoutDashboard, Calendar, Users, Scissors,
  Package, UserCog, BarChart3, Settings, LogOut,
  MessageCircle, Palmtree,
} from 'lucide-vue-next'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

// Estructura de items según rol
const itemsAdmin = [
  { path: '/admin/dashboard',  label: 'Panel de control', icon: LayoutDashboard },
  { path: '/agenda',           label: 'Agenda',           icon: Calendar },
  { path: '/admin/clientes',   label: 'Clientes',         icon: Users },
  { path: '/admin/servicios',  label: 'Servicios',        icon: Scissors },
  { path: '/admin/inventario', label: 'Inventario',       icon: Package },
  { path: '/admin/empleados',  label: 'Empleados',        icon: UserCog },
  { path: '/admin/resultados', label: 'Resultados',       icon: BarChart3 },
]

const itemsEmpleado = [
  { path: '/agenda',     label: 'Agenda',     icon: Calendar },
  { path: '/mensajes',   label: 'Mensajes',   icon: MessageCircle },
  { path: '/vacaciones', label: 'Vacaciones', icon: Palmtree },
]

const items = computed(() => authStore.isAdmin ? itemsAdmin : itemsEmpleado)

function esActivo(path: string): boolean {
  if (path === '/admin/dashboard') return route.path === path
  return route.path.startsWith(path)
}

function cerrarSesion() {
  authStore.cerrarSesion()
  router.push('/login')
}
</script>

<template>
  <!--
    Sidebar fijo.
    Fondo: bg-surface-container-low = #F4F3F7 (gris muy claro del diseño)
    w-sidebar = 256px (w-64 del stitch original)
  -->
  <aside class="w-sidebar flex-shrink-0 h-screen flex flex-col bg-surface-container-low select-none">

    <!-- ── Logo ─────────────────────────────────────────── -->
    <div class="px-6 pt-8 pb-6">
      <h1 class="text-xl font-extrabold tracking-tighter text-primary leading-none">
        Atelier Sapphire
      </h1>
      <p class="text-[10px] uppercase tracking-[0.2em] text-on-surface-variant/60 mt-0.5">
        {{ authStore.isAdmin ? 'Management Suite' : 'Portal Empleado' }}
      </p>
    </div>

    <!-- ── Navegación ─────────────────────────────────────── -->
    <nav class="flex-1 overflow-y-auto px-2 space-y-0.5">
      <NuxtLink
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        :class="esActivo(item.path) ? 'nav-item-active' : 'nav-item'"
      >
        <component :is="item.icon" class="w-[18px] h-[18px] flex-shrink-0" />
        <span>{{ item.label }}</span>
      </NuxtLink>

      <!-- Separador + Configuración (solo admin) -->
      <template v-if="authStore.isAdmin">
        <div class="my-4 mx-2 border-t border-outline-variant/20" />
        <NuxtLink
          to="/admin/configuracion"
          :class="esActivo('/admin/configuracion') ? 'nav-item-active' : 'nav-item'"
        >
          <Settings class="w-[18px] h-[18px] flex-shrink-0" />
          <span>Configuración</span>
        </NuxtLink>
      </template>
    </nav>

    <!-- ── Pie: info de la app + cerrar sesión ────────────── -->
    <div class="px-4 pb-6 pt-4 border-t border-outline-variant/10 space-y-2">
      <!-- Info de versión (igual que en el stitch) -->
      <div class="flex items-center gap-3 px-2">
        <div class="w-8 h-8 rounded-full bg-primary-container flex items-center justify-center text-white text-xs font-bold flex-shrink-0">
          AS
        </div>
        <div>
          <p class="text-xs font-bold text-primary leading-none">Atelier Sapphire</p>
          <p class="text-[10px] text-on-surface-variant">v1.0.0</p>
        </div>
      </div>

      <!-- Botón cerrar sesión -->
      <button
        class="nav-item w-full text-error hover:bg-error-container/30 hover:text-error"
        @click="cerrarSesion"
      >
        <LogOut class="w-[18px] h-[18px]" />
        <span>Cerrar sesión</span>
      </button>
    </div>

  </aside>
</template>
