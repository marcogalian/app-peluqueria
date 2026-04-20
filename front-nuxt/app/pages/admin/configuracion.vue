<script setup lang="ts">
/**
 * Configuración — ajustes del centro, ofertas/promociones, días especiales y comunicación.
 * Diseño en bento grid de 12 columnas: 2 bloques grandes + 2 laterales.
 */
import { Plus, Save, X, Loader2, Trash2 } from 'lucide-vue-next'

definePageMeta({ middleware: ['auth', 'admin'] })

// ── Tipos ─────────────────────────────────────────────────
interface Oferta {
  id: number
  nombre: string
  descripcion: string
  descuentoPorcentaje: number
  fechaInicio: string
  fechaFin: string
  activa: boolean
}

interface DiaEspecial {
  id: number
  fecha: string
  nombre: string
  multiplicadorPrecio: number
}

interface ConfigCentro {
  nombreNegocio: string
  telefono: string
  email: string
  direccion: string
  politicaFotos: string
}

interface ConfigComunicacion {
  whatsappAutomatico: boolean
  emailRecordatorio: boolean
  horasAntelacionRecordatorio: number
}

// ── Estado ────────────────────────────────────────────────
const cargando    = ref(true)
const guardando   = ref(false)
const ofertas     = ref<Oferta[]>([])
const dias        = ref<DiaEspecial[]>([])
const configCentro = ref<ConfigCentro>({
  nombreNegocio: '', telefono: '', email: '', direccion: '', politicaFotos: '',
})
const configComun = ref<ConfigComunicacion>({
  whatsappAutomatico: true, emailRecordatorio: true, horasAntelacionRecordatorio: 24,
})

// Modales
const modalOferta     = ref(false)
const modalDia        = ref(false)
const ofertaEditar    = ref<Partial<Oferta>>({})
const diaEditar       = ref<Partial<DiaEspecial>>({})
const guardandoModal  = ref(false)

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const [resOfertas, resDias, resConfig] = await Promise.all([
      api.get('/v1/ofertas'),
      api.get('/v1/dias-especiales'),
      api.get('/configuracion'),
    ])
    ofertas.value     = resOfertas.data
    dias.value        = resDias.data
    configCentro.value = resConfig.data.centro
    configComun.value  = resConfig.data.comunicacion
  } catch { /* valores por defecto */ }
  finally { cargando.value = false }
})

// ── Guardar configuración del centro ─────────────────────
async function guardarConfigCentro() {
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.put('/configuracion/centro', configCentro.value)
  } catch { /* toast */ } finally {
    guardando.value = false
  }
}

async function guardarConfigComun() {
  const { api } = await import('~/infrastructure/http/api')
  await api.put('/configuracion/comunicacion', configComun.value)
}

// ── Ofertas ───────────────────────────────────────────────
function abrirNuevaOferta() {
  ofertaEditar.value = { activa: true, descuentoPorcentaje: 10 }
  modalOferta.value = true
}

async function guardarOferta() {
  guardandoModal.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (ofertaEditar.value.id) {
      const { data } = await api.put(`/v1/ofertas/${ofertaEditar.value.id}`, ofertaEditar.value)
      const idx = ofertas.value.findIndex(o => o.id === data.id)
      if (idx !== -1) ofertas.value[idx] = data
    } else {
      const { data } = await api.post('/v1/ofertas', ofertaEditar.value)
      ofertas.value.unshift(data)
    }
    modalOferta.value = false
  } catch { /* toast */ } finally {
    guardandoModal.value = false
  }
}

async function eliminarOferta(id: number) {
  const { api } = await import('~/infrastructure/http/api')
  await api.delete(`/v1/ofertas/${id}`)
  ofertas.value = ofertas.value.filter(o => o.id !== id)
}

async function toggleOferta(o: Oferta) {
  const { api } = await import('~/infrastructure/http/api')
  await api.patch(`/v1/ofertas/${o.id}`, { activa: !o.activa })
  o.activa = !o.activa
}

