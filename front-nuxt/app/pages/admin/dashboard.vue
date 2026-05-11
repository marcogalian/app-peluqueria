<script setup lang="ts">
/**
 * Panel de control del administrador.
 * Rediseñado a "lo ancho" con gráfica unificada y filtros interactivos de fecha.
 */
import {
  TrendingUp, TrendingDown, Wallet, Calendar, Loader2,
  Users, UserCheck, Scissors, AlertTriangle, Package,
  Trophy, ArrowDown, User, Star, Activity, BadgeCheck,
} from 'lucide-vue-next'
import { Line } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  LineElement, PointElement, Title, Tooltip, Legend, Filler
} from 'chart.js'

ChartJS.register(CategoryScale, LinearScale, LineElement, PointElement, Title, Tooltip, Legend, Filler)

definePageMeta({ middleware: ['auth', 'admin'] })

const cargando = ref(true)

// Filtros
const fechaFiltro = ref(new Date().toISOString().substring(0, 10)) // YYYY-MM-DD
const agrupacion = ref('MES') // 'DIA', 'MES'

// Estado del backend
const stats = ref<any>({
  ingresosTotales: 0,
  gastosTotales: 0,
  balance: 0,
  citasCompletadasMes: 0,
  ticketMedio: 0,
  ingresosPorDia: {},
  gastosPorDia: {},
  empleadosStats: [],
  peluquerosActivosAhora: [],
  productosStats: {
    rankingProductos: [],
    pocoStock: [],
    productoMasVendido: '',
    ventasMasVendido: 0,
    productoMenosVendido: '',
    ventasMenosVendido: 0,
  }
})

// Variables computadas de la fecha elegida
const selectedYear = computed(() => parseInt(fechaFiltro.value.substring(0, 4)))
const selectedMonth = computed(() => parseInt(fechaFiltro.value.substring(5, 7)))
const selectedDay = computed(() => parseInt(fechaFiltro.value.substring(8, 10)))

// Formateador de fecha para los textos de la interfaz
const fechaFormateada = computed(() => {
  if (!fechaFiltro.value) return ''
  const [year, month, day] = fechaFiltro.value.split('-')
  return `${day}/${month}/${year}`
})

// Si el usuario cambia de mes/año en el calendario, pedimos nuevos datos
watch([selectedMonth, selectedYear], async ([newM, newY], [oldM, oldY]) => {
  if (newM !== oldM || newY !== oldY) {
    await fetchDashboardData()
  }
})

// Tarjetas de KPIs laterales (adaptativas al filtro DIA o MES)
const kpis = computed(() => {
  if (agrupacion.value === 'DIA') {
    const ing = stats.value.ingresosPorDia?.[selectedDay.value] || 0
    const gas = stats.value.gastosPorDia?.[selectedDay.value] || 0
    return { ingresos: ing, gastos: gas, balance: ing - gas }
  } else {
    return { 
      ingresos: stats.value.ingresosTotales || 0, 
      gastos: stats.value.gastosTotales || 0, 
      balance: stats.value.balance || 0 
    }
  }
})

// Gráfica hermosa unificada
const chartData = computed(() => {
  const daysInMonth = new Date(selectedYear.value, selectedMonth.value, 0).getDate()
  const labels = Array.from({ length: daysInMonth }, (_, i) => `${i + 1}/${selectedMonth.value}`)
  
  const ingresosData = labels.map((_, i) => stats.value.ingresosPorDia?.[i + 1] || 0)
  const gastosData = labels.map((_, i) => stats.value.gastosPorDia?.[i + 1] || 0)

  return {
    labels,
    datasets: [
      {
        label: 'Ingresos (€)',
        data: ingresosData,
        borderColor: '#10b981', // green-500
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        borderWidth: 2,
        pointBackgroundColor: '#10b981',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Gastos (€)',
        data: gastosData,
        borderColor: '#ef4444', // red-500
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        borderWidth: 2,
        pointBackgroundColor: '#ef4444',
        tension: 0.4,
        fill: true,
      }
    ]
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'top' as const },
    tooltip: { mode: 'index' as const, intersect: false }
  },
  interaction: { mode: 'nearest' as const, axis: 'x' as const, intersect: false },
  scales: { 
    y: { beginAtZero: true, border: { display: false } },
    x: { grid: { display: false } }
  },
}

