<script setup lang="ts">
/**
 * Página de Calendario — vista mensual con número de citas por día.
 * Al hacer click en un día se despliega un popover con el detalle.
 *
 * Usa FullCalendar para el renderizado del calendario.
 * El badge numérico es un evento personalizado sobre cada celda.
 */
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import interactionPlugin from '@fullcalendar/interaction'
// El locale español viene incluido en @fullcalendar/core
import esLocale from '@fullcalendar/core/locales/es'
import { X, Clock, Scissors, User, Loader2, Plus } from 'lucide-vue-next'
import type { EventClickArg, DatesSetArg } from '@fullcalendar/core'
import type { ResumenDia, CitaResumen } from '~/modules/calendario/types/calendario.types'
import { calendarioService } from '~/modules/calendario/services/calendarioService'
import { format, parseISO } from 'date-fns'
import { es } from 'date-fns/locale'

definePageMeta({ middleware: 'auth' })

// ── Estado ────────────────────────────────────────────────
const resumenMes     = ref<ResumenDia[]>([])
const cargando       = ref(false)
const mesActual      = ref(new Date())

// Popover — citas del día seleccionado
const popoverVisible   = ref(false)
const diaSeleccionado  = ref<ResumenDia | null>(null)
const popoverX         = ref(0)
const popoverY         = ref(0)

// Mini-form nueva cita
const mostrarFormNuevaCita = ref(false)
const guardandoCita        = ref(false)
const nuevaCitaHora        = ref('09:00')
const nuevaCitaClienteId   = ref('')
const nuevaCitaPeluqueroId = ref('')
const nuevaCitaServicioId  = ref('')

// ── Eventos de FullCalendar ───────────────────────────────
// Cada evento es un punto en el calendario con el número de citas del día
const eventos = computed(() =>
  resumenMes.value.map((dia) => ({
    id:    dia.fecha,
    title: String(dia.totalCitas),   // el número que muestra el badge
    date:  dia.fecha,
    extendedProps: { resumen: dia },
    // Color del badge según ocupación
    backgroundColor: colorPorOcupacion(dia.totalCitas),
    borderColor:     'transparent',
    textColor:       '#ffffff',
  })),
)

/** Devuelve el color del badge según cuántas citas hay */
function colorPorOcupacion(total: number): string {
  if (total === 0) return '#e5e7eb'
  if (total <= 2)  return '#3b82f6'   // azul — poca actividad
  if (total <= 5)  return '#f59e0b'   // ámbar — moderada
  return '#ef4444'                     // rojo — día completo
}

// ── Opciones de FullCalendar ──────────────────────────────
const opcionesCalendario = computed(() => ({
  plugins:        [dayGridPlugin, interactionPlugin],
  locale:         esLocale,
  initialView:    'dayGridMonth',
  headerToolbar: {
    left:   'prev,next today',
    center: 'title',
    right:  '',
  },
  events:       eventos.value,
  // Cuando el calendario cambia de mes, cargamos los datos del nuevo mes
  datesSet:     handleDatesSet,
  // Click en un evento (badge de número) → muestra el popover
  eventClick:   handleEventClick,
  height:       'auto',
  // Ocultamos el área de eventos (solo usamos el número del badge)
  dayMaxEvents: false,
  eventDisplay: 'block',
}))

// ── Funciones ─────────────────────────────────────────────

async function cargarMes(anio: number, mes: number) {
  cargando.value = true
  try {
    resumenMes.value = await calendarioService.getResumenMes(anio, mes)
  } catch {
    resumenMes.value = []
  } finally {
    cargando.value = false
  }
}

/** FullCalendar llama a esta función cuando el rango de fechas visibles cambia */
function handleDatesSet(arg: DatesSetArg) {
  const centro = new Date((arg.start.getTime() + arg.end.getTime()) / 2)
  cargarMes(centro.getFullYear(), centro.getMonth() + 1)
}

/** Al hacer click en un badge, muestra el popover con las citas del día */
function handleEventClick(arg: EventClickArg) {
  const resumen = arg.event.extendedProps.resumen as ResumenDia
  diaSeleccionado.value = resumen

  // Posicionamos el popover cerca del click
  const rect = arg.el.getBoundingClientRect()
  popoverX.value = Math.min(rect.left, window.innerWidth - 320)
  popoverY.value = rect.bottom + 8

  popoverVisible.value = true
}

