<script setup lang="ts">
/**
 * Mensajes y correo — bandeja de mensajes por contacto con envío manual por email.
 */
import { Send, Loader2, MessageSquare, Mail, CheckCircle2, Archive, Trash2, Clock3 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

definePageMeta({ middleware: 'auth' })

interface Mensaje {
  id: string
  emisorId: string
  emisorNombre: string
  asunto?: string
  contenido: string
  enviadoEn: string
  leido: boolean
  archivado?: boolean
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
const toast = useToast()

const mensajes = ref<Mensaje[]>([])
const contactos = ref<Contacto[]>([])
const contactoActivo = ref<Contacto | null>(null)
const cargando = ref(true)
const conectado = ref(false)
const enviandoEmail = ref(false)
const emailEstado = ref<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

const emailForm = reactive({
  asunto: '',
  cuerpo: '',
})

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
  } catch {
    // vacío
  }
}

async function enviarEmailManual() {
  if (!contactoActivo.value?.email) {
    emailEstado.value = { tipo: 'error', texto: 'Este contacto no tiene email configurado.' }
    toast.error('Este contacto no tiene email configurado.')
    return
  }

  if (!emailForm.asunto.trim() || !emailForm.cuerpo.trim()) {
    emailEstado.value = { tipo: 'error', texto: 'Escribe asunto y mensaje antes de enviar.' }
    toast.error('Escribe asunto y mensaje antes de enviar.')
    return
  }

  enviandoEmail.value = true
  emailEstado.value = null

  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post(`/mensajes/email/${contactoActivo.value.id}`, {
      asunto: emailForm.asunto.trim(),
      cuerpo: emailForm.cuerpo.trim(),
    })

    if (data?.mensajeChat) {
      mensajes.value.push(data.mensajeChat)
    }

    emailEstado.value = { tipo: 'ok', texto: 'Mensaje enviado correctamente.' }
    toast.success(`Mensaje enviado a ${contactoActivo.value.nombre}.`)
    prepararBorrador(contactoActivo.value)
  } catch (error: any) {
    emailEstado.value = {
      tipo: 'error',
      texto: error?.response?.data?.message || 'No se pudo enviar el correo.',
    }
    toast.error(emailEstado.value.texto)
  } finally {
    enviandoEmail.value = false
  }
}

function esMio(m: Mensaje): boolean {
  return m.emisorId === authStore.usuario?.id || m.emisorId === authStore.usuario?.username
}

function formatFechaLarga(iso: string): string {
  return new Date(iso).toLocaleString('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const mensajesVisibles = computed(() => mensajes.value.filter(m => !m.archivado))

async function archivarMensaje(mensajeId: string) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/mensajes/${mensajeId}/archivar`)
    mensajes.value = mensajes.value.filter(m => m.id !== mensajeId)
    toast.info('Mensaje archivado.')
  } catch {
    toast.error('No se pudo archivar el mensaje.')
  }
}

async function eliminarMensaje(mensajeId: string) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/mensajes/${mensajeId}`)
    mensajes.value = mensajes.value.filter(m => m.id !== mensajeId)
    toast.success('Mensaje eliminado.')
  } catch {
    toast.error('No se pudo eliminar el mensaje.')
  }
}
</script>

