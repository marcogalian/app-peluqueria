<script setup lang="ts">
/**
 * Gestión de Servicios — catálogo completo del atelier.
 * 3 métricas arriba + tabla con filtros por categoría + drawer lateral.
 */
import { Plus, Save, X, Loader2 } from 'lucide-vue-next'

definePageMeta({ middleware: ['auth', 'admin'] })

interface Servicio {
  id: number
  nombre: string
  categoria: 'CABALLERO' | 'SENORA' | 'TRATAMIENTO'
  duracionMinutos: number
  precio: number
  descripcion: string
  activo: boolean
}

const servicios      = ref<Servicio[]>([])
const cargando       = ref(true)
const filtroActivo   = ref<string>('TODOS')
const drawerAbierto  = ref(false)
const guardando      = ref(false)
const servicioEditar = ref<Partial<Servicio>>({})

const filtros = ['TODOS', 'CABALLERO', 'SENORA', 'TRATAMIENTO']
const labelFiltro: Record<string, string> = {
  TODOS: 'Todos', CABALLERO: 'Caballero', SENORA: 'Señora', TRATAMIENTO: 'Tratamientos',
}

const serviciosFiltrados = computed(() =>
  filtroActivo.value === 'TODOS'
    ? servicios.value
    : servicios.value.filter(s => s.categoria === filtroActivo.value),
)

const totalCaballero    = computed(() => servicios.value.filter(s => s.categoria === 'CABALLERO').length)
const totalSenora       = computed(() => servicios.value.filter(s => s.categoria === 'SENORA').length)

onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/servicios')
    servicios.value = data
  } catch { /* vacío */ } finally { cargando.value = false }
})

function abrirCrear() {
  servicioEditar.value = { categoria: 'CABALLERO', duracionMinutos: 30, precio: 0, activo: true }
  drawerAbierto.value = true
}

function abrirEditar(s: Servicio) {
  servicioEditar.value = { ...s }
  drawerAbierto.value = true
}

async function guardar() {
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (servicioEditar.value.id) {
      const { data } = await api.put(`/servicios/${servicioEditar.value.id}`, servicioEditar.value)
      const idx = servicios.value.findIndex(s => s.id === data.id)
      if (idx !== -1) servicios.value[idx] = data
    } else {
      const { data } = await api.post('/servicios', servicioEditar.value)
      servicios.value.unshift(data)
    }
    drawerAbierto.value = false
  } catch { /* toast */ } finally { guardando.value = false }
}

async function eliminar(id: number) {
  if (!confirm('¿Eliminar este servicio?')) return
  const { api } = await import('~/infrastructure/http/api')
  await api.delete(`/servicios/${id}`)
  servicios.value = servicios.value.filter(s => s.id !== id)
  if (servicioEditar.value.id === id) drawerAbierto.value = false
}

function badgeClase(cat: string): string {
  return {
    CABALLERO:  'bg-secondary-fixed text-on-secondary-fixed-variant',
    SENORA:     'bg-tertiary-fixed text-on-tertiary-fixed-variant',
    TRATAMIENTO:'bg-primary-fixed text-on-primary-fixed-variant',
  }[cat] ?? 'bg-surface-container text-on-surface-variant'
}

