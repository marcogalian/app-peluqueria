<script setup lang="ts">
/**
 * Header superior — diseño Atelier Sapphire.
 *
 * Izquierda: búsqueda global (rounded-full, fondo gris)
 * Derecha: notificaciones, ajustes, divisor, nombre usuario + avatar
 */
import { Bell, Settings, PanelLeftClose, PanelLeftOpen, Menu } from 'lucide-vue-next'
import { useSidebarCollapsed } from '~/modules/shared/composables/useSidebarCollapsed'

const authStore = useAuthStore()
const route     = useRoute()
const router    = useRouter()

const { collapsed, toggle: toggleSidebar, openMobile } = useSidebarCollapsed()

const conteo = ref(0)
let pollingNotificaciones: ReturnType<typeof setInterval> | null = null

async function consultarNoLeidos() {
  if (!import.meta.client || !authStore.usuario) return
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/mensajes/no-leidos')
    conteo.value = Number(data?.total ?? 0)
    if (route.path === '/mensajes' && conteo.value > 0) {
      await marcarMensajesComoLeidos()
    }
  } catch {
    // La campana no debe romper la navegacion si el backend no responde.
  }
}

async function marcarMensajesComoLeidos() {
  if (!import.meta.client || !authStore.usuario) return
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch('/mensajes/no-leidos/marcar-leidos')
  } catch {
    // Si falla, se reintentara en el siguiente polling.
  } finally {
    limpiarConteo()
  }
}

function activarPollingNotificaciones() {
  consultarNoLeidos()
  pollingNotificaciones = setInterval(consultarNoLeidos, 60_000)
}

function onVentanaEnfocada() {
  consultarNoLeidos()
}

onMounted(() => {
  activarPollingNotificaciones()
  if (route.path === '/mensajes') marcarMensajesComoLeidos()
  window.addEventListener('focus', onVentanaEnfocada)
})

onUnmounted(() => {
  if (pollingNotificaciones) clearInterval(pollingNotificaciones)
  window.removeEventListener('focus', onVentanaEnfocada)
})

// Limpiar conteo al entrar a mensajes
watch(() => route.path, (path) => {
  if (path === '/mensajes') marcarMensajesComoLeidos()
})

// Dropdown de notificaciones
const dropdownAbierto = ref(false)
const campanaRef      = ref<HTMLElement | null>(null)
const dropdownRef     = ref<HTMLElement | null>(null)

onClickOutside(dropdownRef, (event) => {
  const target = event.target as Node | null
  if (target && campanaRef.value?.contains(target)) return
  dropdownAbierto.value = false
})

function toggleDropdown() {
  dropdownAbierto.value = !dropdownAbierto.value
}

function irAMensajes() {
  limpiarConteo()
  dropdownAbierto.value = false
  router.push('/mensajes')
}

function limpiarConteo() {
  conteo.value = 0
}

const iniciales = computed(() => {
  const username = authStore.usuario?.username?.trim()
  if (username) return username.slice(0, 2).toUpperCase()
  return authStore.isAdmin ? 'AD' : 'EM'
})

function irAConfiguracion() {
  router.push('/admin/configuracion')
}

</script>

