<script setup lang="ts">
/**
 * Inventario — gestión de productos con alertas de stock bajo.
 * KPIs + alertas de stock + tabla con filtros por categoría.
 */
import { Plus, AlertTriangle, Package, Loader2, X, Save } from 'lucide-vue-next'

definePageMeta({ middleware: ['auth', 'admin'] })

// ── Tipos ─────────────────────────────────────────────────
interface Producto {
  id: number
  nombre: string
  categoria: string
  precio: number
  precioDescuento: number | null
  stock: number
  stockMinimo: number
  activo: boolean
}

// ── Estado ────────────────────────────────────────────────
const productos      = ref<Producto[]>([])
const cargando       = ref(true)
const filtroCategoria = ref('TODOS')
const drawerAbierto  = ref(false)
const guardando      = ref(false)
const productoEditar = ref<Partial<Producto>>({})

const categorias = ['TODOS', 'CERA', 'CHAMPU', 'ACONDICIONADOR', 'COLORACION', 'HERRAMIENTA', 'OTRO']
const labelCat: Record<string, string> = {
  TODOS: 'Todos', CERA: 'Cera', CHAMPU: 'Champú', ACONDICIONADOR: 'Acondicionador',
  COLORACION: 'Coloración', HERRAMIENTA: 'Herramienta', OTRO: 'Otro',
}

// ── Computed ──────────────────────────────────────────────
const productosFiltrados = computed(() =>
  filtroCategoria.value === 'TODOS'
    ? productos.value
    : productos.value.filter(p => p.categoria === filtroCategoria.value),
)

const stockBajo      = computed(() => productos.value.filter(p => p.stock <= p.stockMinimo))
const valorInventario = computed(() =>
  productos.value.reduce((acc, p) => acc + p.precio * p.stock, 0),
)

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/v1/productos')
    productos.value = data
  } catch {
    // vacío si falla
  } finally {
    cargando.value = false
  }
})

// ── Acciones ──────────────────────────────────────────────
function abrirCrear() {
  productoEditar.value = { categoria: 'OTRO', precio: 0, stock: 0, stockMinimo: 5, activo: true }
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
  } catch { /* toast */ } finally {
    guardando.value = false
  }
}

async function eliminar(id: number) {
  if (!confirm('¿Eliminar este producto?')) return
  const { api } = await import('~/infrastructure/http/api')
  await api.delete(`/v1/productos/${id}`)
  productos.value = productos.value.filter(p => p.id !== id)
}

function badgeStock(p: Producto): string {
  if (p.stock === 0) return 'bg-error/10 text-error font-bold'
  if (p.stock <= p.stockMinimo) return 'bg-amber-100 text-amber-700 font-bold'
  return 'bg-green-100 text-green-700'
}