// ── Días especiales ───────────────────────────────────────
function abrirNuevoDia() {
  diaEditar.value = { multiplicadorPrecio: 1.5 }
  modalDia.value = true
}

async function guardarDia() {
  guardandoModal.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (diaEditar.value.id) {
      const { data } = await api.put(`/v1/dias-especiales/${diaEditar.value.id}`, diaEditar.value)
      const idx = dias.value.findIndex(d => d.id === data.id)
      if (idx !== -1) dias.value[idx] = data
    } else {
      const { data } = await api.post('/v1/dias-especiales', diaEditar.value)
      dias.value.push(data)
    }
    modalDia.value = false
  } catch { /* toast */ } finally {
    guardandoModal.value = false
  }
}

async function eliminarDia(id: number) {
  const { api } = await import('~/infrastructure/http/api')
  await api.delete(`/v1/dias-especiales/${id}`)
  dias.value = dias.value.filter(d => d.id !== id)
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Configuración</h2>
      <p class="text-on-surface-variant text-sm">Ajustes generales del atelier</p>
    </div>

    <!-- Spinner inicial -->
    <div v-if="cargando" class="flex items-center justify-center py-16">
      <Loader2 class="w-6 h-6 animate-spin text-primary" />
    </div>

    <div v-else class="grid grid-cols-12 gap-6">

      <!-- ── 1. Ofertas y Promociones (8 cols) ────────────── -->
      <section class="col-span-12 lg:col-span-8 card p-8">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-lg font-bold text-primary">Ofertas y Promociones</h3>
            <p class="text-sm text-on-surface-variant">Ofertas activas y campañas temporales</p>
          </div>
          <button
            class="bg-primary-container text-white px-5 py-2 rounded-full text-sm font-bold hover:opacity-90 transition-opacity flex items-center gap-2"
            @click="abrirNuevaOferta"
          >
            <Plus class="w-4 h-4" />
            Nueva Promo
          </button>
        </div>

        <div class="space-y-3">
          <div
            v-for="o in ofertas"
            :key="o.id"
            class="flex items-center justify-between p-4 bg-surface-container-low rounded-2xl hover:bg-surface-container transition-colors"
          >
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 bg-primary-fixed rounded-xl flex items-center justify-center">
                <span class="text-primary-container font-black text-sm">-{{ o.descuentoPorcentaje }}%</span>
              </div>
              <div>
                <h4 class="font-bold text-primary text-sm">{{ o.nombre }}</h4>
                <p class="text-xs text-on-surface-variant">{{ o.descripcion }}</p>
              </div>
            </div>
            <div class="flex items-center gap-4">
              <!-- Toggle activa -->
              <div
                class="relative w-10 h-6 rounded-full cursor-pointer transition-colors flex-shrink-0"
                :class="o.activa ? 'bg-primary-container' : 'bg-surface-container-high'"
                @click="toggleOferta(o)"
              >
                <div
                  class="absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform"
                  :class="o.activa ? 'translate-x-5' : 'translate-x-1'"
                />
              </div>
              <span class="text-[10px] font-bold uppercase tracking-widest" :class="o.activa ? 'text-green-600' : 'text-on-surface-variant'">
                {{ o.activa ? 'Activo' : 'Inactivo' }}
              </span>
              <button
                class="p-1.5 rounded-lg hover:bg-error/10 text-error transition-colors"
                @click="eliminarOferta(o.id)"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>
          <div v-if="ofertas.length === 0" class="text-center py-8 text-sm text-on-surface-variant">
            No hay promociones configuradas
          </div>
        </div>
      </section>

      <!-- ── 2. Ajustes del Centro (4 cols) ────────────────── -->
      <section class="col-span-12 lg:col-span-4 card-kpi p-8">
        <h3 class="text-lg font-bold text-primary mb-6">Ajustes del Centro</h3>
        <form class="space-y-5" @submit.prevent="guardarConfigCentro">
          <div>
            <label class="label">Nombre del Negocio</label>
            <input v-model="configCentro.nombreNegocio" type="text" class="input" />
          </div>
          <div>
            <label class="label">Teléfono</label>
            <input v-model="configCentro.telefono" type="tel" class="input" />
          </div>
          <div>
            <label class="label">Email de contacto</label>
            <input v-model="configCentro.email" type="email" class="input" />
          </div>
          <div>
            <label class="label">Dirección</label>
            <input v-model="configCentro.direccion" type="text" class="input" />
          </div>
          <button
            type="submit"
            class="w-full bg-primary-container text-white py-3 rounded-xl font-bold hover:shadow-lg hover:shadow-primary-container/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            :disabled="guardando"
          >
            <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
            <Save v-else class="w-4 h-4" />
            Guardar cambios
          </button>
        </form>
      </section>

      <!-- ── 3. Días Especiales y Precios (8 cols) ─────────── -->
      <section class="col-span-12 lg:col-span-8 card p-8">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="text-lg font-bold text-primary">Días Especiales y Precios</h3>
            <p class="text-sm text-on-surface-variant">Tarifas diferenciales para festivos</p>
          </div>
          <button
            class="bg-primary-container text-white px-5 py-2 rounded-full text-sm font-bold hover:opacity-90 transition-opacity flex items-center gap-2"
            @click="abrirNuevoDia"
          >
            <Plus class="w-4 h-4" />
            Añadir día
          </button>
        </div>

        <div class="space-y-3">
          <div
            v-for="d in dias"
            :key="d.id"
            class="flex items-center justify-between p-4 bg-surface-container-low rounded-2xl hover:bg-surface-container transition-colors"
          >
            <div>
              <h4 class="font-bold text-primary text-sm">{{ d.nombre }}</h4>
              <p class="text-xs text-on-surface-variant">{{ d.fecha }} · x{{ d.multiplicadorPrecio }} precio</p>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm font-black text-primary-container">+{{ Math.round((d.multiplicadorPrecio - 1) * 100) }}%</span>
              <button
                class="p-1.5 rounded-lg hover:bg-error/10 text-error transition-colors"
                @click="eliminarDia(d.id)"
              >
                <Trash2 class="w-4 h-4" />
              </button>
            </div>
          </div>
          <div v-if="dias.length === 0" class="text-center py-6 text-sm text-on-surface-variant">
            No hay días especiales configurados
          </div>
        </div>
      </section>

      <!-- ── 4. Preferencias de Comunicación (4 cols) ──────── -->
      <section class="col-span-12 lg:col-span-4 space-y-6">

        <!-- Comunicación -->
        <div class="card-kpi p-8">
          <h3 class="text-lg font-bold text-primary mb-5">Preferencias de Comunicación</h3>
          <div class="space-y-5">

            <!-- WhatsApp automático -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5 text-[#25D366]" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
                </svg>
                <span class="text-sm font-bold text-primary">WhatsApp Automático</span>
              </div>
              <div
                class="relative w-10 h-6 rounded-full cursor-pointer transition-colors"
                :class="configComun.whatsappAutomatico ? 'bg-primary-container' : 'bg-surface-container-high'"
                @click="configComun.whatsappAutomatico = !configComun.whatsappAutomatico; guardarConfigComun()"
              >
                <div
                  class="absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform"
                  :class="configComun.whatsappAutomatico ? 'translate-x-5' : 'translate-x-1'"
                />
              </div>
            </div>

            <!-- Email recordatorio -->
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-3">
                <svg class="w-5 h-5 text-primary-container" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
                </svg>
                <span class="text-sm font-bold text-primary">Email Recordatorio</span>
              </div>
              <div
                class="relative w-10 h-6 rounded-full cursor-pointer transition-colors"
                :class="configComun.emailRecordatorio ? 'bg-primary-container' : 'bg-surface-container-high'"
                @click="configComun.emailRecordatorio = !configComun.emailRecordatorio; guardarConfigComun()"
              >
                <div
                  class="absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform"
                  :class="configComun.emailRecordatorio ? 'translate-x-5' : 'translate-x-1'"
                />
              </div>
            </div>

            <!-- Horas de antelación -->
            <div>
              <label class="label">Horas de antelación para recordatorio</label>
              <select
                v-model.number="configComun.horasAntelacionRecordatorio"
                class="input mt-1"
                @change="guardarConfigComun"
              >
                <option :value="12">12 horas antes</option>
                <option :value="24">24 horas antes</option>
                <option :value="48">48 horas antes</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Política de fotos -->
        <div class="card-kpi p-8">
          <h3 class="text-lg font-bold text-primary mb-4">Políticas de Imagen</h3>
          <textarea
            v-model="configCentro.politicaFotos"
            rows="5"
            class="input resize-none text-sm"
            placeholder="Escribe los términos para el uso de fotos de clientes..."
          />
          <button
            class="mt-3 w-full bg-primary-container text-white py-2.5 rounded-xl font-bold text-sm hover:shadow-lg hover:shadow-primary-container/30 transition-all"
            @click="guardarConfigCentro"
          >
            Guardar política
          </button>
        </div>
      </section>

    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       MODALES
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">

    <!-- Modal nueva oferta -->
    <Transition name="modal-overlay">
      <div
        v-if="modalOferta"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalOferta = false"
      >
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary">{{ ofertaEditar.id ? 'Editar Oferta' : 'Nueva Oferta' }}</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" @click="modalOferta = false">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="space-y-4">
            <div>
              <label class="label">Nombre</label>
              <input v-model="ofertaEditar.nombre" type="text" class="input" placeholder="Ej. Martes de Color" />
            </div>
            <div>
              <label class="label">Descripción</label>
              <input v-model="ofertaEditar.descripcion" type="text" class="input" />
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">Descuento %</label>
                <input v-model.number="ofertaEditar.descuentoPorcentaje" type="number" min="1" max="100" class="input" />
              </div>
            </div>
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">Fecha inicio</label>
                <input v-model="ofertaEditar.fechaInicio" type="date" class="input" />
              </div>
              <div>
                <label class="label">Fecha fin</label>
                <input v-model="ofertaEditar.fechaFin" type="date" class="input" />
              </div>
            </div>
          </div>
          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalOferta = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="guardandoModal" @click="guardarOferta">
              <Loader2 v-if="guardandoModal" class="w-4 h-4 animate-spin" />
              <span>{{ guardandoModal ? 'Guardando...' : 'Guardar' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Modal nuevo día especial -->
    <Transition name="modal-overlay">
      <div
        v-if="modalDia"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalDia = false"
      >
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary">Día Especial</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" @click="modalDia = false">
              <X class="w-4 h-4" />
            </button>
          </div>
          <div class="space-y-4">
            <div>
              <label class="label">Nombre del día</label>
              <input v-model="diaEditar.nombre" type="text" class="input" placeholder="Ej. Navidad, Año Nuevo..." />
            </div>
            <div>
              <label class="label">Fecha</label>
              <input v-model="diaEditar.fecha" type="date" class="input" />
            </div>
            <div>
              <label class="label">Multiplicador de precio (ej. 1.5 = +50%)</label>
              <input v-model.number="diaEditar.multiplicadorPrecio" type="number" min="1" max="3" step="0.1" class="input" />
            </div>
          </div>
          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalDia = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="guardandoModal" @click="guardarDia">
              <Loader2 v-if="guardandoModal" class="w-4 h-4 animate-spin" />
              <span>{{ guardandoModal ? 'Guardando...' : 'Guardar' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

  </Teleport>
</template>
