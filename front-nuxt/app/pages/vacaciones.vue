<script setup lang="ts">
/**
 * Vacaciones — empleado solicita ausencias, ve el estado de sus peticiones.
 * Reglas: vacaciones = mín. 7 días antelación, asuntos propios = 5 días.
 * Admin ve todas las solicitudes con botones de aprobar/rechazar.
 */
import { Plus, Check, X, Loader2, Calendar } from 'lucide-vue-next'
import { addDays, differenceInDays, parseISO, format, isBefore } from 'date-fns'
import { es } from 'date-fns/locale'

definePageMeta({ middleware: 'auth' })

// ── Tipos ─────────────────────────────────────────────────
type TipoAusencia = 'VACACIONES' | 'ASUNTO_PROPIO' | 'BAJA'
type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA'

interface Solicitud {
  id: number
  tipo: TipoAusencia
  fechaInicio: string
  fechaFin: string
  motivo: string
  estado: EstadoSolicitud
  solicitadaEn: string
  empleadoNombre?: string    // solo visible para admin
  motivoRechazo?: string
}

// ── Estado ────────────────────────────────────────────────
const authStore  = useAuthStore()
const solicitudes = ref<Solicitud[]>([])
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
const solicitudId   = ref<number | null>(null)
const motivoRechazo = ref('')
const procesando    = ref(false)

// ── Computed ──────────────────────────────────────────────
const pendientes = computed(() => solicitudes.value.filter(s => s.estado === 'PENDIENTE'))
const aprobadas  = computed(() => solicitudes.value.filter(s => s.estado === 'APROBADA'))
const rechazadas = computed(() => solicitudes.value.filter(s => s.estado === 'RECHAZADA'))

/** Mínimo de días de antelación según el tipo de ausencia */
const diasAntelacionMinimos = computed(() =>
  form.tipo === 'VACACIONES' ? 7 : form.tipo === 'ASUNTO_PROPIO' ? 5 : 0,
)

/** Fecha mínima de inicio según tipo */
const fechaInicioMinima = computed(() => {
  const min = addDays(new Date(), diasAntelacionMinimos.value)
  return format(min, 'yyyy-MM-dd')
})

/** Duración en días de la solicitud en curso */
const duracionDias = computed(() => {
  if (!form.fechaInicio || !form.fechaFin) return 0
  return differenceInDays(parseISO(form.fechaFin), parseISO(form.fechaInicio)) + 1
})

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    // Admin ve todas; empleado solo las suyas (el backend filtra según JWT)
    const { data } = await api.get('/ausencias')
    solicitudes.value = data
  } catch { /* vacío */ }
  finally { cargando.value = false }
})

// ── Enviar solicitud ──────────────────────────────────────
async function enviarSolicitud() {
  errorForm.value = ''

  // Validar antelación mínima
  if (form.tipo !== 'BAJA' && form.fechaInicio) {
    const diasHastaInicio = differenceInDays(parseISO(form.fechaInicio), new Date())
    if (diasHastaInicio < diasAntelacionMinimos.value) {
      errorForm.value = `Las ${form.tipo === 'VACACIONES' ? 'vacaciones' : 'ausencias por asunto propio'} requieren mínimo ${diasAntelacionMinimos.value} días de antelación.`
      return
    }
  }

  if (!form.fechaInicio || !form.fechaFin) {
    errorForm.value = 'Selecciona fechas de inicio y fin.'
    return
  }

  enviando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post('/ausencias', form)
    solicitudes.value.unshift(data)
    modalAbierto.value = false
    Object.assign(form, { tipo: 'VACACIONES', fechaInicio: '', fechaFin: '', motivo: '' })
  } catch { errorForm.value = 'No se pudo enviar la solicitud.' }
  finally { enviando.value = false }
}

// ── Aprobar / rechazar (solo admin) ──────────────────────
async function aprobar(id: number) {
  procesando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/ausencias/${id}/aprobar`)
    const sol = solicitudes.value.find(s => s.id === id)
    if (sol) sol.estado = 'APROBADA'
  } catch { /* toast */ } finally { procesando.value = false }
}

async function rechazar() {
  if (!solicitudId.value) return
  procesando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/ausencias/${solicitudId.value}/rechazar`, { motivo: motivoRechazo.value })
    const sol = solicitudes.value.find(s => s.id === solicitudId.value)
    if (sol) { sol.estado = 'RECHAZADA'; sol.motivoRechazo = motivoRechazo.value }
    modalRechazo.value = false
    motivoRechazo.value = ''
  } catch { /* toast */ } finally { procesando.value = false }
}

// ── Helpers ───────────────────────────────────────────────
function badgeEstado(e: EstadoSolicitud): string {
  return {
    PENDIENTE:  'bg-amber-100 text-amber-700',
    APROBADA:   'bg-green-100 text-green-700',
    RECHAZADA:  'bg-red-100 text-red-700',
  }[e] ?? 'bg-surface-container text-on-surface-variant'
}

function labelTipo(t: TipoAusencia): string {
  return { VACACIONES: 'Vacaciones', ASUNTO_PROPIO: 'Asunto propio', BAJA: 'Baja médica' }[t]
}