function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Inventario</h2>
      <p class="text-on-surface-variant text-sm">Control de stock y productos del atelier</p>
    </div>

    <!-- ── KPI Cards ─────────────────────────────────────── -->
    <div class="grid grid-cols-3 gap-6">

      <div class="bg-primary-container text-white p-6 rounded-2xl shadow-lg shadow-primary-container/20">
        <p class="text-[10px] font-bold uppercase tracking-widest text-white/70 mb-1">Valor del Inventario</p>
        <h3 class="text-3xl font-extrabold">{{ formatEur(valorInventario) }}</h3>
        <p class="text-xs text-white/60 mt-2">{{ productos.length }} productos en total</p>
      </div>

      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Stock bajo</p>
        <h3 class="text-4xl font-extrabold text-primary">{{ stockBajo.length }}</h3>
        <p class="text-xs text-error mt-2 font-medium">Requieren reposición</p>
      </div>

      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1">Sin stock</p>
        <h3 class="text-4xl font-extrabold text-primary">{{ productos.filter(p => p.stock === 0).length }}</h3>
        <p class="text-xs text-on-surface-variant mt-2">Agotados actualmente</p>
      </div>

    </div>

    <!-- ── Alertas de stock bajo ──────────────────────────── -->
    <div v-if="stockBajo.length > 0" class="card p-5 !bg-amber-50 border-amber-200">
      <div class="flex items-center gap-2 mb-3">
        <AlertTriangle class="w-4 h-4 text-amber-600" />
        <p class="text-sm font-bold text-amber-800">{{ stockBajo.length }} producto{{ stockBajo.length !== 1 ? 's' : '' }} con stock bajo</p>
      </div>
      <div class="flex flex-wrap gap-2">
        <span
          v-for="p in stockBajo"
          :key="p.id"
          class="px-3 py-1 bg-amber-100 text-amber-800 rounded-full text-xs font-medium cursor-pointer hover:bg-amber-200 transition-colors"
          @click="abrirEditar(p)"
        >
          {{ p.nombre }} ({{ p.stock }} uds)
        </span>
      </div>
    </div>

    <!-- ── Tabla principal ────────────────────────────────── -->
    <div class="card p-6">

      <!-- Filtros + acción -->
      <div class="flex items-center justify-between gap-4 mb-8 flex-wrap">
        <div class="flex items-center gap-2 flex-wrap">
          <button
            v-for="cat in categorias"
            :key="cat"
            class="px-5 py-2 rounded-full text-xs font-bold transition-all"
            :class="filtroCategoria === cat
              ? 'bg-primary-container text-white shadow-md shadow-primary-container/20'
              : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
            @click="filtroCategoria = cat"
          >
            {{ labelCat[cat] }}
          </button>
        </div>
        <button
          class="flex items-center gap-2 bg-primary-container text-white px-6 py-2.5 rounded-full font-bold text-sm hover:opacity-90 transition-all"
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

      <!-- Tabla -->
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-separate border-spacing-y-3">
          <thead>
            <tr class="text-on-surface-variant/60 text-[10px] uppercase tracking-[0.15em] font-bold">
              <th class="pb-4 px-4">Producto</th>
              <th class="pb-4 px-4">Categoría</th>
              <th class="pb-4 px-4">Precio</th>
              <th class="pb-4 px-4">Stock</th>
              <th class="pb-4 px-4 text-right">Acciones</th>
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
                {{ formatEur(p.precio) }}
                <span v-if="p.precioDescuento" class="ml-2 text-xs text-green-600 font-normal line-through">
                  {{ formatEur(p.precioDescuento) }}
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
                No hay productos en esta categoría
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       DRAWER — Crear / Editar producto
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div v-if="drawerAbierto" class="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm" @click.self="drawerAbierto = false" />
    </Transition>

    <Transition name="slide-right">
      <aside v-if="drawerAbierto" class="fixed right-0 top-0 h-screen w-96 bg-white shadow-2xl border-l border-outline-variant/20 z-50 flex flex-col">

        <div class="p-8 border-b border-outline-variant/10 flex items-center justify-between flex-shrink-0">
          <div>
            <h3 class="text-xl font-bold text-primary">{{ productoEditar.id ? 'Editar Producto' : 'Nuevo Producto' }}</h3>
            <p class="text-xs text-on-surface-variant">Gestión de inventario</p>
          </div>
          <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" @click="drawerAbierto = false">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 space-y-6">

          <div class="space-y-2">
            <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Nombre</label>
            <input v-model="productoEditar.nombre" type="text" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container font-bold text-primary" placeholder="Nombre del producto" />
          </div>

          <div class="space-y-2">
            <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Categoría</label>
            <select v-model="productoEditar.categoria" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container">
              <option v-for="cat in categorias.filter(c => c !== 'TODOS')" :key="cat" :value="cat">{{ labelCat[cat] }}</option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Precio (€)</label>
              <input v-model.number="productoEditar.precio" type="number" min="0" step="0.01" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">P. Descuento</label>
              <input v-model.number="productoEditar.precioDescuento" type="number" min="0" step="0.01" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container" placeholder="Opcional" />
            </div>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Stock actual</label>
              <input v-model.number="productoEditar.stock" type="number" min="0" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container" />
            </div>
            <div class="space-y-2">
              <label class="text-[10px] uppercase tracking-widest font-bold text-on-surface-variant px-1">Stock mínimo</label>
              <input v-model.number="productoEditar.stockMinimo" type="number" min="0" class="w-full bg-surface-container-highest border-none rounded-xl px-4 py-3 text-sm focus:ring-2 focus:ring-primary-container" />
            </div>
          </div>

        </div>

        <div class="p-8 border-t border-outline-variant/10 bg-surface-container-lowest flex-shrink-0">
          <button
            class="w-full bg-primary-container text-white py-4 rounded-xl font-bold hover:shadow-lg hover:shadow-primary-container/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
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
.slide-right-enter-active, .slide-right-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
</style>