<template>
  <header class="h-16 flex-shrink-0 flex items-center justify-between px-4 sm:px-6 lg:px-8">

    <!-- Boton para colapsar / expandir el sidebar -->
    <button
      class="inline-flex p-2 rounded-lg text-on-surface-variant lg:hidden
             hover:text-primary-container hover:bg-surface-container-low
             transition-colors"
      aria-label="Abrir menú lateral"
      @click="openMobile"
    >
      <Menu class="w-5 h-5" aria-hidden="true" />
    </button>

    <button
      class="hidden p-2 rounded-lg text-on-surface-variant lg:inline-flex
             hover:text-primary-container hover:bg-surface-container-low
             transition-colors"
      :aria-label="collapsed ? 'Expandir menú lateral' : 'Colapsar menú lateral'"
      :aria-pressed="collapsed"
      @click="toggleSidebar"
    >
      <PanelLeftOpen v-if="collapsed" class="w-5 h-5" aria-hidden="true" />
      <PanelLeftClose v-else class="w-5 h-5" aria-hidden="true" />
    </button>

    <div class="flex-1" />

    <!-- ── Acciones + Usuario ───────────────────────────── -->
    <div class="flex items-center gap-2">

      <!-- Campana de notificaciones -->
      <div ref="campanaRef" class="relative">
        <button
          class="relative p-2 rounded-lg text-on-surface-variant
                 hover:text-primary-container hover:bg-surface-container-low
                 transition-colors"
          :aria-label="conteo > 0 ? `Notificaciones, ${conteo} sin leer` : 'Notificaciones'"
          :aria-expanded="dropdownAbierto"
          aria-haspopup="menu"
          @click="toggleDropdown"
        >
          <Bell class="w-5 h-5" aria-hidden="true" />
          <span
            v-if="conteo > 0"
            class="absolute top-1 right-1 min-w-[16px] h-4 px-0.5
                   bg-error text-white text-[9px] font-bold
                   rounded-full flex items-center justify-center leading-none"
            aria-hidden="true"
          >
            {{ conteo > 9 ? '9+' : conteo }}
          </span>
        </button>

        <Teleport to="body">
          <!-- Dropdown de notificaciones -->
          <Transition
            enter-active-class="transition ease-out duration-150"
            enter-from-class="opacity-0 scale-95 -translate-y-1"
            enter-to-class="opacity-100 scale-100 translate-y-0"
            leave-active-class="transition ease-in duration-100"
            leave-from-class="opacity-100 scale-100 translate-y-0"
            leave-to-class="opacity-0 scale-95 -translate-y-1"
          >
            <div
              v-show="dropdownAbierto"
              ref="dropdownRef"
              role="menu"
              aria-label="Notificaciones"
              class="fixed left-1/2 top-16 z-50 w-[calc(100vw-2rem)] max-w-[360px] -translate-x-1/2
                     overflow-hidden rounded-xl border border-outline-variant/20 bg-white shadow-lg
                     sm:left-auto sm:right-6 sm:w-72 sm:translate-x-0 lg:right-8"
            >
              <!-- Cabecera -->
              <div class="px-4 py-3 border-b border-surface-container flex items-center justify-between">
                <p class="text-sm font-bold text-on-surface">Notificaciones</p>
                <button
                  v-if="conteo > 0"
                  class="text-[10px] text-primary-container hover:underline"
                  role="menuitem"
                  @click="marcarMensajesComoLeidos"
                >
                  Marcar todo leído
                </button>
              </div>

              <!-- Resumen de mensajes no leídos -->
              <div v-if="conteo > 0" class="max-h-64 overflow-y-auto">
                <button
                  class="w-full flex items-start gap-3 px-4 py-3
                         hover:bg-surface-container-low transition-colors text-left"
                  role="menuitem"
                  :aria-label="`${conteo} mensajes internos sin leer`"
                  @click="irAMensajes"
                >
                  <div class="w-8 h-8 rounded-full bg-primary-container text-white
                              flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5"
                       aria-hidden="true">
                    {{ conteo > 9 ? '9+' : conteo }}
                  </div>
                  <div class="min-w-0 flex-1">
                    <p class="text-sm font-bold text-on-surface truncate">Mensajes internos pendientes</p>
                    <p class="text-xs text-on-surface-variant">
                      {{ conteo === 1 ? 'Tienes 1 mensaje sin leer' : `Tienes ${conteo} mensajes sin leer` }}
                    </p>
                  </div>
                </button>
              </div>

              <!-- Sin notificaciones -->
              <div v-else class="px-4 py-8 text-center">
                <Bell class="w-8 h-8 text-on-surface-variant/30 mx-auto mb-2" aria-hidden="true" />
                <p class="text-sm text-on-surface-variant">Sin mensajes nuevos</p>
              </div>

              <!-- Pie -->
              <div class="px-4 py-3 border-t border-surface-container">
                <button
                  class="w-full text-center text-xs font-bold text-primary-container
                         hover:underline transition-colors"
                  role="menuitem"
                  @click="irAMensajes"
                >
                  Ver todos los mensajes
                </button>
              </div>
            </div>
          </Transition>
        </Teleport>
      </div>

      <!-- Ajustes (solo admin) -->
      <button
        v-if="authStore.isAdmin"
        class="p-2 rounded-lg text-on-surface-variant
               hover:text-primary-container hover:bg-surface-container-low
               transition-colors"
        aria-label="Configuración"
        @click="irAConfiguracion"
      >
        <Settings class="w-5 h-5" aria-hidden="true" />
      </button>

      <!-- Divisor vertical -->
      <div class="hidden h-8 w-px bg-outline-variant/30 mx-2 sm:block" />

      <!-- Nombre + rol + avatar -->
      <div class="flex items-center gap-3">
        <div class="hidden text-right sm:block">
          <p class="text-sm font-bold text-on-surface leading-none">
            {{ authStore.usuario?.username ?? 'Usuario' }}
          </p>
          <p class="text-[10px] text-on-surface-variant mt-0.5">
            {{ authStore.isAdmin ? 'Administrador' : 'Peluquero/a' }}
          </p>
        </div>
        <div class="w-9 h-9 rounded-full bg-primary-container flex items-center justify-center
                    text-white text-xs font-bold border-2 border-primary-fixed shadow-sm flex-shrink-0">
          {{ iniciales }}
        </div>
      </div>

    </div>
  </header>
</template>
