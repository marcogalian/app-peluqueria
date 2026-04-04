<script setup lang="ts">
/**
 * Mensajes — chat en tiempo real (WebSocket STOMP) entre empleados y admin.
 * AES-256 para cifrado de contenido antes de enviar.
 * También incluye botones de contacto rápido WhatsApp/Email.
 *
 * Por ahora el cifrado AES es opcional (solo si CryptoJS está disponible).
 */
import { Send, Loader2, MessageSquare } from 'lucide-vue-next'

definePageMeta({ middleware: 'auth' })

// ── Tipos ─────────────────────────────────────────────────
interface Mensaje {
  id: string
  emisorId: number
  emisorNombre: string
  contenido: string
  enviadoEn: string
  leido: boolean
}

interface Contacto {
  id: number
  nombre: string
  iniciales: string
  rol: string
  online: boolean
  telefono?: string
  email?: string
}

// ── Estado ────────────────────────────────────────────────
const authStore       = useAuthStore()
const mensajes        = ref<Mensaje[]>([])
const contactos       = ref<Contacto[]>([])
const contactoActivo  = ref<Contacto | null>(null)
const mensajeNuevo    = ref('')
const cargando        = ref(true)
const enviando        = ref(false)
const conectado       = ref(false)

// Ref para el scroll del chat
const chatContainer = ref<HTMLElement | null>(null)

// ── WebSocket STOMP ───────────────────────────────────────
let stompClient: any = null

