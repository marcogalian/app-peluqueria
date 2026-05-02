<script setup lang="ts">
/**
 * Página Agenda de Citas — diseño Atelier Sapphire.
 *
 * Layout: calendario mensual custom (izquierda) + panel "Detalle del Día" (derecha).
 * Al hacer click en un día con citas, el panel derecho muestra la lista del día.
 *
 * Los empleados ven nombre del cliente pero NO teléfono, email ni dirección,
 * para proteger la privacidad y evitar que se lleven clientela.
 */
import {
  User, MoreVertical, Plus, CheckCircle2, X, Loader2,
} from 'lucide-vue-next'
import {
  format, startOfMonth, endOfMonth, startOfWeek, endOfWeek,
  addDays, isSameMonth, isToday, isSameDay,
  setMonth as dfSetMonth, setDate as dfSetDate,
  getDaysInMonth, getMonth, getDate,
} from 'date-fns'
import { es } from 'date-fns/locale'
import type { CitaAgenda } from '~/modules/agenda/types/agenda.types'
import type { ResumenDia } from '~/modules/calendario/types/calendario.types'
import { agendaService } from '~/modules/agenda/services/agendaService'
import { calendarioService } from '~/modules/calendario/services/calendarioService'

definePageMeta({ middleware: 'auth' })

// ── Estado ────────────────────────────────────────────────
const mesActual       = ref(new Date())
const diaSeleccionado = ref(new Date())       // hoy por defecto
const resumenMes      = ref<ResumenDia[]>([])
const citasDia        = ref<CitaAgenda[]>([])
const cargandoCitas   = ref(false)
const citaEnEdicion   = ref<string | null>(null)
const comentarioTemp  = ref('')

// ── Selects de mes y día ───────────────────────────────────
const meses = ['Enero','Febrero','Marzo','Abril','Mayo','Junio','Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre']

const mesSeleccionado = computed({
  get: () => getMonth(mesActual.value) + 1,
  set: (val: number) => {
    const nueva = dfSetMonth(mesActual.value, val - 1)
    mesActual.value = nueva
    const maxDia = getDaysInMonth(nueva)
    const diaActual = getDate(diaSeleccionado.value)
    seleccionarDia(dfSetDate(nueva, Math.min(diaActual, maxDia)))
  },
})

const diasDelMes = computed(() => {
  const n = getDaysInMonth(mesActual.value)
  return Array.from({ length: n }, (_, i) => i + 1)
})

const numeroDia = computed({
  get: () => getDate(diaSeleccionado.value),
  set: (val: number) => seleccionarDia(dfSetDate(mesActual.value, val)),
})

// ── Modal nueva cita ───────────────────────────────────────
const modalAbierto   = ref(false)
const cargandoModal  = ref(false)
const guardandoCita  = ref(false)
const errorModal     = ref('')
const clientes       = ref<any[]>([])
const serviciosLista = ref<any[]>([])
const peluqueros     = ref<any[]>([])

const formNuevaCita = ref({
  fecha:       '',
  hora:        '10:00',
  clienteId:   '',
  servicioId:  '',
  peluqueroId: '',
})

async function abrirModal() {
  formNuevaCita.value = {
    fecha:       format(diaSeleccionado.value, 'yyyy-MM-dd'),
    hora:        '10:00',
    clienteId:   '',
    servicioId:  '',
    peluqueroId: '',
  }
  errorModal.value = ''
  modalAbierto.value = true

  if (!clientes.value.length) {
    cargandoModal.value = true
    try {
      const { api } = await import('~/infrastructure/http/api')
      const [resC, resS, resP] = await Promise.all([
        api.get('/v1/clientes'),
        api.get('/v1/servicios'),
        api.get('/peluqueros'),
      ])
      clientes.value       = resC.data
      serviciosLista.value = resS.data
      peluqueros.value     = resP.data
    } catch {
      errorModal.value = 'Error cargando datos. Comprueba que el backend esté activo.'
    } finally {
      cargandoModal.value = false
    }
  }
}

