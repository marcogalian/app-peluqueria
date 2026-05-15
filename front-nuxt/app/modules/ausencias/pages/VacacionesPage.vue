<script setup lang="ts">
/**
 * Vacaciones — empleado solicita ausencias, ve el estado de sus peticiones.
 *
 * - Vacaciones: minimo 7 dias de antelacion.
 * - Asunto propio: minimo 5 dias.
 * - Baja medica: sin antelacion.
 *
 * Al entrar, se muestra un card de notificacion para cada solicitud que ha
 * sido aprobada o rechazada y aun no se ha confirmado lectura
 * (vistaPorEmpleado=false). Cuando el empleado cierra el card, se hace
 * PATCH /api/v1/ausencias/{id}/marcar-vista.
 *
 * Se cargan los dias bloqueados por el admin: si las fechas seleccionadas
 * solapan con uno, se avisa antes de enviar (y el backend tambien valida).
 *
 * Admin ve todas las solicitudes con botones de aprobar/rechazar.
 */
import { Plus, Check, X, Loader2, Calendar, AlertTriangle, CheckCircle2 } from 'lucide-vue-next'
import { addDays, differenceInDays, parseISO, format } from 'date-fns'
import { es } from 'date-fns/locale'
import { useToast } from '~/modules/shared/composables/useToast'

const toast = useToast()

// ── Tipos ─────────────────────────────────────────────────
type TipoAusencia = 'VACACIONES' | 'ASUNTO_PROPIO' | 'BAJA'
type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA'

interface Solicitud {
  id: string
  tipo: TipoAusencia
  fechaInicio: string
  fechaFin: string
  motivo: string
  estado: EstadoSolicitud
  solicitadaEn: string
  empleadoNombre?: string
  motivoRechazo?: string
  vistaPorEmpleado?: boolean
}

interface DiaBloqueado {
  id: string
  fechaInicio: string
  fechaFin: string
  motivo: string
}

// ── Estado ────────────────────────────────────────────────
const authStore  = useAuthStore()
const solicitudes = ref<Solicitud[]>([])
const diasBloqueados = ref<DiaBloqueado[]>([])
const cargando   = ref(true)
const modalAbierto = ref(false)
const enviando   = ref(false)
const errorForm  = ref('')

// Form nueva solicitud
const form = reactive<{
  tipo: TipoAusencia
  fechaInicio: string
  fechaFin: string
  motivo: string
}>({
  tipo: 'VACACIONES',
  fechaInicio: '',
  fechaFin: '',
  motivo: '',
})

// Modal de rechazo (solo admin)
const modalRechazo  = ref(false)
const solicitudIdSeleccionada = ref<string | null>(null)
const motivoRechazo = ref('')
const procesando    = ref(false)

// ── Computed ──────────────────────────────────────────────
const pendientes = computed(() => solicitudes.value.filter(s => s.estado === 'PENDIENTE'))
const aprobadas  = computed(() => solicitudes.value.filter(s => s.estado === 'APROBADA'))
const rechazadas = computed(() => solicitudes.value.filter(s => s.estado === 'RECHAZADA'))

// Solicitudes propias resueltas que el empleado todavia no ha confirmado leer.
// Estas se muestran como cards de notificacion al entrar.
const notificacionesPendientes = computed(() => {
  if (authStore.isAdmin) return []
  return solicitudes.value.filter(s =>
    !s.vistaPorEmpleado &&
    (s.estado === 'APROBADA' || s.estado === 'RECHAZADA'),
  )
})

const diasAntelacionMinimos = computed(() =>
  form.tipo === 'VACACIONES' ? 7 : form.tipo === 'ASUNTO_PROPIO' ? 5 : 0,
)

const fechaInicioMinima = computed(() => {
  const minima = addDays(new Date(), diasAntelacionMinimos.value)
  return format(minima, 'yyyy-MM-dd')
})

const duracionDias = computed(() => {
  if (!form.fechaInicio || !form.fechaFin) return 0
  return differenceInDays(parseISO(form.fechaFin), parseISO(form.fechaInicio)) + 1
})