<template>
  <div class="flex gap-6 min-h-full items-start pb-6">
    <aside class="w-72 self-start card flex flex-col overflow-hidden flex-shrink-0" aria-label="Contactos">
      <div class="p-5 border-b border-surface-container">
        <h3 class="font-bold text-primary text-sm">Mensajes</h3>
        <div class="flex items-center gap-2 mt-1">
          <div class="w-2 h-2 rounded-full" :class="conectado ? 'bg-green-500' : 'bg-surface-container-high'" aria-hidden="true" />
          <p class="text-[10px] text-on-surface-variant" role="status" aria-live="polite">{{ conectado ? 'Conectado' : 'Desconectado' }}</p>
        </div>
      </div>

      <div v-if="cargando" class="flex items-center justify-center py-8" aria-busy="true" aria-label="Cargando contactos">
        <Loader2 class="w-5 h-5 animate-spin text-primary" aria-hidden="true" />
      </div>

      <ul v-else class="flex-1 overflow-y-auto" role="list" aria-label="Lista de contactos">
        <li v-for="c in contactos" :key="c.id">
        <button
          class="w-full flex items-center gap-3 px-4 py-3 hover:bg-surface-container-low transition-colors text-left"
          :class="contactoActivo?.id === c.id ? 'bg-surface-container-low border-r-4 border-primary-container' : ''"
          :aria-label="`${c.nombre}, ${c.rol === 'ROLE_ADMIN' ? 'Administrador' : 'Peluquero/a'}${c.online ? ', en línea' : ''}`"
          :aria-pressed="contactoActivo?.id === c.id"
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
            <p class="text-[10px] text-on-surface-variant">{{ c.rol === 'ROLE_ADMIN' ? 'Administrador' : 'Peluquero/a' }}</p>
          </div>
        </button>
        </li>

        <li v-if="contactos.length === 0" class="px-4 py-8 text-center text-sm text-on-surface-variant">
          No hay contactos disponibles
        </li>
      </ul>
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

        <div class="px-6 pt-5 pb-10 bg-surface-container-low/40 flex-shrink-0">
          <div class="space-y-4">
            <div class="rounded-2xl bg-white border border-surface-container px-6 pt-6 pb-6 shadow-sm">
            <div class="flex items-center gap-2 mb-4">
              <Mail class="w-4 h-4 text-primary-container" />
              <h4 class="font-bold text-primary text-sm">Redactar correo</h4>
            </div>

            <div v-if="contactoActivo.email" class="space-y-4">
              <div>
                <label class="label">Para</label>
                <input :value="contactoActivo.email" type="text" class="input" readonly />
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

              <div class="mt-2 rounded-2xl bg-surface-container-low/70 px-4 py-4 flex justify-end">
                <button
                  class="min-w-36 bg-primary-container text-white px-4 py-2.5 rounded-full text-sm font-bold hover:opacity-90 transition-opacity disabled:opacity-50 flex items-center justify-center gap-2"
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
                role="alert"
                class="mt-2 rounded-2xl px-4 py-3.5 text-sm flex items-start gap-2"
                :class="emailEstado.tipo === 'ok' ? 'bg-green-50 text-green-700' : 'bg-red-50 text-red-700'"
              >
                <CheckCircle2 v-if="emailEstado.tipo === 'ok'" class="w-4 h-4 flex-shrink-0" aria-hidden="true" />
                <span>{{ emailEstado.texto }}</span>
              </div>

            </div>

            <div v-else class="rounded-2xl bg-amber-50 text-amber-700 px-4 py-3 text-sm">
              Este contacto no tiene email configurado, así que no se puede enviar por Mailtrap.
            </div>
            </div>

            <div class="rounded-2xl bg-white border border-surface-container px-6 py-5 shadow-sm space-y-3">
              <div class="flex items-center justify-between gap-3 border-b border-surface-container pb-3">
                <div>
                  <h5 class="text-sm font-bold text-primary">Mensajes para {{ contactoActivo.nombre }}</h5>
                  <p class="text-[11px] text-on-surface-variant">Se muestran solo los mensajes activos de este peluquero.</p>
                </div>
                <div class="rounded-lg bg-surface-container-low px-2.5 py-1 text-xs font-bold text-primary">
                  {{ mensajesVisibles.length }}
                </div>
              </div>

              <div v-if="mensajesVisibles.length === 0" class="rounded-xl border border-dashed border-surface-container px-4 py-6 text-sm text-on-surface-variant">
                No hay mensajes guardados para este contacto.
              </div>

              <article
                v-for="m in mensajesVisibles"
                :key="m.id"
                class="rounded-xl border border-surface-container bg-white px-4 py-4 transition-colors hover:bg-surface-container-lowest/60"
              >
                <div class="flex flex-col gap-3">
                  <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                    <div class="min-w-0 space-y-2 flex-1">
                    <div class="flex flex-wrap items-center gap-2">
                      <span class="rounded-lg bg-primary/8 px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.08em] text-primary">
                        {{ esMio(m) ? 'Enviado por ti' : m.emisorNombre }}
                      </span>
                      <span class="inline-flex items-center gap-1 text-[11px] text-on-surface-variant">
                        <Clock3 class="h-3.5 w-3.5" />
                        {{ formatFechaLarga(m.enviadoEn) }}
                      </span>
                    </div>
                      <h6 class="text-sm font-semibold text-on-surface leading-5">{{ m.asunto || 'Sin asunto' }}</h6>
                    </div>

                    <div class="flex items-center gap-1 lg:pl-4">
                      <button
                        type="button"
                        class="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-[11px] font-medium text-on-surface-variant transition-colors hover:bg-surface-container-low hover:text-on-surface"
                        title="Archivar mensaje"
                        aria-label="Archivar mensaje"
                        @click="archivarMensaje(m.id)"
                      >
                        <Archive class="h-3.5 w-3.5" />
                        Archivar
                      </button>
                      <button
                        type="button"
                        class="inline-flex items-center gap-1.5 rounded-lg px-2.5 py-2 text-[11px] font-medium text-on-surface-variant transition-colors hover:bg-red-50 hover:text-red-700"
                        title="Eliminar mensaje"
                        aria-label="Eliminar mensaje"
                        @click="eliminarMensaje(m.id)"
                      >
                        <Trash2 class="h-3.5 w-3.5" />
                        Eliminar
                      </button>
                    </div>
                  </div>

                  <p class="whitespace-pre-line text-sm leading-relaxed text-on-surface-variant border-l-2 border-surface-container pl-3">
                    {{ m.contenido }}
                  </p>
                </div>
              </article>
            </div>
          </div>
        </div>
      </template>
    </section>
  </div>
</template>
