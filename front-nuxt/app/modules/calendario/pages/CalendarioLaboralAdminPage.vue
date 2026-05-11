<script setup lang="ts">
/**
 * Calendario laboral del empleado.
 *
 * Muestra el mes con codigos de color:
 *  - Azul claro: dia laborable normal
 *  - Verde:      vacaciones aprobadas
 *  - Amarillo:   asuntos propios aprobados
 *  - Rojo:       baja medica
 *  - Gris:       dia no laborable (domingos y horario reducido sabado tarde)
 *
 * Solo accesible para admin. Recibe el id del empleado por query param.
 */
import { Loader2, ArrowLeft, Calendar as CalIcon } from 'lucide-vue-next'
import {
  startOfMonth, endOfMonth, eachDayOfInterval, format, isSameDay,
  addMonths, subMonths, parseISO, isWithinInterval, getDay,
} from 'date-fns'
import { es } from 'date-fns/locale'

definePageMeta({ middleware: ['auth', 'admin'] })

type EstadoAusencia = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | 'CANCELADA'
type TipoAusencia = 'VACACIONES' | 'ASUNTO_PROPIO' | 'BAJA'

interface Ausencia {
  id: string
  tipo: TipoAusencia
  fechaInicio: string
  fechaFin: string
  estado: EstadoAusencia
  motivo: string
}

interface Empleado {
  id: string
  nombre: string
  especialidad?: string
  horarioBase?: string
  enBaja?: boolean
  enVacaciones?: boolean
}

// ── Estado ─────────────────────────────────────────────────
const route = useRoute()
const router = useRouter()
const empleadoId = computed(() => String(route.query.empleado ?? ''))

const empleado = ref<Empleado | null>(null)
const ausencias = ref<Ausencia[]>([])
const cargando = ref(true)
const mesActual = ref(new Date())

// ── Carga ──────────────────────────────────────────────────
onMounted(async () => {
  if (!empleadoId.value) {
    router.push('/admin/empleados')
    return
  }
  await cargarDatos()
})

async function cargarDatos() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const [respEmpleado, respAusencias] = await Promise.all([
      api.get(`/v1/peluqueros/${empleadoId.value}`),
      api.get(`/v1/ausencias`, { params: { peluqueroId: empleadoId.value } }),
    ])
    empleado.value = respEmpleado.data
    ausencias.value = respAusencias.data
  } catch (err) {
    console.error('[CalendarioLaboral] Error:', err)
  } finally {
    cargando.value = false
  }
}

// ── Calendario ─────────────────────────────────────────────
const diasMes = computed(() => {
  return eachDayOfInterval({
    start: startOfMonth(mesActual.value),
    end: endOfMonth(mesActual.value),
  })
})

// Espacios vacios al inicio para alinear con el dia de la semana (lunes = 1)
const espaciosInicio = computed(() => {
  const primerDia = startOfMonth(mesActual.value)
  // getDay: domingo=0..sabado=6, queremos lunes=0..domingo=6
  const diaSemana = (getDay(primerDia) + 6) % 7
  return diaSemana
})

function tipoDelDia(dia: Date): 'VACACIONES' | 'ASUNTO_PROPIO' | 'BAJA' | 'NO_LABORABLE' | 'LABORABLE' {
  // Domingo siempre cerrado
  if (getDay(dia) === 0) return 'NO_LABORABLE'

  // Buscar ausencia aprobada que cubra este dia
  for (const ausencia of ausencias.value) {
    if (ausencia.estado !== 'APROBADA') continue
    const inicio = parseISO(ausencia.fechaInicio)
    const fin = parseISO(ausencia.fechaFin)
    if (isWithinInterval(dia, { start: inicio, end: fin })) {
      return ausencia.tipo
    }
  }

  return 'LABORABLE'
}

function clasesCelda(dia: Date): string {
  const tipo = tipoDelDia(dia)
  const base = 'aspect-square rounded-lg flex flex-col items-center justify-center text-sm font-semibold p-2 transition-colors'
  switch (tipo) {
    case 'VACACIONES':    return `${base} bg-green-100 text-green-700 ring-1 ring-green-200`
    case 'ASUNTO_PROPIO': return `${base} bg-yellow-100 text-yellow-700 ring-1 ring-yellow-200`
    case 'BAJA':          return `${base} bg-red-100 text-red-700 ring-1 ring-red-200`
    case 'NO_LABORABLE':  return `${base} bg-surface-container/50 text-on-surface-variant/40`
    default:              return `${base} bg-blue-50 text-blue-800 hover:bg-blue-100`
  }
}

function etiquetaTipo(dia: Date): string {
  const tipo = tipoDelDia(dia)
  switch (tipo) {
    case 'VACACIONES':    return 'Vac.'
    case 'ASUNTO_PROPIO': return 'A.P.'
    case 'BAJA':          return 'Baja'
    case 'NO_LABORABLE':  return ''
    default:              return ''
  }
}

function mesAnterior() { mesActual.value = subMonths(mesActual.value, 1) }
function mesSiguiente() { mesActual.value = addMonths(mesActual.value, 1) }

