<script setup lang="ts">
/**
 * Resultados — análisis financiero completo.
 * KPIs de ingresos + gráfica de tendencias (Chart.js) + top servicios + top empleados.
 */
import { TrendingUp, TrendingDown, Loader2 } from 'lucide-vue-next'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  Title, Tooltip, Legend, Filler,
} from 'chart.js'

definePageMeta({ middleware: ['auth', 'admin'] })

ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Title, Tooltip, Legend, Filler)

// ── Tipos ─────────────────────────────────────────────────
type Periodo = 'semana' | 'mes' | 'trimestre' | 'anio'

interface ResultadosData {
  kpis: {
    ingresosDia: number
    ingresosSemana: number
    ingresosMes: number
    ingresosAnio: number
    ticketMedio: number
    citasCompletadas: number
    tasaCancelacion: number
    variacionMes: number   // % vs mes anterior
  }
  evolucion: { labels: string[]; valores: number[] }
  topServicios: Array<{ nombre: string; ingresos: number; citas: number }>
  topEmpleados: Array<{ nombre: string; citas: number; ingresos: number }>
}

// ── Estado ────────────────────────────────────────────────
const cargando = ref(true)
const periodo  = ref<Periodo>('mes')
const datos    = ref<ResultadosData | null>(null)

const periodos: { key: Periodo; label: string }[] = [
  { key: 'semana', label: 'Esta semana' },
  { key: 'mes',    label: 'Este mes' },
  { key: 'trimestre', label: 'Trimestre' },
  { key: 'anio',   label: 'Este año' },
]

// ── Gráfica de evolución ──────────────────────────────────
const datosGraficaLinea = computed(() => ({
  labels: datos.value?.evolucion.labels ?? [],
  datasets: [{
    label: 'Ingresos',
    data: datos.value?.evolucion.valores ?? [],
    borderColor: '#1a365d',
    backgroundColor: 'rgba(26,54,93,0.08)',
    tension: 0.4,
    fill: true,
    pointBackgroundColor: '#fff',
    pointBorderColor: '#1a365d',
    pointBorderWidth: 2,
    pointRadius: 4,
  }],
}))

const datosGraficaBarras = computed(() => ({
  labels: datos.value?.topServicios.map(s => s.nombre) ?? [],
  datasets: [{
    label: 'Ingresos (€)',
    data: datos.value?.topServicios.map(s => s.ingresos) ?? [],
    backgroundColor: '#1a365d',
    borderRadius: 8,
  }],
}))

const opcionesLinea = {
  responsive: true,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: false, grid: { color: 'rgba(0,0,0,0.04)' } } },
}

const opcionesBarras = {
  responsive: true,
  indexAxis: 'y' as const,
  plugins: { legend: { display: false } },
  scales: { x: { beginAtZero: true } },
}

// ── Carga ─────────────────────────────────────────────────
async function cargarDatos() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get(`/finanzas/resultados?periodo=${periodo.value}`)
    datos.value = data
  } catch {
    // vacío si falla
  } finally {
    cargando.value = false
  }
}

watch(periodo, cargarDatos)
onMounted(cargarDatos)

