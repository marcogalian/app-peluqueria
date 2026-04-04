<script setup lang="ts">
/**
 * Gestión de Personal — lista de empleados con panel lateral de detalle.
 * WhatsApp y Email por empleado. Registro de bajas y vacaciones.
 * Solo admin puede ver todos; panel lateral con mini-calendario del mes.
 */
import {
  Plus, X, Loader2, Save, Phone, Mail, ChevronLeft, ChevronRight,
} from 'lucide-vue-next'
import { format, startOfMonth, endOfMonth, startOfWeek, endOfWeek, addDays, isSameMonth, isToday } from 'date-fns'
import { es } from 'date-fns/locale'

definePageMeta({ middleware: ['auth', 'admin'] })

// ── Tipos ─────────────────────────────────────────────────
interface Empleado {
  id: number
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
}

// ── Estado ────────────────────────────────────────────────
const empleados         = ref<Empleado[]>([])
const cargando          = ref(true)
const empleadoSeleccionado = ref<Empleado | null>(null)
const modalBaja         = ref(false)
const modalNuevo        = ref(false)
const guardando         = ref(false)
const mesCalendario     = ref(new Date())

// Form nueva baja
const formBaja = reactive({ fechaInicio: '', fechaFin: '', motivo: '' })
// Form nuevo empleado
const formNuevo = reactive({ nombre: '', apellidos: '', email: '', telefono: '', especialidades: '' })

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
    empleados.value = data
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
  } catch { /* toast */ } finally {
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
  } catch { /* toast */ } finally {
    guardando.value = false
  }
}

function abrirWhatsApp(telefono: string) {
  const numero = telefono.replace(/\D/g, '')
  window.open(`https://wa.me/34${numero}`, '_blank')
}

function abrirEmail(email: string) {
  window.location.href = `mailto:${email}`
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
            class="w-20 h-20 rounded-full mx-auto flex items-center justify-center text-white text-2xl font-bold mb-3"
            :class="empleadoSeleccionado.enBaja ? 'bg-red-400' : 'bg-primary-container'"
          >
            {{ (empleadoSeleccionado.nombre[0] + empleadoSeleccionado.apellidos[0]).toUpperCase() }}
          </div>
          <h3 class="text-lg font-bold text-on-surface">{{ empleadoSeleccionado.nombre }} {{ empleadoSeleccionado.apellidos }}</h3>
          <p class="text-sm text-on-surface-variant">{{ empleadoSeleccionado.especialidades || 'Sin especialidad' }}</p>

          <!-- Botones WhatsApp + Email -->
          <div class="flex gap-3 mt-4">
            <button
              class="flex-1 bg-white hover:bg-surface-container text-on-surface font-bold py-2.5 px-4 rounded-xl text-[11px] flex items-center justify-center gap-2 transition-colors border border-outline-variant/20 shadow-sm"
              @click="abrirWhatsApp(empleadoSeleccionado.telefono)"
            >
              <svg class="w-4 h-4 text-[#25D366]" fill="currentColor" viewBox="0 0 24 24">
                <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
              </svg>
              WhatsApp
            </button>
            <button
              class="flex-1 bg-white hover:bg-surface-container text-on-surface font-bold py-2.5 px-4 rounded-xl text-[11px] flex items-center justify-center gap-2 transition-colors border border-outline-variant/20 shadow-sm"
              @click="abrirEmail(empleadoSeleccionado.email)"
            >
              <Mail class="w-4 h-4 text-primary-container" />
              Email
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

        <!-- Acciones -->
        <div class="space-y-2">
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
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-bold text-primary">Registrar Baja Médica</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" @click="modalBaja = false">
              <X class="w-4 h-4" />
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
        <div class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-bold text-primary">Nuevo Empleado</h3>
            <button class="p-1 rounded hover:bg-surface-container-low" @click="modalNuevo = false">
              <X class="w-4 h-4" />
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
  </Teleport>
</template>
