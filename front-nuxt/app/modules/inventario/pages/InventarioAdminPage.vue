<script setup lang="ts">
/**
 * Inventario — gestión de productos con alertas de stock bajo.
 * KPIs + alertas de stock + tabla con filtros por categoría.
 */
import { Plus, AlertTriangle, Package, Loader2, X, Save, ShoppingCart, Search } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

const toast = useToast()

// ── Tipos ─────────────────────────────────────────────────
interface Producto {
  id: string
  nombre: string
  categoria: string
  genero: 'MASCULINO' | 'FEMENINO' | 'UNISEX'
  precio: number
  precioDescuento: number | null
  stock: number
  stockMinimo: number
  activo: boolean
}

interface ResumenVentas {
  ingresosSemana: number
  ingresosMes: number
  ingresosAnio: number
  unidadesSemana: number
  unidadesMes: number
  unidadesAnio: number
}

interface VentaProductoResponse {
  producto: Producto
  venta: {
    id: string
    cantidad: number
    precioUnitario: number
    total: number
    fechaVenta: string
  }
  resumen: ResumenVentas
}

// ── Estado ────────────────────────────────────────────────
const productos      = ref<Producto[]>([])
const cargando       = ref(true)
const filtroCategoria = ref('TODOS')
const filtroStock = ref<'TODOS' | 'BAJO' | 'SIN_STOCK'>('TODOS')
const busqueda = ref('')
const drawerAbierto  = ref(false)
const guardando      = ref(false)
const productoEditar = ref<Partial<Producto>>({})
const resumenVentas = ref<ResumenVentas>({
  ingresosSemana: 0,
  ingresosMes: 0,
  ingresosAnio: 0,
  unidadesSemana: 0,
  unidadesMes: 0,
  unidadesAnio: 0,
})
const modalVentaAbierto = ref(false)
const vendiendo = ref(false)
const productoVenta = ref<Producto | null>(null)
const cantidadVenta = ref(1)
const errorVenta = ref('')

const categorias = ['TODOS', 'CERA', 'CHAMPU', 'ACONDICIONADOR', 'COLORACION', 'HERRAMIENTA', 'OTRO']
const labelCat: Record<string, string> = {
  TODOS: 'Todos', CERA: 'Cera', CHAMPU: 'Champú', ACONDICIONADOR: 'Acondicionador',
  COLORACION: 'Coloración', HERRAMIENTA: 'Herramienta', OTRO: 'Otro',
}
const opcionesCategoria = categorias.map(cat => ({ value: cat, label: labelCat[cat] }))
const filtrosStock: { id: 'TODOS' | 'BAJO' | 'SIN_STOCK', label: string }[] = [
  { id: 'TODOS', label: 'Todos' },
  { id: 'BAJO', label: 'Bajo stock' },
  { id: 'SIN_STOCK', label: 'Sin stock' },
]

// ── Computed ──────────────────────────────────────────────
const productosFiltrados = computed(() => {
  const term = busqueda.value.trim().toLowerCase()

  return productos.value.filter((p) => {
    const coincideCategoria = filtroCategoria.value === 'TODOS' || p.categoria === filtroCategoria.value
    const coincideStock =
      filtroStock.value === 'TODOS'
      || (filtroStock.value === 'BAJO' && p.stock <= p.stockMinimo)
      || (filtroStock.value === 'SIN_STOCK' && p.stock === 0)
    const coincideBusqueda =
      !term
      || p.nombre.toLowerCase().includes(term)
      || (labelCat[p.categoria] ?? p.categoria).toLowerCase().includes(term)

    return coincideCategoria && coincideStock && coincideBusqueda
  })
})

const stockBajo      = computed(() => productos.value.filter(p => p.stock <= p.stockMinimo))
const productosPorReponer = computed(() =>
  [...stockBajo.value]
    .sort((a, b) => a.stock - b.stock || a.nombre.localeCompare(b.nombre))
    .slice(0, 3),
)
const sinStock = computed(() => productos.value.filter(p => p.stock === 0))
const valorInventario = computed(() =>
  productos.value.reduce((acc, p) => acc + p.precio * p.stock, 0),
)

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const [productosResponse, resumenResponse] = await Promise.all([
      api.get('/v1/productos'),
      api.get('/v1/productos/ventas/resumen'),
    ])
    productos.value = productosResponse.data
    resumenVentas.value = resumenResponse.data
  } catch {
    // vacío si falla
  } finally {
    cargando.value = false
  }
})