async function fetchDashboardData() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get(`/finanzas/dashboard?mes=${selectedMonth.value}&anio=${selectedYear.value}`)
    if (data) {
      stats.value = data
    }
  } catch (error) {
    stats.value = {
      ingresosTotales: 0, gastosTotales: 0, balance: 0,
      citasCompletadasMes: 0, ticketMedio: 0,
      ingresosPorDia: {}, gastosPorDia: {},
      empleadosStats: [], peluquerosActivosAhora: [],
      productosStats: { rankingProductos: [], pocoStock: [], productoMasVendido: '', ventasMasVendido: 0, productoMenosVendido: '', ventasMenosVendido: 0 }
    }
  } finally {
    cargando.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})

function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n || 0)
}

// Insights financieros extra (calculados del mapa ingresosPorDia)
const insightsFinancieros = computed(() => {
  const entradas = Object.entries(stats.value.ingresosPorDia || {}) as [string, number][]
  const diasConVentas = entradas.filter(([, v]) => v > 0).length
  const mejor = entradas.reduce<[string, number]>((a, b) => (b[1] > a[1] ? b : a), ['0', 0])
  return {
    diasConVentas,
    mejorDia: mejor[0],
    citasCompletadas: stats.value.citasCompletadasMes || 0,
    ticketMedio: stats.value.ticketMedio || 0,
  }
})

// Pestaña activa del ranking de productos
const tabProductos = ref<'todos' | 'hombres' | 'mujeres'>('todos')

const rankingActivo = computed(() => {
  const ps = stats.value.productosStats
  if (!ps) return []
  if (tabProductos.value === 'hombres') return ps.rankingHombres || []
  if (tabProductos.value === 'mujeres') return ps.rankingMujeres || []
  return ps.rankingProductos || []
})

// Máximo de consumidos del ranking activo para calcular barras proporcionales
const maxConsumidos = computed(() =>
  rankingActivo.value.reduce((m: number, p: any) => Math.max(m, p.consumidos), 1)
)

// Colores por categoría de producto
const colorCategoria: Record<string, string> = {
  CHAMPU:       'bg-blue-100 text-blue-700',
  ACONDICIONADOR: 'bg-cyan-100 text-cyan-700',
  COLORACION:   'bg-purple-100 text-purple-700',
  CERA:         'bg-amber-100 text-amber-700',
  HERRAMIENTA:  'bg-slate-100 text-slate-700',
  OTRO:         'bg-gray-100 text-gray-600',
}
function badgeCategoria(cat: string) {
  return colorCategoria[cat] ?? colorCategoria['OTRO']
}

// Resumen rápido del equipo
const resumenEquipo = computed(() => {
  const lista: any[] = stats.value.empleadosStats || []
  return {
    total:        lista.length,
    enBaja:       lista.filter(e => e.diasBaja > 0).length,
    enVacaciones: lista.filter(e => e.vacacionesEsteMes > 0).length,
    activos:      (stats.value.peluquerosActivosAhora || []).length,
  }
})

// % de stock restante para la barra de alerta (max 100%)
function pctStock(prod: { stock: number; stockMinimo: number }): number {
  if (!prod.stockMinimo) return 100
  return Math.min(Math.round((prod.stock / prod.stockMinimo) * 100), 100)
}
</script>