// ¿Las fechas seleccionadas solapan con algun día bloqueado por el admin?
const bloqueoSolapado = computed<DiaBloqueado | null>(() => {
  if (!form.fechaInicio || !form.fechaFin) return null
  const inicio = parseISO(form.fechaInicio)
  const fin = parseISO(form.fechaFin)
  for (const dia of diasBloqueados.value) {
    const bInicio = parseISO(dia.fechaInicio)
    const bFin = parseISO(dia.fechaFin)
    // Solapamiento: inicio <= bFin AND fin >= bInicio
    if (inicio <= bFin && fin >= bInicio) {
      return dia
    }
  }
  return null
})

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const [resAusencias, resBloqueados] = await Promise.all([
      api.get('/v1/ausencias'),
      api.get('/v1/dias-bloqueados'),
    ])
    solicitudes.value = resAusencias.data
    diasBloqueados.value = resBloqueados.data
  } catch { /* vacio */ }
  finally { cargando.value = false }
})

// ── Enviar solicitud ──────────────────────────────────────
async function enviarSolicitud() {
  errorForm.value = ''

  if (!form.fechaInicio || !form.fechaFin) {
    errorForm.value = 'Selecciona fechas de inicio y fin.'
    return
  }

  if (form.tipo !== 'BAJA') {
    const diasHastaInicio = differenceInDays(parseISO(form.fechaInicio), new Date())
    if (diasHastaInicio < diasAntelacionMinimos.value) {
      errorForm.value = `Las ${form.tipo === 'VACACIONES' ? 'vacaciones' : 'ausencias por asunto propio'} requieren minimo ${diasAntelacionMinimos.value} dias de antelacion.`
      return
    }
    if (bloqueoSolapado.value) {
      errorForm.value = `Las fechas seleccionadas chocan con un dia bloqueado: "${bloqueoSolapado.value.motivo || 'fecha bloqueada'}".`
      return
    }
  }

  enviando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post('/v1/ausencias', form)
    solicitudes.value.unshift(data)
    modalAbierto.value = false
    Object.assign(form, { tipo: 'VACACIONES', fechaInicio: '', fechaFin: '', motivo: '' })
    toast.success('Solicitud enviada')
  } catch (err: any) {
    errorForm.value = err?.response?.data?.message || 'No se pudo enviar la solicitud.'
  } finally {
    enviando.value = false
  }
}

// ── Aprobar / rechazar (solo admin) ──────────────────────
async function aprobar(id: string) {
  procesando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/v1/ausencias/${id}/aprobar`)
    const sol = solicitudes.value.find(s => s.id === id)
    if (sol) sol.estado = 'APROBADA'
    toast.success('Ausencia aprobada')
  } catch { toast.error('Error al aprobar') } finally { procesando.value = false }
}

async function rechazar() {
  if (!solicitudIdSeleccionada.value) return
  procesando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/v1/ausencias/${solicitudIdSeleccionada.value}/rechazar`, { motivo: motivoRechazo.value })
    const sol = solicitudes.value.find(s => s.id === solicitudIdSeleccionada.value)
    if (sol) { sol.estado = 'RECHAZADA'; sol.motivoRechazo = motivoRechazo.value }
    modalRechazo.value = false
    motivoRechazo.value = ''
    toast.success('Ausencia rechazada')
  } catch { toast.error('Error al rechazar') } finally { procesando.value = false }
}

async function cancelarSolicitud(id: string) {
  procesando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/v1/ausencias/${id}/cancelar`)
    solicitudes.value = solicitudes.value.filter(s => s.id !== id)
    toast.success('Solicitud cancelada')
  } catch { toast.error('Error al cancelar') } finally { procesando.value = false }
}

// ── Cerrar card de notificacion ──────────────────────────
async function cerrarNotificacion(solicitud: Solicitud) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/v1/ausencias/${solicitud.id}/marcar-vista`)
    solicitud.vistaPorEmpleado = true
  } catch {
    // Si falla, marca igualmente para no bloquear al empleado en esta sesion
    solicitud.vistaPorEmpleado = true
  }
}

// ── Helpers ───────────────────────────────────────────────
function badgeEstado(estado: EstadoSolicitud): string {
  return {
    PENDIENTE:  'bg-amber-100 text-amber-700',
    APROBADA:   'bg-green-100 text-green-700',
    RECHAZADA:  'bg-red-100 text-red-700',
    CANCELADA:  'bg-surface-container text-on-surface-variant',
  }[estado] ?? 'bg-surface-container text-on-surface-variant'
}