function cerrarPopover() {
  popoverVisible.value     = false
  diaSeleccionado.value    = null
  mostrarFormNuevaCita.value = false
}

async function guardarNuevaCita() {
  if (!diaSeleccionado.value || !nuevaCitaClienteId.value || !nuevaCitaPeluqueroId.value || !nuevaCitaServicioId.value) return
  guardandoCita.value = true
  try {
    const fechaHora = `${diaSeleccionado.value.fecha}T${nuevaCitaHora.value}:00`
    await calendarioService.crearCita({
      fechaHora,
      peluquero: { id: nuevaCitaPeluqueroId.value },
      cliente:   { id: nuevaCitaClienteId.value },
      servicios: [{ id: nuevaCitaServicioId.value }],
      estado:    'PENDIENTE',
    })
    await cargarMes(
      new Date(diaSeleccionado.value.fecha).getFullYear(),
      new Date(diaSeleccionado.value.fecha).getMonth() + 1,
    )
    mostrarFormNuevaCita.value = false
    nuevaCitaClienteId.value   = ''
    nuevaCitaPeluqueroId.value = ''
    nuevaCitaServicioId.value  = ''
  } finally {
    guardandoCita.value = false
  }
}

// ── Helpers ───────────────────────────────────────────────

function formatearFechaDia(fechaStr: string): string {
  return format(parseISO(fechaStr), "EEEE d 'de' MMMM", { locale: es })
}

function claseEstadoCita(estado: string): string {
  const mapa: Record<string, string> = {
    PENDIENTE:  'text-yellow-700 bg-yellow-100',
    EN_CURSO:   'text-blue-700 bg-blue-100',
    COMPLETADA: 'text-green-700 bg-green-100',
    CANCELADO:  'text-red-700 bg-red-100',
  }
  return mapa[estado] ?? 'text-gray-700 bg-gray-100'
}

// Cargamos el mes actual al montar la página
onMounted(() => cargarMes(
  mesActual.value.getFullYear(),
  mesActual.value.getMonth() + 1,
))

// Cierra el popover al hacer click fuera de él
onMounted(() => {
  document.addEventListener('click', (e) => {
    const popoverEl = document.getElementById('popover-citas')
    if (popoverEl && !popoverEl.contains(e.target as Node)) {
      cerrarPopover()
    }
  })
})
</script>

