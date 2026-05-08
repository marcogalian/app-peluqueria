<script setup lang="ts">
/**
 * Gestión de Personal — lista de empleados con panel lateral de detalle.
 * Email por empleado y registro de bajas/vacaciones.
 * Solo admin puede ver todos; panel lateral con mini-calendario del mes.
 */
import {
  Plus, X, Loader2, Save, Phone, Mail, ChevronLeft, ChevronRight,
} from 'lucide-vue-next'
import { format, startOfMonth, endOfMonth, startOfWeek, endOfWeek, addDays, isSameMonth, isToday } from 'date-fns'
import { useToast } from '~/modules/shared/composables/useToast'

const toast = useToast()
import { es } from 'date-fns/locale'
import { useAuthStore } from '~/modules/auth/store/auth.store'

definePageMeta({ middleware: ['auth', 'admin'] })

const authStore = useAuthStore()

// ── Tipos ─────────────────────────────────────────────────
interface Empleado {
  id: string
  nombre: string
  apellidos: string
  email: string
  telefono: string
  especialidades: string
  disponible: boolean
  enBaja: boolean
  enVacaciones: boolean
  citasMes: number
  iniciales: string
  fotoUrl?: string
  porcentajeComision: number
}

// ── Estado ────────────────────────────────────────────────
const empleados         = ref<Empleado[]>([])
const cargando          = ref(true)
const empleadoSeleccionado = ref<Empleado | null>(null)
const modalBaja         = ref(false)
const modalNuevo        = ref(false)
const modalEditar       = ref(false)
const guardando         = ref(false)
const mesCalendario     = ref(new Date())

const formBaja   = reactive({ fechaInicio: '', fechaFin: '', motivo: '' })
const formNuevo  = reactive({ nombre: '', apellidos: '', email: '', telefono: '', especialidades: '' })
const formEditar = reactive({ nombre: '', telefono: '', especialidades: '', horarioBase: '', porcentajeComision: 0 })

// ── Computed ──────────────────────────────────────────────
const enTurnoHoy     = computed(() => empleados.value.filter(e => e.disponible && !e.enBaja && !e.enVacaciones).length)
const enBaja         = computed(() => empleados.value.filter(e => e.enBaja).length)
const enVacaciones   = computed(() => empleados.value.filter(e => e.enVacaciones).length)

const diasCalendario = computed(() => {
  const inicio = startOfWeek(startOfMonth(mesCalendario.value), { weekStartsOn: 1 })
  const fin    = endOfWeek(endOfMonth(mesCalendario.value), { weekStartsOn: 1 })
  const dias: Date[] = []
  let d = inicio
  while (d <= fin) { dias.push(d); d = addDays(d, 1) }
  return dias
})

const textoMesCalendario = computed(() =>
  format(mesCalendario.value, 'MMMM yyyy', { locale: es }).replace(/^\w/, c => c.toUpperCase()),
)

// ── Carga ─────────────────────────────────────────────────
onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/peluqueros')
    empleados.value = data.map((p: any) => {
      const partes = (p.nombre || '').trim().split(' ')
      return {
        id:            p.id,
        nombre:        partes[0] || '',
        apellidos:     partes.slice(1).join(' '),
        email:         p.user?.email || '',
        telefono:      p.telefono || '',
        especialidades:     p.especialidades || p.especialidad || '',
        disponible:         p.disponible,
        enBaja:             p.enBaja,
        enVacaciones:       p.enVacaciones,
        citasMes:           0,
        iniciales:          partes.map((n: string) => n[0]).slice(0, 2).join('').toUpperCase(),
        fotoUrl:            p.fotoUrl || undefined,
        porcentajeComision: p.porcentajeComision ?? 0,
      }
    })
  } catch {
    // vacío
  } finally {
    cargando.value = false
    // Seleccionar el primero por defecto
    if (empleados.value.length > 0) empleadoSeleccionado.value = empleados.value[0]
  }
})

// ── Acciones ──────────────────────────────────────────────
function estadoEmpleado(e: Empleado): string {
  if (e.enBaja)       return 'Baja médica'
  if (e.enVacaciones) return 'Vacaciones'
  if (e.disponible)   return 'Disponible'
  return 'No disponible'
}

function badgeEstado(e: Empleado): string {
  if (e.enBaja)       return 'bg-red-100 text-red-700'
  if (e.enVacaciones) return 'bg-blue-100 text-blue-700'
  if (e.disponible)   return 'bg-green-100 text-green-700'
  return 'bg-surface-container text-on-surface-variant'
}

