<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

import { ShoppingCart, X, Loader2, Search } from 'lucide-vue-next'

interface Producto {
  id: string
  nombre: string
  descripcion: string
  categoria: string
  precio: number
  precioDescuento: number | null
  stock: number
  stockMinimo: number
  activo: boolean
}

interface VentaProductoResponse {
  producto: Producto
  venta: { id: string; cantidad: number; precioUnitario: number; total: number; fechaVenta: string }
  resumen: { ingresosSemana: number; ingresosMes: number; ingresosAnio: number; unidadesSemana: number; unidadesMes: number; unidadesAnio: number }
}

const productos     = ref<Producto[]>([])
const cargando      = ref(true)
const busqueda      = ref('')
const categoriaFiltro = ref('TODOS')
const categorias    = ['TODOS', 'CERA', 'CHAMPU', 'ACONDICIONADOR', 'COLORACION', 'HERRAMIENTA', 'OTRO']

const modalAbierto  = ref(false)
const vendiendo     = ref(false)
const productoVenta = ref<Producto | null>(null)
const cantidad      = ref(1)
const errorVenta    = ref('')
const confirmado    = ref(false)

const productosFiltrados = computed(() => {
  let lista = productos.value.filter(p => p.activo && p.stock > 0)
  if (categoriaFiltro.value !== 'TODOS') {
    lista = lista.filter(p => p.categoria === categoriaFiltro.value)
  }
  if (busqueda.value.trim()) {
    const q = busqueda.value.toLowerCase()
    lista = lista.filter(p => p.nombre.toLowerCase().includes(q))
  }
  return lista
})

function precioFinal(p: Producto): number {
  return p.precioDescuento && p.precioDescuento > 0 ? p.precioDescuento : p.precio
}

const totalEstimado = computed(() =>
  productoVenta.value ? precioFinal(productoVenta.value) * cantidad.value : 0,
)

function formatEur(v: number) {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(v)
}

function etiCategoria(cat: string) {
  const mapa: Record<string, string> = {
    CHAMPU: 'Champú', ACONDICIONADOR: 'Acond.', COLORACION: 'Color',
    CERA: 'Cera', HERRAMIENTA: 'Herramienta', OTRO: 'Otro',
  }
  return mapa[cat] ?? cat
}

onMounted(async () => {
  const { api } = await import('~/infrastructure/http/api')
  try {
    const { data } = await api.get('/v1/productos')
    productos.value = data
  } finally {
    cargando.value = false
  }
})

function abrirVenta(p: Producto) {
  productoVenta.value = p
  cantidad.value = 1
  errorVenta.value = ''
  confirmado.value = false
  modalAbierto.value = true
}

function cerrarModal() {
  modalAbierto.value = false
  productoVenta.value = null
  confirmado.value = false
  errorVenta.value = ''
}

async function confirmarVenta() {
  if (!productoVenta.value) return
  vendiendo.value = true
  errorVenta.value = ''
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post<VentaProductoResponse>(
      `/v1/productos/${productoVenta.value.id}/vender`,
      { cantidad: cantidad.value },
    )
    const idx = productos.value.findIndex(p => p.id === data.producto.id)
    if (idx !== -1) productos.value[idx] = data.producto
    confirmado.value = true
  } catch (error) {
    errorVenta.value = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
      ?? 'No se pudo registrar la venta.'
  } finally {
    vendiendo.value = false
  }
}
</script>

