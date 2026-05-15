<script setup lang="ts">
/**
 * Gestión de Servicios — catálogo completo del atelier.
 * 3 métricas arriba + tabla con filtros por categoría + drawer lateral.
 */
import { Plus, Save, X, Loader2 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

const toast = useToast()

interface Servicio {
  id: string
  nombre: string
  genero: 'FEMENINO' | 'MASCULINO' | 'UNISEX'
  categoria: 'CABALLERO' | 'SENORA' | 'TRATAMIENTO'
  duracionMinutos: number
  precio: number
  precioDescuento: number | null
  descripcion: string
  activo?: boolean
}

const servicios      = ref<Servicio[]>([])
const cargando       = ref(true)
const filtroActivo   = ref<string>('TODOS')
const drawerAbierto  = ref(false)
const guardando      = ref(false)
const eliminando     = ref(false)
const confirmandoEliminar = ref(false)
const servicioEditar = ref<Partial<Servicio>>({})
const servicioAEliminar = ref<Servicio | null>(null)

const filtros = ['TODOS', 'FEMENINO', 'MASCULINO', 'UNISEX']
const labelFiltro: Record<string, string> = {
  TODOS: 'Todos',
  FEMENINO: 'Mujer',
  MASCULINO: 'Hombre',
  UNISEX: 'Unisex',
}

const serviciosFiltrados = computed(() =>
  filtroActivo.value === 'TODOS'
    ? servicios.value
    : servicios.value.filter(s => s.genero === filtroActivo.value),
)

const totalHombre = computed(() => servicios.value.filter(s => s.genero === 'MASCULINO').length)
const totalMujer = computed(() => servicios.value.filter(s => s.genero === 'FEMENINO').length)

onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/v1/servicios')
    servicios.value = data ?? []
  } catch {
    servicios.value = []
    toast.error('No se pudieron cargar los servicios. Revisa la conexión e inténtalo de nuevo.')
  } finally { cargando.value = false }
})

function abrirCrear() {
  servicioEditar.value = { genero: 'FEMENINO', categoria: 'SENORA', duracionMinutos: 30, precio: 0, precioDescuento: null, activo: true }
  drawerAbierto.value = true
}

function abrirEditar(s: Servicio) {
  servicioEditar.value = { ...s, activo: s.activo ?? true }
  drawerAbierto.value = true
}

async function guardar() {
  const precio = Number(servicioEditar.value.precio ?? 0)
  const precioDescuento = Number(servicioEditar.value.precioDescuento ?? 0)

  if (precioDescuento > 0 && precioDescuento >= precio) {
    toast.error('El precio promo debe ser menor que el precio normal')
    return
  }

  servicioEditar.value.precioDescuento = precioDescuento > 0 ? precioDescuento : null

  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (servicioEditar.value.id) {
      const { data } = await api.put(`/v1/servicios/${servicioEditar.value.id}`, servicioEditar.value)
      const idx = servicios.value.findIndex(s => s.id === data.id)
      if (idx !== -1) servicios.value[idx] = data
    } else {
      const { data } = await api.post('/v1/servicios', servicioEditar.value)
      servicios.value.unshift(data)
    }
    drawerAbierto.value = false
    toast.success(servicioEditar.value.id ? 'Servicio actualizado' : 'Servicio creado')
  } catch {
    toast.error('Error al guardar el servicio')
  } finally { guardando.value = false }
}

function pedirEliminar(servicio: Servicio) {
  servicioAEliminar.value = servicio
  confirmandoEliminar.value = true
}

function cancelarEliminar() {
  confirmandoEliminar.value = false
  servicioAEliminar.value = null
}

