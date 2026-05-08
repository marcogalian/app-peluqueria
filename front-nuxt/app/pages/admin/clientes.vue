<script setup lang="ts">
/**
 * Gestión de clientes — solo visible para el admin.
 *
 * Funcionalidades:
 * - Listado con búsqueda
 * - Badge VIP y descuento
 * - Ver/editar perfil completo (con datos de contacto)
 * - Sección de fotos del historial de peinados
 * - Quién agregó cada cliente
 */
import {
  Search, Plus, Star, Percent, Camera,
  Phone, Mail, User, Loader2, ChevronRight,
  Upload, X, ZoomIn, Trash2, CalendarDays, Scissors,
} from 'lucide-vue-next'
import type {
  Cliente,
  FotoCliente,
  Genero,
  HistorialCliente,
  CitaHistorialCliente,
} from '~/modules/clientes/types/cliente.types'

definePageMeta({ middleware: ['auth', 'admin'] })

const config = useRuntimeConfig()

// ── Estado ────────────────────────────────────────────────
const clientes    = ref<Cliente[]>([])
const busqueda    = ref('')
const cargando    = ref(true)
const vistaClientes = ref<'activos' | 'archivados'>('activos')
const filtroTipoCliente = ref<'todos' | 'vip' | 'normales'>('todos')
const panelClienteRef = ref<HTMLElement | null>(null)

// Drawer de detalle del cliente (panel derecho como en el diseño)
const clienteSeleccionado = ref<Cliente | null>(null)
const drawerAbierto       = ref(false)
const tabActiva           = ref<'info' | 'fotos'>('info')

// Fotos del cliente seleccionado
const fotos            = ref<FotoCliente[]>([])
const cargandoFotos    = ref(false)
const fotoAmpliada     = ref<FotoCliente | null>(null)
const subiendo         = ref(false)
const descripcionFoto  = ref('')
const historialCliente = ref<HistorialCliente | null>(null)
const cargandoHistorial = ref(false)
const periodoActividad = ref<'mes' | 'anio'>('mes')

// Modal de confirmación de consentimiento
const modalConsentimiento = ref(false)

// Modal crear/editar cliente
const modalClienteAbierto = ref(false)
const guardandoCliente    = ref(false)
const clienteEditandoId   = ref<string | null>(null)
const formCliente = ref({
  nombre:    '',
  apellidos: '',
  telefono:  '',
  email:     '',
  genero:    'OTRO' as Genero,
  notas:     '',
  esVip:     false,
})

const mostrandoArchivados = computed(() => vistaClientes.value === 'archivados')
const ahora = computed(() => new Date())

// ── Filtrado local ────────────────────────────────────────
const clientesFiltrados = computed(() => {
  const q = busqueda.value.toLowerCase()
  return clientes.value.filter((c) => {
    const coincideBusqueda = !q ||
      `${c.nombre || ''} ${c.apellidos || ''}`.toLowerCase().includes(q) ||
      (c.telefono || '').includes(q) ||
      (c.email || '').toLowerCase().includes(q)

    const coincideTipo =
      filtroTipoCliente.value === 'todos' ||
      (filtroTipoCliente.value === 'vip' && c.esVip) ||
      (filtroTipoCliente.value === 'normales' && !c.esVip)

    return coincideBusqueda && coincideTipo
  })
})

const citasCompletadas = computed(() =>
  (historialCliente.value?.citasList || []).filter(cita => cita.estado === 'COMPLETADO'),
)

const visitasMesActual = computed(() => citasCompletadas.value.filter((cita) => {
  const fecha = new Date(cita.fechaHora)
  return fecha.getMonth() === ahora.value.getMonth() && fecha.getFullYear() === ahora.value.getFullYear()
}).length)

const visitasAnioActual = computed(() => citasCompletadas.value.filter((cita) => {
  const fecha = new Date(cita.fechaHora)
  return fecha.getFullYear() === ahora.value.getFullYear()
}).length)

const citasPeriodoSeleccionado = computed(() => citasCompletadas.value.filter((cita) => {
  const fecha = new Date(cita.fechaHora)
  if (periodoActividad.value === 'mes') {
    return fecha.getMonth() === ahora.value.getMonth() && fecha.getFullYear() === ahora.value.getFullYear()
  }
  return fecha.getFullYear() === ahora.value.getFullYear()
}))

