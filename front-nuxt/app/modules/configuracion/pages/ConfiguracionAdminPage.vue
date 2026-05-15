<script setup lang="ts">
/**
 * Configuración — ajustes del centro, días especiales y comunicación.
 * Diseño en bento grid de 12 columnas: 2 bloques grandes + 2 laterales.
 */
import { Plus, Save, X, Loader2, Trash2, CalendarOff } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'
import ContrasenasEmpleadosAdminPage from '~/modules/auth/pages/ContrasenasEmpleadosAdminPage.vue'

const toast = useToast()

// ── Tipos ─────────────────────────────────────────────────
interface DiaEspecial {
  id: number
  fecha: string
  nombre: string
  multiplicadorPrecio: number
}

interface DiaBloqueado {
  id: string
  fechaInicio: string
  fechaFin: string
  motivo: string
}

interface ConfigCentro {
  nombreNegocio: string
  telefono: string
  email: string
  direccion: string
  politicaFotos: string
  // Horario laboral del salon. El admin lo configura aqui y se aplica en agenda y calendarios.
  horarioApertura: string         // formato HH:mm, ej "09:00"
  horarioCierre: string           // formato HH:mm, ej "21:00"
  horarioPausaInicio: string      // cierre al mediodia, opcional
  horarioPausaFin: string         // reapertura por la tarde, opcional
  horarioAperturaSabado: string   // apertura sabado, puede diferir de L-V
  horarioCierreSabado: string     // formato HH:mm, ej "14:00"
  abreSabado: boolean
  abreDomingo: boolean
}

interface ConfigComunicacion {
  emailRecordatorio: boolean
  horasAntelacionRecordatorio: number
}

// ── Estado ────────────────────────────────────────────────
const cargando    = ref(true)
const guardando   = ref(false)
const dias        = ref<DiaEspecial[]>([])
const diasBloqueados = ref<DiaBloqueado[]>([])

// Form para añadir un nuevo dia bloqueado (todo en linea, sin modal)
const formBloqueado = reactive({
  fechaInicio: '',
  fechaFin: '',
  motivo: '',
})
const guardandoBloqueado = ref(false)
const configCentro = ref<ConfigCentro>({
  nombreNegocio: '', telefono: '', email: '', direccion: '', politicaFotos: '',
  horarioApertura: '09:00', horarioCierre: '21:00',
  horarioPausaInicio: '', horarioPausaFin: '',
  horarioAperturaSabado: '09:00',
  horarioCierreSabado: '14:00', abreSabado: true, abreDomingo: false,
})
const configComun = ref<ConfigComunicacion>({
  emailRecordatorio: true, horasAntelacionRecordatorio: 24,
})

// Modales
const modalDia        = ref(false)
const diaEditar       = ref<Partial<DiaEspecial>>({})
const descuentoDiaEspecial = ref(10)
const guardandoModal  = ref(false)
const eliminandoConfirmado = ref(false)
const confirmacionEliminacion = ref<{
  tipo: 'dia-especial' | 'dia-bloqueado'
  id: number | string
  titulo: string
  descripcion: string
} | null>(null)

function normalizarConfigCentro(centro?: Partial<ConfigCentro>): ConfigCentro {
  const defaults = configCentro.value

  return {
    ...defaults,
    ...centro,
    nombreNegocio: centro?.nombreNegocio ?? defaults.nombreNegocio,
    telefono: centro?.telefono ?? defaults.telefono,
    email: centro?.email ?? defaults.email,
    direccion: centro?.direccion ?? defaults.direccion,
    politicaFotos: centro?.politicaFotos ?? defaults.politicaFotos,
    horarioApertura: centro?.horarioApertura || defaults.horarioApertura,
    horarioCierre: centro?.horarioCierre || defaults.horarioCierre,
    horarioPausaInicio: centro?.horarioPausaInicio || '',
    horarioPausaFin: centro?.horarioPausaFin || '',
    horarioAperturaSabado: centro?.horarioAperturaSabado || centro?.horarioApertura || defaults.horarioAperturaSabado,
    horarioCierreSabado: centro?.horarioCierreSabado || defaults.horarioCierreSabado,
    abreSabado: centro?.abreSabado ?? defaults.abreSabado,
    abreDomingo: centro?.abreDomingo ?? defaults.abreDomingo,
  }
}

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const [resDias, resConfig, resBloqueados] = await Promise.all([
      api.get('/v1/dias-especiales'),
      api.get('/configuracion'),
      api.get('/v1/dias-bloqueados'),
    ])
    dias.value        = resDias.data
    configCentro.value = normalizarConfigCentro(resConfig.data.centro)
    configComun.value  = { ...configComun.value, ...resConfig.data.comunicacion }
    diasBloqueados.value = resBloqueados.data
  } catch { /* valores por defecto */ }
  finally { cargando.value = false }
})