<template>
  <div class="space-y-4">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-text-secondary">
          Haz click en cualquier día para ver el detalle de las citas
        </p>
      </div>
      <div v-if="cargando" class="flex items-center gap-2 text-sm text-text-secondary">
        <Loader2 class="w-4 h-4 animate-spin" />
        Actualizando...
      </div>
    </div>

    <!-- ── Leyenda de colores ────────────────────────────── -->
    <div class="card p-3 flex items-center gap-6 text-sm flex-wrap">
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-blue-500" />
        <span class="text-text-secondary">1–2 citas</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-amber-500" />
        <span class="text-text-secondary">3–5 citas</span>
      </div>
      <div class="flex items-center gap-2">
        <span class="w-3 h-3 rounded-full bg-red-500" />
        <span class="text-text-secondary">6+ citas</span>
      </div>
    </div>

    <!-- ── Calendario FullCalendar ───────────────────────── -->
    <div class="card p-4">
      <FullCalendar :options="opcionesCalendario" />
    </div>

  </div>

  <!-- ══════════════════════════════════════════════════════
       POPOVER — Detalle de citas del día seleccionado
       Aparece flotante cerca del día clickado
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="popoverVisible && diaSeleccionado"
        id="popover-citas"
        class="fixed z-50 bg-white rounded-card shadow-card-lg border border-surface-border w-80 animate-fade-scale-in"
        :style="{ top: popoverY + 'px', left: popoverX + 'px' }"
      >
        <!-- Cabecera del popover -->
        <div class="flex items-center justify-between px-4 py-3 border-b border-surface-border">
          <div>
            <p class="font-semibold text-text-primary capitalize">
              {{ formatearFechaDia(diaSeleccionado.fecha) }}
            </p>
            <p class="text-xs text-text-secondary mt-0.5">
              {{ diaSeleccionado.totalCitas }} cita{{ diaSeleccionado.totalCitas !== 1 ? 's' : '' }}
            </p>
          </div>
          <button class="btn-ghost py-1 px-1.5" @click="cerrarPopover">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Lista de citas del día -->
        <div class="divide-y divide-surface-border max-h-72 overflow-y-auto">
          <div
            v-for="cita in diaSeleccionado.citas"
            :key="cita.id"
            class="px-4 py-3 hover:bg-surface-muted transition-colors"
          >
            <div class="flex items-start justify-between gap-2">
              <!-- Hora -->
              <div class="flex items-center gap-1.5 text-xs text-text-muted flex-shrink-0 mt-0.5">
                <Clock class="w-3 h-3" />
                {{ cita.horaInicio }}
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium text-text-primary truncate">
                  {{ cita.clienteNombre }} {{ cita.clienteApellidos }}
                </p>
                <div class="flex items-center gap-1 mt-0.5">
                  <Scissors class="w-3 h-3 text-text-muted" />
                  <p class="text-xs text-text-secondary truncate">{{ cita.servicioNombre }}</p>
                </div>
                <div class="flex items-center gap-1 mt-0.5">
                  <User class="w-3 h-3 text-text-muted" />
                  <p class="text-xs text-text-muted truncate">{{ cita.peluqueroNombre }}</p>
                </div>
              </div>

              <!-- Estado -->
              <span
                class="text-2xs font-medium px-1.5 py-0.5 rounded-full flex-shrink-0"
                :class="claseEstadoCita(cita.estado)"
              >
                {{ cita.estado.toLowerCase().replace('_', ' ') }}
              </span>
            </div>
          </div>
        </div>

        <!-- Mini-form nueva cita -->
        <div v-if="mostrarFormNuevaCita" class="px-4 py-3 border-t border-surface-border space-y-2">
          <p class="text-xs font-semibold text-text-primary mb-1">Nueva cita</p>
          <input
            v-model="nuevaCitaHora"
            type="time"
            class="input-field w-full text-sm"
          />
          <input
            v-model="nuevaCitaClienteId"
            type="text"
            placeholder="ID cliente"
            class="input-field w-full text-sm"
          />
          <input
            v-model="nuevaCitaPeluqueroId"
            type="text"
            placeholder="ID peluquero"
            class="input-field w-full text-sm"
          />
          <input
            v-model="nuevaCitaServicioId"
            type="text"
            placeholder="ID servicio"
            class="input-field w-full text-sm"
          />
          <div class="flex gap-2">
            <button class="btn-primary flex-1 text-xs py-1.5" :disabled="guardandoCita" @click="guardarNuevaCita">
              <Loader2 v-if="guardandoCita" class="w-3 h-3 animate-spin inline mr-1" />
              Guardar
            </button>
            <button class="btn-ghost text-xs py-1.5 px-2" @click="mostrarFormNuevaCita = false">Cancelar</button>
          </div>
        </div>

        <!-- Footer con enlace a la agenda del día y botón nueva cita -->
        <div class="px-4 py-3 border-t border-surface-border flex items-center justify-between">
          <NuxtLink
            :to="`/agenda?fecha=${diaSeleccionado.fecha}`"
            class="text-sm text-primary hover:text-primary-light font-medium transition-colors"
            @click="cerrarPopover"
          >
            Ver agenda completa →
          </NuxtLink>
          <button
            v-if="!mostrarFormNuevaCita"
            class="btn-ghost text-xs flex items-center gap-1 py-1 px-2"
            @click="mostrarFormNuevaCita = true"
          >
            <Plus class="w-3 h-3" />
            Nueva cita
          </button>
        </div>

      </div>
    </Transition>
  </Teleport>
</template>

<style>
/* Estilos de FullCalendar — sobrescribimos colores con el design system */
.fc .fc-button-primary {
  background-color: #1a2d5a !important;
  border-color: #1a2d5a !important;
}
.fc .fc-button-primary:hover {
  background-color: #2a4080 !important;
}
.fc .fc-button-primary:not(:disabled):active,
.fc .fc-button-primary:not(:disabled).fc-button-active {
  background-color: #2a4080 !important;
}
.fc .fc-daygrid-day-top {
  padding: 4px 6px;
}
.fc-event {
  cursor: pointer !important;
  border-radius: 9999px !important;
  font-size: 0.7rem !important;
  font-weight: 600 !important;
  width: 24px !important;
  height: 24px !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  margin: 2px auto !important;
}
</style>