async function eliminarConfirmado() {
  if (!servicioAEliminar.value?.id) return

  eliminando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/v1/servicios/${servicioAEliminar.value.id}`)
    servicios.value = servicios.value.filter(s => s.id !== servicioAEliminar.value?.id)
    if (servicioEditar.value.id === servicioAEliminar.value.id) drawerAbierto.value = false
    cancelarEliminar()
    toast.success('Servicio eliminado')
  } catch {
    toast.error('Error al eliminar el servicio')
  } finally {
    eliminando.value = false
  }
}

function badgeClase(genero: string): string {
  return {
    MASCULINO: 'bg-secondary-fixed text-on-secondary-fixed-variant',
    FEMENINO: 'bg-tertiary-fixed text-on-tertiary-fixed-variant',
    UNISEX: 'bg-primary-fixed text-on-primary-fixed-variant',
  }[genero] ?? 'bg-surface-container text-on-surface-variant'
}

function labelCategoria(cat: string): string {
  return { CABALLERO: 'Caballero', SENORA: 'Señora', TRATAMIENTO: 'Tratamiento' }[cat] ?? cat
}

function labelGenero(genero: string): string {
  return { MASCULINO: 'Hombre', FEMENINO: 'Mujer', UNISEX: 'Unisex' }[genero] ?? genero
}

function formatEur(n: number): string {
  return new Intl.NumberFormat('es-ES', { style: 'currency', currency: 'EUR' }).format(n)
}

function precioServicio(s: Servicio): number {
  if (s.precioDescuento && s.precioDescuento > 0) return s.precioDescuento
  return s.precio
}
</script>

<template>
  <div class="space-y-5 sm:space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-2xl font-extrabold tracking-tight text-primary mb-1 sm:text-3xl">Gestión de Servicios</h2>
      <p class="text-on-surface-variant text-sm">Catálogo de experiencias y tarifas del atelier</p>
    </div>

    <!-- ── KPI Cards ─────────────────────────────────────── -->
    <div class="grid grid-cols-1 gap-3 sm:grid-cols-3 sm:gap-6">
      <div class="servicio-panel-kpi flex items-center justify-between">
        <div>
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1 sm:text-xs">Total Servicios</p>
          <h3 class="text-3xl font-extrabold text-primary sm:text-4xl">{{ servicios.length }}</h3>
        </div>
        <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-primary-fixed text-xl font-bold text-primary-container sm:h-auto sm:w-auto sm:p-3 sm:text-2xl">#</div>
      </div>
      <div class="servicio-panel-kpi flex items-center justify-between">
        <div>
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1 sm:text-xs">Servicios Hombre</p>
          <h3 class="text-3xl font-extrabold text-primary sm:text-4xl">{{ totalHombre }}</h3>
        </div>
        <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-secondary-fixed text-xl text-on-secondary-fixed-variant sm:h-auto sm:w-auto sm:p-3 sm:text-2xl" aria-hidden="true">♂</div>
      </div>
      <div class="servicio-panel-kpi flex items-center justify-between">
        <div>
          <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant mb-1 sm:text-xs">Servicios Mujer</p>
          <h3 class="text-3xl font-extrabold text-primary sm:text-4xl">{{ totalMujer }}</h3>
        </div>
        <div class="flex h-11 w-11 items-center justify-center rounded-xl bg-tertiary-fixed text-xl text-on-tertiary-fixed-variant sm:h-auto sm:w-auto sm:p-3 sm:text-2xl" aria-hidden="true">♀</div>
      </div>
    </div>

    <!-- ── Tabla ─────────────────────────────────────────── -->
    <div class="servicio-panel-card p-4 sm:p-6">

      <div class="mb-5 flex flex-col gap-3 sm:mb-8 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
        <div class="w-full sm:w-56">
          <label for="servicios-filtro-genero" class="sr-only">Filtrar servicios por público</label>
          <select
            id="servicios-filtro-genero"
            v-model="filtroActivo"
            class="select-field"
            aria-label="Filtrar servicios por público"
          >
            <option v-for="f in filtros" :key="f" :value="f">
              {{ labelFiltro[f] }}
            </option>
          </select>
        </div>
        <button
          class="flex w-full items-center justify-center gap-2 rounded-xl bg-primary-container px-5 py-3 text-sm font-bold text-white transition-all hover:opacity-90 sm:w-auto sm:rounded-full sm:px-6 sm:py-2.5"
          aria-label="Crear nuevo servicio"
          @click="abrirCrear"
        >
          <Plus class="w-4 h-4" aria-hidden="true" />
          Nuevo Servicio
        </button>
      </div>

      <div v-if="cargando" class="flex items-center justify-center py-16">
        <Loader2 class="w-6 h-6 animate-spin text-primary" />
      </div>

      <template v-else>
        <div class="space-y-3 md:hidden">
          <article
            v-for="s in serviciosFiltrados"
            :key="s.id"
            class="rounded-2xl border border-outline-variant/20 bg-white p-4 shadow-sm"
            @click="abrirEditar(s)"
          >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <h3 class="truncate text-sm font-extrabold text-primary">{{ s.nombre }}</h3>
              <p class="mt-1 text-[11px] text-on-surface-variant">{{ labelCategoria(s.categoria) }}</p>
            </div>
            <span class="shrink-0 rounded-full px-3 py-1 text-[10px] font-bold" :class="badgeClase(s.genero)">
              {{ labelGenero(s.genero) }}
            </span>
          </div>

          <div class="mt-4 grid grid-cols-2 gap-3 text-sm">
            <div class="rounded-xl bg-surface-container-low px-3 py-2">
              <p class="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Duración</p>
              <p class="mt-1 font-bold text-on-surface">{{ s.duracionMinutos }} min</p>
            </div>
            <div class="rounded-xl bg-surface-container-low px-3 py-2">
              <p class="text-[10px] font-bold uppercase tracking-wider text-on-surface-variant">Precio</p>
              <p class="mt-1 font-bold text-primary">{{ formatEur(precioServicio(s)) }}</p>
              <p v-if="s.precioDescuento && s.precioDescuento < s.precio" class="text-[11px] text-on-surface-variant line-through">
                {{ formatEur(s.precio) }}
              </p>
            </div>
          </div>

          <div class="mt-4 flex gap-2" @click.stop>
            <button
              class="flex flex-1 items-center justify-center rounded-xl bg-primary-container px-3 py-2.5 text-xs font-bold text-white"
              :aria-label="`Editar servicio: ${s.nombre}`"
              @click="abrirEditar(s)"
            >
              Editar
            </button>
            <button
              class="flex flex-1 items-center justify-center rounded-xl bg-red-50 px-3 py-2.5 text-xs font-bold text-error"
              :aria-label="`Eliminar servicio: ${s.nombre}`"
              @click="pedirEliminar(s)"
            >
              Eliminar
            </button>
          </div>
          </article>

          <div v-if="serviciosFiltrados.length === 0" class="rounded-2xl border border-dashed border-outline-variant/30 px-4 py-10 text-center text-sm text-on-surface-variant">
            No hay servicios en este filtro
          </div>
        </div>

        <div class="hidden overflow-x-auto md:block">
          <table class="w-full text-left border-separate border-spacing-y-3" aria-label="Catálogo de servicios">
          <thead>
            <tr class="text-on-surface-variant/60 text-[10px] uppercase tracking-[0.15em] font-bold">
              <th scope="col" class="pb-4 px-4">Servicio</th>
              <th scope="col" class="pb-4 px-4">Público</th>
              <th scope="col" class="pb-4 px-4">Duración</th>
              <th scope="col" class="pb-4 px-4">Precio</th>
              <th scope="col" class="pb-4 px-4 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="text-sm">
            <tr
              v-for="s in serviciosFiltrados"
              :key="s.id"
              class="group hover:bg-surface-container-low transition-colors rounded-xl cursor-pointer"
              @click="abrirEditar(s)"
            >
              <td class="py-4 px-4 font-bold text-primary rounded-l-xl">{{ s.nombre }}</td>
              <td class="py-4 px-4">
                <div class="flex flex-col gap-1">
                  <span class="w-fit px-3 py-1 rounded-full text-[10px] font-bold" :class="badgeClase(s.genero)">
                    {{ labelGenero(s.genero) }}
                  </span>
                  <span class="text-[11px] text-on-surface-variant">{{ labelCategoria(s.categoria) }}</span>
                </div>
              </td>
              <td class="py-4 px-4 text-on-surface-variant">{{ s.duracionMinutos }} min</td>
              <td class="py-4 px-4 font-bold">
                {{ formatEur(precioServicio(s)) }}
                <span v-if="s.precioDescuento && s.precioDescuento < s.precio" class="ml-2 text-xs text-on-surface-variant font-normal line-through">
                  {{ formatEur(s.precio) }}
                </span>
              </td>
              <td class="py-4 px-4 text-right rounded-r-xl" @click.stop>
                <div class="flex justify-end gap-2">
                  <button
                    class="w-8 h-8 rounded-full hover:bg-primary-container hover:text-white flex items-center justify-center transition-all text-primary-container"
                    :aria-label="`Editar servicio: ${s.nombre}`"
                    @click="abrirEditar(s)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button
                    class="w-8 h-8 rounded-full hover:bg-error hover:text-white flex items-center justify-center transition-all text-error"
                    :aria-label="`Eliminar servicio: ${s.nombre}`"
                    @click="pedirEliminar(s)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="serviciosFiltrados.length === 0">
              <td colspan="5" class="py-12 text-center text-on-surface-variant text-sm">
                No hay servicios en este filtro
              </td>
            </tr>
          </tbody>
          </table>
        </div>
      </template>
    </div>
  </div>

  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="confirmandoEliminar"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4 backdrop-blur-sm"
        @click.self="cancelarEliminar"
      >
        <section class="w-full max-w-md rounded-[28px] border border-outline-variant/15 bg-white p-6 shadow-2xl sm:p-7">
          <div class="space-y-2">
            <h3 class="text-lg font-bold text-primary">Eliminar servicio</h3>
            <p class="text-sm leading-relaxed text-on-surface-variant">
              Vas a eliminar <strong class="text-on-surface">{{ servicioAEliminar?.nombre }}</strong>. Esta acción no se puede deshacer.
            </p>
          </div>

          <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button class="btn-secondary" :disabled="eliminando" @click="cancelarEliminar">
              Cancelar
            </button>
            <button class="btn-danger" :disabled="eliminando" @click="eliminarConfirmado">
              <Loader2 v-if="eliminando" class="h-4 w-4 animate-spin" aria-hidden="true" />
              <span>{{ eliminando ? 'Eliminando...' : 'Sí, eliminar' }}</span>
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>

  <!-- ══════════════════════════════════════════════════════
       DRAWER — Crear / Editar servicio
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div v-if="drawerAbierto" class="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm" @click.self="drawerAbierto = false" />
    </Transition>

    <Transition name="slide-right">
      <aside
        v-if="drawerAbierto"
        role="dialog"
        aria-modal="true"
        :aria-label="servicioEditar.id ? `Editar servicio: ${servicioEditar.nombre}` : 'Nuevo servicio'"
        class="fixed right-0 top-0 z-50 flex h-screen w-full max-w-md flex-col border-l border-outline-variant/20 bg-white shadow-2xl"
      >

        <div class="flex flex-shrink-0 items-center justify-between border-b border-outline-variant/10 p-5 sm:p-8">
          <div>
            <h3 class="text-lg font-bold text-primary sm:text-xl">{{ servicioEditar.id ? 'Detalle del Servicio' : 'Nuevo Servicio' }}</h3>
            <p class="text-xs text-on-surface-variant">{{ servicioEditar.id ? 'Editando configuración actual' : 'Completa los campos' }}</p>
          </div>
          <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" aria-label="Cerrar" @click="drawerAbierto = false">
            <X class="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        <div class="flex-1 space-y-5 overflow-y-auto p-5 sm:space-y-6 sm:p-8">

          <div class="space-y-2">
            <label class="label" for="srv-nombre">Nombre del Servicio</label>
            <input id="srv-nombre" v-model="servicioEditar.nombre" type="text" class="input font-bold" placeholder="Ej. Balayage Premium" />
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <label class="label" for="srv-genero">Público</label>
              <select id="srv-genero" v-model="servicioEditar.genero" class="select-field">
                <option value="FEMENINO">Mujer</option>
                <option value="MASCULINO">Hombre</option>
                <option value="UNISEX">Unisex</option>
              </select>
            </div>

            <div class="space-y-2">
              <label class="label" for="srv-categoria">Categoría</label>
              <select id="srv-categoria" v-model="servicioEditar.categoria" class="select-field">
                <option value="SENORA">Señora</option>
                <option value="CABALLERO">Caballero</option>
                <option value="TRATAMIENTO">Tratamientos</option>
              </select>
            </div>
          </div>

          <div class="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <div class="space-y-2">
              <label class="label" for="srv-duracion">Duración (min)</label>
              <input id="srv-duracion" v-model.number="servicioEditar.duracionMinutos" type="number" min="5" class="input" />
            </div>
            <div class="space-y-2">
              <label class="label" for="srv-precio">Precio (€)</label>
              <input id="srv-precio" v-model.number="servicioEditar.precio" type="number" min="0" step="0.5" class="input font-bold" />
            </div>
          </div>

          <div class="space-y-2">
            <label class="label" for="srv-precio-descuento">Precio promo (€)</label>
            <input
              id="srv-precio-descuento"
              v-model.number="servicioEditar.precioDescuento"
              type="number"
              min="0"
              step="0.5"
              class="input font-bold"
              placeholder="Opcional"
            />
          </div>

          <div class="space-y-2">
            <label class="label" for="srv-descripcion">Descripción</label>
            <textarea id="srv-descripcion" v-model="servicioEditar.descripcion" rows="5" class="input resize-none leading-relaxed" placeholder="Describe el servicio..." />
          </div>

          <div class="flex items-center gap-3">
            <button
              role="switch"
              :aria-checked="servicioEditar.activo ?? true"
              aria-label="Servicio activo"
              class="relative w-10 h-6 rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-primary-container/50"
              :class="(servicioEditar.activo ?? true) ? 'bg-primary-container' : 'bg-surface-container-high'"
              @click="servicioEditar.activo = !(servicioEditar.activo ?? true)"
            >
              <div class="absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform"
                   :class="(servicioEditar.activo ?? true) ? 'translate-x-5' : 'translate-x-1'" aria-hidden="true" />
            </button>
            <span class="text-sm font-medium text-on-surface">Servicio activo</span>
          </div>

        </div>

        <div class="flex-shrink-0 border-t border-outline-variant/10 bg-surface-container-lowest p-5 sm:p-8">
          <button
            class="btn-primary w-full py-3.5 sm:py-4"
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
.servicio-panel-card,
.servicio-panel-kpi {
  background: #fcfcfd;
  border: 1px solid rgb(104 111 122 / 0.1);
  border-radius: 1rem;
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.06);
}

.servicio-panel-kpi {
  padding: 1.5rem;
}

.slide-right-enter-active, .slide-right-leave-active { transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-right-enter-from, .slide-right-leave-to { transform: translateX(100%); }
</style>