// ── Guardar configuración del centro ─────────────────────
async function guardarConfigCentro() {
  if (configCentro.value.horarioApertura >= configCentro.value.horarioCierre) {
    toast.error('La apertura debe ser anterior al cierre final')
    return
  }

  const hayPausa = Boolean(configCentro.value.horarioPausaInicio || configCentro.value.horarioPausaFin)
  if (hayPausa && (!configCentro.value.horarioPausaInicio || !configCentro.value.horarioPausaFin)) {
    toast.error('Rellena sobremesa y tarde, o deja ambas vacías')
    return
  }

  if (hayPausa) {
    if (configCentro.value.horarioApertura >= configCentro.value.horarioPausaInicio) {
      toast.error('Sobremesa debe ir después de mañana')
      return
    }
    if (configCentro.value.horarioPausaInicio >= configCentro.value.horarioPausaFin) {
      toast.error('Tarde debe ir después de sobremesa')
      return
    }
    if (configCentro.value.horarioPausaFin >= configCentro.value.horarioCierre) {
      toast.error('Noche debe ir después de tarde')
      return
    }
  }

  if (configCentro.value.abreSabado && configCentro.value.horarioAperturaSabado >= configCentro.value.horarioCierreSabado) {
    toast.error('La apertura del sábado debe ser anterior al cierre del sábado')
    return
  }

  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.put('/configuracion/centro', configCentro.value)
    toast.success('Configuración guardada')
  } catch { toast.error('Error al guardar configuración') } finally {
    guardando.value = false
  }
}

async function guardarConfigComun() {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.put('/configuracion/comunicacion', configComun.value)
  } catch {
    toast.error('Error al guardar preferencias de comunicación')
  }
}

// ── Días especiales ───────────────────────────────────────
function abrirNuevoDia() {
  diaEditar.value = {}
  descuentoDiaEspecial.value = 10
  modalDia.value = true
}

async function guardarDia() {
  if (descuentoDiaEspecial.value <= 0 || descuentoDiaEspecial.value >= 100) {
    toast.error('El descuento debe estar entre 1% y 99%')
    return
  }

  const payload = {
    ...diaEditar.value,
    multiplicadorPrecio: multiplicadorDesdeDescuento(descuentoDiaEspecial.value),
  }

  guardandoModal.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (diaEditar.value.id) {
      const { data } = await api.put(`/v1/dias-especiales/${diaEditar.value.id}`, payload)
      const idx = dias.value.findIndex(d => d.id === data.id)
      if (idx !== -1) dias.value[idx] = data
    } else {
      const { data } = await api.post('/v1/dias-especiales', payload)
      dias.value.push(data)
    }
    modalDia.value = false
    toast.success(diaEditar.value.id ? 'Día actualizado' : 'Día especial creado')
  } catch { toast.error('Error al guardar día especial') } finally {
    guardandoModal.value = false
  }
}

function pedirEliminarDia(dia: DiaEspecial) {
  confirmacionEliminacion.value = {
    tipo: 'dia-especial',
    id: dia.id,
    titulo: 'Eliminar día especial',
    descripcion: `Voy a eliminar ${dia.nombre} del ${dia.fecha}. Esta acción no se puede deshacer.`,
  }
}

function multiplicadorDesdeDescuento(descuento: number): number {
  return Number((1 - descuento / 100).toFixed(2))
}

function descuentoDia(dia: DiaEspecial): number {
  const multiplicador = Number(dia.multiplicadorPrecio)
  if (!Number.isFinite(multiplicador)) return 0

  const descuento = multiplicador <= 1
    ? (1 - multiplicador) * 100
    : (multiplicador - 1) * 100

  return Math.max(0, Math.round(descuento))
}

// ── Días bloqueados para vacaciones ──────────────────────
async function anadirDiaBloqueado() {
  if (!formBloqueado.fechaInicio) {
    toast.error('Indica al menos la fecha de inicio')
    return
  }
  guardandoBloqueado.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post('/v1/dias-bloqueados', {
      fechaInicio: formBloqueado.fechaInicio,
      fechaFin: formBloqueado.fechaFin || formBloqueado.fechaInicio,
      motivo: formBloqueado.motivo,
    })
    diasBloqueados.value.push(data)
    formBloqueado.fechaInicio = ''
    formBloqueado.fechaFin = ''
    formBloqueado.motivo = ''
    toast.success('Día bloqueado añadido')
  } catch {
    toast.error('No se pudo añadir el día bloqueado')
  } finally {
    guardandoBloqueado.value = false
  }
}