function labelTipo(tipo: TipoAusencia): string {
  return { VACACIONES: 'Vacaciones', ASUNTO_PROPIO: 'Asunto propio', BAJA: 'Baja médica' }[tipo]
}

function formatFecha(fecha: string): string {
  return format(parseISO(fecha), "d 'de' MMMM yyyy", { locale: es })
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cards de notificacion (solo empleado, solo si hay sin leer) ── -->
    <div v-if="notificacionesPendientes.length > 0" class="space-y-3">
      <div
        v-for="notif in notificacionesPendientes"
        :key="notif.id"
        :class="[
          'card flex flex-col gap-4 border-l-4 p-4 sm:flex-row sm:items-start sm:gap-4 sm:p-5',
          notif.estado === 'APROBADA'
            ? 'bg-green-50 border-green-500'
            : 'bg-red-50 border-red-500',
        ]"
      >
        <div
          :class="[
            'w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0',
            notif.estado === 'APROBADA' ? 'bg-green-200' : 'bg-red-200',
          ]"
        >
          <CheckCircle2 v-if="notif.estado === 'APROBADA'" class="w-5 h-5 text-green-700" />
          <AlertTriangle v-else class="w-5 h-5 text-red-700" />
        </div>
        <div class="min-w-0 flex-1">
          <p
            :class="[
              'text-sm font-bold',
              notif.estado === 'APROBADA' ? 'text-green-800' : 'text-red-800',
            ]"
          >
            {{ notif.estado === 'APROBADA' ? 'Solicitud aprobada' : 'Solicitud denegada' }}
          </p>
          <p class="text-sm text-on-surface mt-1">
            Tu solicitud de <span class="font-bold">{{ labelTipo(notif.tipo).toLowerCase() }}</span>
            del <span class="font-bold">{{ formatFecha(notif.fechaInicio) }}</span>
            al <span class="font-bold">{{ formatFecha(notif.fechaFin) }}</span>
            ha sido <span class="font-bold">{{ notif.estado === 'APROBADA' ? 'aprobada' : 'denegada' }}</span>.
          </p>
          <p
            v-if="notif.estado === 'RECHAZADA' && notif.motivoRechazo"
            class="text-sm text-red-700 mt-2 bg-white/50 p-2 rounded-lg"
          >
            <span class="font-bold">Motivo:</span> {{ notif.motivoRechazo }}
          </p>
        </div>
        <button
          class="self-end rounded-lg p-2 transition-colors hover:bg-white/50 sm:self-start"
          aria-label="Cerrar notificación"
          @click="cerrarNotificacion(notif)"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">
          {{ authStore.isAdmin ? 'Gestión de Ausencias' : 'Mis Vacaciones' }}
        </h2>
        <p class="text-on-surface-variant text-sm">
          {{ authStore.isAdmin
            ? 'Solicitudes de ausencia del equipo'
            : 'Solicita y consulta el estado de tus ausencias' }}
        </p>
      </div>
      <button
        v-if="!authStore.isAdmin"
        class="flex w-full items-center justify-center gap-2 rounded-full bg-primary-container px-6 py-2.5 text-sm font-bold text-white transition-all hover:opacity-90 sm:w-auto"
        @click="modalAbierto = true"
      >
        <Plus class="w-4 h-4" />
        Nueva solicitud
      </button>
    </div>

    <!-- ── Aviso de días bloqueados (visible siempre) ───── -->
    <div
      v-if="!authStore.isAdmin && diasBloqueados.length > 0"
      class="card flex flex-col gap-3 border border-amber-200 bg-amber-50 p-4 sm:flex-row sm:items-start"
    >
      <AlertTriangle class="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
      <div class="flex-1">
        <p class="text-sm font-bold text-amber-800 mb-1">
          Hay {{ diasBloqueados.length }} fecha{{ diasBloqueados.length !== 1 ? 's' : '' }} bloqueada{{ diasBloqueados.length !== 1 ? 's' : '' }} para vacaciones
        </p>
        <ul class="text-xs text-amber-700 space-y-0.5">
          <li v-for="dia in diasBloqueados" :key="dia.id">
            <span class="font-bold">{{ dia.fechaInicio === dia.fechaFin ? dia.fechaInicio : `${dia.fechaInicio} → ${dia.fechaFin}` }}</span>
            <span v-if="dia.motivo"> — {{ dia.motivo }}</span>
          </li>
        </ul>
      </div>
    </div>

    <!-- ── KPI cards (solo empleado) ─────────────────────── -->
    <div v-if="!authStore.isAdmin" class="grid grid-cols-1 gap-3 sm:grid-cols-3 sm:gap-4">
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Pendientes</p>
        <p class="text-3xl font-extrabold text-amber-600">{{ pendientes.length }}</p>
      </div>
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Aprobadas</p>
        <p class="text-3xl font-extrabold text-green-600">{{ aprobadas.length }}</p>
      </div>
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Rechazadas</p>
        <p class="text-3xl font-extrabold text-red-500">{{ rechazadas.length }}</p>
      </div>
    </div>

    <!-- ── Spinner ────────────────────────────────────────── -->
    <div v-if="cargando" class="flex items-center justify-center py-16">
      <Loader2 class="w-6 h-6 animate-spin text-primary" />
    </div>

    <!-- ── Lista de solicitudes ───────────────────────────── -->
    <div v-else class="space-y-3">
      <div
        v-for="solicitud in solicitudes"
        :key="solicitud.id"
        class="card flex flex-col gap-4 p-4 sm:flex-row sm:items-start sm:gap-4 sm:p-5"
      >
        <div class="flex items-start gap-3 sm:contents">
        <div class="w-10 h-10 bg-primary-fixed rounded-xl flex items-center justify-center flex-shrink-0">
          <Calendar class="w-5 h-5 text-primary-container" />
        </div>

        <div class="flex-1 min-w-0">
          <div class="mb-1 flex flex-wrap items-center gap-2">
            <span class="font-bold text-primary text-sm">{{ labelTipo(solicitud.tipo) }}</span>
            <span v-if="authStore.isAdmin && solicitud.empleadoNombre" class="text-xs text-on-surface-variant">
              — {{ solicitud.empleadoNombre }}
            </span>
          </div>
          <p class="text-sm leading-relaxed text-on-surface-variant">
            {{ formatFecha(solicitud.fechaInicio) }}
            <span class="mx-1 hidden sm:inline">→</span>
            <span class="block sm:inline">{{ formatFecha(solicitud.fechaFin) }}</span>
            <span class="mt-1 block text-xs font-bold text-primary sm:ml-2 sm:mt-0 sm:inline">
              ({{ differenceInDays(parseISO(solicitud.fechaFin), parseISO(solicitud.fechaInicio)) + 1 }} días)
            </span>
          </p>
          <p v-if="solicitud.motivo" class="text-xs text-on-surface-variant mt-1">{{ solicitud.motivo }}</p>
          <p v-if="solicitud.motivoRechazo" class="text-xs text-red-600 mt-1 font-medium">
            Motivo de rechazo: {{ solicitud.motivoRechazo }}
          </p>
          <p class="text-[10px] text-on-surface-variant/60 mt-1">
            Solicitada el {{ formatFecha(solicitud.solicitadaEn) }}
          </p>
        </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 sm:flex-shrink-0 sm:justify-end">
          <span class="text-[10px] font-bold px-2.5 py-1 rounded-full whitespace-nowrap" :class="badgeEstado(solicitud.estado)">
            {{ solicitud.estado.toLowerCase() }}
          </span>

          <template v-if="authStore.isAdmin && solicitud.estado === 'PENDIENTE'">
            <button
              class="w-8 h-8 rounded-full bg-green-100 hover:bg-green-200 text-green-700 flex items-center justify-center transition-colors"
              :aria-label="`Aprobar solicitud de ${solicitud.empleadoNombre || 'empleado'}`"
              :disabled="procesando"
              @click="aprobar(solicitud.id)"
            >
              <Check class="w-4 h-4" aria-hidden="true" />
            </button>
            <button
              class="w-8 h-8 rounded-full bg-red-100 hover:bg-red-200 text-red-700 flex items-center justify-center transition-colors"
              :aria-label="`Rechazar solicitud de ${solicitud.empleadoNombre || 'empleado'}`"
              @click="solicitudIdSeleccionada = solicitud.id; modalRechazo = true"
            >
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </template>

          <button
            v-else-if="!authStore.isAdmin && solicitud.estado === 'PENDIENTE'"
            class="rounded-lg bg-red-50 px-3 py-2 text-xs font-semibold text-error transition-colors hover:bg-red-100"
            :disabled="procesando"
            @click="cancelarSolicitud(solicitud.id)"
          >
            Cancelar
          </button>
        </div>
      </div>

      <div v-if="solicitudes.length === 0" class="text-center py-16 text-on-surface-variant text-sm">
        No hay solicitudes registradas
      </div>
    </div>

  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Nueva solicitud
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalAbierto"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalAbierto = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-ausencia-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-center justify-between mb-5">
            <h3 id="modal-ausencia-titulo" class="text-lg font-bold text-primary">Nueva Solicitud de Ausencia</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" aria-label="Cerrar" @click="modalAbierto = false">
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <div class="space-y-4">

            <div>
              <label class="label">Tipo de ausencia</label>
              <select v-model="form.tipo" class="select-field">
                <option value="VACACIONES">Vacaciones</option>
                <option value="ASUNTO_PROPIO">Asunto propio</option>
                <option v-if="authStore.isAdmin" value="BAJA">Baja médica</option>
              </select>
              <p v-if="form.tipo !== 'BAJA'" class="text-xs text-on-surface-variant mt-1">
                Mínimo {{ diasAntelacionMinimos }} días de antelación requeridos
              </p>
            </div>

            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">Fecha inicio</label>
                <input v-model="form.fechaInicio" type="date" :min="form.tipo === 'BAJA' ? '' : fechaInicioMinima" class="input" />
              </div>
              <div>
                <label class="label">Fecha fin</label>
                <input v-model="form.fechaFin" type="date" :min="form.fechaInicio" class="input" />
              </div>
            </div>

            <p v-if="duracionDias > 0" class="text-xs font-bold text-primary-container bg-primary-fixed px-3 py-1.5 rounded-lg">
              Duración: {{ duracionDias }} día{{ duracionDias !== 1 ? 's' : '' }}
            </p>

            <!-- Aviso de bloqueo -->
            <div
              v-if="bloqueoSolapado"
              class="text-xs text-red-700 bg-red-50 border border-red-200 rounded-lg px-3 py-2 flex items-start gap-2"
            >
              <AlertTriangle class="w-4 h-4 flex-shrink-0 mt-0.5" />
              <span>
                Las fechas seleccionadas <span class="font-bold">no están disponibles</span>.
                <span v-if="bloqueoSolapado.motivo">Motivo: {{ bloqueoSolapado.motivo }}.</span>
              </span>
            </div>

            <div>
              <label class="label">Motivo (opcional)</label>
              <textarea v-model="form.motivo" rows="2" class="input resize-none" placeholder="Descripción breve..." />
            </div>

            <Transition name="modal-overlay">
              <p v-if="errorForm" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {{ errorForm }}
              </p>
            </Transition>
          </div>

          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalAbierto = false">Cancelar</button>
            <button
              class="btn-primary flex-1"
              :disabled="enviando || !!bloqueoSolapado"
              @click="enviarSolicitud"
            >
              <Loader2 v-if="enviando" class="w-4 h-4 animate-spin" />
              <span>{{ enviando ? 'Enviando...' : 'Enviar solicitud' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Modal motivo de rechazo (admin) -->
    <Transition name="modal-overlay">
      <div
        v-if="modalRechazo"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalRechazo = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-rechazo-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-xs p-6 animate-fade-scale-in"
        >
          <h3 id="modal-rechazo-titulo" class="text-lg font-bold text-primary mb-4">Rechazar solicitud</h3>
          <div>
            <label class="label" for="motivo-rechazo">Motivo del rechazo</label>
            <textarea id="motivo-rechazo" v-model="motivoRechazo" rows="3" class="input resize-none" placeholder="Explica el motivo..." />
          </div>
          <div class="flex gap-3 mt-4">
            <button class="btn-secondary flex-1" @click="modalRechazo = false">Cancelar</button>
            <button class="btn-danger flex-1" :disabled="procesando" @click="rechazar">
              <Loader2 v-if="procesando" class="w-4 h-4 animate-spin" />
              <span>{{ procesando ? 'Rechazando...' : 'Confirmar' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </Teleport>
</template>
