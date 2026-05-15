<script setup lang="ts">
import {
  ChevronLeft,
  ChevronRight,
  Clock,
  Loader2,
  Plus,
  Search,
  Trash2,
  Users,
  X,
} from 'lucide-vue-next'
import {
  addDays,
  format,
  isSameDay,
  isToday,
  startOfWeek,
  subDays,
} from 'date-fns'
import { es } from 'date-fns/locale'
import { isAxiosError } from 'axios'
import { api } from '~/infrastructure/http/api'
import { useToast } from '~/modules/shared/composables/useToast'

type EstadoCita = 'PENDIENTE' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADO'
type GeneroCliente = 'FEMENINO' | 'MASCULINO'

interface ClienteAgenda {
  id: string
  nombre: string
  apellidos: string
  telefono?: string
  email?: string
  genero?: GeneroCliente
  esVip?: boolean
  descuentoPorcentaje?: number | null
}

interface ServicioAgenda {
  id: string
  nombre: string
  duracionMinutos?: number
}

interface PeluqueroAgenda {
  id: string
  nombre: string
  disponible?: boolean
  user?: {
    id?: string
    username?: string
  }
}

interface CitaApi {
  id: string
  fechaHora: string
  duracionTotal?: number
  estado: EstadoCita
  comentarios?: string
  motivoCancelacion?: string | null
  cliente: ClienteAgenda
  peluquero: PeluqueroAgenda
  servicios: ServicioAgenda[]
}

interface CitaAgendaItem {
  id: string
  horaInicio: string
  duracionMinutos: number
  estado: EstadoCita
  clienteId: string
  clienteNombre: string
  clienteApellidos: string
  clienteTelefono: string
  clienteEmail: string
  clienteEsVip: boolean
  peluqueroId: string
  servicioId: string
  servicioNombre: string
  comentarios: string
  motivoCancelacion: string | null
}

const toast = useToast()
const authStore = useAuthStore()

const HORA_INICIO = 9
const HORA_FIN = 21
const SLOT_MINUTOS = 15

const diaActual = ref(new Date())
const cargando = ref(true)
const guardando = ref(false)
const errorCarga = ref('')

const peluqueros = ref<PeluqueroAgenda[]>([])
const clientes = ref<ClienteAgenda[]>([])
const servicios = ref<ServicioAgenda[]>([])
const citasDia = ref<CitaAgendaItem[]>([])

const miPeluqueroId = ref<string | null>(null)
const peluqueroSeleccionadoId = ref<string | null>(null)
const mostrandoAgendas = ref(false)
const modalCitaAbierto = ref(false)
const citaEditando = ref<CitaAgendaItem | null>(null)
const horaSeleccionada = ref<string | null>(null)

const clienteQuery = ref('')
const clienteSeleccionadoId = ref<string | null>(null)
const notasCita = ref('')
const errorGeneralFormulario = ref('')

const erroresFormulario = reactive({
  cliente: '',
  telefono: '',
  servicio: '',
})

const formCita = reactive({
  servicioId: '',
})

const formNuevoCliente = reactive({
  telefono: '',
  email: '',
  genero: 'FEMENINO' as GeneroCliente,
})

const formEdicion = reactive({
  estado: 'PENDIENTE' as EstadoCita,
  motivoCancelacion: '',
})

