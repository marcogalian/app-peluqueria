<script setup lang="ts">
/**
 * Header superior — diseño Atelier Sapphire.
 *
 * Izquierda: búsqueda global (rounded-full, fondo gris)
 * Derecha: notificaciones (campana con contador WS), ajustes, divisor, nombre usuario + avatar
 */
import { Bell, Settings } from 'lucide-vue-next'

const authStore = useAuthStore()
const route     = useRoute()
const router    = useRouter()

const { noLeidos, conteo, conectar, limpiarConteo } = useNotificacionesChat()

// Conectar al WebSocket al montar el header
onMounted(() => conectar())

// Limpiar conteo al entrar a mensajes
watch(() => route.path, (path) => {
  if (path === '/mensajes') limpiarConteo()
})

// Dropdown de notificaciones
const dropdownAbierto = ref(false)
const campanaRef      = ref<HTMLElement | null>(null)

onClickOutside(campanaRef, () => { dropdownAbierto.value = false })

function toggleDropdown() {
  dropdownAbierto.value = !dropdownAbierto.value
}

function irAMensajes() {
  limpiarConteo()
  dropdownAbierto.value = false
  router.push('/mensajes')
}


const iniciales = computed(() => {
  const u = authStore.usuario?.username ?? ''
  return u.slice(0, 2).toUpperCase()
})

function irAConfiguracion() {
  router.push('/admin/configuracion')
}

function formatHora(timestamp: number): string {
  return new Date(timestamp).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <header class="h-16 flex-shrink-0 flex items-center justify-between px-8">

    <div class="flex-1" />

    <!-- ── Acciones + Usuario ───────────────────────────── -->
    <div class="flex items-center gap-2">

      <!-- Campana de notificaciones -->
      <div ref="campanaRef" class="relative">
        <button
          class="relative p-2 rounded-lg text-on-surface-variant
                 hover:text-primary-container hover:bg-surface-container-low
                 transition-colors"
          @click="toggleDropdown"
        >
          <Bell class="w-5 h-5" />
          <!-- Badge con conteo -->
          <span
            v-if="conteo > 0"
            class="absolute top-1 right-1 min-w-[16px] h-4 px-0.5
                   bg-error text-white text-[9px] font-bold
                   rounded-full flex items-center justify-center leading-none"
          >
            {{ conteo > 9 ? '9+' : conteo }}
          </span>
          <!-- Punto rojo cuando no hay conteo pero sí hay mensajes previos -->
          <span
            v-else
            class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full"
          />
        </button>

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
            class="absolute right-0 top-full mt-2 w-72 bg-white rounded-xl shadow-lg
                   border border-outline-variant/20 z-50 overflow-hidden"
          >
            <!-- Cabecera -->
            <div class="px-4 py-3 border-b border-surface-container flex items-center justify-between">
              <p class="text-sm font-bold text-on-surface">Notificaciones</p>
              <button
                v-if="conteo > 0"
                class="text-[10px] text-primary-container hover:underline"
                @click="limpiarConteo"
              >
                Marcar todo leído
              </button>
            </div>

            <!-- Lista de mensajes no leídos -->
            <div v-if="noLeidos.length > 0" class="max-h-64 overflow-y-auto">
              <button
                v-for="(m, i) in noLeidos"
                :key="i"
                class="w-full flex items-start gap-3 px-4 py-3
                       hover:bg-surface-container-low transition-colors text-left"
                @click="irAMensajes"
              >
                <div class="w-8 h-8 rounded-full bg-primary-container text-white
                            flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                  {{ m.emisor.slice(0, 2).toUpperCase() }}
                </div>
                <div class="min-w-0 flex-1">
                  <p class="text-sm font-bold text-on-surface truncate">{{ m.emisor }}</p>
                  <p class="text-xs text-on-surface-variant">Nuevo mensaje</p>
                </div>
                <p class="text-[10px] text-on-surface-variant flex-shrink-0 mt-1">{{ formatHora(m.timestamp) }}</p>
              </button>
            </div>

            <!-- Sin notificaciones -->
            <div v-else class="px-4 py-8 text-center">
              <Bell class="w-8 h-8 text-on-surface-variant/30 mx-auto mb-2" />
              <p class="text-sm text-on-surface-variant">Sin mensajes nuevos</p>
            </div>

            <!-- Pie -->
            <div class="px-4 py-3 border-t border-surface-container">
              <button
                class="w-full text-center text-xs font-bold text-primary-container
                       hover:underline transition-colors"
                @click="irAMensajes"
              >
                Ver todos los mensajes
              </button>
            </div>
          </div>
        </Transition>
      </div>

      <!-- Ajustes rápidos -->
      <button
        class="p-2 rounded-lg text-on-surface-variant
               hover:text-primary-container hover:bg-surface-container-low
               transition-colors"
        @click="irAConfiguracion"
      >
        <Settings class="w-5 h-5" />
      </button>

      <!-- Divisor vertical -->
      <div class="h-8 w-px bg-outline-variant/30 mx-2" />

      <!-- Nombre + rol + avatar -->
      <div class="flex items-center gap-3">
        <div class="text-right">
          <p class="text-sm font-bold text-on-surface leading-none">
            {{ authStore.usuario?.username ?? 'Usuario' }}
          </p>
          <p class="text-[10px] text-on-surface-variant mt-0.5">
            {{ authStore.isAdmin ? 'Administrador' : 'Empleado' }}
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