function pedirEliminarDiaBloqueado(dia: DiaBloqueado) {
  confirmacionEliminacion.value = {
    tipo: 'dia-bloqueado',
    id: dia.id,
    titulo: 'Eliminar día bloqueado',
    descripcion: `Voy a eliminar el bloqueo ${formatearRangoFechas(dia)}${dia.motivo ? ` (${dia.motivo})` : ''}. Esta acción no se puede deshacer.`,
  }
}

function cancelarEliminacion() {
  confirmacionEliminacion.value = null
}

async function confirmarEliminacion() {
  if (!confirmacionEliminacion.value) return

  eliminandoConfirmado.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (confirmacionEliminacion.value.tipo === 'dia-especial') {
      await api.delete(`/v1/dias-especiales/${confirmacionEliminacion.value.id}`)
      dias.value = dias.value.filter(d => d.id !== confirmacionEliminacion.value?.id)
      toast.success('Día especial eliminado')
    } else {
      await api.delete(`/v1/dias-bloqueados/${confirmacionEliminacion.value.id}`)
      diasBloqueados.value = diasBloqueados.value.filter(d => d.id !== confirmacionEliminacion.value?.id)
      toast.success('Día bloqueado eliminado')
    }
    cancelarEliminacion()
  } catch {
    toast.error(
      confirmacionEliminacion.value.tipo === 'dia-especial'
        ? 'No se pudo eliminar el día especial'
        : 'No se pudo eliminar el día bloqueado',
    )
  } finally {
    eliminandoConfirmado.value = false
  }
}

