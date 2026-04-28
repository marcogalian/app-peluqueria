<script setup lang="ts">
/**
 * Mensajes y correo — chat interno entre empleados/admin y compositor de email.
 * El envío manual de correo usa Mailtrap desde backend.
 */
import { Send, Loader2, MessageSquare, Mail, CheckCircle2 } from 'lucide-vue-next'

definePageMeta({ middleware: 'auth' })

interface Mensaje {
  id: string
  emisorId: string
  emisorNombre: string
  contenido: string
  enviadoEn: string
  leido: boolean
}

interface Contacto {
  id: string
  nombre: string
  iniciales: string
  rol: string
  online: boolean
  telefono?: string
  email?: string
}

const authStore = useAuthStore()
const route = useRoute()
const runtimeConfig = useRuntimeConfig()

const mensajes = ref<Mensaje[]>([])
const contactos = ref<Contacto[]>([])
const contactoActivo = ref<Contacto | null>(null)
const mensajeNuevo = ref('')
const cargando = ref(true)
const enviando = ref(false)
const conectado = ref(false)
const enviandoEmail = ref(false)
const emailEstado = ref<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

const emailForm = reactive({
  asunto: '',
  cuerpo: '',
})

const chatContainer = ref<HTMLElement | null>(null)
let stompClient: any = null

function normalizarBrokerURL(raw: string) {
  if (raw.startsWith('https://')) return raw.replace('https://', 'wss://')
  if (raw.startsWith('http://')) return raw.replace('http://', 'ws://')
  return raw
}

function prepararBorrador(contacto: Contacto | null) {
  emailEstado.value = null
  if (!contacto?.email) {
    emailForm.asunto = ''
    emailForm.cuerpo = ''
    return
  }

  emailForm.asunto = ''
  emailForm.cuerpo = `Hola ${contacto.nombre},\n\n`
}

async function conectarWebSocket() {
  try {
    const { Client } = await import('@stomp/stompjs')
    const token = localStorage.getItem('access_token')

    stompClient = new Client({
      brokerURL: normalizarBrokerURL(runtimeConfig.public.wsBase || 'http://localhost:8080/chat-websocket'),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      reconnectDelay: 5000,

      onConnect: () => {
        conectado.value = true
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
    conectado.value = false
  }
}

onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/mensajes/contactos')
    contactos.value = data

    const contactoId = typeof route.query.contacto === 'string' ? route.query.contacto : ''
    const preseleccionado = data.find((c: Contacto) => c.id === contactoId)
    if (preseleccionado) {
      await seleccionarContacto(preseleccionado)
    } else if (data.length > 0) {
      await seleccionarContacto(data[0])
    }
  } catch {
    // vacío
  } finally {
    cargando.value = false
    await conectarWebSocket()
  }
})

onUnmounted(() => {
  stompClient?.deactivate()
})

async function seleccionarContacto(c: Contacto) {
  contactoActivo.value = c
  prepararBorrador(c)
  mensajes.value = []

  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get(`/mensajes/historial/${c.id}`)
    mensajes.value = data
    await nextTick()
    scrollAbajo()
  } catch {
    // vacío
  }
}

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
      const { api } = await import('~/infrastructure/http/api')
      const { data } = await api.post('/mensajes/enviar', {
        destinatarioId: contactoActivo.value.id,
        contenido,
      })
      mensajes.value.push(data)
    }
    await nextTick()
    scrollAbajo()
  } catch {
    // vacío
  } finally {
    enviando.value = false
  }
}

async function enviarEmailManual() {
  if (!contactoActivo.value?.email) {
    emailEstado.value = { tipo: 'error', texto: 'Este contacto no tiene email configurado.' }
    return
  }

  if (!emailForm.asunto.trim() || !emailForm.cuerpo.trim()) {
    emailEstado.value = { tipo: 'error', texto: 'Escribe asunto y mensaje antes de enviar.' }
    return
  }

  enviandoEmail.value = true
  emailEstado.value = null

  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.post(`/mensajes/email/${contactoActivo.value.id}`, {
      asunto: emailForm.asunto.trim(),
      cuerpo: emailForm.cuerpo.trim(),
    })

    emailEstado.value = { tipo: 'ok', texto: 'Correo enviado a Mailtrap correctamente.' }
    prepararBorrador(contactoActivo.value)
  } catch (error: any) {
    emailEstado.value = {
      tipo: 'error',
      texto: error?.response?.data?.message || 'No se pudo enviar el correo.',
    }
  } finally {
    enviandoEmail.value = false
  }
}

function scrollAbajo() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

function esMio(m: Mensaje): boolean {
  return m.emisorId === authStore.usuario?.id
}