// Resumen de dias del mes
const resumen = computed(() => {
  const totales = { laborables: 0, vacaciones: 0, asuntosPropios: 0, baja: 0, noLaborables: 0 }
  for (const dia of diasMes.value) {
    const tipo = tipoDelDia(dia)
    if (tipo === 'LABORABLE') totales.laborables++
    else if (tipo === 'VACACIONES') totales.vacaciones++
    else if (tipo === 'ASUNTO_PROPIO') totales.asuntosPropios++
    else if (tipo === 'BAJA') totales.baja++
    else totales.noLaborables++
  }
  return totales
})
</script>

<template>
  <div class="space-y-6">

    <!-- Cabecera -->
    <div class="flex items-center justify-between gap-4">
      <button
        class="flex items-center gap-2 text-sm font-bold text-primary hover:underline"
        @click="router.back()"
      >
        <ArrowLeft class="w-4 h-4" />
        Volver
      </button>
      <h1 class="text-xl font-bold text-on-surface flex items-center gap-2">
        <CalIcon class="w-5 h-5 text-primary" />
        Calendario laboral
      </h1>
      <div class="w-20"><!-- spacer --></div>
    </div>

    <div v-if="cargando" class="min-h-[300px] flex items-center justify-center">
      <Loader2 class="w-8 h-8 animate-spin text-primary" />
    </div>

    <template v-else-if="empleado">

      <!-- Info del empleado -->
      <div class="card p-5 flex items-center gap-4">
        <div class="w-12 h-12 rounded-full bg-primary text-white flex items-center justify-center text-lg font-bold">
          {{ empleado.nombre.charAt(0).toUpperCase() }}
        </div>
        <div>
          <p class="text-lg font-bold text-on-surface">{{ empleado.nombre }}</p>
          <p class="text-sm text-on-surface-variant">
            {{ empleado.especialidad || 'Sin especialidad' }}
            <span v-if="empleado.horarioBase"> · {{ empleado.horarioBase }}</span>
          </p>
        </div>
      </div>

      <!-- Resumen del mes -->
      <div class="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div class="card p-3 text-center">
          <p class="text-xs text-on-surface-variant uppercase font-bold">Laborables</p>
          <p class="text-2xl font-extrabold text-blue-600">{{ resumen.laborables }}</p>
        </div>
        <div class="card p-3 text-center">
          <p class="text-xs text-on-surface-variant uppercase font-bold">Vacaciones</p>
          <p class="text-2xl font-extrabold text-green-600">{{ resumen.vacaciones }}</p>
        </div>
        <div class="card p-3 text-center">
          <p class="text-xs text-on-surface-variant uppercase font-bold">Asuntos propios</p>
          <p class="text-2xl font-extrabold text-yellow-600">{{ resumen.asuntosPropios }}</p>
        </div>
        <div class="card p-3 text-center">
          <p class="text-xs text-on-surface-variant uppercase font-bold">Baja</p>
          <p class="text-2xl font-extrabold text-red-600">{{ resumen.baja }}</p>
        </div>
        <div class="card p-3 text-center">
          <p class="text-xs text-on-surface-variant uppercase font-bold">Cerrado</p>
          <p class="text-2xl font-extrabold text-on-surface-variant/60">{{ resumen.noLaborables }}</p>
        </div>
      </div>

      <!-- Calendario mensual -->
      <div class="card p-5">
        <div class="flex items-center justify-between mb-4">
          <button class="px-3 py-1.5 rounded-lg hover:bg-surface-container text-sm font-bold" @click="mesAnterior">
            ← Anterior
          </button>
          <h2 class="text-lg font-bold text-on-surface capitalize">
            {{ format(mesActual, 'MMMM yyyy', { locale: es }) }}
          </h2>
          <button class="px-3 py-1.5 rounded-lg hover:bg-surface-container text-sm font-bold" @click="mesSiguiente">
            Siguiente →
          </button>
        </div>

        <!-- Cabecera dias semana -->
        <div class="grid grid-cols-7 gap-2 mb-2">
          <div
            v-for="dia in ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom']"
            :key="dia"
            class="text-center text-xs font-bold text-on-surface-variant uppercase"
          >
            {{ dia }}
          </div>
        </div>

        <!-- Celdas del mes -->
        <div class="grid grid-cols-7 gap-2">
          <div v-for="n in espaciosInicio" :key="`vacio-${n}`" class="aspect-square" />
          <div v-for="dia in diasMes" :key="dia.toISOString()" :class="clasesCelda(dia)">
            <span class="text-base">{{ format(dia, 'd') }}</span>
            <span v-if="etiquetaTipo(dia)" class="text-[9px] uppercase tracking-wide font-bold opacity-80">
              {{ etiquetaTipo(dia) }}
            </span>
          </div>
        </div>

        <!-- Leyenda -->
        <div class="mt-5 flex flex-wrap gap-3 text-xs text-on-surface-variant">
          <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-blue-50 ring-1 ring-blue-200" />Laborable</span>
          <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-green-100 ring-1 ring-green-200" />Vacaciones</span>
          <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-yellow-100 ring-1 ring-yellow-200" />Asuntos propios</span>
          <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-red-100 ring-1 ring-red-200" />Baja médica</span>
          <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-surface-container ring-1 ring-outline-variant/20" />Cerrado / Domingo</span>
        </div>
      </div>

    </template>
  </div>
</template>