// ── Acciones ──────────────────────────────────────────────
function abrirCrear() {
  productoEditar.value = { categoria: 'OTRO', genero: 'UNISEX', precio: 0, stock: 0, stockMinimo: 5, activo: true }
  drawerAbierto.value = true
}

function abrirEditar(p: Producto) {
  productoEditar.value = { ...p }
  drawerAbierto.value = true
}

async function guardar() {
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (productoEditar.value.id) {
      const { data } = await api.put(`/v1/productos/${productoEditar.value.id}`, productoEditar.value)
      const idx = productos.value.findIndex(p => p.id === data.id)
      if (idx !== -1) productos.value[idx] = data
    } else {
      const { data } = await api.post('/v1/productos', productoEditar.value)
      productos.value.unshift(data)
    }
    drawerAbierto.value = false
    toast.success(productoEditar.value.id ? 'Producto actualizado' : 'Producto creado')
  } catch (error) {
    toast.error(mensajeErrorProducto(error))
  } finally {
    guardando.value = false
  }
}

function mensajeErrorProducto(error: unknown): string {
  const responseData = (error as { response?: { data?: unknown } })?.response?.data
  if (typeof responseData === 'string' && responseData.trim()) return responseData
  if (responseData && typeof responseData === 'object') {
    const data = responseData as { message?: string, error?: string, detail?: string }
    return data.message || data.detail || data.error || 'Error al guardar el producto'
  }
  return 'Error al guardar el producto'
}