const historialServiciosPeriodo = computed(() =>
  citasPeriodoSeleccionado.value
    .flatMap((cita) => cita.servicios.map(servicio => ({
      citaId: cita.id,
      fechaHora: cita.fechaHora,
      estado: cita.estado,
      nombre: servicio.nombre,
      categoria: servicio.categoria ?? 'Sin categoria',
      precio: servicio.precio ?? null,
    })))
    .sort((a, b) => new Date(b.fechaHora).getTime() - new Date(a.fechaHora).getTime()),
)

// ── Funciones ─────────────────────────────────────────────

onMounted(async () => {
  await cargarClientes()
})

async function cargarClientes() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get<Cliente[]>('/v1/clientes', {
      params: { archivado: mostrandoArchivados.value },
    })
    clientes.value = data || []
  } catch {
    clientes.value = []
  } finally {
    cargando.value = false
  }
}

async function abrirCliente(cliente: Cliente) {
  clienteSeleccionado.value = cliente
  tabActiva.value           = 'info'
  drawerAbierto.value       = true
  await cargarHistorialCliente(cliente.id)
  await nextTick()
  panelClienteRef.value?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

async function cargarHistorialCliente(clienteId: string) {
  cargandoHistorial.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get<HistorialCliente>(`/v1/clientes/${clienteId}/historial`)
    historialCliente.value = data
  } catch {
    historialCliente.value = null
  } finally {
    cargandoHistorial.value = false
  }
}

async function cargarFotos() {
  if (!clienteSeleccionado.value) return
  cargandoFotos.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get<FotoCliente[]>(
      `/v1/clientes/${clienteSeleccionado.value.id}/fotos`,
    )
    fotos.value = data
  } finally {
    cargandoFotos.value = false
  }
}

/** Se llama al cambiar a la tab de fotos — carga si no están en memoria */
function handleTabFotos() {
  tabActiva.value = 'fotos'
  // Solo comprobamos el consentimiento si no hay fotos ya cargadas
  if (!clienteSeleccionado.value?.consentimientoFotos) {
    modalConsentimiento.value = true
    return
  }
  cargarFotos()
}

async function registrarConsentimiento() {
  if (!clienteSeleccionado.value) return
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.post(`/v1/clientes/${clienteSeleccionado.value.id}/consentimiento`)
    clienteSeleccionado.value.consentimientoFotos = true
    modalConsentimiento.value = false
    cargarFotos()
  } catch {
    // El modal permanece abierto
  }
}

/** Sube una foto al servidor (multipart/form-data) */
async function subirFoto(event: Event) {
  const input = event.target as HTMLInputElement
  const file  = input.files?.[0]
  if (!file || !clienteSeleccionado.value) return

  subiendo.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const formData = new FormData()
    formData.append('foto', file)
    formData.append('descripcion', descripcionFoto.value)
    await api.post(
      `/v1/clientes/${clienteSeleccionado.value.id}/fotos`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    )
    descripcionFoto.value = ''
    await cargarFotos()
  } finally {
    subiendo.value = false
    input.value    = ''  // reseteamos el input para poder subir la misma foto
  }
}

async function eliminarFoto(foto: FotoCliente) {
  try {
    const { api } = await import('~/infrastructure/http/api')
    await api.delete(`/v1/fotos/${foto.id}`)
    fotos.value = fotos.value.filter(f => f.id !== foto.id)
    if (fotoAmpliada.value?.id === foto.id) fotoAmpliada.value = null
  } catch {
    // La foto permanece en la lista si falla
  }
}

/** Construye la URL completa de una foto */
function urlFoto(ruta: string): string {
  return `${config.public.uploadsBase}/${ruta}`
}

async function toggleVip(cliente: Cliente) {
  const { api } = await import('~/infrastructure/http/api')
  const { data } = await api.put<Cliente>(`/v1/clientes/${cliente.id}`, { esVip: !cliente.esVip })
  reemplazarClienteEnLista(data)
}

