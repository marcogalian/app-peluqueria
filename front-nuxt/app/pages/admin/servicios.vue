<script setup lang="ts">
/**
 * Gestión de Servicios — catálogo completo del atelier.
 * 3 métricas arriba + tabla con filtros por categoría + drawer lateral.
 */
import { Plus, Save, X, Loader2 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

definePageMeta({ middleware: ['auth', 'admin'] })

const toast = useToast()

interface Servicio {
  id: string
  nombre: string
  genero: 'FEMENINO' | 'MASCULINO' | 'UNISEX'
  categoria: 'CABALLERO' | 'SENORA' | 'TRATAMIENTO'
  duracionMinutos: number
  precio: number
  descripcion: string
  activo?: boolean
}

const servicios      = ref<Servicio[]>([])
const cargando       = ref(true)
const filtroActivo   = ref<string>('TODOS')
const drawerAbierto  = ref(false)
const guardando      = ref(false)
const servicioEditar = ref<Partial<Servicio>>({})

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
    servicios.value = data
  } catch { /* vacío */ } finally { cargando.value = false }
})

function abrirCrear() {
  servicioEditar.value = { genero: 'FEMENINO', categoria: 'SENORA', duracionMinutos: 30, precio: 0, activo: true }
  drawerAbierto.value = true
}

function abrirEditar(s: Servicio) {
  servicioEditar.value = { ...s, activo: s.activo ?? true }
  drawerAbierto.value = true
}

async function guardar() {
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

async function eliminar(id: number) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/v1/servicios/${id}`)
    servicios.value = servicios.value.filter(s => s.id !== id)
    if (servicioEditar.value.id === id) drawerAbierto.value = false
    toast.success('Servicio eliminado')
  } catch {
    toast.error('Error al eliminar el servicio')
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
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Gestión de Servicios</h2>
      <p class="text-on-surface-variant text-sm">Catálogo de experiencias y tarifas del atelier</p>
    </div>

    <!-- ── KPI Cards ─────────────────────────────────────── -->
    <div class="grid grid-cols-3 gap-6">
      <div class="card-kpi flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Total Servicios</p>
          <h3 class="text-4xl font-extrabold text-primary">{{ servicios.length }}</h3>
        </div>
        <div class="p-3 bg-primary-fixed rounded-xl text-primary-container text-2xl font-bold">#</div>
      </div>
      <div class="card-kpi flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Servicios Hombre</p>
          <h3 class="text-4xl font-extrabold text-primary">{{ totalHombre }}</h3>
        </div>
        <div class="p-3 bg-secondary-fixed rounded-xl text-on-secondary-fixed-variant text-2xl" aria-hidden="true">♂</div>
      </div>
      <div class="card-kpi flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Servicios Mujer</p>
          <h3 class="text-4xl font-extrabold text-primary">{{ totalMujer }}</h3>
        </div>
        <div class="p-3 bg-tertiary-fixed rounded-xl text-on-tertiary-fixed-variant text-2xl" aria-hidden="true">♀</div>
      </div>
    </div>

    <!-- ── Tabla ─────────────────────────────────────────── -->
    <div class="card p-6">

      <div class="flex items-center justify-between gap-4 mb-8 flex-wrap">
        <div class="w-full sm:w-56">
          <label for="servicios-filtro-genero" class="sr-only">Filtrar servicios por público</label>
          <select
            id="servicios-filtro-genero"
            v-model="filtroActivo"
            class="select-field rounded-full bg-surface-container-lowest"
            aria-label="Filtrar servicios por público"
          >
            <option v-for="f in filtros" :key="f" :value="f">
              {{ labelFiltro[f] }}
            </option>
          </select>
        </div>
        <button
          class="flex items-center gap-2 bg-primary-container text-white px-6 py-2.5 rounded-full font-bold text-sm hover:opacity-90 transition-all"
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

      <div v-else class="overflow-x-auto">
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
              <td class="py-4 px-4 font-bold">{{ formatEur(s.precio) }}</td>
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
                    @click="eliminar(s.id)"
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
    </div>
  </div>

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
        class="fixed right-0 top-0 h-screen w-96 bg-white shadow-2xl border-l border-outline-variant/20 z-50 flex flex-col"
      >

        <div class="p-8 border-b border-outline-variant/10 flex items-center justify-between flex-shrink-0">
          <div>
            <h3 class="text-xl font-bold text-primary">{{ servicioEditar.id ? 'Detalle del Servicio' : 'Nuevo Servicio' }}</h3>
            <p class="text-xs text-on-surface-variant">{{ servicioEditar.id ? 'Editando configuración actual' : 'Completa los campos' }}</p>
          </div>
          <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" aria-label="Cerrar" @click="drawerAbierto = false">
            <X class="w-5 h-5" aria-hidden="true" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 space-y-6">

          <div class="space-y-2">
            <label class="label" for="srv-nombre">Nombre del Servicio</label>
            <input id="srv-nombre" v-model="servicioEditar.nombre" type="text" class="input font-bold" placeholder="Ej. Balayage Premium" />
          </div>

          <div class="grid grid-cols-2 gap-4">
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

          <div class="grid grid-cols-2 gap-4">
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

        <div class="p-8 border-t border-outline-variant/10 bg-surface-container-lowest flex-shrink-0">
          <button
            class="btn-primary w-full py-4"
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