async function crearCita() {
  const { fecha, hora, clienteId, servicioId, peluqueroId } = formNuevaCita.value
  if (!clienteId || !servicioId || !peluqueroId) {
    errorModal.value = 'Completa todos los campos.'
    return
  }
  guardandoCita.value = true
  errorModal.value = ''
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.post('/citas', {
      fechaHora: `${fecha}T${hora}:00`,
      estado: 'PENDIENTE',
      cliente:   { id: clienteId },
      peluquero: { id: peluqueroId },
      servicios: [{ id: servicioId }],
    })
    modalAbierto.value = false
    await cargarResumenMes()
    if (format(diaSeleccionado.value, 'yyyy-MM-dd') === fecha) {
      await seleccionarDia(diaSeleccionado.value)
    }
  } catch {
    errorModal.value = 'Error al crear la cita. Inténtalo de nuevo.'
  } finally {
    guardandoCita.value = false
  }
}

// ── Días del calendario (grid 7 cols) ─────────────────────
const diasDelCalendario = computed(() => {
  const inicio = startOfWeek(startOfMonth(mesActual.value), { weekStartsOn: 1 }) // empieza lunes
  const fin    = endOfWeek(endOfMonth(mesActual.value), { weekStartsOn: 1 })
  const dias: Date[] = []
  let d = inicio
  while (d <= fin) {
    dias.push(d)
    d = addDays(d, 1)
  }
  return dias
})

// Encabezados de días de la semana
const diasSemana = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']

// ── Helpers ───────────────────────────────────────────────

/** Resumen de un día concreto (número de citas y lista resumida) */
function resumenDe(dia: Date): ResumenDia | undefined {
  const key = format(dia, 'yyyy-MM-dd')
  return resumenMes.value.find(r => r.fecha === key)
}

/** Texto del mes y año para el encabezado del calendario */
const textoMes = computed(() =>
  format(mesActual.value, 'MMMM yyyy', { locale: es })
    .replace(/^\w/, c => c.toUpperCase()),
)

/** Texto del día seleccionado para el panel lateral */
const textoDiaSeleccionado = computed(() => ({
  numero:     format(diaSeleccionado.value, 'd'),
  mesCorto:   format(diaSeleccionado.value, 'MMM', { locale: es }).toUpperCase(),
  diaSemana:  format(diaSeleccionado.value, 'EEEE', { locale: es }).replace(/^\w/, c => c.toUpperCase()),
}))

/** Capacidad del día en % (basada en número de citas vs máximo asumido de 8) */
const capacidadDia = computed(() => {
  const total = citasDia.value.length
  return Math.min(Math.round((total / 8) * 100), 100)
})

// ── Acciones ──────────────────────────────────────────────

async function seleccionarDia(dia: Date) {
  diaSeleccionado.value = dia
  cargandoCitas.value   = true
  try {
    citasDia.value = await agendaService.getCitasDelDia(dia)
  } finally {
    cargandoCitas.value = false
  }
}

async function cargarResumenMes() {
  try {
    resumenMes.value = await calendarioService.getResumenMes(
      mesActual.value.getFullYear(),
      mesActual.value.getMonth() + 1,
    )
  } catch {
    resumenMes.value = []
  }
}


async function iniciarEdicionComentario(cita: CitaAgenda) {
  citaEnEdicion.value = cita.id
  comentarioTemp.value = cita.comentarios
}

async function guardarComentario(cita: CitaAgenda) {
  if (comentarioTemp.value === cita.comentarios) {
    citaEnEdicion.value = null
    return
  }

  try {
    await agendaService.actualizarComentario(cita.id, comentarioTemp.value)
    cita.comentarios = comentarioTemp.value
    citaEnEdicion.value = null
  } catch (error) {
    console.error('Error al guardar comentario:', error)
  }
}

function cancelarEdicion() {
  citaEnEdicion.value = null
  comentarioTemp.value = ''
}

// Clases CSS de cada celda del calendario
function claseCelda(dia: Date): string {
  const base  = 'min-h-[70px] sm:min-h-[110px] p-1 sm:p-2 border-r border-b border-surface-container cursor-pointer transition-colors group'
  const esHoy = isToday(dia)
  const selec = isSameDay(dia, diaSeleccionado.value)
  const fuera = !isSameMonth(dia, mesActual.value)

  if (fuera)  return `${base} opacity-30 bg-surface-container-low/10`
  if (selec)  return `${base} bg-primary-container/5 ring-2 ring-inset ring-primary-container/20`
  if (esHoy)  return `${base} bg-surface-container-low/50`
  return       `${base} hover:bg-surface-container-low`
}