const slots = computed(() => {
  const resultado: string[] = []
  for (let h = HORA_INICIO; h < HORA_FIN; h++) {
    for (let m = 0; m < 60; m += SLOT_MINUTOS) {
      resultado.push(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`)
    }
  }
  return resultado
})

const textoFecha = computed(() => {
  const value = format(diaActual.value, "EEEE d 'de' MMMM", { locale: es })
  return value.charAt(0).toUpperCase() + value.slice(1)
})

const diasSemana = computed(() => {
  const inicio = startOfWeek(diaActual.value, { weekStartsOn: 1 })
  return Array.from({ length: 7 }, (_, i) => addDays(inicio, i))
})

const peluqueroActual = computed(() =>
  peluqueros.value.find(p => p.id === peluqueroSeleccionadoId.value) ?? null,
)

const viendoAgendaAjena = computed(() =>
  Boolean(
    miPeluqueroId.value &&
    peluqueroSeleccionadoId.value &&
    miPeluqueroId.value !== peluqueroSeleccionadoId.value,
  ),
)

const puedeEditarAgendaActual = computed(() =>
  authStore.isAdmin || !viendoAgendaAjena.value,
)

const clienteSeleccionado = computed(() =>
  clientes.value.find(c => c.id === clienteSeleccionadoId.value) ?? null,
)

const servicioSeleccionado = computed(() =>
  servicios.value.find(servicio => servicio.id === formCita.servicioId) ?? null,
)

const coincidenciasClientes = computed(() => {
  const query = clienteQuery.value.trim().toLowerCase()
  if (!query || clienteSeleccionadoId.value) return []

  return clientes.value
    .filter((cliente) => {
      const nombreCompleto = `${cliente.nombre} ${cliente.apellidos}`.trim().toLowerCase()
      return nombreCompleto.includes(query) || (cliente.telefono ?? '').includes(query)
    })
    .slice(0, 6)
})

const primerClienteCoincidente = computed(() => coincidenciasClientes.value[0] ?? null)

const clienteEsNuevo = computed(() =>
  clienteQuery.value.trim().length > 0 && !clienteSeleccionado.value,
)

const telefonoCliente = computed(() => clienteSeleccionado.value?.telefono ?? '')
const emailCliente = computed(() => clienteSeleccionado.value?.email ?? '')
const generoCliente = computed(() => clienteSeleccionado.value?.genero ?? 'FEMENINO')

const modalEnEdicion = computed(() => Boolean(citaEditando.value))

watch(clienteQuery, (nuevoValor) => {
  erroresFormulario.cliente = ''
  errorGeneralFormulario.value = ''

  if (!nuevoValor.trim()) {
    clienteSeleccionadoId.value = null
    return
  }

  if (clienteSeleccionado.value) {
    const nombreActual = nombreCompleto(clienteSeleccionado.value).toLowerCase()
    if (nuevoValor.trim().toLowerCase() !== nombreActual) {
      clienteSeleccionadoId.value = null
    }
  }

  const matchExacto = clientes.value.find((cliente) =>
    nombreCompleto(cliente).toLowerCase() === nuevoValor.trim().toLowerCase(),
  )

  if (matchExacto) {
    seleccionarCliente(matchExacto)
  }
})

watch([diaActual, peluqueroSeleccionadoId], async () => {
  if (peluqueroSeleccionadoId.value) {
    await cargarCitasDia()
  }
})

watch(() => formNuevoCliente.telefono, () => {
  erroresFormulario.telefono = ''
  errorGeneralFormulario.value = ''
})

watch(() => formCita.servicioId, () => {
  erroresFormulario.servicio = ''
  errorGeneralFormulario.value = ''
})

function nombreCompleto(cliente: ClienteAgenda) {
  return `${cliente.nombre} ${cliente.apellidos ?? ''}`.trim()
}

function getFechaHoraIso(hora: string) {
  return `${format(diaActual.value, 'yyyy-MM-dd')}T${hora}:00`
}

function mapearCita(cita: CitaApi): CitaAgendaItem {
  return {
    id: cita.id,
    horaInicio: format(new Date(cita.fechaHora), 'HH:mm'),
    duracionMinutos: cita.duracionTotal || cita.servicios?.[0]?.duracionMinutos || 30,
    estado: cita.estado,
    clienteId: cita.cliente?.id ?? '',
    clienteNombre: cita.cliente?.nombre ?? '',
    clienteApellidos: cita.cliente?.apellidos ?? '',
    clienteTelefono: cita.cliente?.telefono ?? '',
    clienteEmail: cita.cliente?.email ?? '',
    clienteEsVip: Boolean(cita.cliente?.esVip),
    peluqueroId: cita.peluquero?.id ?? '',
    servicioId: cita.servicios?.[0]?.id ?? '',
    servicioNombre: cita.servicios?.[0]?.nombre ?? 'Sin servicio',
    comentarios: cita.comentarios ?? '',
    motivoCancelacion: cita.motivoCancelacion ?? null,
  }
}

function slotsCita(cita: CitaAgendaItem) {
  return Math.max(1, Math.ceil((cita.duracionMinutos || 30) / SLOT_MINUTOS))
}

function parseHora(hora: string) {
  const [h, m] = hora.split(':').map(Number)
  return h * 60 + m
}

function duracionNuevaCita() {
  return Math.max(SLOT_MINUTOS, servicioSeleccionado.value?.duracionMinutos || 30)
}

function citaOcupaSlot(hora: string) {
  const minutoSlot = parseHora(hora)
  return citasDia.value.find((cita) => {
    const minutoCita = parseHora(cita.horaInicio)
    return minutoSlot >= minutoCita && minutoSlot < minutoCita + cita.duracionMinutos
  })
}

function esInicioCita(hora: string) {
  return citasDia.value.find(cita => cita.horaInicio === hora)
}

function puedeEmpezarCita(hora: string) {
  if (citaOcupaSlot(hora)) return false

  const inicio = parseHora(hora)
  const fin = inicio + duracionNuevaCita()
  const cierre = HORA_FIN * 60

  if (fin > cierre) return false

  return !citasDia.value.some((cita) => {
    const citaInicio = parseHora(cita.horaInicio)
    const citaFin = citaInicio + cita.duracionMinutos
    return inicio < citaFin && fin > citaInicio
  })
}

function seleccionarCliente(cliente: ClienteAgenda) {
  clienteSeleccionadoId.value = cliente.id
  clienteQuery.value = nombreCompleto(cliente)
}

function limpiarClienteSeleccionado() {
  const habiaCliente = Boolean(clienteSeleccionado.value)
  clienteSeleccionadoId.value = null
  if (habiaCliente) {
    clienteQuery.value = ''
  }
}

function resolverClienteDesdeBusqueda() {
  if (clienteSeleccionado.value) return

  const query = clienteQuery.value.trim()
  if (!query) return

  const matchExacto = clientes.value.find((cliente) =>
    nombreCompleto(cliente).toLowerCase() === query.toLowerCase(),
  )

  if (matchExacto) {
    seleccionarCliente(matchExacto)
    return
  }

  if (primerClienteCoincidente.value) {
    seleccionarCliente(primerClienteCoincidente.value)
  }
}

function resetFormularioCita() {
  horaSeleccionada.value = null
  citaEditando.value = null
  clienteQuery.value = ''
  clienteSeleccionadoId.value = null
  formCita.servicioId = ''
  notasCita.value = ''
  formNuevoCliente.telefono = ''
  formNuevoCliente.email = ''
  formNuevoCliente.genero = 'FEMENINO'
  formEdicion.estado = 'PENDIENTE'
  formEdicion.motivoCancelacion = ''
  errorGeneralFormulario.value = ''
  erroresFormulario.cliente = ''
  erroresFormulario.telefono = ''
  erroresFormulario.servicio = ''
}

function seleccionarHora(hora: string) {
  if (!puedeEmpezarCita(hora)) return
  resetFormularioCita()
  horaSeleccionada.value = hora
  modalCitaAbierto.value = true
}

function cerrarModalCita() {
  modalCitaAbierto.value = false
  resetFormularioCita()
}

function abrirEditarCita(cita: CitaAgendaItem) {
  resetFormularioCita()
  citaEditando.value = cita
  horaSeleccionada.value = cita.horaInicio
  formCita.servicioId = cita.servicioId
  notasCita.value = cita.comentarios
  formEdicion.estado = cita.estado
  formEdicion.motivoCancelacion = cita.motivoCancelacion ?? ''

  const cliente = clientes.value.find(c => c.id === cita.clienteId)
  if (cliente) {
    seleccionarCliente(cliente)
  } else {
    clienteQuery.value = `${cita.clienteNombre} ${cita.clienteApellidos}`.trim()
    formNuevoCliente.telefono = cita.clienteTelefono
    formNuevoCliente.email = cita.clienteEmail
  }

  modalCitaAbierto.value = true
}

function irDia(dia: Date) {
  diaActual.value = dia
}

async function cargarDatosBase() {
  const [resPeluqueros, resClientes, resServicios] = await Promise.all([
    api.get<PeluqueroAgenda[]>('/peluqueros'),
    api.get<ClienteAgenda[]>('/v1/clientes'),
    api.get<ServicioAgenda[]>('/v1/servicios'),
  ])

  peluqueros.value = resPeluqueros.data ?? []
  clientes.value = resClientes.data ?? []
  servicios.value = resServicios.data ?? []

  const mio = peluqueros.value.find((peluquero) =>
    peluquero.user?.username === authStore.usuario?.username ||
    peluquero.user?.id === authStore.usuario?.id,
  )

  miPeluqueroId.value = mio?.id ?? null
  peluqueroSeleccionadoId.value = miPeluqueroId.value ?? peluqueros.value[0]?.id ?? null
}

async function cargarCitasDia() {
  if (!peluqueroSeleccionadoId.value) {
    citasDia.value = []
    return
  }

  cargando.value = true
  errorCarga.value = ''

  try {
    const inicio = `${format(diaActual.value, 'yyyy-MM-dd')}T00:00:00`
    const fin = `${format(diaActual.value, 'yyyy-MM-dd')}T23:59:59`
    const { data } = await api.get<CitaApi[]>('/citas', {
      params: {
        start: inicio,
        end: fin,
        peluqueroId: peluqueroSeleccionadoId.value,
      },
    })

    citasDia.value = (data ?? []).map(mapearCita)
  } catch {
    citasDia.value = []
    errorCarga.value = 'No se pudieron cargar las citas del día.'
  } finally {
    cargando.value = false
  }
}

function separarNombreCompleto(value: string) {
  const partes = value.trim().split(/\s+/)
  return {
    nombre: partes[0] ?? '',
    apellidos: partes.slice(1).join(' '),
  }
}

async function crearClienteSiHaceFalta() {
  if (clienteSeleccionado.value) {
    return clienteSeleccionado.value.id
  }

  const nombrePlano = clienteQuery.value.trim()
  if (!nombrePlano) {
    throw new Error('Escribe el nombre del cliente.')
  }
  if (!formNuevoCliente.telefono.trim()) {
    throw new Error('Añade un teléfono para guardar el cliente.')
  }

  const { nombre, apellidos } = separarNombreCompleto(nombrePlano)
  if (!nombre) {
    throw new Error('Escribe al menos el nombre del cliente.')
  }

  const { data } = await api.post<ClienteAgenda>('/v1/clientes', {
    nombre,
    apellidos,
    telefono: formNuevoCliente.telefono.trim(),
    email: formNuevoCliente.email.trim(),
    genero: formNuevoCliente.genero,
    esVip: false,
  })

  clientes.value.unshift(data)
  seleccionarCliente(data)
  return data.id
}

async function guardarCita() {
  errorGeneralFormulario.value = ''
  erroresFormulario.cliente = ''
  erroresFormulario.telefono = ''
  erroresFormulario.servicio = ''

  if (!horaSeleccionada.value) {
    errorGeneralFormulario.value = 'Selecciona una hora en la agenda antes de guardar.'
    return
  }
  if (!puedeEditarAgendaActual.value) {
    errorGeneralFormulario.value = 'Solo puedes crear citas en tu propia agenda.'
    return
  }
  if (!peluqueroSeleccionadoId.value) {
    errorGeneralFormulario.value = 'No hay una agenda disponible para guardar esta cita.'
    return
  }
  if (!clienteQuery.value.trim()) {
    erroresFormulario.cliente = 'Escribe el nombre del cliente.'
  }
  if (!clienteSeleccionado.value && !formNuevoCliente.telefono.trim()) {
    erroresFormulario.telefono = 'Añade un teléfono para guardar al cliente.'
  }
  if (!formCita.servicioId) {
    erroresFormulario.servicio = 'Selecciona un servicio.'
  }

  if (erroresFormulario.cliente || erroresFormulario.telefono || erroresFormulario.servicio) {
    return
  }

  guardando.value = true
  try {
    const clienteId = await crearClienteSiHaceFalta()
    const payload = {
      id: citaEditando.value?.id,
      fechaHora: getFechaHoraIso(horaSeleccionada.value),
      duracionTotal: servicioSeleccionado.value?.duracionMinutos || citaEditando.value?.duracionMinutos || 30,
      estado: modalEnEdicion.value ? formEdicion.estado : 'PENDIENTE',
      comentarios: notasCita.value.trim(),
      motivoCancelacion: formEdicion.estado === 'CANCELADO' ? formEdicion.motivoCancelacion.trim() : null,
      cliente: { id: clienteId },
      peluquero: { id: peluqueroSeleccionadoId.value },
      servicios: [{ id: formCita.servicioId }],
    }

    if (citaEditando.value) {
      await api.put(`/citas/${citaEditando.value.id}`, payload)
      toast.success('Cita actualizada')
    } else {
      await api.post('/citas', payload)
      toast.success('Cita guardada')
    }

    cerrarModalCita()
    await cargarCitasDia()
  } catch (error) {
    let message = citaEditando.value ? 'No se pudo actualizar la cita.' : 'No se pudo guardar la cita.'

    if (isAxiosError(error)) {
      const status = error.response?.status
      const apiMessage = error.response?.data?.message

      if (status === 403) {
        message = viendoAgendaAjena.value
          ? 'Solo puedes crear citas en tu propia agenda.'
          : 'No tienes permisos para guardar esta cita.'
      } else if (typeof apiMessage === 'string' && apiMessage.trim()) {
        message = apiMessage
      } else if (status === 409) {
        message = 'Ese hueco ya no está libre. Recarga la agenda y prueba otra hora.'
      }
    } else if (error instanceof Error && error.message.trim()) {
      message = error.message
    }

    errorGeneralFormulario.value = message
  } finally {
    guardando.value = false
  }
}

async function eliminarCita() {
  if (!citaEditando.value || !authStore.isAdmin) return
  const confirmar = window.confirm('¿Eliminar esta cita definitivamente?')
  if (!confirmar) return

  guardando.value = true
  errorGeneralFormulario.value = ''
  try {
    await api.delete(`/citas/${citaEditando.value.id}`)
    toast.success('Cita eliminada')
    cerrarModalCita()
    await cargarCitasDia()
  } catch (error) {
    if (isAxiosError(error) && error.response?.status === 403) {
      errorGeneralFormulario.value = 'No tienes permisos para eliminar esta cita.'
    } else {
      errorGeneralFormulario.value = 'No se pudo eliminar la cita.'
    }
  } finally {
    guardando.value = false
  }
}

async function seleccionarAgenda(peluqueroId: string) {
  peluqueroSeleccionadoId.value = peluqueroId
  mostrandoAgendas.value = false
  resetFormularioCita()
  await cargarCitasDia()
}

onMounted(async () => {
  cargando.value = true
  try {
    await cargarDatosBase()
    await cargarCitasDia()
  } catch {
    errorCarga.value = 'No se pudo preparar la agenda.'
    cargando.value = false
  }
})
</script>

<template>
  <div class="flex flex-col gap-5 min-h-full" role="main" aria-label="Agenda diaria">
    <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
      <div class="flex items-center gap-2 min-w-0">
        <button
          class="min-h-11 min-w-11 rounded-xl hover:bg-surface-container-low transition-colors inline-flex items-center justify-center"
          aria-label="Día anterior"
          @click="irDia(subDays(diaActual, 1))"
        >
          <ChevronLeft class="w-5 h-5 text-on-surface-variant" />
        </button>
        <h1 class="text-lg sm:text-2xl font-extrabold text-primary truncate">{{ textoFecha }}</h1>
        <button
          class="min-h-11 min-w-11 rounded-xl hover:bg-surface-container-low transition-colors inline-flex items-center justify-center"
          aria-label="Día siguiente"
          @click="irDia(addDays(diaActual, 1))"
        >
          <ChevronRight class="w-5 h-5 text-on-surface-variant" />
        </button>
        <button
          v-if="!isToday(diaActual)"
          class="min-h-11 px-4 rounded-full bg-surface-container-low text-sm font-semibold text-on-surface-variant hover:bg-surface-container transition-colors"
          @click="irDia(new Date())"
        >
          Hoy
        </button>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <div class="px-4 py-2 rounded-full bg-surface-container-low text-sm font-semibold text-primary-container">
          {{ peluqueroActual?.nombre ?? 'Sin agenda' }}
        </div>
        <button
          class="btn-secondary"
          :disabled="peluqueros.length <= 1"
          @click="mostrandoAgendas = true"
        >
          <Users class="w-4 h-4" aria-hidden="true" />
          Ver agendas
        </button>
        <div class="text-sm font-semibold text-on-surface-variant">
          {{ citasDia.length }} cita{{ citasDia.length !== 1 ? 's' : '' }}
        </div>
      </div>
    </div>

    <div
      v-if="viendoAgendaAjena && miPeluqueroId"
      class="rounded-2xl border border-primary/10 bg-primary/5 px-4 py-3 text-sm text-primary-container flex items-center justify-between gap-3"
    >
      <span>Estás viendo la agenda de {{ peluqueroActual?.nombre }}.</span>
      <button class="btn-ghost" @click="seleccionarAgenda(miPeluqueroId)">
        Volver a mi agenda
      </button>
    </div>

    <div class="grid grid-cols-7 gap-2">
      <button
        v-for="dia in diasSemana"
        :key="dia.toISOString()"
        class="min-h-16 rounded-2xl text-center transition-colors text-xs font-medium"
        :class="[
          isSameDay(dia, diaActual)
            ? 'bg-primary-container text-white font-bold'
            : isToday(dia)
              ? 'bg-primary/5 text-primary-container hover:bg-primary/10'
              : 'text-on-surface-variant hover:bg-surface-container-low'
        ]"
        @click="irDia(dia)"
      >
        <div class="text-[10px] uppercase">{{ format(dia, 'EEE', { locale: es }) }}</div>
        <div class="text-lg font-black">{{ format(dia, 'd') }}</div>
      </button>
    </div>

    <div v-if="cargando" class="card min-h-[420px] flex items-center justify-center gap-3">
      <Loader2 class="w-6 h-6 animate-spin text-primary" aria-hidden="true" />
      <span class="text-sm text-on-surface-variant">Cargando agenda...</span>
    </div>

    <div v-else-if="errorCarga" class="card min-h-[420px] flex flex-col items-center justify-center gap-4 text-center px-6">
      <p class="text-sm font-bold text-error">{{ errorCarga }}</p>
      <button class="btn-secondary" @click="Promise.all([cargarDatosBase(), cargarCitasDia()])">
        Reintentar
      </button>
    </div>

    <div v-else>
      <section class="card overflow-hidden">
        <div class="sticky top-0 z-10 grid grid-cols-[76px_minmax(0,1fr)] border-b border-surface-container bg-surface-container-low/95 backdrop-blur">
          <div class="px-3 py-4 flex items-center justify-center border-r border-surface-container text-on-surface-variant">
            <Clock class="w-4 h-4" aria-hidden="true" />
          </div>
          <div class="px-4 py-4 text-sm font-bold text-primary-container">
            {{ peluqueroActual?.nombre ?? 'Agenda diaria' }}
          </div>
        </div>

        <div class="overflow-auto">
          <div
            v-for="hora in slots"
            :key="hora"
            class="grid grid-cols-[76px_minmax(0,1fr)] border-b border-surface-container/40"
          >
            <div class="px-3 py-3 text-xs font-semibold text-on-surface-variant/70 text-right border-r border-surface-container">
              {{ hora.endsWith(':00') ? hora : '' }}
            </div>

            <div
              class="relative min-h-16"
              :class="puedeEmpezarCita(hora)
                ? 'cursor-pointer hover:bg-primary/[0.03] transition-colors'
                : 'bg-surface-container-low/30'"
              role="button"
              :tabindex="puedeEmpezarCita(hora) ? 0 : -1"
              :aria-label="`Seleccionar ${hora} para nueva cita`"
              @click="seleccionarHora(hora)"
              @keydown.enter.prevent="seleccionarHora(hora)"
            >
              <div
                v-if="esInicioCita(hora)"
                class="absolute inset-x-2 top-2 rounded-xl border-l-4 px-4 py-3 z-[1] overflow-hidden cursor-pointer"
                :class="esInicioCita(hora)!.estado === 'COMPLETADA'
                  ? 'bg-green-50 border-green-500 text-green-900'
                  : esInicioCita(hora)!.estado === 'CANCELADO'
                    ? 'bg-red-50 border-red-400 text-red-900'
                  : 'bg-primary/5 border-primary-container text-primary-container'"
                :style="{ height: `${slotsCita(esInicioCita(hora)!) * 64 - 8}px` }"
                role="button"
                tabindex="0"
                :aria-label="`Abrir cita de ${esInicioCita(hora)!.clienteNombre} a las ${esInicioCita(hora)!.horaInicio}`"
                @click.stop="abrirEditarCita(esInicioCita(hora)!)"
                @keydown.enter.stop.prevent="abrirEditarCita(esInicioCita(hora)!)"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="min-w-0 text-sm font-bold truncate">
                    <span class="font-black">{{ esInicioCita(hora)!.horaInicio }}</span>
                    <span class="mx-2 opacity-40">·</span>
                    {{ esInicioCita(hora)!.clienteNombre }} {{ esInicioCita(hora)!.clienteApellidos }}
                    <span class="mx-2 opacity-40">·</span>
                    <span class="font-semibold opacity-80">{{ esInicioCita(hora)!.servicioNombre }}</span>
                  </p>
                  <span
                    v-if="esInicioCita(hora)!.clienteEsVip"
                    class="px-2 py-1 rounded-full bg-amber-100 text-amber-800 text-[10px] font-bold shrink-0"
                  >
                    VIP
                  </span>
                </div>
              </div>

              <div
                v-else-if="puedeEmpezarCita(hora)"
                class="absolute inset-0 px-4 flex items-center text-xs text-on-surface-variant/0 hover:text-on-surface-variant transition-colors"
              >
                <Plus class="w-4 h-4 mr-2" aria-hidden="true" />
                Añadir cita
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>

    <Teleport to="body">
      <Transition name="modal-overlay">
        <div
          v-if="modalCitaAbierto"
          class="fixed inset-0 z-50 bg-black/35 backdrop-blur-sm flex items-center justify-center px-4 py-6"
          @click.self="cerrarModalCita"
        >
          <section class="w-full max-w-2xl max-h-[calc(100dvh-3rem)] overflow-y-auto rounded-2xl bg-white shadow-2xl border border-outline-variant/20">
            <div class="sticky top-0 z-10 bg-white/95 backdrop-blur border-b border-outline-variant/20 px-5 py-4 flex items-center justify-between gap-4">
              <div class="min-w-0">
                <h2 class="text-lg font-black text-primary truncate">
                  {{ modalEnEdicion ? 'Editar cita' : 'Nueva cita' }}
                </h2>
                <p class="text-xs text-on-surface-variant mt-0.5">
                  {{ horaSeleccionada ? `${horaSeleccionada} · ${peluqueroActual?.nombre}` : 'Selecciona una hora en la agenda' }}
                </p>
              </div>
              <button class="btn-ghost shrink-0" aria-label="Cerrar cita" @click="cerrarModalCita">
                <X class="w-4 h-4" aria-hidden="true" />
              </button>
            </div>

            <div class="p-5 space-y-4">
              <div
                v-if="errorGeneralFormulario"
                class="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
              >
                {{ errorGeneralFormulario }}
              </div>

              <div
                v-if="viendoAgendaAjena && !authStore.isAdmin"
                class="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
              >
                Puedes consultar la agenda de tus compañeros, pero las citas nuevas solo se crean en tu agenda.
              </div>

              <div class="grid gap-4 sm:grid-cols-[1fr_180px]">
                <div>
                  <label for="agenda-cliente-nombre" class="label">Cliente</label>
                  <div class="relative">
                    <div class="absolute left-3 inset-y-0 flex items-center pointer-events-none">
                      <Search class="w-4 h-4 text-on-surface-variant" aria-hidden="true" />
                    </div>
                    <input
                      id="agenda-cliente-nombre"
                      v-model="clienteQuery"
                      type="text"
                      class="input pl-10"
                      placeholder="Escribe nombre y apellidos"
                      autocomplete="off"
                      :disabled="!puedeEditarAgendaActual"
                      @keydown.enter.prevent="resolverClienteDesdeBusqueda"
                    />
                    <div
                      v-if="coincidenciasClientes.length"
                      class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-20 rounded-xl border border-outline-variant/20 bg-white shadow-card overflow-hidden"
                    >
                      <button
                        v-for="cliente in coincidenciasClientes"
                        :key="cliente.id"
                        class="w-full px-4 py-3 text-left hover:bg-surface-container-low transition-colors border-b border-outline-variant/10 last:border-b-0"
                        @click="seleccionarCliente(cliente)"
                      >
                        <p class="text-sm font-semibold text-primary">{{ nombreCompleto(cliente) }}</p>
                        <p class="text-xs text-on-surface-variant">{{ cliente.telefono || 'Sin teléfono' }}</p>
                      </button>
                    </div>
                  </div>
                  <p v-if="erroresFormulario.cliente" class="mt-2 text-xs font-semibold text-red-600">
                    {{ erroresFormulario.cliente }}
                  </p>
                  <p v-if="clienteSeleccionado" class="mt-2 text-xs font-semibold text-green-700">
                    Cliente encontrado. Datos listos para usar.
                  </p>
                  <p v-else-if="clienteEsNuevo" class="mt-2 text-xs font-semibold text-primary-container">
                    Cliente nuevo. Completa el contacto y lo guardamos al crear la cita.
                  </p>
                </div>

                <div>
                  <label for="agenda-hora-modal" class="label">Hora</label>
                  <select
                    id="agenda-hora-modal"
                    v-model="horaSeleccionada"
                    class="select-field"
                    :disabled="!puedeEditarAgendaActual"
                  >
                    <option v-for="hora in slots" :key="hora" :value="hora">{{ hora }}</option>
                  </select>
                </div>
              </div>

              <div
                v-if="clienteSeleccionado"
                class="rounded-xl border border-green-200 bg-green-50/70 px-4 py-3 flex items-start justify-between gap-3"
              >
                <div class="min-w-0">
                  <p class="text-sm font-bold text-green-800 truncate">
                    {{ nombreCompleto(clienteSeleccionado) }}
                  </p>
                  <p class="text-xs text-green-700/80">
                    {{ telefonoCliente || 'Sin teléfono' }} · {{ emailCliente || 'Sin email' }}
                  </p>
                </div>
                <button class="btn-ghost shrink-0" @click="limpiarClienteSeleccionado">
                  Cambiar
                </button>
              </div>

              <div class="grid gap-4 sm:grid-cols-2">
                <div>
                  <label for="agenda-cliente-telefono" class="label">Teléfono</label>
                  <input
                    v-if="clienteSeleccionado"
                    id="agenda-cliente-telefono"
                    type="text"
                    class="input"
                    :value="telefonoCliente"
                    readonly
                    placeholder="Sin teléfono"
                  />
                  <input
                    v-else
                    id="agenda-cliente-telefono"
                    v-model="formNuevoCliente.telefono"
                    type="text"
                    class="input"
                    placeholder="Ej. 612345678"
                    :disabled="!puedeEditarAgendaActual"
                  />
                  <p v-if="erroresFormulario.telefono" class="mt-2 text-xs font-semibold text-red-600">
                    {{ erroresFormulario.telefono }}
                  </p>
                </div>

                <div>
                  <label for="agenda-cliente-email" class="label">Email</label>
                  <input
                    v-if="clienteSeleccionado"
                    id="agenda-cliente-email"
                    type="email"
                    class="input"
                    :value="emailCliente"
                    readonly
                    placeholder="Sin email"
                  />
                  <input
                    v-else
                    id="agenda-cliente-email"
                    v-model="formNuevoCliente.email"
                    type="email"
                    class="input"
                    placeholder="Opcional"
                    :disabled="!puedeEditarAgendaActual"
                  />
                </div>
              </div>

              <div class="grid gap-4 sm:grid-cols-2">
                <div>
                  <label for="agenda-cliente-genero" class="label">Género</label>
                  <select
                    id="agenda-cliente-genero"
                    v-model="formNuevoCliente.genero"
                    class="select-field"
                    :disabled="Boolean(clienteSeleccionado) || !puedeEditarAgendaActual"
                  >
                    <option value="FEMENINO">Femenino</option>
                    <option value="MASCULINO">Masculino</option>
                  </select>
                </div>

                <div v-if="modalEnEdicion">
                  <label for="agenda-estado" class="label">Estado</label>
                  <select
                    id="agenda-estado"
                    v-model="formEdicion.estado"
                    class="select-field"
                    :disabled="!puedeEditarAgendaActual"
                  >
                    <option value="PENDIENTE">Pendiente</option>
                    <option value="EN_CURSO">En curso</option>
                    <option value="COMPLETADA">Completada</option>
                    <option value="CANCELADO">Cancelada</option>
                  </select>
                </div>
              </div>

              <div>
                <label for="agenda-servicio" class="label">Servicio</label>
                <select
                  id="agenda-servicio"
                  v-model="formCita.servicioId"
                  class="select-field"
                  :disabled="!puedeEditarAgendaActual"
                >
                  <option value="" disabled>Selecciona un servicio</option>
                  <option v-for="servicio in servicios" :key="servicio.id" :value="servicio.id">
                    {{ servicio.nombre }}
                  </option>
                </select>
                <p v-if="erroresFormulario.servicio" class="mt-2 text-xs font-semibold text-red-600">
                  {{ erroresFormulario.servicio }}
                </p>
              </div>

              <div v-if="formEdicion.estado === 'CANCELADO'">
                <label for="agenda-motivo-cancelacion" class="label">Motivo de cancelación</label>
                <textarea
                  id="agenda-motivo-cancelacion"
                  v-model="formEdicion.motivoCancelacion"
                  rows="2"
                  class="input resize-none"
                  placeholder="Opcional"
                  :disabled="!puedeEditarAgendaActual"
                />
              </div>

              <div>
                <label for="agenda-notas" class="label">Notas de la cita</label>
                <textarea
                  id="agenda-notas"
                  v-model="notasCita"
                  rows="3"
                  class="input resize-none"
                  placeholder="Opcional"
                  :disabled="!puedeEditarAgendaActual"
                />
              </div>

              <div class="flex flex-col-reverse gap-3 sm:flex-row sm:items-center sm:justify-between pt-2">
                <button
                  v-if="modalEnEdicion && authStore.isAdmin"
                  class="btn-danger"
                  :disabled="guardando"
                  @click="eliminarCita"
                >
                  <Trash2 class="w-4 h-4" aria-hidden="true" />
                  Eliminar
                </button>
                <span v-else />

                <div class="flex gap-3 sm:justify-end">
                  <button class="btn-secondary" :disabled="guardando" @click="cerrarModalCita">
                    Cancelar
                  </button>
                  <button
                    class="btn-primary"
                    :disabled="guardando || !puedeEditarAgendaActual || !horaSeleccionada || !clienteQuery.trim() || !formCita.servicioId"
                    @click="guardarCita"
                  >
                    <Loader2 v-if="guardando" class="w-4 h-4 animate-spin" aria-hidden="true" />
                    <span>{{ guardando ? 'Guardando...' : modalEnEdicion ? 'Guardar cambios' : 'Guardar cita' }}</span>
                  </button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="modal-overlay">
        <div
          v-if="mostrandoAgendas"
          class="fixed inset-0 z-50 bg-black/30 backdrop-blur-sm flex justify-end"
          @click.self="mostrandoAgendas = false"
        >
          <aside class="h-full w-full max-w-sm bg-white shadow-2xl p-5 flex flex-col">
            <div class="flex items-center justify-between mb-4">
              <div>
                <h3 class="text-lg font-bold text-primary">Ver agendas</h3>
                <p class="text-sm text-on-surface-variant">Cambia rápidamente entre compañeros</p>
              </div>
              <button class="btn-ghost" aria-label="Cerrar selector de agendas" @click="mostrandoAgendas = false">
                <X class="w-4 h-4" aria-hidden="true" />
              </button>
            </div>

            <div class="space-y-2 overflow-y-auto">
              <button
                v-for="peluquero in peluqueros"
                :key="peluquero.id"
                class="w-full text-left rounded-2xl border px-4 py-3 transition-colors"
                :class="peluquero.id === peluqueroSeleccionadoId
                  ? 'border-primary-container bg-primary/5 text-primary-container'
                  : 'border-outline-variant/20 hover:bg-surface-container-low text-on-surface'"
                @click="seleccionarAgenda(peluquero.id)"
              >
                <p class="font-semibold">{{ peluquero.nombre }}</p>
                <p class="text-xs text-on-surface-variant mt-1">
                  {{ peluquero.id === miPeluqueroId ? 'Mi agenda' : 'Compañero/a' }}
                </p>
              </button>
            </div>
          </aside>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>
