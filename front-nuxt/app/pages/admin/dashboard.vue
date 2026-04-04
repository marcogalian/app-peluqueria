<script setup lang="ts">
/**
 * Panel de control del administrador.
 * Muestra KPIs de ingresos (día/mes/año) + gráficas + actividad reciente.
 */
import { TrendingUp, Users, Calendar, Package, Loader2 } from 'lucide-vue-next'
import { Bar, Line } from 'vue-chartjs'
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  BarElement, LineElement, PointElement,
  Title, Tooltip, Legend,
} from 'chart.js'

definePageMeta({ middleware: ['auth', 'admin'] })

// Registramos los componentes de Chart.js que vamos a usar
ChartJS.register(CategoryScale, LinearScale, BarElement, LineElement, PointElement, Title, Tooltip, Legend)

// ── Datos de KPIs (se cargan del backend) ────────────────
const kpis = ref({
  ingresosDia:   0,
  ingresosMes:   0,
  ingresosAnio:  0,
  clientesTotal: 0,
  citasHoy:      0,
  stockBajo:     0,
})
const cargando = ref(true)

// ── Datos para la gráfica de barras (ingresos por día) ───
const datosGraficaBarras = ref({
  labels: [] as string[],
  datasets: [{
    label: 'Ingresos (€)',
    data:  [] as number[],
    backgroundColor: '#1a2d5a',
    borderRadius: 6,
  }],
})

// ── Datos para la gráfica de línea (citas) ───────────────
const datosGraficaLinea = ref({
  labels: [] as string[],
  datasets: [
    {
      label: 'Completadas',
      data:  [] as number[],
      borderColor: '#22c55e',
      backgroundColor: 'rgba(34,197,94,0.1)',
      tension: 0.4,
      fill: true,
    },
    {
      label: 'Canceladas',
      data:  [] as number[],
      borderColor: '#ef4444',
      backgroundColor: 'rgba(239,68,68,0.05)',
      tension: 0.4,
      fill: true,
    },
  ],
})

const opcionesGrafica = {
  responsive: true,
  plugins: { legend: { display: false } },
  scales: { y: { beginAtZero: true } },
}

onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/finanzas/dashboard')
    kpis.value = data.kpis
    datosGraficaBarras.value.labels   = data.ingresosPorDia.labels
    datosGraficaBarras.value.datasets[0].data = data.ingresosPorDia.valores
    datosGraficaLinea.value.labels    = data.citasPorDia.labels
    datosGraficaLinea.value.datasets[0].data = data.citasPorDia.completadas
    datosGraficaLinea.value.datasets[1].data = data.citasPorDia.canceladas
  } catch {
    // Dejamos los valores a 0 si falla la carga
  } finally {
    cargando.value = false
  }
})

/** Formatea un número como euros */
function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}
</script>

<template>
  <div v-if="cargando" class="flex items-center justify-center py-16">
    <Loader2 class="w-6 h-6 animate-spin text-primary" />
  </div>

  <div v-else class="space-y-6">

    <!-- ── KPI Cards ──────────────────────────────────────── -->
    <div class="grid grid-cols-2 xl:grid-cols-3 gap-4">

      <!-- Ingresos hoy -->
      <div class="card-kpi">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-text-secondary font-medium">Ingresos hoy</p>
          <div class="w-9 h-9 rounded-lg bg-primary/10 flex items-center justify-center">
            <TrendingUp class="w-4.5 h-4.5 text-primary" />
          </div>
        </div>
        <p class="text-2xl font-bold text-text-primary">{{ formatEur(kpis.ingresosDia) }}</p>
        <p class="text-xs text-text-muted mt-1">{{ kpis.citasHoy }} cita{{ kpis.citasHoy !== 1 ? 's' : '' }} hoy</p>
      </div>

      <!-- Ingresos mes -->
      <div class="card-kpi">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-text-secondary font-medium">Ingresos este mes</p>
          <div class="w-9 h-9 rounded-lg bg-blue-50 flex items-center justify-center">
            <Calendar class="w-4.5 h-4.5 text-blue-600" />
          </div>
        </div>
        <p class="text-2xl font-bold text-text-primary">{{ formatEur(kpis.ingresosMes) }}</p>
      </div>

      <!-- Ingresos año -->
      <div class="card-kpi">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-text-secondary font-medium">Ingresos este año</p>
          <div class="w-9 h-9 rounded-lg bg-green-50 flex items-center justify-center">
            <TrendingUp class="w-4.5 h-4.5 text-green-600" />
          </div>
        </div>
        <p class="text-2xl font-bold text-text-primary">{{ formatEur(kpis.ingresosAnio) }}</p>
      </div>

      <!-- Total clientes -->
      <div class="card-kpi">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-text-secondary font-medium">Clientes activos</p>
          <div class="w-9 h-9 rounded-lg bg-vip-light flex items-center justify-center">
            <Users class="w-4.5 h-4.5 text-vip" />
          </div>
        </div>
        <p class="text-2xl font-bold text-text-primary">{{ kpis.clientesTotal }}</p>
      </div>

      <!-- Stock bajo -->
      <div class="card-kpi">
        <div class="flex items-center justify-between mb-3">
          <p class="text-sm text-text-secondary font-medium">Productos con stock bajo</p>
          <div class="w-9 h-9 rounded-lg bg-red-50 flex items-center justify-center">
            <Package class="w-4.5 h-4.5 text-red-500" />
          </div>
        </div>
        <p class="text-2xl font-bold text-text-primary">{{ kpis.stockBajo }}</p>
      </div>

    </div>

    <!-- ── Gráficas ────────────────────────────────────────── -->
    <div class="grid grid-cols-1 xl:grid-cols-2 gap-4">

      <!-- Ingresos por día (últimos 30 días) -->
      <div class="card-kpi">
        <h3 class="text-sm font-semibold text-text-primary mb-4">Ingresos diarios (últimos 30 días)</h3>
        <Bar :data="datosGraficaBarras" :options="opcionesGrafica" />
      </div>

      <!-- Citas completadas vs canceladas -->
      <div class="card-kpi">
        <h3 class="text-sm font-semibold text-text-primary mb-1">Citas completadas vs canceladas</h3>
        <div class="flex gap-4 mb-4">
          <span class="flex items-center gap-1.5 text-xs text-text-secondary">
            <span class="w-3 h-1 rounded-full bg-green-500 block" /> Completadas
          </span>
          <span class="flex items-center gap-1.5 text-xs text-text-secondary">
            <span class="w-3 h-1 rounded-full bg-red-500 block" /> Canceladas
          </span>
        </div>
        <Line :data="datosGraficaLinea" :options="{ ...opcionesGrafica, plugins: { legend: { display: false } } }" />
      </div>

    </div>

  </div>
</template>
