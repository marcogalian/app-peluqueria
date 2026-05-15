<script setup lang="ts">
/**
 * GastosPanel — registro y consulta de gastos operativos mes a mes.
 * Nóminas se pre-cargan desde el listado de empleados.
 */
import { Plus, Pencil, Trash2, ChevronLeft, ChevronRight, Loader2 } from 'lucide-vue-next'
import type { Gasto, CategoriaGasto } from '../types/gasto.types'
import { CATEGORIAS, CATEGORIAS_LABEL, CATEGORIAS_ICONO } from '../types/gasto.types'

// ── Estado mes/año ────────────────────────────────────────
const hoy = new Date()
const mesActual  = ref(hoy.getMonth() + 1)
const anioActual = ref(hoy.getFullYear())

const MESES = ['Enero','Febrero','Marzo','Abril','Mayo','Junio',
               'Julio','Agosto','Septiembre','Octubre','Noviembre','Diciembre']

const labelMes = computed(() => `${MESES[mesActual.value - 1]} ${anioActual.value}`)

function mesSiguiente() {
  if (mesActual.value === 12) { mesActual.value = 1; anioActual.value++ }
  else mesActual.value++
}
function mesAnterior() {
  if (mesActual.value === 1) { mesActual.value = 12; anioActual.value-- }
  else mesActual.value--
}

// ── Datos ─────────────────────────────────────────────────
const gastos     = ref<Gasto[]>([])
const empleados  = ref<{ id: string; nombre: string }[]>([])
const cargando   = ref(false)

async function cargarGastos() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/finanzas/gastos', {
      params: { mes: mesActual.value, anio: anioActual.value },
    })
    gastos.value = data
  } catch {
    gastos.value = []
  } finally {
    cargando.value = false
  }
}

async function cargarEmpleados() {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/peluqueros')
    empleados.value = data
  } catch {
    empleados.value = []
  }
}

watch([mesActual, anioActual], cargarGastos)
onMounted(() => { cargarGastos(); cargarEmpleados() })

// ── Resumen por categoría ─────────────────────────────────
const totalPorCategoria = computed(() => {
  const mapa: Record<CategoriaGasto, number> = {} as Record<CategoriaGasto, number>
  CATEGORIAS.forEach(c => { mapa[c] = 0 })
  gastos.value.forEach(g => { mapa[g.categoria] = (mapa[g.categoria] ?? 0) + Number(g.importe) })
  return mapa
})

const totalMes = computed(() =>
  gastos.value.reduce((s, g) => s + Number(g.importe), 0)
)

const gastosPorCategoria = computed(() => {
  const mapa: Record<CategoriaGasto, Gasto[]> = {} as Record<CategoriaGasto, Gasto[]>
  CATEGORIAS.forEach(c => { mapa[c] = [] })
  gastos.value.forEach(g => { mapa[g.categoria].push(g) })
  return mapa
})

// ── Modal añadir/editar ───────────────────────────────────
const modalAbierto    = ref(false)
const modoEdicion     = ref(false)
const guardando       = ref(false)

interface FormGasto {
  id?: string
  concepto: string
  importe: string
  fecha: string
  categoria: CategoriaGasto
  empleadoSeleccionado: string
}

const primerDiaMes = computed(() => {
  const m = String(mesActual.value).padStart(2, '0')
  return `${anioActual.value}-${m}-01`
})

function formVacio(): FormGasto {
  return { concepto: '', importe: '', fecha: primerDiaMes.value, categoria: 'OTROS', empleadoSeleccionado: '' }
}

const form = ref<FormGasto>(formVacio())

function abrirCrear() {
  form.value = formVacio()
  modoEdicion.value = false
  modalAbierto.value = true
}

function abrirEditar(gasto: Gasto) {
  form.value = {
    id: gasto.id,
    concepto: gasto.concepto,
    importe: String(gasto.importe),
    fecha: gasto.fecha,
    categoria: gasto.categoria,
    empleadoSeleccionado: '',
  }
  modoEdicion.value = true
  modalAbierto.value = true
}

function cerrarModal() { modalAbierto.value = false }

// Al cambiar categoría a SALARIOS: limpiar concepto para que el select lo maneje
watch(() => form.value.categoria, (cat) => {
  if (cat !== 'SALARIOS') form.value.empleadoSeleccionado = ''
})

// Al seleccionar empleado: auto-rellenar concepto
watch(() => form.value.empleadoSeleccionado, (empId) => {
  if (!empId) return
  const emp = empleados.value.find(e => e.id === empId)
  if (emp) form.value.concepto = `Nómina bruta ${emp.nombre}`
})

async function guardarGasto() {
  if (!form.value.concepto || !form.value.importe) return
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const payload: Gasto = {
      concepto:  form.value.concepto,
      importe:   parseFloat(form.value.importe),
      fecha:     form.value.fecha,
      categoria: form.value.categoria,
    }
    if (modoEdicion.value && form.value.id) {
      await api.put(`/finanzas/gastos/${form.value.id}`, payload)
    } else {
      await api.post('/finanzas/gastos', payload)
    }
    cerrarModal()
    await cargarGastos()
  } catch {
    // mantener modal abierto
  } finally {
    guardando.value = false
  }
}