// ── Helpers ───────────────────────────────────────────────
function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera + selector de período ────────────────── -->
    <div class="flex items-end justify-between">
      <div>
        <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Rendimiento Comercial</h2>
        <p class="text-on-surface-variant text-sm">Análisis detallado de Atelier Sapphire</p>
      </div>
      <div class="flex items-center gap-2 bg-surface-container-low p-1.5 rounded-2xl">
        <button
          v-for="p in periodos"
          :key="p.key"
          class="px-4 py-1.5 text-xs font-bold rounded-xl transition-all"
          :class="periodo === p.key
            ? 'bg-primary-container text-white shadow-lg shadow-primary-container/20'
            : 'text-on-surface-variant hover:bg-surface-container'"
          @click="periodo = p.key"
        >
          {{ p.label }}
        </button>
      </div>
    </div>

    <!-- Spinner -->
    <div v-if="cargando" class="flex items-center justify-center py-20">
      <Loader2 class="w-8 h-8 animate-spin text-primary" />
    </div>

    <template v-else-if="datos">

      <!-- ── KPI Cards ──────────────────────────────────── -->
      <div class="grid grid-cols-2 xl:grid-cols-4 gap-4">

        <!-- Ingresos del período -->
        <div class="card-kpi relative overflow-hidden">
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ingresos del período</p>
          <h3 class="text-3xl font-extrabold text-primary">{{ formatEur(datos.kpis.ingresosMes) }}</h3>
          <div class="flex items-center gap-1 mt-2">
            <TrendingUp v-if="datos.kpis.variacionMes >= 0" class="w-3 h-3 text-green-600" />
            <TrendingDown v-else class="w-3 h-3 text-red-600" />
            <span class="text-xs font-bold" :class="datos.kpis.variacionMes >= 0 ? 'text-green-600' : 'text-red-600'">
              {{ datos.kpis.variacionMes >= 0 ? '+' : '' }}{{ datos.kpis.variacionMes }}% vs anterior
            </span>
          </div>
        </div>

        <!-- Ticket medio -->
        <div class="card-kpi">
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ticket Medio</p>
          <h3 class="text-3xl font-extrabold text-primary">{{ formatEur(datos.kpis.ticketMedio) }}</h3>
          <p class="text-xs text-on-surface-variant mt-2">Por cita completada</p>
        </div>

        <!-- Citas completadas -->
        <div class="card-kpi !bg-primary-container shadow-lg shadow-primary-container/20">
          <p class="text-[10px] font-bold uppercase tracking-widest text-white/70 mb-1">Citas Completadas</p>
          <h3 class="text-3xl font-extrabold text-white">{{ datos.kpis.citasCompletadas }}</h3>
          <p class="text-xs text-white/60 mt-2">Ingresos: {{ formatEur(datos.kpis.ingresosAnio) }} año</p>
        </div>

        <!-- Tasa de cancelación -->
        <div class="card-kpi">
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Tasa Cancelación</p>
          <h3 class="text-3xl font-extrabold" :class="datos.kpis.tasaCancelacion > 15 ? 'text-red-500' : 'text-primary'">
            {{ datos.kpis.tasaCancelacion }}%
          </h3>
          <p class="text-xs text-on-surface-variant mt-2">Objetivo: &lt; 15%</p>
        </div>

      </div>

      <!-- ── Gráfica evolución + Top servicios ─────────── -->
      <div class="grid grid-cols-1 xl:grid-cols-5 gap-6">

        <!-- Gráfica de línea — evolución de ingresos -->
        <div class="xl:col-span-3 card p-8">
          <div class="flex items-start justify-between mb-6">
            <div>
              <h5 class="text-lg font-bold text-primary">Evolución de Ingresos</h5>
              <p class="text-sm text-on-surface-variant">Facturación bruta del período seleccionado</p>
            </div>
          </div>
          <Line :data="datosGraficaLinea" :options="opcionesLinea" />
        </div>

        <!-- Top servicios -->
        <div class="xl:col-span-2 card p-8">
          <h5 class="text-lg font-bold text-primary mb-6">Top Servicios</h5>
          <Bar :data="datosGraficaBarras" :options="opcionesBarras" />
        </div>

      </div>

      <!-- ── Ranking empleados ──────────────────────────── -->
      <div class="card p-8">
        <h5 class="text-lg font-bold text-primary mb-6">Ranking de Empleados</h5>
        <div class="space-y-4">
          <div
            v-for="(emp, i) in datos.topEmpleados"
            :key="emp.nombre"
            class="flex items-center gap-4"
          >
            <!-- Posición -->
            <span
              class="w-7 h-7 rounded-full flex items-center justify-center text-xs font-black flex-shrink-0"
              :class="i === 0 ? 'bg-primary-container text-white' : 'bg-surface-container text-on-surface-variant'"
            >
              {{ i + 1 }}
            </span>

            <!-- Nombre -->
            <p class="flex-1 font-bold text-sm text-on-surface">{{ emp.nombre }}</p>

            <!-- Barra de progreso -->
            <div class="w-32 bg-surface-container-low rounded-full h-2 overflow-hidden">
              <div
                class="bg-primary-container h-full rounded-full transition-all"
                :style="{ width: (emp.citas / (datos!.topEmpleados[0]?.citas || 1) * 100) + '%' }"
              />
            </div>

            <!-- Citas + ingresos -->
            <div class="text-right w-28 flex-shrink-0">
              <p class="text-sm font-bold text-primary">{{ formatEur(emp.ingresos) }}</p>
              <p class="text-[10px] text-on-surface-variant">{{ emp.citas }} citas</p>
            </div>
          </div>
          <div v-if="datos.topEmpleados.length === 0" class="text-center py-6 text-sm text-on-surface-variant">
            Sin datos disponibles para este período
          </div>
        </div>
      </div>

    </template>
  </div>
</template>