function abrirModalNuevoCliente() {
  clienteEditandoId.value = null
  formCliente.value = {
    nombre: '',
    apellidos: '',
    telefono: '',
    email: '',
    genero: 'OTRO',
    notas: '',
    esVip: false,
  }
  modalClienteAbierto.value = true
}

function abrirModalEditarCliente(cliente: Cliente) {
  clienteEditandoId.value = cliente.id
  formCliente.value = {
    nombre: cliente.nombre ?? '',
    apellidos: cliente.apellidos ?? '',
    telefono: cliente.telefono ?? '',
    email: cliente.email ?? '',
    genero: cliente.genero ?? 'OTRO',
    notas: cliente.notas ?? '',
    esVip: cliente.esVip ?? false,
  }
  modalClienteAbierto.value = true
}

function reemplazarClienteEnLista(clienteActualizado: Cliente) {
  clientes.value = clientes.value.map(cliente =>
    cliente.id === clienteActualizado.id ? clienteActualizado : cliente,
  )
  if (clienteSeleccionado.value?.id === clienteActualizado.id) {
    clienteSeleccionado.value = clienteActualizado
  }
}

async function cambiarVista(vista: 'activos' | 'archivados') {
  if (vistaClientes.value === vista) return
  vistaClientes.value = vista
  drawerAbierto.value = false
  clienteSeleccionado.value = null
  historialCliente.value = null
  await cargarClientes()
}

async function archivarOReactivarCliente(cliente: Cliente) {
  const { api } = await import('~/infrastructure/http/api')
  const endpoint = cliente.archivado ? 'reactivar' : 'archivar'
  await api.post(`/v1/clientes/${cliente.id}/${endpoint}`)
  drawerAbierto.value = false
  clienteSeleccionado.value = null
  historialCliente.value = null
  await cargarClientes()
}

function formatearFecha(fechaIso: string) {
  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(fechaIso))
}

async function guardarCliente() {
  if (!formCliente.value.nombre || !formCliente.value.telefono) return
  guardandoCliente.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    if (clienteEditandoId.value) {
      const { data } = await api.put<Cliente>(`/v1/clientes/${clienteEditandoId.value}`, formCliente.value)
      reemplazarClienteEnLista(data)
    } else {
      const { data } = await api.post<Cliente>('/v1/clientes', formCliente.value)
      clientes.value.unshift(data)
    }
    modalClienteAbierto.value = false
  } finally {
    guardandoCliente.value = false
  }
}
</script>