async function eliminar(id: string) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/v1/productos/${id}`)
    productos.value = productos.value.filter(p => p.id !== id)
    toast.success('Producto eliminado')
  } catch {
    toast.error('Error al eliminar el producto')
  }
}

function verBajoStock() {
  filtroCategoria.value = 'TODOS'
  filtroStock.value = 'BAJO'
  busqueda.value = ''
}

function limpiarFiltros() {
  filtroCategoria.value = 'TODOS'
  filtroStock.value = 'TODOS'
  busqueda.value = ''
}

function abrirVenta(producto: Producto) {
  productoVenta.value = producto
  cantidadVenta.value = 1
  errorVenta.value = ''
  modalVentaAbierto.value = true
}

function cerrarVenta() {
  modalVentaAbierto.value = false
  productoVenta.value = null
  cantidadVenta.value = 1
  errorVenta.value = ''
}

const totalVentaEstimado = computed(() => {
  if (!productoVenta.value) return 0
  return precioVenta(productoVenta.value) * cantidadVenta.value
})

async function venderProducto() {
  if (!productoVenta.value) return

  vendiendo.value = true
  errorVenta.value = ''

  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post<VentaProductoResponse>(`/v1/productos/${productoVenta.value.id}/vender`, {
      cantidad: cantidadVenta.value,
    })

    const idx = productos.value.findIndex(p => p.id === data.producto.id)
    if (idx !== -1) {
      productos.value[idx] = data.producto
    }
    resumenVentas.value = data.resumen
    cerrarVenta()
  } catch (error) {
    errorVenta.value = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      ?? 'No se pudo registrar la venta.'
  } finally {
    vendiendo.value = false
  }
}

function badgeStock(p: Producto): string {
  if (p.stock === 0) return 'bg-error/10 text-error font-bold'
  if (p.stock <= p.stockMinimo) return 'bg-amber-100 text-amber-700 font-bold'
  return 'bg-green-100 text-green-700'
}

function precioVenta(p: Producto): number {
  if (p.precioDescuento && p.precioDescuento > 0) return p.precioDescuento
  return p.precio
}

function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}
</script>

<template>
  <div class="space-y-5 sm:space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-2xl font-extrabold tracking-tight text-primary mb-1 sm:text-3xl">Inventario</h2>
      <p class="text-on-surface-variant text-sm">Control de stock y productos del atelier</p>
    </div>

    <!-- ── KPI Cards ─────────────────────────────────────── -->
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-6 xl:grid-cols-3">

      <div class="inventario-panel-highlight rounded-2xl p-5 text-white sm:p-6">
        <p class="text-[10px] font-bold uppercase tracking-widest text-white/70 mb-1">Valor del Inventario</p>
        <h3 class="text-2xl font-extrabold sm:text-3xl">{{ formatEur(valorInventario) }}</h3>
        <p class="text-xs text-white/60 mt-2">{{ productos.length }} productos en total</p>
      </div>

      <div class="inventario-panel-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Productos por reponer</p>
        <div class="flex items-end justify-between gap-3">
          <h3 class="text-3xl font-extrabold text-primary sm:text-4xl">{{ stockBajo.length }}</h3>
          <button
            v-if="stockBajo.length"
            class="rounded-full bg-amber-100 px-3 py-1 text-[11px] font-bold text-amber-800 transition-colors hover:bg-amber-200"
            type="button"
            @click="verBajoStock"
          >
            Ver bajos
          </button>
        </div>
        <div v-if="productosPorReponer.length" class="mt-3 space-y-1.5">
          <p
            v-for="producto in productosPorReponer"
            :key="producto.id"
            class="truncate text-xs font-medium text-on-surface-variant"
          >
            {{ producto.nombre }} · {{ producto.stock }} uds
          </p>
        </div>
        <p v-else class="text-xs text-green-700 mt-2 font-medium">Todo el stock está controlado</p>
      </div>

      <div class="inventario-panel-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Sin stock</p>
        <h3 class="text-3xl font-extrabold text-primary sm:text-4xl">{{ sinStock.length }}</h3>
        <p class="text-xs text-on-surface-variant mt-2">Agotados actualmente</p>
      </div>

      <div class="inventario-panel-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ventas Semana</p>
        <h3 class="text-2xl font-extrabold text-primary sm:text-3xl">{{ formatEur(resumenVentas.ingresosSemana) }}</h3>
        <p class="text-xs text-on-surface-variant mt-2">{{ resumenVentas.unidadesSemana }} uds vendidas</p>
      </div>

      <div class="inventario-panel-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ventas Mes</p>
        <h3 class="text-2xl font-extrabold text-primary sm:text-3xl">{{ formatEur(resumenVentas.ingresosMes) }}</h3>
        <p class="text-xs text-on-surface-variant mt-2">{{ resumenVentas.unidadesMes }} uds vendidas</p>
      </div>

      <div class="inventario-panel-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Ventas Año</p>
        <h3 class="text-2xl font-extrabold text-primary sm:text-3xl">{{ formatEur(resumenVentas.ingresosAnio) }}</h3>
        <p class="text-xs text-on-surface-variant mt-2">{{ resumenVentas.unidadesAnio }} uds vendidas</p>
      </div>

    </div>

    <!-- ── Alertas de stock bajo ──────────────────────────── -->
    <div v-if="stockBajo.length > 0" class="inventario-alert-card p-4 sm:p-5">
      <div class="flex items-center gap-2 mb-3">
        <AlertTriangle class="w-4 h-4 text-amber-600" />
        <p class="text-sm font-bold text-amber-800">
          {{ stockBajo.length }} producto{{ stockBajo.length !== 1 ? 's' : '' }} para reponer
        </p>
      </div>
      <div class="flex flex-wrap gap-2">
        <button
          v-for="p in stockBajo"
          :key="p.id"
          class="px-3 py-1 bg-amber-100 text-amber-800 rounded-full text-xs font-medium hover:bg-amber-200 transition-colors"
          type="button"
          @click="abrirEditar(p)"
        >
          {{ p.nombre }} ({{ p.stock }} uds)
        </button>
      </div>
      <p class="mt-3 text-xs text-amber-800/80">
        Pulsa en cualquier producto para editar stock o ajustar el mínimo.
      </p>
    </div>

    <!-- ── Tabla principal ────────────────────────────────── -->
    <div class="inventario-panel-card p-4 sm:p-6">

      <!-- Filtros + acción -->
      <div class="mb-5 flex flex-col gap-3 sm:mb-8 xl:flex-row xl:items-center xl:gap-4">
        <div class="relative min-w-0 flex-1">
          <Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" />
          <label for="inventario-busqueda" class="sr-only">Buscar producto</label>
          <input
            id="inventario-busqueda"
            v-model="busqueda"
            type="search"
            class="input pl-9"
            placeholder="Buscar producto..."
          />
        </div>
        <div class="w-full xl:w-64">
          <label for="inventario-categoria" class="sr-only">Filtrar productos por categoría</label>
          <AppSelect
            id="inventario-categoria"
            v-model="filtroCategoria"
            :options="opcionesCategoria"
            aria-label="Filtrar productos por categoría"
          />
        </div>
        <div class="grid min-h-10 grid-cols-3 rounded-md border border-outline-variant/40 bg-white p-1">
          <button
            v-for="opcion in filtrosStock"
            :key="opcion.id"
            class="rounded px-2 py-2 text-[11px] font-bold transition-colors sm:px-4 sm:py-1.5 sm:text-xs"
            :class="filtroStock === opcion.id ? 'bg-surface-container-low text-primary' : 'text-on-surface-variant hover:bg-surface-container-lowest'"
            type="button"
            @click="filtroStock = opcion.id"
          >
            {{ opcion.label }}
          </button>
        </div>
        <button
          v-if="busqueda || filtroCategoria !== 'TODOS' || filtroStock !== 'TODOS'"
          class="min-h-10 rounded-md border border-outline-variant/40 bg-white px-4 text-xs font-bold text-on-surface-variant transition-colors hover:bg-surface-container-lowest"
          type="button"
          @click="limpiarFiltros"
        >
          Limpiar
        </button>
        <button
          type="button"
          class="flex min-h-11 items-center justify-center gap-2 rounded-xl bg-primary-container px-6 text-sm font-bold text-white transition-all hover:opacity-90 xl:min-h-10 xl:rounded-md"
          @click="abrirCrear"
        >
          <Plus class="w-4 h-4" />
          Nuevo Producto
        </button>
      </div>

      <!-- Spinner -->
      <div v-if="cargando" class="flex items-center justify-center py-16">
        <Loader2 class="w-6 h-6 animate-spin text-primary" />
      </div>

      <template v-else>
        <div class="grid gap-3 md:grid-cols-2 xl:hidden">
          <article
            v-for="p in productosFiltrados"
            :key="p.id"
            class="rounded-2xl border border-outline-variant/20 bg-white p-4 shadow-sm"
            @click="abrirEditar(p)"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex min-w-0 items-center gap-2">
                  <Package class="h-4 w-4 shrink-0 text-on-surface-variant/50" />
                  <h3 class="truncate text-sm font-extrabold text-primary">{{ p.nombre }}</h3>
                </div>
                <p class="mt-1 text-[11px] text-on-surface-variant">{{ labelCat[p.categoria] ?? p.categoria }}</p>
              </div>
              <span class="shrink-0 rounded-full px-3 py-1 text-xs" :class="badgeStock(p)">
                {{ p.stock }} uds
              </span>
            </div>

            <div class="mt-4 grid grid-cols-2 gap-3">
              <div class="rounded-xl bg-surface-container-low px-3 py-2">
                <p class="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Precio</p>
                <p class="mt-1 font-bold text-primary">{{ formatEur(precioVenta(p)) }}</p>
                <p v-if="p.precioDescuento && p.precioDescuento < p.precio" class="text-[11px] text-on-surface-variant line-through">
                  {{ formatEur(p.precio) }}
                </p>
              </div>
              <div class="rounded-xl bg-surface-container-low px-3 py-2">
                <p class="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Mínimo</p>
                <p class="mt-1 font-bold text-on-surface">{{ p.stockMinimo }} uds</p>
              </div>
            </div>

            <div class="mt-4 grid grid-cols-3 gap-2" @click.stop>
              <button
                class="flex items-center justify-center rounded-xl bg-primary-container px-2 py-2.5 text-xs font-bold text-white disabled:opacity-40"
                :disabled="p.stock === 0"
                @click="abrirVenta(p)"
              >
                Vender
              </button>
              <button
                class="flex items-center justify-center rounded-xl bg-surface-container-low px-2 py-2.5 text-xs font-bold text-primary"
                @click="abrirEditar(p)"
              >
                Editar
              </button>
              <button
                class="flex items-center justify-center rounded-xl bg-red-50 px-2 py-2.5 text-xs font-bold text-error"
                @click="eliminar(p.id)"
              >
                Borrar
              </button>
            </div>
          </article>

          <div v-if="productosFiltrados.length === 0" class="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-10 text-center text-sm text-on-surface-variant">
            No hay productos con los filtros actuales
          </div>
        </div>

        <!-- Tabla -->
        <div class="hidden overflow-x-auto xl:block">
          <table class="w-full text-left border-separate border-spacing-y-3" aria-label="Inventario de productos">
          <thead>
            <tr class="text-on-surface-variant/60 text-[10px] uppercase tracking-[0.15em] font-bold">
              <th scope="col" class="pb-4 px-4">Producto</th>
              <th scope="col" class="pb-4 px-4">Categoría</th>
              <th scope="col" class="pb-4 px-4">Precio</th>
              <th scope="col" class="pb-4 px-4">Stock</th>
              <th scope="col" class="pb-4 px-4 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr
              v-for="p in productosFiltrados"
              :key="p.id"
              class="group hover:bg-surface-container-low transition-colors cursor-pointer"
              @click="abrirEditar(p)"
            >
              <td class="py-4 px-4 font-bold text-primary rounded-l-xl">
                <div class="flex items-center gap-2">
                  <Package class="w-4 h-4 text-on-surface-variant/40" />
                  {{ p.nombre }}
                </div>
              </td>
              <td class="py-4 px-4">
                <span class="px-2 py-0.5 bg-surface-container text-on-surface-variant text-[10px] font-bold rounded-full">
                  {{ labelCat[p.categoria] ?? p.categoria }}
                </span>
              </td>
              <td class="py-4 px-4 font-bold">
                {{ formatEur(precioVenta(p)) }}
                <span v-if="p.precioDescuento && p.precioDescuento < p.precio" class="ml-2 text-xs text-on-surface-variant font-normal line-through">
                  {{ formatEur(p.precio) }}
                </span>
              </td>
              <td class="py-4 px-4">
                <span class="px-3 py-1 rounded-full text-xs" :class="badgeStock(p)">
                  {{ p.stock }} uds
                </span>
              </td>
              <td class="py-4 px-4 text-right rounded-r-xl" @click.stop>
                <div class="flex justify-end gap-2">
                  <button
                    class="h-8 px-3 rounded-full bg-primary-container text-white flex items-center justify-center gap-1 transition-all hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed"
                    :disabled="p.stock === 0"
                    @click="abrirVenta(p)"
                  >
                    <ShoppingCart class="w-3.5 h-3.5" />
                    <span class="text-[11px] font-bold">Vender</span>
                  </button>
                  <button
                    class="w-8 h-8 rounded-full hover:bg-primary-container hover:text-white flex items-center justify-center transition-all text-primary-container"
                    @click="abrirEditar(p)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button
                    class="w-8 h-8 rounded-full hover:bg-error hover:text-white flex items-center justify-center transition-all text-error"
                    @click="eliminar(p.id)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="productosFiltrados.length === 0">
              <td colspan="5" class="py-12 text-center text-on-surface-variant text-sm">
                No hay productos con los filtros actuales
              </td>
            </tr>
          </tbody>
          </table>
        </div>
      </template>
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Venta de producto
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div v-if="modalVentaAbierto" class="fixed inset-0 z-40 bg-black/25 backdrop-blur-sm" @click.self="cerrarVenta" />
    </Transition>

    <Transition name="slide-right">
      <div v-if="modalVentaAbierto" class="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6">
        <div class="w-full max-w-md rounded-2xl border border-outline-variant/10 bg-white p-5 shadow-2xl sm:rounded-[28px] sm:p-8 space-y-5 sm:space-y-6">
          <div class="flex items-start justify-between gap-4">
            <div>
              <h3 class="text-lg font-bold text-primary sm:text-xl">Registrar venta</h3>
              <p v-if="productoVenta" class="text-sm text-on-surface-variant mt-1">
                {{ productoVenta.nombre }} · Stock actual: {{ productoVenta.stock }} uds
              </p>
            </div>
            <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" @click="cerrarVenta">
              <X class="w-5 h-5" />
            </button>
          </div>

          <div class="space-y-2">
            <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Cantidad</label>
            <input
              v-model.number="cantidadVenta"
              type="number"
              min="1"
              :max="productoVenta?.stock ?? 1"
              class="input"
            />
          </div>

          <div class="rounded-2xl bg-surface-container-low px-5 py-4 space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-on-surface-variant">Precio unitario</span>
              <strong class="text-primary">{{ formatEur(productoVenta ? precioVenta(productoVenta) : 0) }}</strong>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-on-surface-variant">Total estimado</span>
              <strong class="text-primary text-lg">{{ formatEur(totalVentaEstimado) }}</strong>
            </div>
          </div>

          <div v-if="errorVenta" class="rounded-2xl border border-error/10 bg-error/5 px-4 py-3 text-sm text-error">
            {{ errorVenta }}
          </div>

          <button
            class="w-full bg-primary-container text-white py-4 rounded-xl font-bold hover:shadow-lg hover:shadow-primary-container/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            :disabled="vendiendo || !productoVenta || cantidadVenta < 1 || cantidadVenta > (productoVenta?.stock ?? 0)"
            @click="venderProducto"
          >
            <Loader2 v-if="vendiendo" class="w-4 h-4 animate-spin" />
            <ShoppingCart v-else class="w-4 h-4" />
            {{ vendiendo ? 'Registrando venta...' : 'Confirmar venta' }}
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- ══════════════════════════════════════════════════════
       DRAWER — Crear / Editar producto
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div v-if="drawerAbierto" class="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm" @click.self="drawerAbierto = false" />
    </Transition>

    <Transition name="slide-right">
      <aside v-if="drawerAbierto" class="fixed right-0 top-0 z-50 flex h-screen w-full max-w-md flex-col border-l border-outline-variant/20 bg-white shadow-2xl">

        <div class="flex flex-shrink-0 items-center justify-between border-b border-outline-variant/10 p-5 sm:p-8">
          <div>
            <h3 class="text-lg font-bold text-primary sm:text-xl">{{ productoEditar.id ? 'Editar Producto' : 'Nuevo Producto' }}</h3>
            <p class="text-xs text-on-surface-variant">Gestión de inventario</p>
          </div>
          <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" @click="drawerAbierto = false">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="flex-1 space-y-5 overflow-y-auto p-5 sm:space-y-6 sm:p-8">

          <div class="space-y-2">
            <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Nombre</label>
            <input v-model="productoEditar.nombre" type="text" class="input font-bold text-primary" placeholder="Nombre del producto" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Categoría</label>
            <select v-model="productoEditar.categoria" class="select-field">
              <option v-for="cat in categorias.filter(c => c !== 'TODOS')" :key="cat" :value="cat">{{ labelCat[cat] }}</option>
            </select>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Precio (€)</label>
              <input v-model.number="productoEditar.precio" type="number" min="0" step="0.01" class="input" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">P. Descuento</label>
              <input v-model.number="productoEditar.precioDescuento" type="number" min="0" step="0.01" class="input" placeholder="Opcional" />
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Stock actual</label>
              <input v-model.number="productoEditar.stock" type="number" min="0" class="input" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Stock mínimo</label>
              <input v-model.number="productoEditar.stockMinimo" type="number" min="0" class="input" />
            </div>
          </div>

        </div>

        <div class="flex-shrink-0 border-t border-outline-variant/10 bg-surface-container-lowest p-5 sm:p-8">
          <button
            class="flex w-full items-center justify-center gap-2 rounded-xl bg-primary-container py-3.5 font-bold text-white transition-all hover:shadow-lg hover:shadow-primary-container/30 disabled:opacity-50 sm:py-4"
            :disabled="guardando"
            @click="guardar"
          >
            <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
            <Save v-else class="w-4 h-4" />
            {{ guardando ? 'Guardando...' : 'Guardar Cambios' }}
          </button>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>

<style scoped>
.inventario-panel-card,
.inventario-panel-kpi,
.inventario-alert-card {
  background: #fcfcfd;
  border: 1px solid rgb(104 111 122 / 0.1);
  border-radius: 1rem;
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.06);
}

.inventario-panel-kpi {
  padding: 1.5rem;
}

.inventario-panel-highlight {
  background: rgb(29 55 95);
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.12);
}

.inventario-alert-card {
  background: rgb(255 251 235);
  border-color: rgb(253 230 138);
}

.slide-right-enter-active, .slide-right-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
</style>