function formatHora(iso: string): string {
  return new Date(iso).toLocaleTimeString('es-ES', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <div class="flex gap-6 min-h-full items-start pb-6">
    <aside class="w-72 self-start card flex flex-col overflow-hidden flex-shrink-0">
      <div class="p-5 border-b border-surface-container">
        <h3 class="font-bold text-primary text-sm">Mensajes</h3>
        <div class="flex items-center gap-2 mt-1">
          <div class="w-2 h-2 rounded-full" :class="conectado ? 'bg-green-500' : 'bg-surface-container-high'" />
          <p class="text-[10px] text-on-surface-variant">{{ conectado ? 'Conectado' : 'Desconectado' }}</p>
        </div>
      </div>

      <div v-if="cargando" class="flex items-center justify-center py-8">
        <Loader2 class="w-5 h-5 animate-spin text-primary" />
      </div>

      <div v-else class="flex-1 overflow-y-auto">
        <button
          v-for="c in contactos"
          :key="c.id"
          class="w-full flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors text-left"
          :class="contactoActivo?.id === c.id ? 'bg-surface-container-low border-r-4 border-primary-container' : ''"
          @click="seleccionarContacto(c)"
        >
          <div class="relative flex-shrink-0">
            <div class="w-9 h-9 rounded-full bg-primary-container text-white flex items-center justify-center text-xs font-bold">
              {{ c.iniciales }}
            </div>
            <div v-if="c.online" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white" />
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

    <section class="flex-1 self-start card flex flex-col overflow-hidden">
      <div v-if="!contactoActivo" class="flex-1 flex flex-col items-center justify-center text-on-surface-variant gap-3">
        <MessageSquare class="w-10 h-10 opacity-20" />
        <p class="text-sm">Selecciona un contacto para escribirle</p>
      </div>

      <template v-else>
        <div class="px-6 py-4 border-b border-surface-container flex items-center justify-between flex-shrink-0">
          <div class="flex items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-primary-container text-white flex items-center justify-center text-xs font-bold">
              {{ contactoActivo.iniciales }}
            </div>
            <div>
              <p class="font-bold text-on-surface text-sm">{{ contactoActivo.nombre }}</p>
              <p class="text-[10px] text-on-surface-variant">{{ contactoActivo.email || 'Sin email configurado' }}</p>
            </div>
          </div>

          <div class="flex items-center gap-2 rounded-full bg-surface-container-low px-3 py-1.5 text-xs font-bold text-primary">
            <Mail class="w-3.5 h-3.5" />
            Mailtrap
          </div>
        </div>

        <div class="px-6 pt-5 pb-10 border-b border-surface-container bg-surface-container-low/40 flex-shrink-0">
          <div class="rounded-3xl bg-white border border-surface-container px-6 pt-6 pb-10 shadow-sm">
            <div class="flex items-center gap-2 mb-4">
              <Mail class="w-4 h-4 text-primary-container" />
              <h4 class="font-bold text-primary text-sm">Redactar correo</h4>
            </div>

            <div v-if="contactoActivo.email" class="space-y-4">
              <div>
                <label class="label">Para</label>
                <input :value="contactoActivo.email" type="text" class="input bg-surface-container-low" readonly />
              </div>
              <div>
                <label class="label">Asunto</label>
                <input v-model="emailForm.asunto" type="text" maxlength="160" class="input" placeholder="Ej. Seguimiento de cita" />
              </div>
              <div>
                <label class="label">Mensaje</label>
                <textarea
                  v-model="emailForm.cuerpo"
                  rows="5"
                  maxlength="4000"
                  class="input resize-none"
                  placeholder="Escribe el correo que quieres enviar..."
                />
              </div>

              <div class="mt-2 rounded-2xl bg-surface-container-low/70 px-4 py-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                <p class="text-[11px] leading-relaxed text-on-surface-variant max-w-xl">
                  El correo se enviará desde el backend y lo verás en Mailtrap.
                </p>
                <button
                  class="self-end lg:self-auto min-w-36 bg-primary-container text-white px-4 py-2.5 rounded-full text-sm font-bold hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center justify-center gap-2"
                  :disabled="enviandoEmail"
                  @click="enviarEmailManual"
                >
                  <Loader2 v-if="enviandoEmail" class="w-4 h-4 animate-spin" />
                  <Send v-else class="w-4 h-4" />
                  Enviar email
                </button>
              </div>

              <div
                v-if="emailEstado"
                class="mt-2 rounded-2xl px-4 py-3.5 text-sm flex items-start gap-2"
                :class="emailEstado.tipo === 'ok' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
              >
                <CheckCircle2 v-if="emailEstado.tipo === 'ok'" class="w-4 h-4 flex-shrink-0" />
                <span>{{ emailEstado.texto }}</span>
              </div>
            </div>

            <div v-else class="rounded-2xl bg-amber-50 text-amber-700 px-4 py-3 text-sm">
              Este contacto no tiene email configurado, así que no se puede enviar por Mailtrap.
            </div>
          </div>
        </div>

        <div ref="chatContainer" class="flex-1 min-h-40 overflow-y-auto p-6 space-y-4">
          <div
            v-for="m in mensajes"
            :key="m.id"
            class="flex"
            :class="esMio(m) ? 'justify-end' : 'justify-start'"
          >
            <div
              v-if="!esMio(m)"
              class="w-7 h-7 rounded-full bg-surface-container flex items-center justify-center text-[9px] font-bold text-on-surface-variant mr-2 flex-shrink-0 self-end"
            >
              {{ m.emisorNombre.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase() }}
            </div>

            <div
              class="max-w-xs px-4 py-2.5 rounded-2xl"
              :class="esMio(m) ? 'bg-primary-container text-white rounded-br-sm' : 'bg-surface-container-low text-on-surface rounded-bl-sm'"
            >
              <p class="text-sm leading-relaxed">{{ m.contenido }}</p>
              <p class="text-[9px] mt-1 text-right" :class="esMio(m) ? 'text-white/60' : 'text-on-surface-variant'">
                {{ formatHora(m.enviadoEn) }}
              </p>
            </div>
          </div>

          <div v-if="mensajes.length === 0" class="text-center py-8 text-sm text-on-surface-variant">
            No hay mensajes internos todavía con {{ contactoActivo.nombre }}
          </div>
        </div>

        <div class="px-6 py-4 border-t border-surface-container flex-shrink-0">
          <div class="flex items-center gap-3 bg-surface-container-low rounded-full px-4 py-2">
            <input
              v-model="mensajeNuevo"
              type="text"
              placeholder="Escribe un mensaje interno..."
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