async function registrarBaja() {
  if (!empleadoSeleccionado.value) return
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.post(`/peluqueros/${empleadoSeleccionado.value.id}/baja`, formBaja)
    empleadoSeleccionado.value.enBaja = true
    empleadoSeleccionado.value.disponible = false
    modalBaja.value = false
    toast.success('Baja registrada')
  } catch { toast.error('Error al registrar baja') } finally {
    guardando.value = false
  }
}

async function crearEmpleado() {
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post('/peluqueros', formNuevo)
    empleados.value.unshift(data)
    modalNuevo.value = false
    Object.assign(formNuevo, { nombre: '', apellidos: '', email: '', telefono: '', especialidades: '' })
    toast.success('Empleado creado')
  } catch { toast.error('Error al crear empleado') } finally {
    guardando.value = false
  }
}

function abrirEditar() {
  if (!empleadoSeleccionado.value) return
  const e = empleadoSeleccionado.value
  formEditar.nombre              = e.nombre + (e.apellidos ? ' ' + e.apellidos : '')
  formEditar.telefono            = e.telefono
  formEditar.especialidades      = e.especialidades
  formEditar.horarioBase         = ''
  formEditar.porcentajeComision  = e.porcentajeComision
  modalEditar.value = true
}

async function guardarEdicion() {
  if (!empleadoSeleccionado.value) return
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.put(`/peluqueros/${empleadoSeleccionado.value.id}`, {
      nombre:              formEditar.nombre,
      telefono:            formEditar.telefono,
      especialidades:      formEditar.especialidades,
      especialidad:        formEditar.especialidades,
      horarioBase:         formEditar.horarioBase,
      porcentajeComision:  formEditar.porcentajeComision,
    })
    // Actualizar local
    const partes = (data.nombre || '').trim().split(' ')
    empleadoSeleccionado.value.nombre              = partes[0] || ''
    empleadoSeleccionado.value.apellidos           = partes.slice(1).join(' ')
    empleadoSeleccionado.value.telefono            = data.telefono || ''
    empleadoSeleccionado.value.especialidades      = data.especialidades || ''
    empleadoSeleccionado.value.porcentajeComision  = data.porcentajeComision ?? 0
    const idx = empleados.value.findIndex(e => e.id === empleadoSeleccionado.value!.id)
    if (idx !== -1) empleados.value[idx] = { ...empleadoSeleccionado.value }
    modalEditar.value = false
    toast.success('Empleado actualizado')
  } catch { toast.error('Error al guardar cambios') } finally {
    guardando.value = false
  }
}

async function reactivarEmpleado(empleado: Empleado) {
  guardando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.patch(`/peluqueros/${empleado.id}`, { enBaja: false, disponible: true })
    empleado.enBaja     = false
    empleado.disponible = true
    toast.success('Empleado reactivado')
  } catch { toast.error('Error al reactivar') } finally {
    guardando.value = false
  }
}

const inputFoto = ref<HTMLInputElement | null>(null)

async function subirFoto(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file || !empleadoSeleccionado.value) return
  const form = new FormData()
  form.append('file', file)
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post(`/peluqueros/${empleadoSeleccionado.value.id}/foto`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    empleadoSeleccionado.value.fotoUrl = data.fotoUrl
    const idx = empleados.value.findIndex(e => e.id === empleadoSeleccionado.value!.id)
    if (idx !== -1) empleados.value[idx].fotoUrl = data.fotoUrl
    toast.success('Foto actualizada')
  } catch { toast.error('Error al subir foto') }
}

function abrirEmail(empleado: Empleado) {
  navigateTo({
    path: '/mensajes',
    query: { contacto: empleado.id },
  })
}
</script>

