<script setup lang="ts">
definePageMeta({ middleware: 'auth' })

import { ShoppingCart, X, Loader2, Search, Plus, Minus, Trash2 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

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

interface LineaCarrito {
  producto: Producto
  cantidad: number
}

interface VentaAgrupadaResponse {
  id: string
  numero: string
  vendedorNombre: string
  metodoPago: MetodoPago
  total: number
  lineas: Array<{ id: string; productoId: string; productoNombre: string; cantidad: number; precioUnitario: number; total: number }>
  productosActualizados: Producto[]
}

type MetodoPago = 'EFECTIVO' | 'TARJETA' | 'BIZUM' | 'OTRO'

const productos       = ref<Producto[]>([])
const cargando        = ref(true)
const busqueda        = ref('')
const categoriaFiltro = ref('TODOS')
const categorias      = ['TODOS', 'CERA', 'CHAMPU', 'ACONDICIONADOR', 'COLORACION', 'HERRAMIENTA', 'OTRO']
const opcionesCategoria = computed(() =>
  categorias.map(cat => ({ value: cat, label: etiCategoria(cat) })),
)

const toast = useToast()
const carrito         = ref<LineaCarrito[]>([])
const procesando      = ref(false)
const errorVenta      = ref('')
const carritoAbierto  = ref(false)
const metodoPago      = ref<MetodoPago>('TARJETA')
const ultimaVenta     = ref<VentaAgrupadaResponse | null>(null)

const metodosPago: Array<{ key: MetodoPago; label: string }> = [
  { key: 'TARJETA', label: 'Tarjeta' },
  { key: 'EFECTIVO', label: 'Efectivo' },
  { key: 'BIZUM', label: 'Bizum' },
  { key: 'OTRO', label: 'Otro' },
]

const productosFiltrados = computed(() => {
  let lista = productos.value.filter(p => p.activo && p.stock > 0)
  if (categoriaFiltro.value !== 'TODOS') lista = lista.filter(p => p.categoria === categoriaFiltro.value)
  if (busqueda.value.trim()) {
    const q = busqueda.value.toLowerCase()
    lista = lista.filter(p => p.nombre.toLowerCase().includes(q))
  }
  return lista
})

function precioFinal(p: Producto): number {
  return p.precioDescuento && p.precioDescuento > 0 ? p.precioDescuento : p.precio
}

const totalCarrito = computed(() =>
  carrito.value.reduce((acc, l) => acc + precioFinal(l.producto) * l.cantidad, 0),
)

const totalUnidades = computed(() => carrito.value.reduce((acc, l) => acc + l.cantidad, 0))

function formatEur(v: number) {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(v)
}

function etiCategoria(cat: string) {
  const mapa: Record<string, string> = {
    TODOS: 'Todas las categorias',
    CHAMPU: 'Champú', ACONDICIONADOR: 'Acond.', COLORACION: 'Color',
    CERA: 'Cera', HERRAMIENTA: 'Herramienta', OTRO: 'Otro',
  }
  return mapa[cat] ?? cat
}

function cantidadEnCarrito(productoId: string): number {
  return carrito.value.find(l => l.producto.id === productoId)?.cantidad ?? 0
}

function stockDisponible(p: Producto): number {
  return p.stock - cantidadEnCarrito(p.id)
}

function agregarAlCarrito(p: Producto) {
  if (stockDisponible(p) <= 0) return
  const linea = carrito.value.find(l => l.producto.id === p.id)
  if (linea) linea.cantidad++
  else carrito.value.push({ producto: p, cantidad: 1 })
  carritoAbierto.value = true
}

function cambiarCantidad(productoId: string, delta: number) {
  const idx = carrito.value.findIndex(l => l.producto.id === productoId)
  if (idx === -1) return
  const linea = carrito.value[idx]
  const nueva = linea.cantidad + delta
  if (nueva <= 0) carrito.value.splice(idx, 1)
  else if (nueva <= linea.producto.stock) linea.cantidad = nueva
}

function quitarLinea(productoId: string) {
  carrito.value = carrito.value.filter(l => l.producto.id !== productoId)
}

function vaciarCarrito() {
  carrito.value = []
  errorVenta.value = ''
  ultimaVenta.value = null
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

async function confirmarVenta() {
  if (!carrito.value.length) return
  procesando.value = true
  errorVenta.value = ''
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post<VentaAgrupadaResponse>('/v1/productos/ventas', {
      metodoPago: metodoPago.value,
      lineas: carrito.value.map(linea => ({
        productoId: linea.producto.id,
        cantidad: linea.cantidad,
      })),
    })
    for (const productoActualizado of data.productosActualizados || []) {
      const idx = productos.value.findIndex(p => p.id === productoActualizado.id)
      if (idx !== -1) productos.value[idx] = productoActualizado
    }
    ultimaVenta.value = data
    toast.success(`Venta ${data.numero} registrada · ${formatEur(data.total)}`)
    carrito.value = []
    carritoAbierto.value = false
  } catch (err) {
    errorVenta.value = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      ?? 'No se pudo registrar la venta.'
  } finally {
    procesando.value = false
  }
}
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-6 min-h-0" :class="carritoAbierto ? 'pr-0' : ''">

    <!-- ── Productos ───────────────────────────────────────── -->
    <div class="flex-1 min-w-0 space-y-6">

      <div>
        <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Ventas</h2>
        <p class="text-on-surface-variant text-sm">Añade productos al carrito y confirma la venta</p>
      </div>

      <!-- Filtros -->
      <div class="flex flex-col xl:flex-row xl:items-center gap-4">
        <div class="relative min-w-0 flex-1 sm:min-w-[18rem]">
          <div class="absolute left-3 inset-y-0 flex items-center pointer-events-none">
            <Search class="w-4 h-4 text-on-surface-variant" aria-hidden="true" />
          </div>
          <label for="ventas-busqueda" class="sr-only">Buscar producto</label>
          <input
            id="ventas-busqueda"
            v-model="busqueda"
            type="text"
            placeholder="Buscar producto..."
            aria-label="Buscar producto por nombre"
            class="input pl-9"
          />
        </div>
        <div class="w-full xl:w-64">
          <label for="ventas-categoria" class="sr-only">Filtrar por categoría</label>
          <AppSelect
            id="ventas-categoria"
            v-model="categoriaFiltro"
            :options="opcionesCategoria"
            aria-label="Filtrar productos por categoría"
          />
        </div>
      </div>

      <!-- Spinner -->
      <div v-if="cargando" class="flex items-center justify-center py-20">
        <Loader2 class="w-6 h-6 animate-spin text-primary" />
      </div>

      <!-- Grid de productos -->
      <div v-else class="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        <div
          v-for="p in productosFiltrados"
          :key="p.id"
          class="card p-5 flex flex-col gap-3 hover:shadow-md transition-shadow"
        >
          <div class="flex items-center justify-between">
            <span class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant bg-surface-container px-2 py-1 rounded-full">
              {{ etiCategoria(p.categoria) }}
            </span>
            <span class="text-[10px] font-bold" :class="p.stock <= p.stockMinimo ? 'text-amber-500' : 'text-green-600'">
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

            <!-- Control cantidad en carrito o botón añadir -->
            <div v-if="cantidadEnCarrito(p.id) > 0" class="flex items-center gap-1" :aria-label="`${p.nombre} en carrito: ${cantidadEnCarrito(p.id)}`">
              <button
                class="min-w-10 min-h-10 rounded-full bg-surface-container flex items-center justify-center hover:bg-surface-container-high transition-colors"
                :aria-label="`Quitar uno de ${p.nombre}`"
                @click="cambiarCantidad(p.id, -1)"
              >
                <Minus class="w-3.5 h-3.5 text-on-surface-variant" aria-hidden="true" />
              </button>
              <span class="w-7 text-center text-sm font-bold text-primary" aria-live="polite">{{ cantidadEnCarrito(p.id) }}</span>
              <button
                class="min-w-10 min-h-10 rounded-full flex items-center justify-center transition-colors"
                :class="stockDisponible(p) > 0
                  ? 'bg-primary-container hover:opacity-90'
                  : 'bg-surface-container cursor-not-allowed opacity-40'"
                :disabled="stockDisponible(p) <= 0"
                :aria-label="`Añadir uno más de ${p.nombre}`"
                @click="cambiarCantidad(p.id, 1)"
              >
                <Plus class="w-3.5 h-3.5 text-white" aria-hidden="true" />
              </button>
            </div>
            <button
              v-else
              class="min-h-11 flex items-center gap-1.5 bg-primary-container text-white px-4 rounded-full text-xs font-bold hover:opacity-90 transition-opacity"
              :aria-label="`Añadir ${p.nombre} al carrito`"
              @click="agregarAlCarrito(p)"
            >
              <Plus class="w-3.5 h-3.5" aria-hidden="true" />
              Añadir
            </button>
          </div>
        </div>

        <div v-if="productosFiltrados.length === 0" class="col-span-full text-center py-16 text-sm text-on-surface-variant">
          No hay productos disponibles con stock
        </div>
      </div>
    </div>

    <!-- ── Panel carrito ────────────────────────────────────── -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-x-8"
      enter-to-class="opacity-100 translate-x-0"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-x-0"
      leave-to-class="opacity-0 translate-x-8"
    >
      <div
        v-if="carritoAbierto"
        class="w-full lg:w-80 flex-shrink-0 self-start lg:sticky lg:top-6"
      >
        <div class="card p-0 overflow-hidden flex flex-col" style="max-height: calc(100vh - 7rem)">

          <!-- Cabecera carrito -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-outline-variant/10">
            <div class="flex items-center gap-2">
              <ShoppingCart class="w-4 h-4 text-primary" aria-hidden="true" />
              <span class="font-bold text-on-surface text-sm">Carrito</span>
              <span
                v-if="totalUnidades > 0"
                class="min-w-[20px] h-5 px-1 bg-primary-container text-white text-[10px] font-bold rounded-full flex items-center justify-center"
                aria-hidden="true"
              >
                {{ totalUnidades }}
              </span>
            </div>
            <button
              class="p-1.5 rounded-full hover:bg-surface-container-low text-on-surface-variant transition-colors"
              aria-label="Cerrar carrito"
              @click="carritoAbierto = false"
            >
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <template v-if="carrito.length > 0">
            <!-- Lista de líneas -->
            <div class="flex-1 overflow-y-auto divide-y divide-outline-variant/10">
              <div
                v-for="linea in carrito"
                :key="linea.producto.id"
                class="flex items-start gap-3 px-5 py-3"
              >
                <div class="flex-1 min-w-0">
                  <p class="text-xs font-bold text-on-surface leading-snug truncate">{{ linea.producto.nombre }}</p>
                  <p class="text-[10px] text-on-surface-variant mt-0.5">{{ formatEur(precioFinal(linea.producto)) }} / ud</p>
                </div>

                <div class="flex items-center gap-1 flex-shrink-0">
                  <button
                    class="min-w-9 min-h-9 rounded-full bg-surface-container flex items-center justify-center hover:bg-surface-container-high transition-colors"
                    :aria-label="`Quitar uno de ${linea.producto.nombre}`"
                    @click="cambiarCantidad(linea.producto.id, -1)"
                  >
                    <Minus class="w-3 h-3 text-on-surface-variant" aria-hidden="true" />
                  </button>
                  <span class="w-5 text-center text-xs font-bold text-primary">{{ linea.cantidad }}</span>
                  <button
                    class="min-w-9 min-h-9 rounded-full flex items-center justify-center transition-colors"
                    :class="stockDisponible(linea.producto) > 0
                      ? 'bg-primary-container hover:opacity-90'
                      : 'bg-surface-container cursor-not-allowed opacity-40'"
                    :disabled="stockDisponible(linea.producto) <= 0"
                    :aria-label="`Añadir uno más de ${linea.producto.nombre}`"
                    @click="cambiarCantidad(linea.producto.id, 1)"
                  >
                    <Plus class="w-3 h-3 text-white" aria-hidden="true" />
                  </button>
                  <button
                    class="ml-1 min-w-9 min-h-9 rounded-full hover:bg-error/10 flex items-center justify-center transition-colors"
                    :aria-label="`Eliminar ${linea.producto.nombre} del carrito`"
                    @click="quitarLinea(linea.producto.id)"
                  >
                    <Trash2 class="w-3 h-3 text-error" aria-hidden="true" />
                  </button>
                </div>
              </div>

              <div v-if="carrito.length === 0" class="px-5 py-8 text-center text-xs text-on-surface-variant">
                Carrito vacío
              </div>
            </div>

            <!-- Total y acciones -->
            <div class="border-t border-outline-variant/10 px-5 py-4 space-y-3">
              <div class="space-y-1">
                <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant">Método de pago</p>
                <div class="grid grid-cols-2 gap-1 bg-surface-container-low p-1 rounded-xl" role="group" aria-label="Método de pago">
                  <button
                    v-for="metodo in metodosPago"
                    :key="metodo.key"
                    class="min-h-10 px-2 rounded-lg text-xs font-bold transition-all"
                    :class="metodoPago === metodo.key
                      ? 'bg-white shadow-sm text-primary'
                      : 'text-on-surface-variant hover:text-on-surface'"
                    @click="metodoPago = metodo.key"
                    :aria-pressed="metodoPago === metodo.key"
                  >
                    {{ metodo.label }}
                  </button>
                </div>
              </div>

              <div class="flex items-center justify-between">
                <span class="text-sm text-on-surface-variant">Total</span>
                <span class="text-xl font-extrabold text-primary">{{ formatEur(totalCarrito) }}</span>
              </div>

              <div v-if="errorVenta" class="rounded-xl bg-error/5 border border-error/10 px-3 py-2 text-xs text-error">
                {{ errorVenta }}
              </div>

              <button
                class="w-full flex items-center justify-center gap-2 bg-primary-container text-white py-3 rounded-xl text-sm font-bold hover:shadow-lg hover:shadow-primary-container/30 transition-all disabled:opacity-50"
                :disabled="procesando || carrito.length === 0"
                @click="confirmarVenta"
              >
                <Loader2 v-if="procesando" class="w-4 h-4 animate-spin" />
                <ShoppingCart v-else class="w-4 h-4" />
                {{ procesando ? 'Procesando...' : 'Confirmar venta' }}
              </button>

              <button
                class="w-full min-h-10 text-center text-xs text-on-surface-variant hover:text-error transition-colors"
                @click="vaciarCarrito"
              >
                Vaciar carrito
              </button>
            </div>
          </template>

          <div v-else class="px-5 py-8 text-center text-sm text-on-surface-variant">
            El carrito está vacío.
          </div>

        </div>
      </div>
    </Transition>

  </div>

  <!-- Botón flotante carrito (cuando está oculto) -->
  <Transition name="fade">
    <button
      v-if="!carritoAbierto && totalUnidades > 0"
      class="fixed bottom-6 right-6 z-40 flex items-center gap-2 bg-primary-container text-white px-5 py-3 rounded-full shadow-lg hover:shadow-xl hover:shadow-primary-container/30 font-bold text-sm transition-all"
      :aria-label="`Abrir carrito, ${totalUnidades} producto${totalUnidades > 1 ? 's' : ''}, total ${formatEur(totalCarrito)}`"
      @click="carritoAbierto = true"
    >
      <ShoppingCart class="w-4 h-4" aria-hidden="true" />
      {{ totalUnidades }} producto{{ totalUnidades > 1 ? 's' : '' }} · {{ formatEur(totalCarrito) }}
    </button>
  </Transition>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s, transform 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; transform: translateY(8px); }
</style>