function labelCategoria(cat: string): string {
  return { CABALLERO: 'Caballero', SENORA: 'Señora', TRATAMIENTO: 'Tratamiento' }[cat] ?? cat
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
          <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Servicios Caballero</p>
          <h3 class="text-4xl font-extrabold text-primary">{{ totalCaballero }}</h3>
        </div>
        <div class="p-3 bg-secondary-fixed rounded-xl text-on-secondary-fixed-variant text-2xl">♂</div>
      </div>
      <div class="card-kpi flex items-center justify-between">
        <div>
          <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant mb-1">Servicios Señora</p>
          <h3 class="text-4xl font-extrabold text-primary">{{ totalSenora }}</h3>
        </div>
        <div class="p-3 bg-tertiary-fixed rounded-xl text-on-tertiary-fixed-variant text-2xl">♀</div>
      </div>
    </div>

    <!-- ── Tabla ─────────────────────────────────────────── -->
    <div class="card p-6">

      <div class="flex items-center justify-between gap-4 mb-8 flex-wrap">
        <div class="flex items-center gap-2 flex-wrap">
          <button
            v-for="f in filtros"
            :key="f"
            class="px-6 py-2 rounded-full text-xs font-bold transition-all"
            :class="filtroActivo === f
              ? 'bg-primary-container text-white shadow-md shadow-primary-container/20'
              : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'"
            @click="filtroActivo = f"
          >
            {{ labelFiltro[f] }}
          </button>
        </div>
        <button
          class="flex items-center gap-2 bg-primary-container text-white px-6 py-2.5 rounded-full font-bold text-sm hover:opacity-90 transition-all"
          @click="abrirCrear"
        >
          <Plus class="w-4 h-4" />
          Nuevo Servicio
        </button>
      </div>

      <div v-if="cargando" class="flex items-center justify-center py-16">
        <Loader2 class="w-6 h-6 animate-spin text-primary" />
      </div>

      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-separate border-spacing-y-3">
          <thead>
            <tr class="text-on-surface-variant/60 text-[10px] uppercase tracking-[0.15em] font-bold">
              <th class="pb-4 px-4">Servicio</th>
              <th class="pb-4 px-4">Categoría</th>
              <th class="pb-4 px-4">Duración</th>
              <th class="pb-4 px-4">Precio</th>
              <th class="pb-4 px-4 text-right">Acciones</th>
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
                <span class="px-3 py-1 rounded-full text-[10px] font-bold" :class="badgeClase(s.categoria)">
                  {{ labelCategoria(s.categoria) }}
                </span>
              </td>
              <td class="py-4 px-4 text-on-surface-variant">{{ s.duracionMinutos }} min</td>
              <td class="py-4 px-4 font-bold">{{ formatEur(s.precio) }}</td>
              <td class="py-4 px-4 text-right rounded-r-xl" @click.stop>
                <div class="flex justify-end gap-2">
                  <button
                    class="w-8 h-8 rounded-full hover:bg-primary-container hover:text-white flex items-center justify-center transition-all text-primary-container"
                    @click="abrirEditar(s)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                    </svg>
                  </button>
                  <button
                    class="w-8 h-8 rounded-full hover:bg-error hover:text-white flex items-center justify-center transition-all text-error"
                    @click="eliminar(s.id)"
                  >
                    <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                    </svg>
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="serviciosFiltrados.length === 0">
              <td colspan="5" class="py-12 text-center text-on-surface-variant text-sm">
                No hay servicios en esta categoría
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
      <aside v-if="drawerAbierto" class="fixed right-0 top-0 h-screen w-96 bg-white shadow-2xl border-l border-outline-variant/20 z-50 flex flex-col">

        <div class="p-8 border-b border-outline-variant/10 flex items-center justify-between flex-shrink-0">
          <div>
            <h3 class="text-xl font-bold text-primary">{{ servicioEditar.id ? 'Detalle del Servicio' : 'Nuevo Servicio' }}</h3>
            <p class="text-xs text-on-surface-variant">{{ servicioEditar.id ? 'Editando configuración actual' : 'Completa los campos' }}</p>
          </div>
          <button class="p-2 hover:bg-surface-container-low rounded-full text-on-surface-variant" @click="drawerAbierto = false">
            <X class="w-5 h-5" />
          </button>
        </div>

        <div class="flex-1 overflow-y-auto p-8 space-y-6">

          <div class="space-y-2">
            <label class="label">Nombre del Servicio</label>
            <input v-model="servicioEditar.nombre" type="text" class="input font-bold" placeholder="Ej. Balayage Premium" />
          </div>

          <div class="space-y-2">
            <label class="label">Categoría</label>
            <select v-model="servicioEditar.categoria" class="input">
              <option value="SENORA">Señora</option>
              <option value="CABALLERO">Caballero</option>
              <option value="TRATAMIENTO">Tratamientos</option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-2">
              <label class="label">Duración (min)</label>
              <input v-model.number="servicioEditar.duracionMinutos" type="number" min="5" class="input" />
            </div>
            <div class="space-y-2">
              <label class="label">Precio (€)</label>
              <input v-model.number="servicioEditar.precio" type="number" min="0" step="0.5" class="input font-bold" />
            </div>
          </div>

          <div class="space-y-2">
            <label class="label">Descripción</label>
            <textarea v-model="servicioEditar.descripcion" rows="5" class="input resize-none leading-relaxed" placeholder="Describe el servicio..." />
          </div>

          <label class="flex items-center gap-3 cursor-pointer">
            <div
              class="relative w-10 h-6 rounded-full transition-colors"
              :class="servicioEditar.activo ? 'bg-primary-container' : 'bg-surface-container-high'"
              @click="servicioEditar.activo = !servicioEditar.activo"
            >
              <div class="absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform"
                   :class="servicioEditar.activo ? 'translate-x-5' : 'translate-x-1'" />
            </div>
            <span class="text-sm font-medium text-on-surface">Servicio activo</span>
          </label>

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