<template>
  <div class="space-y-6">

    <!-- ══════════════════════════════════════════════════════
         FILTROS SUPERIORES
         ════════════════════════════════════════════════════ -->
    <div class="card p-5 flex flex-col md:flex-row items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
          <Activity class="w-5 h-5 text-primary" />
        </div>
        <div>
          <h1 class="text-xl font-bold text-text-primary">Panel de Control</h1>
          <p class="text-sm text-text-secondary">Finanzas · Personal · Inventario</p>
        </div>
      </div>
      <div class="flex items-center gap-3 w-full md:w-auto">
        <input type="date" v-model="fechaFiltro" class="input bg-white w-full md:w-44" />
        <select v-model="agrupacion" class="select-field bg-white w-full md:w-44 cursor-pointer">
          <option value="DIA">Ver datos del día</option>
          <option value="MES">Ver datos del mes</option>
        </select>
      </div>
    </div>

    <!-- LOADER -->
    <div v-if="cargando" class="min-h-[300px] flex items-center justify-center">
      <Loader2 class="w-8 h-8 animate-spin text-primary" />
    </div>

    <template v-else>

      <!-- ══════════════════════════════════════════════════════
           SECCIÓN 1 · FINANCIERO  (KPIs arriba, gráfica debajo)
           ════════════════════════════════════════════════════ -->
      <div class="flex flex-col gap-6">

        <!-- Gráfica principal -->
        <div class="card p-6 w-full flex flex-col min-h-[360px]">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="text-base font-bold text-text-primary">Evolución mensual</h3>
              <p class="text-xs text-text-secondary mt-0.5">Ingresos vs gastos diarios — {{ selectedYear }}</p>
            </div>
            <div class="flex items-center gap-3 text-xs">
              <span class="flex items-center gap-1.5 text-text-secondary">
                <span class="w-3 h-1 rounded-full bg-emerald-500 block" /> Ingresos
              </span>
              <span class="flex items-center gap-1.5 text-text-secondary">
                <span class="w-3 h-1 rounded-full bg-red-500 block" /> Gastos
              </span>
            </div>
          </div>
          <div class="flex-1 relative">
            <Line :data="chartData" :options="chartOptions" />
          </div>

          <!-- Insights bajo la gráfica -->
          <div class="grid grid-cols-4 gap-3 mt-5 pt-4 border-t border-outline-variant/20">
            <div class="text-center">
              <p class="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1">Días activos</p>
              <p class="text-xl font-black text-text-primary">{{ insightsFinancieros.diasConVentas }}</p>
            </div>
            <div class="text-center border-l border-outline-variant/20">
              <p class="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1">Mejor día</p>
              <p class="text-xl font-black text-text-primary">
                {{ insightsFinancieros.mejorDia !== '0' ? `Día ${insightsFinancieros.mejorDia}` : '—' }}
              </p>
            </div>
            <div class="text-center border-l border-outline-variant/20">
              <p class="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1">Citas facturadas</p>
              <p class="text-xl font-black text-text-primary">{{ insightsFinancieros.citasCompletadas }}</p>
            </div>
            <div class="text-center border-l border-outline-variant/20">
              <p class="text-[10px] font-bold uppercase tracking-widest text-text-muted mb-1">Ticket medio</p>
              <p class="text-xl font-black text-text-primary">{{ formatEur(insightsFinancieros.ticketMedio) }}</p>
            </div>
          </div>
        </div>

        <!-- KPIs financieros (fila de 3 abajo, full width) -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 w-full">

          <!-- Ingresos -->
          <div class="card-kpi flex items-center gap-4">
            <div class="w-11 h-11 rounded-xl bg-emerald-100 flex items-center justify-center flex-shrink-0">
              <TrendingUp class="w-5 h-5 text-emerald-600" />
            </div>
            <div class="min-w-0">
              <p class="text-xs text-text-secondary font-medium truncate">
                Ingresos {{ agrupacion === 'DIA' ? fechaFormateada : 'del mes' }}
              </p>
              <p class="text-2xl font-black text-emerald-600">{{ formatEur(kpis.ingresos) }}</p>
            </div>
          </div>

          <!-- Gastos -->
          <div class="card-kpi flex items-center gap-4">
            <div class="w-11 h-11 rounded-xl bg-red-100 flex items-center justify-center flex-shrink-0">
              <TrendingDown class="w-5 h-5 text-red-500" />
            </div>
            <div class="min-w-0">
              <p class="text-xs text-text-secondary font-medium truncate">
                Gastos {{ agrupacion === 'DIA' ? fechaFormateada : 'del mes' }}
              </p>
              <p class="text-2xl font-black text-red-500">{{ formatEur(kpis.gastos) }}</p>
            </div>
          </div>

          <!-- Balance neto — card destacada -->
          <div class="flex-1 card p-5 bg-gradient-to-br from-primary to-[#1a3a6b] border-none text-white flex flex-col justify-between">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-white/20 flex items-center justify-center">
                <Wallet class="w-5 h-5 text-white" />
              </div>
              <p class="text-sm text-white/80 font-medium">
                Balance {{ agrupacion === 'DIA' ? 'del día' : 'mensual' }}
              </p>
            </div>
            <div>
              <p class="text-4xl font-black text-white mt-3 mb-2">{{ formatEur(kpis.balance) }}</p>
              <span
                class="text-xs px-2.5 py-1 rounded-full font-semibold"
                :class="kpis.balance > 0
                  ? 'bg-emerald-400/25 text-emerald-100 border border-emerald-400/40'
                  : kpis.balance < 0
                    ? 'bg-red-400/25 text-red-100 border border-red-400/40'
                    : 'bg-white/20 text-white/70 border border-white/30'"
              >
                {{ kpis.balance > 0 ? 'Rentable' : kpis.balance < 0 ? 'En pérdidas' : 'Neutro' }}
              </span>
            </div>
          </div>

        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════
           SEPARADOR · PERSONAL
           ════════════════════════════════════════════════════ -->
      <div class="relative py-2">
        <div class="absolute inset-0 flex items-center"><div class="w-full border-t border-outline-variant/20" /></div>
        <div class="relative flex justify-center">
          <span class="bg-surface px-4 text-[10px] font-black uppercase tracking-[0.2em] text-text-muted">
            Gestión de Personal
          </span>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════
           SECCIÓN 2 · EMPLEADOS
           ════════════════════════════════════════════════════ -->

      <!-- Pills de resumen rápido -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="card-kpi flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-primary/10 flex items-center justify-center flex-shrink-0">
            <Users class="w-4.5 h-4.5 text-primary" />
          </div>
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wider text-text-muted">Total equipo</p>
            <p class="text-2xl font-black text-text-primary">{{ resumenEquipo.total }}</p>
          </div>
        </div>
        <div class="card-kpi flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-green-100 flex items-center justify-center flex-shrink-0">
            <UserCheck class="w-4.5 h-4.5 text-green-600" />
          </div>
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wider text-text-muted">Activos ahora</p>
            <p class="text-2xl font-black text-green-600">{{ resumenEquipo.activos }}</p>
          </div>
        </div>
        <div class="card-kpi flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-purple-100 flex items-center justify-center flex-shrink-0">
            <Calendar class="w-4.5 h-4.5 text-purple-600" />
          </div>
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wider text-text-muted">En vacaciones</p>
            <p class="text-2xl font-black text-purple-600">{{ resumenEquipo.enVacaciones }}</p>
          </div>
        </div>
        <div class="card-kpi flex items-center gap-3">
          <div class="w-9 h-9 rounded-xl bg-red-100 flex items-center justify-center flex-shrink-0">
            <AlertTriangle class="w-4.5 h-4.5 text-red-500" />
          </div>
          <div>
            <p class="text-[10px] font-bold uppercase tracking-wider text-text-muted">En baja</p>
            <p class="text-2xl font-black text-red-500">{{ resumenEquipo.enBaja }}</p>
          </div>
        </div>
      </div>

      <!-- Tabla + activos -->
      <div class="grid grid-cols-1 xl:grid-cols-3 gap-6 pb-2">

        <!-- Tabla de ausencias -->
        <div class="card p-6 xl:col-span-2">
          <div class="flex items-center gap-3 mb-5">
            <div class="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center">
              <Users class="w-5 h-5 text-blue-600" />
            </div>
            <div>
              <h3 class="text-base font-bold text-text-primary">Ausencias y Vacaciones</h3>
              <p class="text-xs text-text-secondary">Control de días por empleado este mes</p>
            </div>
          </div>
          <div class="overflow-x-auto rounded-xl border border-outline-variant/20">
            <table class="w-full text-left border-collapse bg-white" aria-label="Ausencias y vacaciones del equipo">
              <caption class="sr-only">Control de días de ausencia por empleado este mes</caption>
              <thead class="bg-surface-container-low">
                <tr class="border-b border-outline-variant/20">
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider">Empleado</th>
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider text-center">Citas mes</th>
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider text-center">Vacaciones</th>
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider text-center">Pendientes</th>
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider text-center">A. Propios</th>
                  <th scope="col" class="py-3 px-4 text-[10px] font-bold text-text-secondary uppercase tracking-wider text-center">Baja</th>
                </tr>
              </thead>
              <tbody class="text-sm divide-y divide-outline-variant/10">
                <tr
                  v-for="(emp, i) in stats.empleadosStats" :key="emp.nombre"
                  :class="[i % 2 === 0 ? 'bg-white' : 'bg-surface-container-lowest', 'hover:bg-primary/5 transition-colors']"
                >
                  <td class="py-3 px-4">
                    <div class="flex items-center gap-2.5">
                      <div class="w-7 h-7 rounded-full bg-primary/10 text-primary text-xs font-bold flex items-center justify-center flex-shrink-0">
                        {{ emp.nombre.charAt(0).toUpperCase() }}
                      </div>
                      <span class="font-semibold text-text-primary">{{ emp.nombre }}</span>
                    </div>
                  </td>
                  <td class="py-3 px-4 text-center">
                    <span class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-bold"
                          :class="emp.citasMes > 0 ? 'bg-primary/10 text-primary' : 'text-text-muted/50'">
                      {{ emp.citasMes > 0 ? emp.citasMes : '—' }}
                    </span>
                  </td>
                  <td class="py-3 px-4 text-center">
                    <span v-if="emp.vacacionesEsteMes > 0" class="badge-vacaciones">{{ emp.vacacionesEsteMes }}d</span>
                    <span v-else class="text-text-muted/50">—</span>
                  </td>
                  <td class="py-3 px-4 text-center">
                    <span
                      :class="emp.vacacionesPendientes < 5
                        ? 'px-2 py-0.5 rounded-full text-xs font-semibold bg-orange-100 text-orange-700'
                        : 'text-text-secondary text-xs font-medium'"
                    >{{ emp.vacacionesPendientes }}d</span>
                  </td>
                  <td class="py-3 px-4 text-center">
                    <span v-if="emp.asuntosPropios > 0" class="px-2 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-700">
                      {{ emp.asuntosPropios }}d
                    </span>
                    <span v-else class="text-text-muted/50">—</span>
                  </td>
                  <td class="py-3 px-4 text-center">
                    <span v-if="emp.diasBaja > 0" class="badge-baja">{{ emp.diasBaja }}d</span>
                    <span v-else class="text-text-muted/50">—</span>
                  </td>
                </tr>
                <tr v-if="!stats.empleadosStats?.length">
                  <td colspan="6" class="py-10 text-center">
                    <Users class="w-8 h-8 text-outline-variant/40 mx-auto mb-2" />
                    <p class="text-sm text-text-muted">Sin datos de empleados disponibles</p>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- Activos en este momento -->
        <div class="card p-6 flex flex-col">
          <div class="flex items-center gap-3 mb-5">
            <div class="w-10 h-10 rounded-xl bg-green-50 flex items-center justify-center">
              <UserCheck class="w-5 h-5 text-green-600" />
            </div>
            <div>
              <h3 class="text-base font-bold text-text-primary">En turno ahora</h3>
              <p class="text-xs text-text-secondary">Atendiendo clientes</p>
            </div>
          </div>

          <div v-if="stats.peluquerosActivosAhora?.length" class="space-y-3 flex-1">
            <div
              v-for="nombre in stats.peluquerosActivosAhora" :key="nombre"
              class="flex items-center gap-3 p-3 rounded-xl border border-green-100 bg-green-50/50"
            >
              <div class="relative w-10 h-10 flex-shrink-0">
                <div class="w-full h-full rounded-full bg-green-100 text-green-700 flex items-center justify-center font-bold text-sm">
                  {{ nombre.charAt(0).toUpperCase() }}
                </div>
                <span class="absolute bottom-0 right-0 w-2.5 h-2.5 bg-green-500 border-2 border-white rounded-full" />
              </div>
              <div>
                <p class="text-sm font-semibold text-text-primary">{{ nombre }}</p>
                <p class="text-xs text-green-600 flex items-center gap-1 mt-0.5">
                  <Scissors class="w-3 h-3" /> Atendiendo cliente
                </p>
              </div>
            </div>
          </div>

          <div v-else class="flex-1 flex flex-col items-center justify-center py-6 text-center">
            <div class="w-14 h-14 bg-surface-container rounded-full flex items-center justify-center mb-3">
              <Users class="w-6 h-6 text-text-muted" />
            </div>
            <p class="text-sm font-medium text-text-secondary">Sin citas activas ahora mismo</p>
            <p class="text-xs text-text-muted mt-1">Aparecerán aquí al iniciar una cita</p>
          </div>
        </div>

      </div>

      <!-- ══════════════════════════════════════════════════════
           SEPARADOR · INVENTARIO
           ════════════════════════════════════════════════════ -->
      <div class="relative py-2">
        <div class="absolute inset-0 flex items-center"><div class="w-full border-t border-outline-variant/20" /></div>
        <div class="relative flex justify-center">
          <span class="bg-surface px-4 text-[10px] font-black uppercase tracking-[0.2em] text-text-muted">
            Inventario y Productos
          </span>
        </div>
      </div>

      <!-- ══════════════════════════════════════════════════════
           SECCIÓN 3 · PRODUCTOS
           ════════════════════════════════════════════════════ -->
      <div v-if="stats.productosStats" class="grid grid-cols-1 xl:grid-cols-3 gap-6 pb-8">

        <!-- ── Ranking de productos con pestañas (2 cols) ─── -->
        <div class="card p-6 xl:col-span-2 flex flex-col">

          <!-- Cabecera + totales de ganancia por género -->
          <div class="flex items-start justify-between mb-4 gap-4">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center">
                <Trophy class="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <h3 class="text-base font-bold text-text-primary">Ranking de Productos</h3>
                <p class="text-xs text-text-secondary">Consumo estimado · ganancia neta por producto</p>
              </div>
            </div>
            <!-- Totales ganancia hombres / mujeres -->
            <div class="flex gap-3 flex-shrink-0">
              <div class="text-center px-3 py-1.5 rounded-xl bg-blue-50 border border-blue-100">
                <p class="text-[10px] font-bold uppercase tracking-wider text-blue-600">♂ Hombres</p>
                <p class="text-sm font-black text-blue-700">{{ formatEur(stats.productosStats.gananciasHombres || 0) }}</p>
              </div>
              <div class="text-center px-3 py-1.5 rounded-xl bg-pink-50 border border-pink-100">
                <p class="text-[10px] font-bold uppercase tracking-wider text-pink-600">♀ Mujeres</p>
                <p class="text-sm font-black text-pink-700">{{ formatEur(stats.productosStats.gananciasMujeres || 0) }}</p>
              </div>
            </div>
          </div>

          <!-- Pestañas Todos / Hombres / Mujeres -->
          <div class="flex gap-1 mb-4 bg-surface-container-low p-1 rounded-xl w-fit">
            <button
              v-for="tab in [{ key: 'todos', label: 'Todos' }, { key: 'hombres', label: '♂ Hombres' }, { key: 'mujeres', label: '♀ Mujeres' }]"
              :key="tab.key"
              class="px-4 py-1.5 rounded-lg text-xs font-bold transition-all"
              :class="tabProductos === tab.key
                ? 'bg-white shadow-sm text-primary'
                : 'text-text-muted hover:text-text-secondary'"
              @click="tabProductos = tab.key as any"
            >
              {{ tab.label }}
              <span class="ml-1 opacity-60">
                ({{ tab.key === 'todos'
                  ? stats.productosStats.rankingProductos?.length || 0
                  : tab.key === 'hombres'
                    ? stats.productosStats.rankingHombres?.length || 0
                    : stats.productosStats.rankingMujeres?.length || 0 }})
              </span>
            </button>
          </div>

          <!-- Lista vacía -->
          <div v-if="!rankingActivo.length" class="flex-1 flex flex-col items-center justify-center py-10 text-center">
            <Package class="w-10 h-10 text-outline-variant/40 mx-auto mb-3" />
            <p class="text-sm text-text-muted">Sin productos en esta categoría</p>
          </div>

          <!-- Tabla de ranking -->
          <div v-else class="overflow-y-auto max-h-[380px] pr-1">
            <!-- Cabecera de columnas -->
            <div class="grid grid-cols-[28px_1fr_80px_90px] gap-3 px-3 mb-2">
              <span />
              <span class="text-[10px] font-bold uppercase tracking-wider text-text-muted">Producto</span>
              <span class="text-[10px] font-bold uppercase tracking-wider text-text-muted text-right">Consumidos</span>
              <span class="text-[10px] font-bold uppercase tracking-wider text-text-muted text-right">Ganancia est.</span>
            </div>

            <div class="space-y-1.5">
              <div
                v-for="(prod, i) in rankingActivo" :key="prod.nombre"
                class="grid grid-cols-[28px_1fr_80px_90px] gap-3 items-center p-3 rounded-xl border transition-colors"
                :class="prod.bajoMinimo
                  ? 'border-red-200/60 bg-red-50/30 hover:bg-red-50/60'
                  : 'border-outline-variant/15 bg-white hover:bg-surface-container-lowest'"
              >
                <!-- Posición -->
                <div
                  class="w-7 h-7 rounded-full text-xs font-black flex items-center justify-center"
                  :class="i === 0 ? 'bg-amber-400 text-white' : i === 1 ? 'bg-slate-300 text-white' : i === 2 ? 'bg-amber-700/60 text-white' : 'bg-surface-container text-text-muted'"
                >{{ i + 1 }}</div>

                <!-- Nombre + barra + badges -->
                <div class="min-w-0">
                  <div class="flex items-center gap-1.5 flex-wrap mb-1">
                    <p class="text-sm font-semibold text-text-primary truncate">{{ prod.nombre }}</p>
                    <span class="text-[10px] font-bold px-1.5 py-0.5 rounded" :class="badgeCategoria(prod.categoria)">
                      {{ prod.categoria }}
                    </span>
                    <span v-if="prod.bajoMinimo" class="text-[10px] font-bold px-1.5 py-0.5 rounded bg-red-100 text-red-700">⚠</span>
                  </div>
                  <div class="flex items-center gap-1.5">
                    <div class="flex-1 h-1 rounded-full bg-surface-container">
                      <div
                        class="h-full rounded-full transition-all duration-500"
                        :class="i === 0 ? 'bg-amber-400' : tabProductos === 'hombres' ? 'bg-blue-400' : tabProductos === 'mujeres' ? 'bg-pink-400' : 'bg-primary/50'"
                        :style="{ width: maxConsumidos > 0 ? `${Math.round((prod.consumidos / maxConsumidos) * 100)}%` : '2%' }"
                      />
                    </div>
                    <span class="text-[10px] text-text-muted flex-shrink-0">
                      stock: <strong :class="prod.bajoMinimo ? 'text-red-600' : 'text-text-primary'">{{ prod.stock }}</strong>/{{ prod.stockMinimo }}
                    </span>
                  </div>
                </div>

                <!-- Consumidos -->
                <div class="text-right">
                  <p class="text-sm font-black text-text-primary">{{ prod.consumidos }}</p>
                  <p class="text-[10px] text-text-muted">uds.</p>
                </div>

                <!-- Ganancia estimada -->
                <div class="text-right">
                  <p class="text-sm font-bold text-emerald-600">{{ formatEur(prod.gananciaEstimada || 0) }}</p>
                  <p class="text-[10px] text-text-muted">estimado</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- ── Alertas de stock ────────────────────────────── -->
        <div class="card p-6 flex flex-col">
          <div class="flex items-center gap-3 mb-5">
            <div class="w-10 h-10 rounded-xl flex items-center justify-center"
                 :class="stats.productosStats.pocoStock?.length ? 'bg-red-100' : 'bg-green-100'">
              <AlertTriangle v-if="stats.productosStats.pocoStock?.length" class="w-5 h-5 text-red-600" />
              <Package v-else class="w-5 h-5 text-green-600" />
            </div>
            <div>
              <h3 class="text-base font-bold text-text-primary">Alertas de Stock</h3>
              <p class="text-xs text-text-secondary">Requieren reposición urgente</p>
            </div>
          </div>

          <!-- Número de alertas -->
          <div v-if="stats.productosStats.pocoStock?.length" class="mb-4 p-3 rounded-xl bg-red-50 border border-red-200/60">
            <p class="text-2xl font-black text-red-600">{{ stats.productosStats.pocoStock.length }}</p>
            <p class="text-xs text-red-500 font-medium">producto{{ stats.productosStats.pocoStock.length !== 1 ? 's' : '' }} bajo mínimo</p>
          </div>

          <div v-if="stats.productosStats.pocoStock?.length" class="space-y-3 overflow-y-auto flex-1 max-h-[320px] pr-1">
            <div
              v-for="prod in stats.productosStats.pocoStock" :key="prod.nombre"
              class="p-3 rounded-xl border border-red-200/70 bg-white hover:bg-red-50/30 transition-colors"
            >
              <div class="flex items-center justify-between mb-2">
                <p class="text-sm font-semibold text-text-primary truncate pr-2">{{ prod.nombre }}</p>
                <span class="text-sm font-black text-red-600 flex-shrink-0">{{ prod.stock }} ud.</span>
              </div>
              <div class="flex items-center gap-2">
                <div class="flex-1 h-1.5 rounded-full bg-red-100">
                  <div
                    class="h-full rounded-full transition-all duration-500"
                    :class="pctStock(prod) < 50 ? 'bg-red-500' : 'bg-orange-400'"
                    :style="{ width: `${pctStock(prod)}%` }"
                  />
                </div>
                <span class="text-[10px] text-text-muted font-medium flex-shrink-0">Mín {{ prod.stockMinimo }}</span>
              </div>
            </div>
          </div>

          <div v-else class="flex-1 flex flex-col items-center justify-center py-8 text-center">
            <div class="w-14 h-14 bg-green-100 rounded-full flex items-center justify-center mb-3">
              <Package class="w-6 h-6 text-green-600" />
            </div>
            <p class="font-bold text-green-700 text-sm">Inventario saludable</p>
            <p class="text-xs text-text-muted mt-1">Todos los productos sobre mínimos</p>
          </div>
        </div>

      </div>

    </template>
  </div>
</template>
