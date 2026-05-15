<script setup lang="ts">
/**
 * Mensajes y correo — bandeja de mensajes por contacto con envío manual por email.
 */
import { Send, Loader2, MessageSquare, Mail, CheckCircle2, Archive, Trash2, Clock3 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

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
const toast = useToast()

const mensajes = ref<Mensaje[]>([])
const contactos = ref<Contacto[]>([])
const contactoActivo = ref<Contacto | null>(null)
const cargando = ref(true)
const enviandoEmail = ref(false)
const eliminandoMensaje = ref(false)
const mensajeAEliminar = ref<Mensaje | null>(null)
const emailEstado = ref<{ tipo: 'ok' | 'error'; texto: string } | null>(null)

const emailForm = reactive({
  asunto: '',
  cuerpo: '',
})

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
  }
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

function pedirEliminarMensaje(mensaje: Mensaje) {
  mensajeAEliminar.value = mensaje
}

function cancelarEliminarMensaje() {
  mensajeAEliminar.value = null
}

async function eliminarMensajeConfirmado() {
  if (!mensajeAEliminar.value?.id) return

  eliminandoMensaje.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/mensajes/${mensajeAEliminar.value.id}`)
    mensajes.value = mensajes.value.filter(m => m.id !== mensajeAEliminar.value?.id)
    cancelarEliminarMensaje()
    toast.success('Mensaje eliminado.')
  } catch {
    toast.error('No se pudo eliminar el mensaje.')
  } finally {
    eliminandoMensaje.value = false
  }
}
</script>

<template>
  <div class="flex min-h-full flex-col items-stretch gap-4 pb-4 sm:gap-5 sm:pb-6">
    <aside class="card flex w-full flex-col overflow-hidden rounded-2xl" aria-label="Contactos">
      <div class="border-b border-surface-container px-4 py-4 sm:flex sm:items-center sm:justify-between sm:p-5">
        <h3 class="font-bold text-primary text-sm">Mensajes</h3>
        <div class="flex items-center gap-2 mt-1">
          <Mail class="w-3.5 h-3.5 text-on-surface-variant" aria-hidden="true" />
          <p class="text-[10px] text-on-surface-variant">Correo interno</p>
        </div>
      </div>

      <div v-if="cargando" class="flex items-center justify-center py-8" aria-busy="true" aria-label="Cargando contactos">
        <Loader2 class="w-5 h-5 animate-spin text-primary" aria-hidden="true" />
      </div>

      <ul v-else class="grid gap-2 px-3 py-3 sm:grid-cols-2 sm:px-4 lg:flex lg:flex-wrap" role="list" aria-label="Lista de contactos">
        <li v-for="c in contactos" :key="c.id" class="min-w-0 lg:flex-1">
        <button
          class="w-full flex items-center gap-3 rounded-2xl px-3 py-3 hover:bg-surface-container-low transition-colors text-left sm:px-4"
          :class="contactoActivo?.id === c.id ? 'bg-surface-container-low ring-2 ring-primary-container/20' : ''"
          :aria-label="`${c.nombre}, ${c.rol === 'ROLE_ADMIN' ? 'Administrador' : 'Peluquero/a'}${c.online ? ', en línea' : ''}`"
          :aria-pressed="contactoActivo?.id === c.id"
          @click="seleccionarContacto(c)"
        >
          <div class="relative flex-shrink-0">
            <div class="w-8 h-8 rounded-full bg-primary-container text-white flex items-center justify-center text-[11px] font-bold sm:h-9 sm:w-9 sm:text-xs">
              {{ c.iniciales }}
            </div>
            <div v-if="c.online" class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 rounded-full border-2 border-white" />
          </div>
          <div class="min-w-0">
            <p class="font-bold text-on-surface text-[13px] truncate sm:text-sm">{{ c.nombre }}</p>
            <p class="text-[10px] text-on-surface-variant">{{ c.rol === 'ROLE_ADMIN' ? 'Administrador' : 'Peluquero/a' }}</p>
          </div>
        </button>
        </li>

        <li v-if="contactos.length === 0" class="px-4 py-8 text-center text-sm text-on-surface-variant">
          No hay contactos disponibles
        </li>
      </ul>
    </aside>

    <section class="card flex w-full min-w-0 flex-1 flex-col overflow-hidden rounded-2xl">
      <div v-if="!contactoActivo" class="flex-1 flex flex-col items-center justify-center text-on-surface-variant gap-3">
        <MessageSquare class="w-10 h-10 opacity-20" />
        <p class="text-sm">Selecciona un contacto para escribirle</p>
      </div>

      <template v-else>
        <div class="flex flex-shrink-0 flex-col gap-3 border-b border-surface-container px-4 py-3.5 sm:flex-row sm:items-center sm:justify-between sm:px-6 sm:py-4">
          <div class="flex min-w-0 items-center gap-3">
            <div class="w-9 h-9 rounded-full bg-primary-container text-white flex items-center justify-center text-xs font-bold">
              {{ contactoActivo.iniciales }}
            </div>
            <div class="min-w-0">
              <p class="truncate font-bold text-on-surface text-sm">{{ contactoActivo.nombre }}</p>
              <p class="truncate text-[10px] text-on-surface-variant">{{ contactoActivo.email || 'Sin email configurado' }}</p>
            </div>
          </div>

          <div class="inline-flex self-start items-center gap-2 rounded-full bg-surface-container-low px-3 py-1.5 text-xs font-bold text-primary sm:self-auto">
            <Mail class="w-3.5 h-3.5" />
            Mailtrap
          </div>
        </div>

        <div class="flex-shrink-0 bg-surface-container-low/30 px-3 py-3 sm:px-6 sm:py-5">
          <div class="space-y-3 sm:space-y-4">
            <div class="rounded-2xl border border-surface-container bg-white px-4 py-4 shadow-sm sm:px-6 sm:py-6">
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
                  rows="4"
                  maxlength="4000"
                  class="input min-h-[7rem] resize-none"
                  placeholder="Escribe el correo que quieres enviar..."
                />
              </div>

              <div class="mt-1 flex sm:justify-end">
                <button
                  class="flex w-full items-center justify-center gap-2 rounded-xl bg-primary-container px-4 py-3 text-sm font-bold text-white transition-opacity hover:opacity-90 disabled:opacity-50 sm:w-auto sm:min-w-36"
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

            <div class="rounded-2xl border border-surface-container bg-white px-4 py-4 shadow-sm space-y-3 sm:px-6 sm:py-5">
              <div class="flex flex-col gap-3 border-b border-surface-container pb-3 sm:flex-row sm:items-center sm:justify-between">
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
                class="rounded-xl border border-surface-container bg-white px-4 py-4"
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
                        @click="pedirEliminarMensaje(m)"
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

  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="mensajeAEliminar"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4 backdrop-blur-sm"
        @click.self="cancelarEliminarMensaje"
      >
        <section class="w-full max-w-md rounded-[28px] border border-outline-variant/15 bg-white p-6 shadow-2xl sm:p-7">
          <div class="space-y-2">
            <h3 class="text-lg font-bold text-primary">Eliminar mensaje</h3>
            <p class="text-sm leading-relaxed text-on-surface-variant">
              Voy a eliminar el mensaje <strong class="text-on-surface">{{ mensajeAEliminar.asunto || 'Sin asunto' }}</strong>. Esta acción no se puede deshacer.
            </p>
          </div>

          <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button class="btn-secondary" :disabled="eliminandoMensaje" @click="cancelarEliminarMensaje">
              Cancelar
            </button>
            <button class="btn-danger" :disabled="eliminandoMensaje" @click="eliminarMensajeConfirmado">
              <Loader2 v-if="eliminandoMensaje" class="h-4 w-4 animate-spin" aria-hidden="true" />
              <span>{{ eliminandoMensaje ? 'Eliminando...' : 'Sí, eliminar' }}</span>
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