<template>
  <div class="space-y-8">

    <div>
      <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Ventas</h2>
      <p class="text-on-surface-variant text-sm">Registrar venta de productos al cliente</p>
    </div>

    <!-- Filtros -->
    <div class="flex flex-col sm:flex-row gap-3">
      <div class="relative flex-1">
        <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant" />
        <input
          v-model="busqueda"
          type="text"
          placeholder="Buscar producto..."
          class="w-full pl-9 pr-4 py-2.5 rounded-full bg-surface-container text-sm border-none focus:ring-2 focus:ring-primary-container"
        />
      </div>
      <div class="flex gap-2 flex-wrap">
        <button
          v-for="cat in categorias"
          :key="cat"
          class="px-4 py-2 rounded-full text-xs font-bold transition-colors"
          :class="categoriaFiltro === cat
            ? 'bg-primary-container text-white'
            : 'bg-surface-container text-on-surface-variant hover:bg-surface-container-high'"
          @click="categoriaFiltro = cat"
        >
          {{ cat === 'TODOS' ? 'Todos' : etiCategoria(cat) }}
        </button>
      </div>
    </div>

    <!-- Spinner -->
    <div v-if="cargando" class="flex items-center justify-center py-20">
      <Loader2 class="w-6 h-6 animate-spin text-primary" />
    </div>

    <!-- Grid de productos -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <div
        v-for="p in productosFiltrados"
        :key="p.id"
        class="card p-5 flex flex-col gap-3 hover:shadow-md transition-shadow"
      >
        <!-- Categoría badge -->
        <div class="flex items-center justify-between">
          <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant bg-surface-container px-2 py-1 rounded-full">
            {{ etiCategoria(p.categoria) }}
          </span>
          <span
            class="text-[10px] font-bold"
            :class="p.stock <= p.stockMinimo ? 'text-amber-500' : 'text-green-600'"
          >
            Stock: {{ p.stock }}
          </span>
        </div>

        <div class="flex-1">
          <h4 class="font-bold text-primary text-sm leading-snug">{{ p.nombre }}</h4>
          <p v-if="p.descripcion" class="text-xs text-on-surface-variant mt-1 line-clamp-2">{{ p.descripcion }}</p>
        </div>

        <div class="flex items-center justify-between pt-2 border-t border-outline-variant/10">
          <div>
            <span class="text-lg font-extrabold text-primary">{{ formatEur(precioFinal(p)) }}</span>
            <span
              v-if="p.precioDescuento && p.precioDescuento < p.precio"
              class="ml-2 text-xs text-on-surface-variant line-through"
            >
              {{ formatEur(p.precio) }}
            </span>
          </div>
          <button
            class="flex items-center gap-1.5 bg-primary-container text-white px-4 py-2 rounded-full text-xs font-bold hover:opacity-90 transition-opacity disabled:opacity-40 disabled:cursor-not-allowed"
            :disabled="p.stock === 0"
            @click="abrirVenta(p)"
          >
            <ShoppingCart class="w-3.5 h-3.5" />
            Vender
          </button>
        </div>
      </div>

      <div v-if="productosFiltrados.length === 0" class="col-span-full text-center py-16 text-sm text-on-surface-variant">
        No hay productos disponibles con stock
      </div>
    </div>
  </div>

  <!-- Modal venta -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div v-if="modalAbierto" class="fixed inset-0 z-40 bg-black/25 backdrop-blur-sm" @click.self="cerrarModal" />
    </Transition>
    <Transition name="slide-right">
      <div v-if="modalAbierto" class="fixed inset-0 z-50 flex items-center justify-center p-6">
        <div class="w-full max-w-sm rounded-[28px] bg-white shadow-2xl border border-outline-variant/10 p-8 space-y-5">

          <!-- Confirmación -->
          <template v-if="confirmado">
            <div class="text-center space-y-3">
              <div class="w-14 h-14 rounded-full bg-green-100 flex items-center justify-center mx-auto">
                <ShoppingCart class="w-6 h-6 text-green-600" />
              </div>
              <h3 class="text-xl font-bold text-primary">¡Venta registrada!</h3>
              <p class="text-sm text-on-surface-variant">
                {{ cantidad }} ud{{ cantidad > 1 ? 's' : '' }} de <strong>{{ productoVenta?.nombre }}</strong>
              </p>
            </div>
            <button
              class="w-full bg-primary-container text-white py-3 rounded-xl font-bold hover:opacity-90 transition-opacity"
              @click="cerrarModal"
            >
              Cerrar
            </button>
          </template>

          <!-- Formulario venta -->
          <template v-else>
            <div class="flex items-start justify-between gap-4">
              <div>
                <h3 class="text-xl font-bold text-primary">Registrar venta</h3>
                <p v-if="productoVenta" class="text-sm text-on-surface-variant mt-1">
                  {{ productoVenta.nombre }} · Stock: {{ productoVenta.stock }} uds
                </p>
              </div>
              <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" @click="cerrarModal">
                <X class="w-5 h-5" />
              </button>
            </div>

            <div class="space-y-1">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Cantidad</label>
              <input
                v-model.number="cantidad"
                type="number"
                min="1"
                :max="productoVenta?.stock ?? 1"
                class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container"
              />
            </div>

            <div class="space-y-2 bg-surface-container-low rounded-2xl px-4 py-3 text-sm">
              <div class="flex items-center justify-between">
                <span class="text-on-surface-variant">Precio unitario</span>
                <strong class="text-primary">{{ formatEur(productoVenta ? precioFinal(productoVenta) : 0) }}</strong>
              </div>
              <div class="flex items-center justify-between border-t border-outline-variant/10 pt-2">
                <span class="text-on-surface-variant">Total</span>
                <strong class="text-primary text-lg">{{ formatEur(totalEstimado) }}</strong>
              </div>
            </div>

            <div v-if="errorVenta" class="rounded-2xl border border-error/10 bg-error/5 px-4 py-3 text-sm text-error">
              {{ errorVenta }}
            </div>

            <button
              class="w-full bg-primary-container text-white py-4 rounded-xl font-bold hover:shadow-lg hover:shadow-primary-container/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
              :disabled="vendiendo || !productoVenta || cantidad < 1 || cantidad > (productoVenta?.stock ?? 0)"
              @click="confirmarVenta"
            >
              <Loader2 v-if="vendiendo" class="w-4 h-4 animate-spin" />
              <ShoppingCart v-else class="w-4 h-4" />
              {{ vendiendo ? 'Registrando...' : 'Confirmar venta' }}
            </button>
          </template>

        </div>
      </div>
    </Transition>
  </Teleport>
</template>