<template>
  <div class="space-y-8">

    <!-- ── Cabecera ──────────────────────────────────────── -->
    <div class="flex items-end justify-between">
      <div>
        <h2 class="text-3xl font-extrabold tracking-tight text-primary mb-1">Gestión de Personal</h2>
        <p class="text-on-surface-variant text-sm">Supervisa y coordina los talentos de Atelier Sapphire</p>
      </div>
      <button
        class="flex items-center gap-2 bg-primary-container text-white px-6 py-2.5 rounded-full font-bold text-sm hover:opacity-90 transition-all"
        @click="modalNuevo = true"
      >
        <Plus class="w-4 h-4" />
        Nuevo Empleado
      </button>
    </div>

    <!-- ── KPI bento ─────────────────────────────────────── -->
    <div class="grid grid-cols-4 gap-4">
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70 mb-1">Total Empleados</p>
        <p class="text-3xl font-bold text-primary-container">{{ empleados.length }}</p>
      </div>
      <div class="card-kpi !bg-primary-container shadow-lg shadow-primary-container/20">
        <p class="text-[10px] font-bold uppercase tracking-widest text-white/70 mb-1">En Turno Hoy</p>
        <p class="text-3xl font-bold text-white">{{ enTurnoHoy }}</p>
      </div>
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70 mb-1">De Baja</p>
        <p class="text-3xl font-bold text-red-500">{{ enBaja }}</p>
      </div>
      <div class="card-kpi">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70 mb-1">De Vacaciones</p>
        <p class="text-3xl font-bold text-blue-500">{{ enVacaciones }}</p>
      </div>
    </div>

    <!-- ── Layout lista + panel ──────────────────────────── -->
    <div class="flex gap-6 items-start">

      <!-- Lista de empleados -->
      <div class="flex-1 card overflow-hidden">
        <div class="p-6 border-b border-surface-container">
          <h3 class="font-bold text-primary-container">Lista de Empleados</h3>
        </div>

        <!-- Spinner -->
        <div v-if="cargando" class="flex items-center justify-center py-16">
          <Loader2 class="w-6 h-6 animate-spin text-primary" />
        </div>

        <!-- Filas -->
        <div v-else class="divide-y divide-surface-container">
          <div
            v-for="e in empleados"
            :key="e.id"
            class="flex items-center gap-4 px-6 py-4 hover:bg-surface-container-low cursor-pointer transition-colors"
            :class="empleadoSeleccionado?.id === e.id ? 'bg-surface-container-low' : ''"
            @click="empleadoSeleccionado = e"
          >
            <!-- Avatar -->
            <div
              class="w-10 h-10 rounded-full flex items-center justify-center text-white text-sm font-bold flex-shrink-0"
              :class="e.enBaja ? 'bg-red-400' : e.enVacaciones ? 'bg-blue-400' : 'bg-primary-container'"
            >
              {{ e.iniciales || (e.nombre[0] + e.apellidos[0]).toUpperCase() }}
            </div>

            <!-- Nombre + especialidad -->
            <div class="flex-1 min-w-0">
              <p class="font-bold text-on-surface text-sm truncate">{{ e.nombre }} {{ e.apellidos }}</p>
              <p class="text-xs text-on-surface-variant truncate">{{ e.especialidades || 'Sin especialidad definida' }}</p>
            </div>

            <!-- Citas del mes -->
            <p class="text-xs text-on-surface-variant w-16 text-center">
              <span class="font-bold text-on-surface">{{ e.citasMes }}</span> citas
            </p>

            <!-- Badge estado -->
            <span class="text-[10px] font-bold px-2.5 py-1 rounded-full flex-shrink-0" :class="badgeEstado(e)">
              {{ estadoEmpleado(e) }}
            </span>
          </div>
          <div v-if="empleados.length === 0" class="px-6 py-12 text-center text-sm text-on-surface-variant">
            No hay empleados registrados
          </div>
        </div>
      </div>

      <!-- Panel lateral de detalle -->
      <aside
        v-if="empleadoSeleccionado"
        class="w-80 card-kpi p-6 sticky top-24 shadow-lg flex-shrink-0"
      >
        <!-- Avatar grande + nombre -->
        <div class="text-center mb-6">
          <div
            class="relative w-20 h-20 rounded-full mx-auto mb-3"
            :class="authStore.isAdmin ? 'cursor-pointer group' : ''"
            @click="authStore.isAdmin && inputFoto?.click()"
          >
            <img
              v-if="empleadoSeleccionado.fotoUrl"
              :src="`${$config.public.uploadsBase}/${empleadoSeleccionado.fotoUrl}`"
              class="w-20 h-20 rounded-full object-cover"
              alt="Foto empleado"
            />
            <div
              v-else
              class="w-20 h-20 rounded-full flex items-center justify-center text-white text-2xl font-bold"
              :class="empleadoSeleccionado.enBaja ? 'bg-red-400' : 'bg-primary-container'"
            >
              {{ (empleadoSeleccionado.nombre[0] + (empleadoSeleccionado.apellidos[0] || '')).toUpperCase() }}
            </div>
            <div class="absolute inset-0 rounded-full bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
              <span class="text-white text-[10px] font-bold">Cambiar</span>
            </div>
          </div>
          <input ref="inputFoto" type="file" accept="image/*" class="hidden" @change="subirFoto" />
          <h3 class="text-lg font-bold text-on-surface">{{ empleadoSeleccionado.nombre }} {{ empleadoSeleccionado.apellidos }}</h3>
          <p class="text-sm text-on-surface-variant">{{ empleadoSeleccionado.especialidades || 'Sin especialidad' }}</p>

          <div class="flex gap-3 mt-4">
            <button
              class="flex-1 bg-white hover:bg-surface-container text-on-surface font-bold py-2.5 px-4 rounded-xl text-[11px] flex items-center justify-center gap-2 transition-colors border border-outline-variant/20 shadow-sm"
              @click="abrirEmail(empleadoSeleccionado)"
            >
              <Mail class="w-4 h-4 text-primary-container" />
              Ir a email
            </button>
          </div>
        </div>

        <!-- Info básica -->
        <div class="space-y-3 mb-6">
          <div class="flex items-center gap-3 text-sm">
            <Phone class="w-4 h-4 text-on-surface-variant flex-shrink-0" />
            <span class="text-on-surface-variant">{{ empleadoSeleccionado.telefono || 'Sin teléfono' }}</span>
          </div>
          <div class="flex items-center gap-3 text-sm">
            <Mail class="w-4 h-4 text-on-surface-variant flex-shrink-0" />
            <span class="text-on-surface-variant truncate">{{ empleadoSeleccionado.email }}</span>
          </div>
          <div class="flex items-center justify-between rounded-xl bg-surface-container-low px-3 py-2 text-sm">
            <span class="text-on-surface-variant font-medium">Comisión</span>
            <span class="font-bold text-primary-container">{{ empleadoSeleccionado.porcentajeComision }}%</span>
          </div>
        </div>

        <!-- Mini-calendario del mes -->
        <div class="mb-6">
          <div class="flex items-center justify-between mb-3">
            <p class="text-xs font-bold uppercase tracking-widest text-on-surface-variant">{{ textoMesCalendario }}</p>
            <div class="flex gap-1">
              <button class="p-1 rounded hover:bg-surface-container transition-colors" @click="mesCalendario = new Date(mesCalendario.getFullYear(), mesCalendario.getMonth() - 1)">
                <ChevronLeft class="w-3 h-3" />
              </button>
              <button class="p-1 rounded hover:bg-surface-container transition-colors" @click="mesCalendario = new Date(mesCalendario.getFullYear(), mesCalendario.getMonth() + 1)">
                <ChevronRight class="w-3 h-3" />
              </button>
            </div>
          </div>
          <div class="grid grid-cols-7 gap-1 text-[9px] text-center font-black text-on-surface-variant/50 mb-2">
            <span v-for="d in ['L','M','X','J','V','S','D']" :key="d">{{ d }}</span>
          </div>
          <div class="grid grid-cols-7 gap-1 text-center text-[11px]">
            <span
              v-for="(dia, i) in diasCalendario"
              :key="i"
              class="py-1.5 rounded-lg"
              :class="[
                !isSameMonth(dia, mesCalendario) ? 'text-on-surface-variant/20' : 'font-bold',
                isToday(dia) ? 'bg-primary-container text-white' : '',
              ]"
            >
              {{ format(dia, 'd') }}
            </span>
          </div>
        </div>

        <!-- Acciones — solo admin -->
        <div v-if="authStore.isAdmin" class="space-y-2">
          <button
            class="w-full bg-primary-container/10 hover:bg-primary-container/20 text-primary-container font-bold py-2.5 rounded-xl text-sm transition-colors"
            @click="abrirEditar"
          >
            Editar empleado
          </button>
          <button
            v-if="!empleadoSeleccionado.enBaja"
            class="w-full bg-red-50 hover:bg-red-100 text-red-700 font-bold py-2.5 rounded-xl text-sm transition-colors"
            @click="modalBaja = true"
          >
            Registrar baja médica
          </button>
          <NuxtLink
            :to="`/admin/calendario-laboral?empleado=${empleadoSeleccionado.id}`"
            class="block w-full text-center bg-surface-container hover:bg-surface-container-high text-on-surface-variant font-bold py-2.5 rounded-xl text-sm transition-colors"
          >
            Ver calendario laboral
          </NuxtLink>
        </div>
      </aside>

    </div>
  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Registrar baja
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalBaja"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalBaja = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-baja-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-center justify-between mb-4">
            <h3 id="modal-baja-titulo" class="text-lg font-bold text-primary">Registrar Baja Médica</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" aria-label="Cerrar" @click="modalBaja = false">
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <p class="text-sm text-on-surface-variant mb-4">
            {{ empleadoSeleccionado?.nombre }} {{ empleadoSeleccionado?.apellidos }}
          </p>

          <div class="space-y-4">
            <div>
              <label class="label">Fecha de inicio</label>
              <input v-model="formBaja.fechaInicio" type="date" class="input" />
            </div>
            <div>
              <label class="label">Fecha de fin estimada</label>
              <input v-model="formBaja.fechaFin" type="date" class="input" />
            </div>
            <div>
              <label class="label">Motivo (opcional)</label>
              <textarea v-model="formBaja.motivo" rows="2" class="input resize-none" placeholder="Descripción breve..." />
            </div>
          </div>

          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalBaja = false">Cancelar</button>
            <button class="btn-danger flex-1" :disabled="guardando" @click="registrarBaja">
              <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
              <span>{{ guardando ? 'Guardando...' : 'Confirmar baja' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Modal nuevo empleado -->
    <Transition name="modal-overlay">
      <div
        v-if="modalNuevo"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalNuevo = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-nuevo-empleado-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-center justify-between mb-5">
            <h3 id="modal-nuevo-empleado-titulo" class="text-lg font-bold text-primary">Nuevo Empleado</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" aria-label="Cerrar" @click="modalNuevo = false">
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <div class="space-y-4">
            <div class="grid grid-cols-2 gap-3">
              <div>
                <label class="label">Nombre</label>
                <input v-model="formNuevo.nombre" type="text" class="input" />
              </div>
              <div>
                <label class="label">Apellidos</label>
                <input v-model="formNuevo.apellidos" type="text" class="input" />
              </div>
            </div>
            <div>
              <label class="label">Email</label>
              <input v-model="formNuevo.email" type="email" class="input" />
            </div>
            <div>
              <label class="label">Teléfono</label>
              <input v-model="formNuevo.telefono" type="tel" class="input" />
            </div>
            <div>
              <label class="label">Especialidades</label>
              <input v-model="formNuevo.especialidades" type="text" class="input" placeholder="Ej. Color, Corte, Barba" />
            </div>
          </div>

          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalNuevo = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="guardando" @click="crearEmpleado">
              <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
              <span>{{ guardando ? 'Creando...' : 'Crear empleado' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
    <!-- Modal editar empleado -->
    <Transition name="modal-overlay">
      <div
        v-if="modalEditar"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalEditar = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-editar-empleado-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-center justify-between mb-5">
            <h3 id="modal-editar-empleado-titulo" class="text-lg font-bold text-primary">Editar Empleado</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" aria-label="Cerrar" @click="modalEditar = false">
              <X class="w-4 h-4" aria-hidden="true" />
            </button>
          </div>

          <div class="space-y-4">
            <div>
              <label class="label">Nombre completo</label>
              <input v-model="formEditar.nombre" type="text" class="input" />
            </div>
            <div>
              <label class="label">Teléfono</label>
              <input v-model="formEditar.telefono" type="tel" class="input" placeholder="Ej. 600123456" />
            </div>
            <div>
              <label class="label">Especialidades</label>
              <input v-model="formEditar.especialidades" type="text" class="input" placeholder="Ej. Colorimetría, Corte, Alisados" />
            </div>
            <div>
              <label class="label">Horario base</label>
              <input v-model="formEditar.horarioBase" type="text" class="input" placeholder="Ej. L-V 09:00-17:00" />
            </div>
            <div>
              <label class="label">Comisión (%)</label>
              <input v-model.number="formEditar.porcentajeComision" type="number" min="0" max="100" step="0.5" class="input" placeholder="Ej. 30" />
            </div>
          </div>

          <div class="flex gap-3 mt-5">
            <button class="btn-secondary flex-1" @click="modalEditar = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="guardando" @click="guardarEdicion">
              <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" />
              <span>{{ guardando ? 'Guardando...' : 'Guardar cambios' }}</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
