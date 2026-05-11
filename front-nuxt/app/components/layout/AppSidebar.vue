<script setup lang="ts">
/**
 * Sidebar de navegacion — diseno Atelier Sapphire.
 *
 * Items agrupados por bloques logicos (General, Gestion, Personal, Analisis).
 * Cada item declara que roles lo pueden ver. Los grupos vacios para el rol
 * actual no se renderizan.
 *
 * Layout sin scroll: padding y separaciones ajustadas para que todos los
 * items quepan en el alto de la pantalla. Cabecera muestra version, pie
 * solo el boton de cerrar sesion.
 *
 * Estado colapsable persistido en localStorage via useSidebarCollapsed.
 *  - Expandido (256px): icono + label + titulo de seccion
 *  - Colapsado  (64px): solo iconos, separador fino entre grupos, sin titulos
 */
import {
  LayoutDashboard, Calendar, Users, Scissors,
  Package, UserCog, BarChart3, LogOut,
  MessageCircle, Palmtree, ShoppingBag, Bot,
} from 'lucide-vue-next'
import { useSidebarCollapsed } from '~/modules/shared/composables/useSidebarCollapsed'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const { collapsed } = useSidebarCollapsed()

type Rol = 'admin' | 'empleado'
type ItemMenu = {
  path: string
  label: string
  icon: any
  visiblePara: Rol[]
}
type GrupoMenu = {
  titulo: string
  items: ItemMenu[]
}

const grupos: GrupoMenu[] = [
  {
    titulo: 'General',
    items: [
      { path: '/admin/dashboard', label: 'Panel de control', icon: LayoutDashboard, visiblePara: ['admin'] },
      { path: '/agenda',          label: 'Agenda',           icon: Calendar,        visiblePara: ['admin', 'empleado'] },
      { path: '/mensajes',        label: 'Mensajes',         icon: MessageCircle,   visiblePara: ['admin', 'empleado'] },
    ],
  },
  {
    titulo: 'Gestión',
    items: [
      { path: '/admin/clientes',   label: 'Clientes',   icon: Users,        visiblePara: ['admin'] },
      { path: '/admin/servicios',  label: 'Servicios',  icon: Scissors,     visiblePara: ['admin'] },
      { path: '/admin/inventario', label: 'Inventario', icon: Package,      visiblePara: ['admin'] },
      { path: '/ventas',           label: 'Ventas',     icon: ShoppingBag,  visiblePara: ['admin', 'empleado'] },
    ],
  },
  {
    titulo: 'Personal',
    items: [
      { path: '/admin/empleados', label: 'Empleados',   icon: UserCog,   visiblePara: ['admin'] },
      { path: '/vacaciones',      label: 'Vacaciones',  icon: Palmtree,  visiblePara: ['empleado'] },
    ],
  },
  {
    titulo: 'Análisis',
    items: [
      { path: '/admin/resultados', label: 'Resultados',  icon: BarChart3, visiblePara: ['admin'] },
      { path: '/chat-ia',          label: 'Asistente IA', icon: Bot,      visiblePara: ['admin', 'empleado'] },
    ],
  },
]

const gruposVisibles = computed<GrupoMenu[]>(() => {
  const rol: Rol = authStore.isAdmin ? 'admin' : 'empleado'
  return grupos
    .map((grupo) => ({
      titulo: grupo.titulo,
      items: grupo.items.filter((item) => item.visiblePara.includes(rol)),
    }))
    .filter((grupo) => grupo.items.length > 0)
})

function esActivo(path: string): boolean {
  if (path === '/admin/dashboard') return route.path === path
  return route.path.startsWith(path)
}

function cerrarSesion() {
  authStore.cerrarSesion()
  router.push('/login')
}

// Bloquea el scroll del wheel cuando esta sobre la sidebar.
// Sin esto, el navegador busca el ancestro scrollable y mueve el contenido principal.
function bloquearScroll(evento: WheelEvent) {
  evento.preventDefault()
  evento.stopPropagation()
}
</script>

<template>
  <aside
    :class="[
      'flex-shrink-0 h-screen flex flex-col bg-surface-container-low select-none overflow-hidden',
      'transition-[width] duration-200 ease-out',
      collapsed ? 'w-16' : 'w-sidebar',
    ]"
    @wheel.prevent="bloquearScroll"
  >

    <!-- ── Cabecera: nombre + version (o avatar si colapsado) ─── -->
    <div :class="['pt-5 pb-4', collapsed ? 'px-3 text-center' : 'px-6']">
      <template v-if="!collapsed">
        <h1 class="text-xl font-extrabold tracking-tighter text-primary leading-none">
          Peluquería Isabella
        </h1>
        <p class="text-[10px] uppercase tracking-[0.2em] text-on-surface-variant/60 mt-1">
          {{ authStore.isAdmin ? 'Panel de administración' : 'Portal Empleado' }}
          <span class="ml-1 text-on-surface-variant/40">· v1.0.0</span>
        </p>
      </template>
      <template v-else>
        <div class="w-10 h-10 mx-auto rounded-full bg-primary text-white
                    flex items-center justify-center text-xs font-bold">
          PI
        </div>
      </template>
    </div>

    <!-- ── Navegacion agrupada (sin scroll, espaciados ajustados) ─ -->
    <nav aria-label="Menú principal" class="flex-1 px-2 min-h-0">
      <div
        v-for="(grupo, indice) in gruposVisibles"
        :key="grupo.titulo"
      >
        <!-- Titulo de seccion (expandido) o separador fino (colapsado) -->
        <p
          v-if="!collapsed"
          class="px-3 pt-2.5 pb-1 text-[10px] uppercase tracking-[0.15em] text-on-surface-variant/50 font-bold"
        >
          {{ grupo.titulo }}
        </p>
        <div
          v-else-if="indice > 0"
          class="my-1.5 mx-3 border-t border-outline-variant/20"
          aria-hidden="true"
        />

        <!-- Items del grupo -->
        <NuxtLink
          v-for="item in grupo.items"
          :key="item.path"
          :to="item.path"
          :class="[
            esActivo(item.path) ? 'nav-item-active' : 'nav-item',
            collapsed ? 'justify-center px-0' : '',
          ]"
          :title="collapsed ? item.label : undefined"
          :aria-label="item.label"
          :aria-current="esActivo(item.path) ? 'page' : undefined"
        >
          <component :is="item.icon" class="w-[18px] h-[18px] flex-shrink-0" aria-hidden="true" />
          <span v-if="!collapsed">{{ item.label }}</span>
        </NuxtLink>
      </div>
    </nav>

    <!-- ── Pie: solo cerrar sesion ────────────────────────────── -->
    <div class="px-2 pb-3 pt-2 border-t border-outline-variant/10">
      <button
        :class="[
          'nav-item w-full text-error hover:bg-error-container/30 hover:text-error',
          collapsed ? 'justify-center px-0' : '',
        ]"
        :title="collapsed ? 'Cerrar sesión' : undefined"
        aria-label="Cerrar sesión"
        @click="cerrarSesion"
      >
        <LogOut class="w-[18px] h-[18px] flex-shrink-0" aria-hidden="true" />
        <span v-if="!collapsed">Cerrar sesión</span>
      </button>
    </div>

  </aside>
</template>