// Recargar mes cuando cambia mesActual
watch(mesActual, cargarResumenMes)

onMounted(async () => {
  await cargarResumenMes()
  await seleccionarDia(new Date())
})
</script>

<template>
  <!-- Layout de dos columnas: calendario izquierda + panel derecha -->
  <div class="flex flex-col lg:flex-row gap-6 h-full overflow-hidden">

    <!-- ══════════════════════════════════════════════════════
         CALENDARIO MENSUAL — columna principal
         ════════════════════════════════════════════════════ -->
    <section class="flex-1 bg-surface-container-lowest rounded-2xl overflow-hidden flex flex-col shadow-card border border-outline-variant/10">

      <!-- Cabecera del calendario -->
      <div class="px-6 py-4 flex items-center justify-between border-b border-surface-container">
        <div class="flex items-center gap-3">
          <!-- Selector de mes -->
          <select
            v-model.number="mesSeleccionado"
            class="px-3 py-1.5 rounded-xl border border-surface-container bg-surface-container-low text-sm font-semibold text-primary-container focus:outline-none focus:ring-2 focus:ring-primary-container/30 cursor-pointer"
          >
            <option v-for="(mes, i) in meses" :key="i" :value="i + 1">{{ mes }}</option>
          </select>
          <!-- Selector de día -->
          <select
            v-model.number="numeroDia"
            class="px-3 py-1.5 rounded-xl border border-surface-container bg-surface-container-low text-sm font-semibold text-primary-container focus:outline-none focus:ring-2 focus:ring-primary-container/30 cursor-pointer"
          >
            <option v-for="d in diasDelMes" :key="d" :value="d">Día {{ d }}</option>
          </select>
        </div>

        <div class="flex items-center gap-2">
          <!-- Botón "Hoy" -->
          <button
            class="px-4 py-1.5 bg-surface-container-low text-on-surface-variant rounded-full text-sm font-medium hover:bg-surface-container transition-colors"
            @click="mesActual = new Date(); seleccionarDia(new Date())"
          >
            Hoy
          </button>

          <!-- Botón nueva cita (solo admin) -->
          <button v-if="useAuthStore().isAdmin" class="btn-primary py-2" @click="abrirModal">
            <Plus class="w-4 h-4" />
            Nueva cita
          </button>
        </div>
      </div>

      <!-- Encabezados de días -->
      <div class="grid grid-cols-7 border-b border-surface-container bg-surface-container-low/30">
        <div
          v-for="dia in diasSemana"
          :key="dia"
          class="py-3 text-center text-[10px] font-bold tracking-[0.15em] text-on-surface-variant uppercase"
        >
          {{ dia }}
        </div>
      </div>

      <!-- Grid del calendario -->
      <div class="flex-1 grid grid-cols-7 overflow-auto" style="grid-template-rows: repeat(auto-fill, minmax(70px, 1fr))">
        <div
          v-for="dia in diasDelCalendario"
          :key="dia.toISOString()"
          :class="claseCelda(dia)"
          @click="seleccionarDia(dia)"
        >
          <!-- Número del día + badge de citas -->
          <div class="flex justify-between items-start">
            <span
              class="text-sm font-bold leading-none"
              :class="[
                isSameDay(dia, diaSeleccionado) ? 'w-6 h-6 rounded-full bg-primary-container text-white flex items-center justify-center text-xs' : 'text-primary',
                !isSameMonth(dia, mesActual) ? 'text-on-surface-variant' : '',
              ]"
            >
              {{ format(dia, 'd') }}
            </span>

            <!-- Badge número de citas -->
            <span
              v-if="resumenDe(dia)?.totalCitas"
              class="text-[10px] px-2 py-0.5 rounded-full font-bold"
              :class="isSameDay(dia, diaSeleccionado)
                ? 'bg-primary-container text-white'
                : 'bg-secondary-container text-on-surface-variant'"
            >
              {{ resumenDe(dia)!.totalCitas }} cita{{ resumenDe(dia)!.totalCitas !== 1 ? 's' : '' }}
            </span>
          </div>

          <!-- Previsualización de los primeros 2 clientes del día -->
          <div v-if="resumenDe(dia)" class="mt-1.5 space-y-1">
            <div
              v-for="(cita, i) in resumenDe(dia)!.citas.slice(0, 2)"
              :key="cita.id"
              class="text-[10px] px-2 py-0.5 rounded truncate border-l-2"
              :class="isSameDay(dia, diaSeleccionado)
                ? 'bg-primary-container text-white border-primary'
                : 'bg-primary/5 text-primary-container border-primary-container'"
            >
              {{ cita.clienteNombre }}
            </div>
            <!-- "+N más" si hay más de 2 -->
            <div
              v-if="resumenDe(dia)!.citas.length > 2"
              class="text-[10px] px-2 py-0.5 rounded truncate border-l-2 opacity-70"
              :class="isSameDay(dia, diaSeleccionado)
                ? 'bg-primary-container/80 text-white border-primary'
                : 'bg-primary/5 text-primary-container border-primary-container'"
            >
              +{{ resumenDe(dia)!.citas.length - 2 }} más
            </div>
          </div>

        </div>
      </div>
    </section>

    <!-- ══════════════════════════════════════════════════════
         PANEL DERECHO — Detalle del Día seleccionado
         ════════════════════════════════════════════════════ -->
    <aside class="w-full lg:w-80 bg-surface-container-low rounded-2xl p-4 sm:p-6 overflow-y-auto flex flex-col gap-6 border border-outline-variant/10">

      <!-- Etiqueta "Detalle del Día" -->
      <p class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
        Detalle del Día
      </p>

      <!-- Bloque de fecha seleccionada -->
      <div class="flex items-center gap-4">
        <div class="bg-primary-container text-white w-14 h-14 rounded-xl flex flex-col items-center justify-center font-bold flex-shrink-0">
          <span class="text-xl font-extrabold leading-none">{{ textoDiaSeleccionado.numero }}</span>
          <span class="text-[9px] uppercase tracking-wider mt-0.5">{{ textoDiaSeleccionado.mesCorto }}</span>
        </div>
        <div>
          <p class="font-extrabold text-lg text-primary leading-tight">
            {{ textoDiaSeleccionado.diaSemana }}
          </p>
          <p class="text-sm text-on-surface-variant">
            {{ citasDia.length }} cita{{ citasDia.length !== 1 ? 's' : '' }} programada{{ citasDia.length !== 1 ? 's' : '' }}
          </p>
        </div>
      </div>

      <!-- Lista de citas del día -->
      <div class="flex-1 space-y-3">

        <!-- Loading -->
        <div v-if="cargandoCitas" class="flex items-center justify-center py-8">
          <div class="w-5 h-5 border-2 border-primary-container border-t-transparent rounded-full animate-spin" />
        </div>

        <!-- Sin citas -->
        <div v-else-if="citasDia.length === 0" class="text-center py-8">
          <p class="text-sm text-on-surface-variant font-medium">Sin citas este día</p>
        </div>

        <!-- Tarjetas de citas -->
        <div
          v-for="cita in citasDia"
          v-else
          :key="cita.id"
          class="bg-surface-container-lowest p-4 rounded-xl shadow-card border border-transparent
                 hover:border-primary-container/20 transition-all cursor-pointer"
        >
          <!-- Hora + acciones -->
          <div class="flex justify-between items-start mb-2">
            <span
              class="text-xs font-bold px-2 py-1 rounded"
              :class="cita.estado === 'COMPLETADA'
                ? 'bg-primary-container text-white'
                : 'bg-primary/5 text-primary-container'"
            >
              {{ cita.horaInicio }}
            </span>
            <component
              :is="cita.estado === 'COMPLETADA' ? CheckCircle2 : MoreVertical"
              class="w-4 h-4"
              :class="cita.estado === 'COMPLETADA' ? 'text-primary-container' : 'text-on-surface-variant/30'"
            />
          </div>

          <!-- Nombre del cliente (sin datos de contacto — privacidad empleados) -->
          <h5 class="font-bold text-on-surface text-sm">
            {{ cita.clienteNombre }} {{ cita.clienteApellidos }}
            <span v-if="cita.clienteEsVip" class="ml-1 text-[10px] bg-tertiary-fixed text-amber-900 px-1.5 py-0.5 rounded-full font-bold">VIP</span>
          </h5>
          <p class="text-sm text-on-surface-variant">{{ cita.servicioNombre }}</p>

          <!-- Duración -->
          <div class="mt-3 flex items-center gap-2 pt-3 border-t border-surface-container">
            <User class="w-3 h-3 text-on-surface-variant" />
            <span class="text-[11px] text-on-surface-variant">
              {{ cita.duracionMinutos }} min
              <span v-if="cita.clienteDescuentoPorcentaje" class="ml-2 text-green-600 font-semibold">
                -{{ cita.clienteDescuentoPorcentaje }}%
              </span>
            </span>
          </div>

          <!-- Comentarios — edición inline -->
          <div class="mt-3 pt-3 border-t border-surface-container">
            <p v-if="citaEnEdicion !== cita.id"
               class="text-[11px] text-on-surface-variant cursor-pointer hover:text-primary transition-colors"
               @click="iniciarEdicionComentario(cita)">
              <span v-if="!cita.comentarios" class="italic opacity-70">+ Añadir comentario</span>
              <span v-else>{{ cita.comentarios }}</span>
            </p>
            <div v-else class="space-y-2">
              <textarea
                v-model="comentarioTemp"
                class="w-full px-2 py-1 text-[11px] border border-primary rounded bg-surface-container-lowest text-on-surface resize-none focus:outline-none focus:ring-1 focus:ring-primary"
                rows="2"
                placeholder="Añadir nota..."
              />
              <div class="flex gap-2">
                <button
                  @click="guardarComentario(cita)"
                  class="flex-1 px-2 py-1 text-[10px] font-bold bg-primary text-white rounded hover:bg-primary/90 transition-colors"
                >
                  Guardar
                </button>
                <button
                  @click="cancelarEdicion"
                  class="flex-1 px-2 py-1 text-[10px] font-bold bg-surface-container text-on-surface rounded hover:bg-surface-container/80 transition-colors"
                >
                  Cancelar
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ── Tarjeta de Capacidad del Día ────────────────── -->
      <div class="bg-primary rounded-2xl p-5 relative overflow-hidden flex-shrink-0">
        <div class="relative z-10">
          <p class="text-primary-fixed text-[10px] font-bold uppercase tracking-widest mb-1">
            Capacidad del Día
          </p>
          <p class="text-white text-3xl font-black mb-4">{{ capacidadDia }}%</p>
          <div class="w-full bg-white/20 h-1.5 rounded-full overflow-hidden">
            <div
              class="bg-white h-full rounded-full transition-all duration-500"
              :style="{ width: capacidadDia + '%' }"
            />
          </div>
        </div>
        <!-- Decoración de fondo -->
        <div class="absolute -right-3 -bottom-3 opacity-10">
          <svg class="w-20 h-20 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M16 6l2.29 2.29-4.88 4.88-4-4L2 16.59 3.41 18l6-6 4 4 6.3-6.29L22 12V6h-6z"/>
          </svg>
        </div>
      </div>

    </aside>

  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Nueva Cita
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modalAbierto"
        class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm"
        @click.self="modalAbierto = false"
      >
        <div class="bg-surface-container-lowest rounded-2xl shadow-2xl w-full max-w-md border border-outline-variant/20">

          <!-- Cabecera del modal -->
          <div class="flex items-center justify-between px-6 pt-6 pb-4 border-b border-surface-container">
            <div class="flex items-center gap-3">
              <div class="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center">
                <Plus class="w-5 h-5 text-primary-container" />
              </div>
              <h2 class="text-base font-bold text-primary">Nueva Cita</h2>
            </div>
            <button
              class="p-1.5 rounded-full hover:bg-surface-container transition-colors text-on-surface-variant"
              @click="modalAbierto = false"
            >
              <X class="w-5 h-5" />
            </button>
          </div>

          <!-- Cuerpo del modal -->
          <div class="px-6 py-5 space-y-4">

            <!-- Loader inicial -->
            <div v-if="cargandoModal" class="flex items-center justify-center py-8">
              <Loader2 class="w-6 h-6 animate-spin text-primary-container" />
            </div>

            <template v-else>
              <!-- Fecha -->
              <div>
                <label class="block text-[11px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5">Fecha</label>
                <input
                  v-model="formNuevaCita.fecha"
                  type="date"
                  class="w-full px-3 py-2 rounded-xl border border-surface-container bg-surface-container-low text-sm font-medium text-on-surface focus:outline-none focus:ring-2 focus:ring-primary-container/30"
                />
              </div>

              <!-- Hora -->
              <div>
                <label class="block text-[11px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5">Hora</label>
                <input
                  v-model="formNuevaCita.hora"
                  type="time"
                  class="w-full px-3 py-2 rounded-xl border border-surface-container bg-surface-container-low text-sm font-medium text-on-surface focus:outline-none focus:ring-2 focus:ring-primary-container/30"
                />
              </div>

              <!-- Cliente -->
              <div>
                <label class="block text-[11px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5">Cliente</label>
                <select
                  v-model="formNuevaCita.clienteId"
                  class="w-full px-3 py-2 rounded-xl border border-surface-container bg-surface-container-low text-sm font-medium text-on-surface focus:outline-none focus:ring-2 focus:ring-primary-container/30 cursor-pointer"
                >
                  <option value="" disabled>Seleccionar cliente…</option>
                  <option v-for="c in clientes" :key="c.id" :value="c.id">
                    {{ c.nombre }} {{ c.apellidos }}
                  </option>
                </select>
              </div>

              <!-- Servicio -->
              <div>
                <label class="block text-[11px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5">Servicio</label>
                <select
                  v-model="formNuevaCita.servicioId"
                  class="w-full px-3 py-2 rounded-xl border border-surface-container bg-surface-container-low text-sm font-medium text-on-surface focus:outline-none focus:ring-2 focus:ring-primary-container/30 cursor-pointer"
                >
                  <option value="" disabled>Seleccionar servicio…</option>
                  <option v-for="s in serviciosLista" :key="s.id" :value="s.id">
                    {{ s.nombre }}
                  </option>
                </select>
              </div>

              <!-- Peluquero -->
              <div>
                <label class="block text-[11px] font-bold uppercase tracking-widest text-on-surface-variant mb-1.5">Peluquero/a</label>
                <select
                  v-model="formNuevaCita.peluqueroId"
                  class="w-full px-3 py-2 rounded-xl border border-surface-container bg-surface-container-low text-sm font-medium text-on-surface focus:outline-none focus:ring-2 focus:ring-primary-container/30 cursor-pointer"
                >
                  <option value="" disabled>Seleccionar peluquero…</option>
                  <option v-for="p in peluqueros" :key="p.id" :value="p.id">
                    {{ p.nombre }} {{ p.apellidos }}
                  </option>
                </select>
              </div>

              <!-- Error -->
              <p v-if="errorModal" class="text-xs text-red-600 font-semibold bg-red-50 px-3 py-2 rounded-lg">
                {{ errorModal }}
              </p>
            </template>
          </div>

          <!-- Pie del modal -->
          <div class="flex gap-3 px-6 pb-6">
            <button
              class="flex-1 px-4 py-2.5 rounded-xl border border-surface-container bg-surface-container-low text-sm font-semibold text-on-surface-variant hover:bg-surface-container transition-colors"
              @click="modalAbierto = false"
            >
              Cancelar
            </button>
            <button
              class="flex-1 btn-primary py-2.5 flex items-center justify-center gap-2"
              :disabled="guardandoCita || cargandoModal"
              @click="crearCita"
            >
              <Loader2 v-if="guardandoCita" class="w-4 h-4 animate-spin" />
              <span>{{ guardandoCita ? 'Guardando…' : 'Crear cita' }}</span>
            </button>
          </div>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