// ── Modal confirmación eliminar ───────────────────────────
const confirmarEliminar   = ref(false)
const gastoAEliminar      = ref<Gasto | null>(null)
const eliminando          = ref(false)

function pedirConfirmacion(gasto: Gasto) {
  gastoAEliminar.value = gasto
  confirmarEliminar.value = true
}

async function ejecutarEliminar() {
  if (!gastoAEliminar.value?.id) return
  eliminando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/finanzas/gastos/${gastoAEliminar.value.id}`)
    confirmarEliminar.value = false
    gastoAEliminar.value = null
    await cargarGastos()
  } catch {
    // mantener modal
  } finally {
    eliminando.value = false
  }
}

// ── Helpers ───────────────────────────────────────────────
function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}

function formatFecha(iso: string): string {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}
</script>

<template>
  <div class="space-y-6">

    <!-- ── Navegador de mes ────────────────────────────── -->
    <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-xl sm:text-2xl font-extrabold text-primary">Gastos operativos</h3>
        <p class="text-sm text-on-surface-variant mt-0.5">Registra los gastos fijos del negocio mes a mes</p>
      </div>
      <div class="flex items-center gap-2 self-start sm:self-auto">
        <button
          class="btn-ghost p-2 rounded-lg"
          aria-label="Mes anterior"
          @click="mesAnterior"
        >
          <ChevronLeft class="w-4 h-4" />
        </button>
        <span class="text-sm font-bold text-primary w-32 sm:w-36 text-center">{{ labelMes }}</span>
        <button
          class="btn-ghost p-2 rounded-lg"
          aria-label="Mes siguiente"
          @click="mesSiguiente"
        >
          <ChevronRight class="w-4 h-4" />
        </button>
      </div>
    </div>

    <!-- ── Cards resumen por categoría ───────────────── -->
    <div class="grid grid-cols-2 xs:grid-cols-3 sm:grid-cols-4 lg:grid-cols-7 gap-3">
      <div
        v-for="cat in CATEGORIAS"
        :key="cat"
        class="card p-3 sm:p-4 text-center"
      >
        <p class="text-lg sm:text-xl mb-1">{{ CATEGORIAS_ICONO[cat] }}</p>
        <p class="text-[9px] sm:text-[10px] font-bold uppercase tracking-wide text-on-surface-variant leading-tight mb-1 sm:mb-2">
          {{ CATEGORIAS_LABEL[cat] }}
        </p>
        <p class="text-xs sm:text-sm font-extrabold text-primary">{{ formatEur(totalPorCategoria[cat]) }}</p>
      </div>
    </div>

    <!-- ── Total mes ──────────────────────────────────── -->
    <div class="flex items-center justify-between rounded-xl bg-primary/5 px-4 sm:px-5 py-3 sm:py-4">
      <p class="text-xs sm:text-sm font-bold text-on-surface-variant uppercase tracking-wider">Total gastos {{ labelMes }}</p>
      <p class="text-xl sm:text-2xl font-extrabold text-primary">{{ formatEur(totalMes) }}</p>
    </div>

    <!-- ── Spinner ────────────────────────────────────── -->
    <div v-if="cargando" class="flex justify-center py-12">
      <Loader2 class="w-8 h-8 animate-spin text-primary" />
    </div>

    <template v-else>

      <!-- ── Lista por categoría ────────────────────── -->
      <div
        v-for="cat in CATEGORIAS"
        v-show="gastosPorCategoria[cat].length > 0"
        :key="cat"
        class="card p-0 overflow-hidden"
      >
        <!-- Cabecera categoría -->
        <div class="flex items-center gap-2 px-5 py-3 bg-surface-container">
          <span>{{ CATEGORIAS_ICONO[cat] }}</span>
          <span class="text-xs font-bold uppercase tracking-wider text-on-surface-variant">
            {{ CATEGORIAS_LABEL[cat] }}
          </span>
          <span class="ml-auto text-sm font-extrabold text-primary">
            {{ formatEur(totalPorCategoria[cat]) }}
          </span>
        </div>

        <!-- Filas de gasto -->
        <div class="divide-y divide-outline-variant/20">
          <div
            v-for="gasto in gastosPorCategoria[cat]"
            :key="gasto.id"
            class="flex items-center gap-2 sm:gap-3 px-3 sm:px-5 py-3 hover:bg-surface-container-low transition-colors"
          >
            <div class="flex-1 min-w-0">
              <p class="text-sm font-medium text-on-surface truncate">{{ gasto.concepto }}</p>
              <p class="text-[11px] text-on-surface-variant">{{ formatFecha(gasto.fecha) }}</p>
            </div>
            <p class="text-sm font-bold text-primary flex-shrink-0 ml-auto">{{ formatEur(gasto.importe) }}</p>
            <div class="flex gap-1 flex-shrink-0">
              <button
                class="p-1.5 rounded-md text-on-surface-variant hover:text-primary hover:bg-primary/10 transition-colors"
                title="Editar gasto"
                @click="abrirEditar(gasto)"
              >
                <Pencil class="w-3.5 h-3.5" />
              </button>
              <button
                class="p-1.5 rounded-md text-on-surface-variant hover:text-red-600 hover:bg-red-50 transition-colors"
                title="Eliminar gasto"
                @click="pedirConfirmacion(gasto)"
              >
                <Trash2 class="w-3.5 h-3.5" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Sin gastos -->
      <div v-if="gastos.length === 0" class="text-center py-12 text-sm text-on-surface-variant">
        No hay gastos registrados en {{ labelMes }}.
      </div>

    </template>

    <!-- ── Botón añadir ───────────────────────────────── -->
    <div class="flex justify-end">
      <button class="btn-primary flex items-center gap-2" @click="abrirCrear">
        <Plus class="w-4 h-4" />
        Añadir gasto
      </button>
    </div>

  </div>

  <!-- ══ Modal añadir/editar ═══════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalAbierto"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="cerrarModal"
      >
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-md p-6 space-y-5">
          <h4 class="text-lg font-extrabold text-primary">
            {{ modoEdicion ? 'Editar gasto' : 'Nuevo gasto' }}
          </h4>

          <!-- Categoría -->
          <div>
            <label class="label" for="gasto-categoria">Categoría</label>
            <select id="gasto-categoria" v-model="form.categoria" class="select-field">
              <option v-for="cat in CATEGORIAS" :key="cat" :value="cat">
                {{ CATEGORIAS_ICONO[cat] }} {{ CATEGORIAS_LABEL[cat] }}
              </option>
            </select>
          </div>

          <!-- Select empleado (solo SALARIOS) -->
          <div v-if="form.categoria === 'SALARIOS' && empleados.length > 0">
            <label class="label" for="gasto-empleado">Empleado</label>
            <select id="gasto-empleado" v-model="form.empleadoSeleccionado" class="select-field">
              <option value="">— Selecciona empleado —</option>
              <option v-for="emp in empleados" :key="emp.id" :value="emp.id">
                {{ emp.nombre }}
              </option>
            </select>
          </div>

          <!-- Concepto -->
          <div>
            <label class="label" for="gasto-concepto">Concepto</label>
            <input
              id="gasto-concepto"
              v-model="form.concepto"
              type="text"
              class="input w-full"
              :placeholder="form.categoria === 'SALARIOS' ? 'Ej: Nómina bruta Ana García' : 'Describe el gasto'"
              required
            />
          </div>

          <!-- Importe + Fecha -->
          <div class="grid grid-cols-1 xs:grid-cols-2 gap-4">
            <div>
              <label class="label" for="gasto-importe">Importe (€)</label>
              <input
                id="gasto-importe"
                v-model="form.importe"
                type="number"
                min="0"
                step="0.01"
                class="input w-full"
                placeholder="0,00"
                required
              />
            </div>
            <div>
              <label class="label" for="gasto-fecha">Fecha</label>
              <input
                id="gasto-fecha"
                v-model="form.fecha"
                type="date"
                class="input w-full"
              />
            </div>
          </div>

          <!-- Acciones -->
          <div class="flex justify-end gap-3 pt-2">
            <button class="btn-secondary" :disabled="guardando" @click="cerrarModal">
              Cancelar
            </button>
            <button
              class="btn-primary flex items-center gap-2"
              :disabled="guardando || !form.concepto || !form.importe"
              @click="guardarGasto"
            >
              <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
              Guardar
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- ══ Modal confirmación eliminar ═══════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="confirmarEliminar"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="confirmarEliminar = false"
      >
        <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm p-6 space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
              <Trash2 class="w-5 h-5 text-red-600" />
            </div>
            <div>
              <h4 class="text-base font-extrabold text-on-surface">Eliminar gasto</h4>
              <p class="text-sm text-on-surface-variant">Esta acción no se puede deshacer.</p>
            </div>
          </div>
          <p class="text-sm text-on-surface bg-surface-container rounded-lg px-4 py-2">
            <span class="font-medium">{{ gastoAEliminar?.concepto }}</span>
            — {{ gastoAEliminar ? formatEur(gastoAEliminar.importe) : '' }}
          </p>
          <div class="flex justify-end gap-3">
            <button class="btn-secondary" :disabled="eliminando" @click="confirmarEliminar = false">
              Cancelar
            </button>
            <button
              class="btn-primary !bg-red-600 hover:!bg-red-700 flex items-center gap-2"
              :disabled="eliminando"
              @click="ejecutarEliminar"
            >
              <Loader2 v-if="eliminando" class="w-4 h-4 animate-spin" />
              Eliminar
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay-enter-active,
.modal-overlay-leave-active {
  transition: opacity 0.18s ease;
}
.modal-overlay-enter-from,
.modal-overlay-leave-to {
  opacity: 0;
}
</style>