function formatFecha(f: string): string {
  return format(parseISO(f), "d 'de' MMMM yyyy", { locale: es })
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div class="flex items-end justify-between">
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
        class="flex items-center gap-2 bg-primary-container text-white px-6 py-2.5 rounded-full font-bold text-sm hover:opacity-90 transition-all"
        @click="modalAbierto = true"
      >
        <Plus class="w-4 h-4" />
        Nueva solicitud
      </button>
    </div>

    <!-- ── KPI cards (solo empleado) ─────────────────────── -->
    <div v-if="!authStore.isAdmin" class="grid grid-cols-3 gap-4">
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
        v-for="s in solicitudes"
        :key="s.id"
        class="card p-5 flex items-start gap-4"
      >
        <!-- Icono -->
        <div class="w-10 h-10 bg-primary-fixed rounded-xl flex items-center justify-center flex-shrink-0">
          <Calendar class="w-5 h-5 text-primary-container" />
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <span class="font-bold text-primary text-sm">{{ labelTipo(s.tipo) }}</span>
            <span v-if="authStore.isAdmin && s.empleadoNombre" class="text-xs text-on-surface-variant">
              — {{ s.empleadoNombre }}
            </span>
          </div>
          <p class="text-sm text-on-surface-variant">
            {{ formatFecha(s.fechaInicio) }} → {{ formatFecha(s.fechaFin) }}
            <span class="ml-2 text-xs font-bold text-primary">
              ({{ differenceInDays(parseISO(s.fechaFin), parseISO(s.fechaInicio)) + 1 }} días)
            </span>
          </p>
          <p v-if="s.motivo" class="text-xs text-on-surface-variant mt-1">{{ s.motivo }}</p>
          <p v-if="s.motivoRechazo" class="text-xs text-red-600 mt-1 font-medium">
            Motivo de rechazo: {{ s.motivoRechazo }}
          </p>
          <p class="text-[10px] text-on-surface-variant/60 mt-1">
            Solicitada el {{ formatFecha(s.solicitadaEn) }}
          </p>
        </div>

        <!-- Estado + acciones admin -->
        <div class="flex items-center gap-3 flex-shrink-0">
          <span class="text-[10px] font-bold px-2.5 py-1 rounded-full" :class="badgeEstado(s.estado)">
            {{ s.estado.toLowerCase() }}
          </span>

          <!-- Botones admin solo para pendientes -->
          <template v-if="authStore.isAdmin && s.estado === 'PENDIENTE'">
            <button
              class="w-8 h-8 rounded-full bg-green-100 hover:bg-green-200 text-green-700 flex items-center justify-center transition-colors"
              title="Aprobar"
              :disabled="procesando"
              @click="aprobar(s.id)"
            >
              <Check class="w-4 h-4" />
            </button>
            <button
              class="w-8 h-8 rounded-full bg-red-100 hover:bg-red-200 text-red-700 flex items-center justify-center transition-colors"
              title="Rechazar"
              @click="solicitudId = s.id; modalRechazo = true"
            >
              <X class="w-4 h-4" />
            </button>
          </template>
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
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary">Nueva Solicitud de Ausencia</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" @click="modalAbierto = false">
              <X class="w-4 h-4" />
            </button>
          </div>

          <div class="space-y-4">

            <!-- Tipo -->
            <div>
              <label class="label">Tipo de ausencia</label>
              <select v-model="form.tipo" class="input">
                <option value="VACACIONES">Vacaciones</option>
                <option value="ASUNTO_PROPIO">Asunto propio</option>
                <option v-if="authStore.isAdmin" value="BAJA">Baja médica</option>
              </select>
              <p v-if="form.tipo !== 'BAJA'" class="text-xs text-on-surface-variant mt-1">
                Mínimo {{ diasAntelacionMinimos }} días de antelación requeridos
              </p>
            </div>

            <!-- Fechas -->
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

            <!-- Duración calculada -->
            <p v-if="duracionDias > 0" class="text-xs font-bold text-primary-container bg-primary-fixed px-3 py-1.5 rounded-lg">
              Duración: {{ duracionDias }} día{{ duracionDias !== 1 ? 's' : '' }}
            </p>

            <!-- Motivo -->
            <div>
              <label class="label">Motivo (opcional)</label>
              <textarea v-model="form.motivo" rows="2" class="input resize-none" placeholder="Descripción breve..." />
            </div>

            <!-- Error -->
            <Transition name="modal-overlay">
              <p v-if="errorForm" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {{ errorForm }}
              </p>
            </Transition>
          </div>

          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalAbierto = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="enviando" @click="enviarSolicitud">
              <Loader2 v-if="enviando" class="w-4 h-4 animate-spin" />
              <span>{{ enviando ? 'Enviando...' : 'Enviar solicitud' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Modal motivo de rechazo -->
    <Transition name="modal-overlay">
      <div
        v-if="modalRechazo"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalRechazo = false"
      >
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-xs p-6 animate-fade-scale-in">
          <h3 class="text-lg font-bold text-primary mb-4">Rechazar solicitud</h3>
          <div>
            <label class="label">Motivo del rechazo</label>
            <textarea v-model="motivoRechazo" rows="3" class="input resize-none" placeholder="Explica el motivo..." />
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