<template>
  <div class="flex flex-col lg:flex-row gap-6 h-full min-h-0 items-start">

    <!-- ══════════════════════════════════════════════════════
         PANEL IZQUIERDO — Lista de clientes
         ════════════════════════════════════════════════════ -->
    <div class="flex flex-col flex-1 min-w-0 space-y-4">

      <!-- Barra de búsqueda + botón añadir -->
      <div class="flex gap-3 items-start">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
          <input
            v-model="busqueda"
            type="search"
            placeholder="Buscar clientes..."
            class="input pl-10"
          />
        </div>
        <div class="flex rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-1">
          <button
            class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="filtroTipoCliente === 'todos' ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
            @click="filtroTipoCliente = 'todos'"
          >
            Todos
          </button>
          <button
            class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="filtroTipoCliente === 'vip' ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
            @click="filtroTipoCliente = 'vip'"
          >
            VIP
          </button>
          <button
            class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="filtroTipoCliente === 'normales' ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
            @click="filtroTipoCliente = 'normales'"
          >
            Normales
          </button>
        </div>
        <div class="flex rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-1">
          <button
            class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="!mostrandoArchivados ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
            @click="cambiarVista('activos')"
          >
            Activos
          </button>
          <button
            class="px-3 py-2 rounded-lg text-sm font-medium transition-colors"
            :class="mostrandoArchivados ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
            @click="cambiarVista('archivados')"
          >
            Archivados
          </button>
        </div>
        <button class="btn-primary" @click="abrirModalNuevoCliente">
          <Plus class="w-4 h-4" />
          Nuevo cliente
        </button>
      </div>

      <!-- Loader -->
      <div v-if="cargando" class="flex items-center justify-center py-12">
        <Loader2 class="w-5 h-5 animate-spin text-primary" />
      </div>

      <!-- Lista -->
      <div v-else class="space-y-2">
        <div
          v-for="cliente in clientesFiltrados"
          :key="cliente.id"
          class="card p-4 cursor-pointer flex items-center gap-4 group"
          :class="clienteSeleccionado?.id === cliente.id ? 'border-primary shadow-card-md' : ''"
          @click="abrirCliente(cliente)"
        >
          <!-- Avatar con inicial -->
          <div class="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
            <span class="text-primary font-semibold text-sm">
              {{ (cliente.nombre || '?').charAt(0).toUpperCase() }}
            </span>
          </div>

          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 flex-wrap">
              <p class="font-medium text-text-primary">{{ cliente.nombre }} {{ cliente.apellidos }}</p>
              <span v-if="cliente.esVip" class="badge-vip inline-flex items-center gap-1.5">
                <Star class="w-3.5 h-3.5 fill-current text-amber-500" />
                VIP
              </span>
              <span
                v-if="cliente.descuentoPorcentaje"
                class="px-2 py-0.5 rounded-pill text-xs font-medium bg-green-100 text-green-700"
              >
                -{{ cliente.descuentoPorcentaje }}%
              </span>
            </div>
            <p class="text-sm text-text-secondary mt-0.5 truncate">{{ cliente.telefono }}</p>
          </div>

          <ChevronRight class="w-4 h-4 text-text-muted group-hover:text-primary transition-colors" />
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
          PANEL DERECHO — Detalle del cliente (drawer lateral)
          Igual que en la imagen de referencia del diseño
          ════════════════════════════════════════════════════ -->
    <Transition name="modal-overlay">
      <aside
        v-if="drawerAbierto && clienteSeleccionado"
        ref="panelClienteRef"
        class="w-96 max-w-full max-h-[calc(100vh-2rem)] flex-shrink-0 self-start sticky top-4 card p-0 flex flex-col min-h-0 overflow-hidden animate-slide-in-right"
      >
        <!-- Cabecera del drawer -->
        <div class="flex items-center justify-between px-5 py-4 border-b border-surface-border">
          <div>
            <p class="font-semibold text-text-primary">
              {{ clienteSeleccionado.nombre }} {{ clienteSeleccionado.apellidos }}
            </p>
            <p class="text-xs text-text-secondary mt-0.5">
              Añadido por {{ clienteSeleccionado.agregadoPorNombre }}
            </p>
          </div>
          <button class="btn-ghost py-1 px-1.5" @click="drawerAbierto = false">
            <X class="w-4 h-4" />
          </button>
        </div>

        <!-- Tabs: Información | Fotos -->
        <div class="flex border-b border-surface-border px-5">
          <button
            class="py-3 px-2 text-sm font-medium border-b-2 transition-colors"
            :class="tabActiva === 'info'
              ? 'border-primary text-primary'
              : 'border-transparent text-text-secondary hover:text-text-primary'"
            @click="tabActiva = 'info'"
          >
            Información
          </button>
          <button
            class="py-3 px-2 text-sm font-medium border-b-2 transition-colors flex items-center gap-1.5"
            :class="tabActiva === 'fotos'
              ? 'border-primary text-primary'
              : 'border-transparent text-text-secondary hover:text-text-primary'"
            @click="handleTabFotos"
          >
            <Camera class="w-3.5 h-3.5" />
            Fotos
            <span v-if="fotos.length > 0" class="text-xs bg-primary/10 text-primary px-1.5 rounded-full">
              {{ fotos.length }}
            </span>
          </button>
        </div>

        <!-- ── TAB: Información ─────────────────────────── -->
        <div v-if="tabActiva === 'info'" class="flex-1 min-h-0 overflow-y-auto p-5 space-y-4">

          <!-- Badges VIP y descuento -->
          <div class="flex items-center gap-2 flex-wrap">
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
              :class="clienteSeleccionado.esVip
                ? 'bg-vip-light text-vip border border-vip/30'
                : 'bg-surface-muted text-text-secondary border border-surface-border'"
              @click="toggleVip(clienteSeleccionado)"
            >
              <Star
                class="w-3.5 h-3.5"
                :class="clienteSeleccionado.esVip ? 'fill-current text-amber-500' : 'text-text-secondary'"
              />
              {{ clienteSeleccionado.esVip ? 'VIP' : 'Normal' }}
            </button>
            <span
              v-if="clienteSeleccionado.descuentoPorcentaje"
              class="flex items-center gap-1 px-3 py-1.5 rounded-lg bg-green-50 text-green-700 text-sm border border-green-200"
            >
              <Percent class="w-3.5 h-3.5" />
              {{ clienteSeleccionado.descuentoPorcentaje }}% descuento
            </span>
          </div>

          <!-- Datos de contacto -->
          <div class="space-y-3">
            <div class="flex items-center gap-3 p-3 bg-surface-muted rounded-lg">
              <Phone class="w-4 h-4 text-text-muted flex-shrink-0" />
              <span class="text-sm text-text-primary">{{ clienteSeleccionado.telefono }}</span>
            </div>
            <div class="flex items-center gap-3 p-3 bg-surface-muted rounded-lg">
              <Mail class="w-4 h-4 text-text-muted flex-shrink-0" />
              <span class="text-sm text-text-primary">{{ clienteSeleccionado.email }}</span>
            </div>
          </div>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <label class="label mb-0">Actividad</label>
              <div class="flex rounded-xl border border-outline-variant/40 bg-surface-container-lowest p-1">
                <button
                  class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
                  :class="periodoActividad === 'mes' ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
                  @click="periodoActividad = 'mes'"
                >
                  Mes
                </button>
                <button
                  class="px-3 py-1.5 rounded-lg text-xs font-medium transition-colors"
                  :class="periodoActividad === 'anio' ? 'bg-primary-container text-white' : 'text-text-secondary hover:text-text-primary'"
                  @click="periodoActividad = 'anio'"
                >
                  Año
                </button>
              </div>
            </div>

            <div v-if="cargandoHistorial" class="flex items-center justify-center py-6">
              <Loader2 class="w-5 h-5 animate-spin text-primary" />
            </div>

            <template v-else>
              <div class="grid grid-cols-2 gap-3">
                <div class="rounded-2xl border border-outline-variant/30 bg-surface-muted p-4">
                  <div class="flex items-center gap-2 text-text-secondary text-sm">
                    <CalendarDays class="w-4 h-4" />
                    Visitas este mes
                  </div>
                  <p class="mt-2 text-2xl font-semibold text-text-primary">{{ visitasMesActual }}</p>
                </div>
                <div class="rounded-2xl border border-outline-variant/30 bg-surface-muted p-4">
                  <div class="flex items-center gap-2 text-text-secondary text-sm">
                    <CalendarDays class="w-4 h-4" />
                    Visitas este año
                  </div>
                  <p class="mt-2 text-2xl font-semibold text-text-primary">{{ visitasAnioActual }}</p>
                </div>
              </div>

              <div class="rounded-2xl border border-outline-variant/30 bg-surface-muted p-4 space-y-3">
                <div class="flex items-center gap-2">
                  <Scissors class="w-4 h-4 text-primary" />
                  <p class="text-sm font-semibold text-text-primary">
                    Servicios del {{ periodoActividad === 'mes' ? 'mes' : 'año' }}
                  </p>
                </div>

                <div v-if="historialServiciosPeriodo.length === 0" class="text-sm text-text-secondary">
                  No hay servicios registrados en este periodo.
                </div>

                <div v-else class="space-y-2">
                  <div
                    v-for="servicio in historialServiciosPeriodo.slice(0, 8)"
                    :key="`${servicio.citaId}-${servicio.nombre}`"
                    class="rounded-xl bg-white/80 px-3 py-2 border border-outline-variant/20"
                  >
                    <div class="flex items-center justify-between gap-3">
                      <div class="min-w-0">
                        <p class="text-sm font-medium text-text-primary truncate">{{ servicio.nombre }}</p>
                        <p class="text-xs text-text-secondary">
                          {{ servicio.categoria }} · {{ formatearFecha(servicio.fechaHora) }}
                        </p>
                      </div>
                      <span v-if="servicio.precio !== null" class="text-xs font-semibold text-primary whitespace-nowrap">
                        {{ servicio.precio }} EUR
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>

          <!-- Notas generales -->
          <div v-if="clienteSeleccionado.notas">
            <label class="label">Notas</label>
            <p class="text-sm text-text-secondary bg-surface-muted rounded-lg p-3">
              {{ clienteSeleccionado.notas }}
            </p>
          </div>

          <div class="flex gap-3">
            <button class="btn-secondary flex-1" @click="abrirModalEditarCliente(clienteSeleccionado)">
              Editar ficha
            </button>
            <button class="btn-secondary flex-1" @click="archivarOReactivarCliente(clienteSeleccionado)">
              {{ clienteSeleccionado.archivado ? 'Reactivar' : 'Archivar' }}
            </button>
            <button class="btn-primary flex-1" @click="drawerAbierto = false">
              Cerrar
            </button>
          </div>
        </div>

        <!-- ── TAB: Fotos del historial ─────────────────── -->
        <div v-if="tabActiva === 'fotos'" class="flex-1 min-h-0 overflow-y-auto p-5 space-y-4">

          <!-- Subir nueva foto -->
          <div class="border-2 border-dashed border-surface-border rounded-lg p-4 text-center">
            <label class="cursor-pointer block">
              <Upload class="w-6 h-6 text-text-muted mx-auto mb-2" />
              <p class="text-sm text-text-secondary mb-2">
                {{ subiendo ? 'Subiendo...' : 'Subir foto del peinado' }}
              </p>
              <input
                type="text"
                v-model="descripcionFoto"
                placeholder="Descripción (opcional)"
                class="input mb-2 text-sm"
                @click.stop
              />
              <input
                type="file"
                accept="image/*"
                class="hidden"
                :disabled="subiendo"
                @change="subirFoto"
              />
              <span class="btn-secondary text-sm py-1.5 px-3">
                <Loader2 v-if="subiendo" class="w-4 h-4 animate-spin" />
                <span v-else>Seleccionar foto</span>
              </span>
            </label>
          </div>

          <!-- Galería de fotos -->
          <div v-if="cargandoFotos" class="flex items-center justify-center py-8">
            <Loader2 class="w-5 h-5 animate-spin text-primary" />
          </div>

          <div v-else-if="fotos.length === 0" class="text-center py-8">
            <Camera class="w-8 h-8 text-surface-border mx-auto mb-2" />
            <p class="text-sm text-text-muted">Sin fotos todavía</p>
          </div>

          <div v-else class="grid grid-cols-2 gap-2">
            <div
              v-for="foto in fotos"
              :key="foto.id"
              class="relative group rounded-lg overflow-hidden aspect-square bg-surface-muted"
            >
              <img
                :src="urlFoto(foto.rutaArchivo)"
                :alt="foto.descripcion || 'Foto peinado'"
                class="w-full h-full object-cover transition-transform duration-200 group-hover:scale-105"
              />
              <!-- Overlay con acciones al hacer hover -->
              <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2">
                <button
                  class="w-8 h-8 rounded-full bg-white/90 flex items-center justify-center text-text-primary hover:bg-white transition-colors"
                  @click="fotoAmpliada = foto"
                >
                  <ZoomIn class="w-4 h-4" />
                </button>
                <button
                  class="w-8 h-8 rounded-full bg-red-500/90 flex items-center justify-center text-white hover:bg-red-600 transition-colors"
                  @click="eliminarFoto(foto)"
                >
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
              <!-- Descripción -->
              <div v-if="foto.descripcion" class="absolute bottom-0 left-0 right-0 bg-black/50 px-2 py-1">
                <p class="text-white text-2xs truncate">{{ foto.descripcion }}</p>
              </div>
            </div>
          </div>

        </div>

      </aside>
    </Transition>

  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Foto ampliada
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalClienteAbierto"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalClienteAbierto = false"
      >
        <div class="bg-white rounded-card shadow-card-lg max-w-2xl w-full max-h-[calc(100vh-2rem)] overflow-y-auto p-6 animate-fade-scale-in">
          <div class="flex items-center justify-between mb-5">
            <div>
              <h3 class="text-lg font-semibold text-text-primary">
                {{ clienteEditandoId ? 'Editar cliente' : 'Nuevo cliente' }}
              </h3>
              <p class="text-sm text-text-secondary">
                {{ clienteEditandoId ? 'Actualiza la ficha del cliente.' : 'Da de alta un nuevo cliente en el sistema.' }}
              </p>
            </div>
            <button class="btn-ghost py-1 px-1.5" @click="modalClienteAbierto = false">
              <X class="w-4 h-4" />
            </button>
          </div>

          <form class="space-y-4" @submit.prevent="guardarCliente">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="label">Nombre</label>
                <input v-model="formCliente.nombre" type="text" class="input" required />
              </div>
              <div>
                <label class="label">Apellidos</label>
                <input v-model="formCliente.apellidos" type="text" class="input" />
              </div>
              <div>
                <label class="label">Teléfono</label>
                <input v-model="formCliente.telefono" type="text" class="input" required />
              </div>
              <div>
                <label class="label">Email</label>
                <input v-model="formCliente.email" type="email" class="input" />
              </div>
              <div>
                <label class="label">Género</label>
                <select v-model="formCliente.genero" class="input">
                  <option value="FEMENINO">Femenino</option>
                  <option value="MASCULINO">Masculino</option>
                  <option value="OTRO">Otro</option>
                </select>
              </div>
            </div>

            <label class="flex items-center gap-3 rounded-lg border border-surface-border bg-surface-muted px-4 py-3">
              <input v-model="formCliente.esVip" type="checkbox" class="h-4 w-4" />
              <span class="text-sm text-text-primary">Cliente VIP</span>
            </label>

            <div>
              <label class="label">Notas</label>
              <textarea v-model="formCliente.notas" rows="3" class="input resize-none" />
            </div>

            <div class="flex justify-end gap-3 pt-2">
              <button type="button" class="btn-secondary" @click="modalClienteAbierto = false">
                Cancelar
              </button>
              <button type="submit" class="btn-primary" :disabled="guardandoCliente">
                <Loader2 v-if="guardandoCliente" class="w-4 h-4 animate-spin" />
                <span v-else>{{ clienteEditandoId ? 'Guardar cambios' : 'Crear cliente' }}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="fotoAmpliada"
        class="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4"
        @click.self="fotoAmpliada = null"
      >
        <div class="relative max-w-2xl w-full animate-fade-scale-in">
          <img
            :src="urlFoto(fotoAmpliada.rutaArchivo)"
            :alt="fotoAmpliada.descripcion"
            class="w-full rounded-card"
          />
          <button
            class="absolute top-3 right-3 w-8 h-8 bg-white/90 rounded-full flex items-center justify-center text-text-primary hover:bg-white"
            @click="fotoAmpliada = null"
          >
            <X class="w-4 h-4" />
          </button>
          <div v-if="fotoAmpliada.descripcion" class="mt-2 text-center">
            <p class="text-white text-sm">{{ fotoAmpliada.descripcion }}</p>
            <p class="text-white/50 text-xs mt-1">Subida por {{ fotoAmpliada.subidaPorNombre }}</p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Consentimiento de fotos
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalConsentimiento"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalConsentimiento = false; tabActiva = 'info'"
      >
        <div class="bg-white rounded-card shadow-card-lg max-w-md w-full p-6 animate-fade-scale-in">
          <Camera class="w-10 h-10 text-primary mx-auto mb-3" />
          <h3 class="text-lg font-semibold text-text-primary text-center mb-2">
            Consentimiento para fotos
          </h3>
          <p class="text-sm text-text-secondary text-center mb-5">
            El cliente debe aceptar expresamente que sus fotos se almacenen en el sistema.
            Este consentimiento quedará registrado con fecha y hora.
          </p>
          <div class="space-y-2 text-sm text-text-secondary bg-surface-muted rounded-lg p-3 mb-5">
            <p>• Las fotos se usarán exclusivamente para consulta de peinados anteriores.</p>
            <p>• No se compartirán con terceros.</p>
            <p>• El cliente puede solicitar su eliminación en cualquier momento.</p>
          </div>
          <div class="flex gap-3">
            <button class="btn-secondary flex-1" @click="modalConsentimiento = false; tabActiva = 'info'">
              Cancelar
            </button>
            <button class="btn-primary flex-1" @click="registrarConsentimiento">
              El cliente acepta
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