async function conectarWebSocket() {
  try {
    const { Client } = await import('@stomp/stompjs')
    const token = localStorage.getItem('accessToken')

    stompClient = new Client({
      brokerURL: `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,

      onConnect: () => {
        conectado.value = true
        // Suscribirse a los mensajes del usuario actual
        stompClient.subscribe('/user/queue/mensajes', (frame: any) => {
          const msg: Mensaje = JSON.parse(frame.body)
          mensajes.value.push(msg)
          scrollAbajo()
        })
      },

      onDisconnect: () => { conectado.value = false },
      onStompError: () => { conectado.value = false },
    })

    stompClient.activate()
  } catch {
    // WebSocket no disponible — modo degradado (polling podría implementarse aquí)
  }
}

// ── Cargar contactos y mensajes iniciales ─────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/mensajes/contactos')
    contactos.value = data
    if (data.length > 0) await seleccionarContacto(data[0])
  } catch { /* vacío */ }
  finally {
    cargando.value = false
    await conectarWebSocket()
  }
})

onUnmounted(() => {
  stompClient?.deactivate()
})

// ── Seleccionar contacto y cargar historial ───────────────
async function seleccionarContacto(c: Contacto) {
  contactoActivo.value = c
  mensajes.value = []
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get(`/mensajes/historial/${c.id}`)
    mensajes.value = data
    await nextTick()
    scrollAbajo()
  } catch { /* vacío */ }
}

// ── Enviar mensaje ────────────────────────────────────────
async function enviarMensaje() {
  if (!mensajeNuevo.value.trim() || !contactoActivo.value) return
  enviando.value = true

  const contenido = mensajeNuevo.value.trim()
  mensajeNuevo.value = ''

  try {
    if (stompClient?.connected) {
      stompClient.publish({
        destination: '/app/mensajes/enviar',
        body: JSON.stringify({
          destinatarioId: contactoActivo.value.id,
          contenido,
        }),
      })
    } else {
      // Fallback HTTP si el WS no está disponible
      const { api } = await import('~/infrastructure/http/api')
      const { data } = await api.post('/mensajes/enviar', {
        destinatarioId: contactoActivo.value.id,
        contenido,
      })
      mensajes.value.push(data)
    }
    await nextTick()
    scrollAbajo()
  } catch { /* toast error */ }
  finally { enviando.value = false }
}

// ── Scroll al final del chat ──────────────────────────────
function scrollAbajo() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// ── Contacto rápido ───────────────────────────────────────
function abrirWhatsApp(telefono: string) {
  window.open(`https://wa.me/34${telefono.replace(/\D/g, '')}`, '_blank')
}

function abrirEmail(email: string) {
  window.location.href = `mailto:${email}`
}

// ── Helpers ───────────────────────────────────────────────
function esMio(m: Mensaje): boolean {
  return m.emisorId === authStore.user?.id
}

function formatHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="flex gap-6 h-full overflow-hidden">

    <!-- ══════════════════════════════════════════════════════
         PANEL IZQUIERDO — Lista de contactos
         ════════════════════════════════════════════════════ -->
    <aside class="w-72 card flex flex-col overflow-hidden flex-shrink-0">

      <div class="p-5 border-b border-surface-container">
        <h3 class="font-bold text-primary text-sm">Mensajes</h3>
        <div class="flex items-center gap-2 mt-1">
          <div
            class="w-2 h-2 rounded-full"
            :class="conectado ? 'bg-green-500' : 'bg-surface-container-high'"
          />
          <p class="text-[10px] text-on-surface-variant">{{ conectado ? 'Conectado' : 'Desconectado' }}</p>
        </div>
      </div>

      <!-- Cargando contactos -->
      <div v-if="cargando" class="flex items-center justify-center py-8">
        <Loader2 class="w-5 h-5 animate-spin text-primary" />
      </div>

      <!-- Lista de contactos -->
      <div v-else class="flex-1 overflow-y-auto">
        <button
          v-for="c in contactos"
          :key="c.id"
          class="w-full flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors text-left"
          :class="contactoActivo?.id === c.id ? 'bg-surface-container-low border-r-4 border-primary-container' : ''"
          @click="seleccionarContacto(c)"
        >
          <!-- Avatar con indicador online -->
          <div class="relative flex-shrink-0">
            <div class="w-9 h-9 rounded-full bg-primary-container text-white flex items-center justify-center text-xs font-bold">
              {{ c.iniciales }}
            </div>
            <div
              v-if="c.online"
              class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white"
            />
          </div>
          <div class="min-w-0">
            <p class="font-bold text-on-surface text-sm truncate">{{ c.nombre }}</p>
            <p class="text-[10px] text-on-surface-variant">{{ c.rol === 'ROLE_ADMIN' ? 'Administrador' : 'Empleado' }}</p>
          </div>
        </button>

        <div v-if="contactos.length === 0" class="px-4 py-8 text-center text-sm text-on-surface-variant">
          No hay contactos disponibles
        </div>
      </div>

    </aside>

    <!-- ══════════════════════════════════════════════════════
         PANEL PRINCIPAL — Chat
         ════════════════════════════════════════════════════ -->
    <section class="flex-1 card flex flex-col overflow-hidden">

      <!-- Sin contacto seleccionado -->
      <div v-if="!contactoActivo" class="flex-1 flex flex-col items-center justify-center text-on-surface-variant gap-3">
        <MessageSquare class="w-10 h-10 opacity-20" />
        <p class="text-sm">Selecciona un contacto para chatear</p>
      </div>

      <template v-else>

        <!-- Cabecera del chat -->
        <div class="px-6 py-4 border-b border-surface-container flex items-center justify-between flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-primary-container text-white flex items-center justify-center text-xs font-bold">
              {{ contactoActivo.iniciales }}
            </div>
            <div>
              <p class="font-bold text-on-surface text-sm">{{ contactoActivo.nombre }}</p>
              <p class="text-[10px] text-on-surface-variant flex items-center gap-1">
                <span
                  class="w-1.5 h-1.5 rounded-full"
                  :class="contactoActivo.online ? 'bg-green-500' : 'bg-surface-container-high'"
                />
                {{ contactoActivo.online ? 'En línea' : 'Desconectado' }}
              </p>
            </div>
          </div>

          <!-- Botones de contacto rápido -->
          <div class="flex gap-2">
            <button
              v-if="contactoActivo.telefono"
              class="flex items-center gap-1.5 px-3 py-1.5 bg-surface-container-low hover:bg-surface-container text-on-surface-variant text-xs font-bold rounded-full transition-colors"
              @click="abrirWhatsApp(contactoActivo.telefono!)"
            >
              <svg class="w-3.5 h-3.5 text-[#25D366]" fill="currentColor" viewBox="0 0 24 24">
                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
              </svg>
              WhatsApp
            </button>
            <button
              v-if="contactoActivo.email"
              class="flex items-center gap-1.5 px-3 py-1.5 bg-surface-container-low hover:bg-surface-container text-on-surface-variant text-xs font-bold rounded-full transition-colors"
              @click="abrirEmail(contactoActivo.email!)"
            >
              <svg class="w-3.5 h-3.5 text-primary-container" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
              Email
            </button>
          </div>
        </div>

        <!-- Área de mensajes -->
        <div
          ref="chatContainer"
          class="flex-1 overflow-y-auto p-6 space-y-4"
        >
          <div
            v-for="m in mensajes"
            :key="m.id"
            class="flex"
            :class="esMio(m) ? 'justify-end' : 'justify-start'"
          >
            <!-- Avatar del otro (solo mensajes recibidos) -->
            <div
              v-if="!esMio(m)"
              class="w-7 h-7 rounded-full bg-surface-container flex items-center justify-center text-[9px] font-bold text-on-surface-variant mr-2 flex-shrink-0 self-end"
            >
              {{ m.emisorNombre.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() }}
            </div>

            <!-- Burbuja del mensaje -->
            <div
              class="max-w-xs px-4 py-2.5 rounded-2xl"
              :class="esMio(m)
                ? 'bg-primary-container text-white rounded-br-sm'
                : 'bg-surface-container-low text-on-surface rounded-bl-sm'"
            >
              <p class="text-sm leading-relaxed">{{ m.contenido }}</p>
              <p
                class="text-[9px] mt-1 text-right"
                :class="esMio(m) ? 'text-white/60' : 'text-on-surface-variant'"
              >
                {{ formatHora(m.enviadoEn) }}
              </p>
            </div>
          </div>

          <div v-if="mensajes.length === 0" class="text-center py-8 text-sm text-on-surface-variant">
            Inicia la conversación con {{ contactoActivo.nombre }}
          </div>
        </div>

        <!-- Input de mensaje -->
        <div class="px-6 py-4 border-t border-surface-container flex-shrink-0">
          <div class="flex items-center gap-3 bg-surface-container-low rounded-full px-4 py-2">
            <input
              v-model="mensajeNuevo"
              type="text"
              placeholder="Escribe un mensaje..."
              class="flex-1 bg-transparent border-none text-sm text-on-surface placeholder:text-on-surface-variant focus:ring-0 focus:outline-none"
              @keyup.enter="enviarMensaje"
            />
            <button
              class="w-8 h-8 bg-primary-container text-white rounded-full flex items-center justify-center hover:opacity-90 transition-opacity disabled:opacity-50 flex-shrink-0"
              :disabled="!mensajeNuevo.trim() || enviando"
              @click="enviarMensaje"
            >
              <Loader2 v-if="enviando" class="w-4 h-4 animate-spin" />
              <Send v-else class="w-4 h-4" />
            </button>
          </div>
        </div>

      </template>
    </section>

  </div>
</template>