function formatearRangoFechas(d: DiaBloqueado): string {
  if (d.fechaInicio === d.fechaFin) return d.fechaInicio
  return `${d.fechaInicio} → ${d.fechaFin}`
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div>
      <h2 class="text-2xl sm:text-3xl font-extrabold tracking-tight text-primary mb-1">Configuración</h2>
      <p class="text-on-surface-variant text-sm">Ajustes generales del atelier</p>
    </div>

    <!-- Spinner inicial -->
    <div v-if="cargando" class="flex items-center justify-center py-16">
      <Loader2 class="w-6 h-6 animate-spin text-primary" />
    </div>

    <div v-else class="grid grid-cols-12 gap-4 sm:gap-6">

      <!-- ── 1. Ajustes del Centro (4 cols) ────────────────── -->
      <section class="col-span-12 xl:col-span-4 card-kpi p-5 sm:p-8 shadow-card">
        <h3 class="text-lg font-bold text-primary mb-5 sm:mb-6">Ajustes del Centro</h3>
        <form class="space-y-4 sm:space-y-5" @submit.prevent="guardarConfigCentro">
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

          <!-- ── Horario laboral del salón ─────────────────── -->
          <div class="pt-4 border-t border-outline-variant/20 space-y-4">
            <h4 class="text-sm font-bold text-on-surface uppercase tracking-wider">
              Horario laboral
            </h4>

            <div class="grid grid-cols-2 gap-2 sm:gap-3">
              <div>
                <label class="label">Mañana</label>
                <input v-model="configCentro.horarioApertura" type="time" class="input" />
              </div>
              <div>
                <label class="label">Sobremesa</label>
                <input v-model="configCentro.horarioPausaInicio" type="time" class="input" />
              </div>
            </div>

            <div class="grid grid-cols-2 gap-2 sm:gap-3">
              <div>
                <label class="label">Tarde</label>
                <input v-model="configCentro.horarioPausaFin" type="time" class="input" />
              </div>
              <div>
                <label class="label">Noche</label>
                <input v-model="configCentro.horarioCierre" type="time" class="input" />
              </div>
            </div>
            <p class="text-[11px] leading-snug text-on-surface-variant">
              Jornada continua: deja vacíos Sobremesa y Tarde.
            </p>

            <label class="flex items-center gap-2 text-sm">
              <input v-model="configCentro.abreSabado" type="checkbox" class="rounded" />
              <span>Abrimos sábados</span>
            </label>
            <div v-if="configCentro.abreSabado" class="grid grid-cols-2 gap-2 sm:gap-3">
              <div>
                <label class="label">Abre sábado</label>
                <input v-model="configCentro.horarioAperturaSabado" type="time" class="input" />
              </div>
              <div>
                <label class="label">Cierra sábado</label>
                <input v-model="configCentro.horarioCierreSabado" type="time" class="input" />
              </div>
            </div>

            <label class="flex items-center gap-2 text-sm">
              <input v-model="configCentro.abreDomingo" type="checkbox" class="rounded" />
              <span>Abrimos domingos</span>
            </label>
          </div>

          <button
            type="submit"
            class="w-full bg-primary-container text-white py-2.5 sm:py-3 rounded-xl font-bold text-sm hover:shadow-lg hover:shadow-primary-container/30 transition-all flex items-center justify-center gap-2 disabled:opacity-50"
            :disabled="guardando"
          >
            <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
            <Save v-else class="w-4 h-4" />
            Guardar cambios
          </button>
        </form>
      </section>

      <!-- ── 2. Días Especiales y Precios (8 cols) ─────────── -->
      <section class="col-span-12 xl:col-span-8 card p-5 sm:p-8">
        <div class="flex items-start justify-between gap-3 mb-5 sm:mb-6">
          <div class="min-w-0">
            <h3 class="text-lg font-bold text-primary">Días Especiales y Precios</h3>
            <p class="text-sm text-on-surface-variant">Descuentos para fechas especiales</p>
          </div>
          <button
            class="shrink-0 bg-primary-container text-white px-3.5 sm:px-5 py-2 rounded-full text-xs sm:text-sm font-bold hover:opacity-90 transition-opacity inline-flex items-center gap-1.5 sm:gap-2 leading-tight"
            @click="abrirNuevoDia"
          >
            <Plus class="w-3.5 h-3.5 sm:w-4 sm:h-4" />
            <span>Añadir día</span>
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
              <p class="text-xs text-on-surface-variant">{{ d.fecha }}</p>
            </div>
            <div class="flex items-center gap-3">
              <span class="text-sm font-black text-primary-container">-{{ descuentoDia(d) }}%</span>
              <button
                class="p-1.5 rounded-lg hover:bg-error/10 text-error transition-colors"
                @click="pedirEliminarDia(d)"
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

      <!-- ── 3. Días bloqueados para vacaciones (12 cols) ─── -->
      <section class="col-span-12 card p-5 sm:p-8">
        <div class="flex items-start gap-3 mb-5">
          <div class="w-10 h-10 rounded-xl bg-red-100 flex shrink-0 items-center justify-center">
            <CalendarOff class="w-5 h-5 text-red-600" />
          </div>
          <div class="min-w-0">
            <h3 class="text-lg font-bold text-primary">Días bloqueados para vacaciones</h3>
            <p class="text-sm text-on-surface-variant">
              Fechas en las que ningún empleado puede solicitar vacaciones (eventos, picos de demanda, etc.)
            </p>
          </div>
        </div>

        <!-- Form para añadir nuevo bloqueo -->
        <div class="grid grid-cols-1 md:grid-cols-12 gap-3 mb-5 p-3 sm:p-4 bg-surface-container-low rounded-2xl">
          <div class="md:col-span-3">
            <label class="label">Fecha inicio</label>
            <input v-model="formBloqueado.fechaInicio" type="date" class="input" />
          </div>
          <div class="md:col-span-3">
            <label class="label">Fecha fin <span class="text-on-surface-variant/60">(opcional)</span></label>
            <input v-model="formBloqueado.fechaFin" type="date" :min="formBloqueado.fechaInicio" class="input" />
          </div>
          <div class="md:col-span-4">
            <label class="label">Motivo</label>
            <input
              v-model="formBloqueado.motivo"
              type="text"
              class="input"
              placeholder="Ej: Black Friday, Navidad, evento especial..."
            />
          </div>
          <div class="md:col-span-2 flex items-end">
            <button
              class="w-full bg-primary-container text-white py-2.5 md:py-3 rounded-xl font-bold text-sm hover:opacity-90 transition-opacity flex items-center justify-center gap-2 disabled:opacity-40"
              :disabled="guardandoBloqueado || !formBloqueado.fechaInicio"
              @click="anadirDiaBloqueado"
            >
              <Loader2 v-if="guardandoBloqueado" class="w-4 h-4 animate-spin" />
              <Plus v-else class="w-4 h-4" />
              Añadir
            </button>
          </div>
        </div>

        <!-- Lista de días bloqueados -->
        <div class="space-y-2">
          <div
            v-for="dia in diasBloqueados"
            :key="dia.id"
            class="flex items-center justify-between p-3 bg-red-50 border border-red-100 rounded-xl"
          >
            <div class="flex items-center gap-3">
              <CalendarOff class="w-4 h-4 text-red-600 flex-shrink-0" />
              <div>
                <p class="font-bold text-red-700 text-sm">{{ formatearRangoFechas(dia) }}</p>
                <p v-if="dia.motivo" class="text-xs text-red-600/80">{{ dia.motivo }}</p>
              </div>
            </div>
            <button
              class="p-2 rounded-lg hover:bg-red-100 text-red-600 transition-colors"
              :aria-label="`Eliminar bloqueo del ${dia.fechaInicio}`"
              @click="pedirEliminarDiaBloqueado(dia)"
            >
              <Trash2 class="w-4 h-4" />
            </button>
          </div>
          <div v-if="diasBloqueados.length === 0" class="text-center py-6 text-sm text-on-surface-variant">
            No hay días bloqueados configurados
          </div>
        </div>
      </section>

      <!-- ── 4. Preferencias de Comunicación ──────────────── -->
      <section class="col-span-12 xl:col-span-6 card-kpi p-5 sm:p-8 shadow-card">
        <h3 class="text-lg font-bold text-primary mb-5">Preferencias de Comunicación</h3>
        <div class="space-y-5">

          <!-- Email recordatorio -->
          <div class="flex items-center justify-between gap-3">
            <div class="flex items-center gap-3 min-w-0">
              <svg class="w-5 h-5 text-primary-container" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"/>
              </svg>
              <span class="text-sm font-bold text-primary">Email Recordatorio</span>
            </div>
            <div
              class="relative w-10 h-6 rounded-full cursor-pointer transition-colors shrink-0"
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
              class="select-field mt-1"
              @change="guardarConfigComun"
            >
              <option :value="12">12 horas antes</option>
              <option :value="24">24 horas antes</option>
              <option :value="48">48 horas antes</option>
            </select>
            <p class="mt-2 text-[11px] text-on-surface-variant">
              Los correos automáticos se enviarán por Mailtrap.
            </p>
          </div>
        </div>
      </section>

      <!-- ── 5. Políticas de Imagen ────────────────────────── -->
      <section class="col-span-12 xl:col-span-6 card-kpi p-5 sm:p-8 shadow-card">
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
      </section>

      <!-- ── 6. Acceso de empleados (contraseñas) ──────────── -->
      <section class="col-span-12 card p-5 sm:p-8">
        <ContrasenasEmpleadosAdminPage />
      </section>

    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       MODALES
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">

    <!-- Modal nuevo día especial -->
    <Transition name="modal-overlay">
      <div
        v-if="modalDia"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalDia = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-dia-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-center justify-between mb-5">
            <h3 id="modal-dia-titulo" class="text-lg font-bold text-primary">Día Especial</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" aria-label="Cerrar" @click="modalDia = false">
              <X class="w-4 h-4" aria-hidden="true" />
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
              <label class="label">Descuento (%)</label>
              <input
                v-model.number="descuentoDiaEspecial"
                type="number"
                min="1"
                max="99"
                step="1"
                class="input"
                placeholder="Ej. 20"
              />
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

    <Transition name="modal-overlay">
      <div
        v-if="confirmacionEliminacion"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/35 p-4 backdrop-blur-sm"
        @click.self="cancelarEliminacion"
      >
        <section class="w-full max-w-md rounded-[28px] border border-outline-variant/15 bg-white p-6 shadow-2xl sm:p-7">
          <div class="space-y-2">
            <h3 class="text-lg font-bold text-primary">{{ confirmacionEliminacion.titulo }}</h3>
            <p class="text-sm leading-relaxed text-on-surface-variant">
              {{ confirmacionEliminacion.descripcion }}
            </p>
          </div>

          <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
            <button class="btn-secondary" :disabled="eliminandoConfirmado" @click="cancelarEliminacion">
              Cancelar
            </button>
            <button class="btn-danger" :disabled="eliminandoConfirmado" @click="confirmarEliminacion">
              <Loader2 v-if="eliminandoConfirmado" class="h-4 w-4 animate-spin" aria-hidden="true" />
              <span>{{ eliminandoConfirmado ? 'Eliminando...' : 'Sí, eliminar' }}</span>
            </button>
          </div>
        </section>
      </div>
    </Transition>

  </Teleport>
</template>
