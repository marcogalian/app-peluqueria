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
  Upload, X, ZoomIn, Trash2,
} from 'lucide-vue-next'
import type { Cliente, FotoCliente, Genero } from '~/modules/clientes/types/cliente.types'

definePageMeta({ middleware: ['auth', 'admin'] })

const config = useRuntimeConfig()

// ── Estado ────────────────────────────────────────────────
const clientes    = ref<Cliente[]>([])
const busqueda    = ref('')
const cargando    = ref(true)

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

// Modal de confirmación de consentimiento
const modalConsentimiento = ref(false)

// Modal crear/editar cliente
const modalClienteAbierto = ref(false)
const guardandoCliente    = ref(false)
const formCliente = ref({
  nombre:    '',
  apellidos: '',
  telefono:  '',
  email:     '',
  genero:    'OTRO' as Genero,
  notas:     '',
})

// ── Filtrado local ────────────────────────────────────────
const clientesFiltrados = computed(() => {
  const q = busqueda.value.toLowerCase()
  if (!q) return clientes.value
  return clientes.value.filter(c =>
    `${c.nombre || ''} ${c.apellidos || ''}`.toLowerCase().includes(q) ||
    (c.telefono || '').includes(q) ||
    (c.email || '').toLowerCase().includes(q),
  )
})

// ── Funciones ─────────────────────────────────────────────

onMounted(async () => {
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get<Cliente[]>('/v1/clientes')
    clientes.value = data || []
  } catch {
    clientes.value = []
  } finally {
    cargando.value = false
  }
})

async function abrirCliente(cliente: Cliente) {
  clienteSeleccionado.value = cliente
  tabActiva.value           = 'info'
  drawerAbierto.value       = true
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
  await api.patch(`/v1/clientes/${cliente.id}`, { esVip: !cliente.esVip })
  cliente.esVip = !cliente.esVip
}

function abrirModalNuevoCliente() {
  formCliente.value = { nombre: '', apellidos: '', telefono: '', email: '', genero: 'OTRO', notas: '' }
  modalClienteAbierto.value = true
}

async function guardarCliente() {
  if (!formCliente.value.nombre || !formCliente.value.telefono) return
  guardandoCliente.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.post<Cliente>('/v1/clientes', formCliente.value)
    clientes.value.unshift(data)
    modalClienteAbierto.value = false
  } finally {
    guardandoCliente.value = false
  }
}
</script>

<template>
  <div class="flex gap-6 h-full">

    <!-- ══════════════════════════════════════════════════════
         PANEL IZQUIERDO — Lista de clientes
         ════════════════════════════════════════════════════ -->
    <div class="flex flex-col flex-1 min-w-0 space-y-4">

      <!-- Barra de búsqueda + botón añadir -->
      <div class="flex gap-3">
        <div class="relative flex-1">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
          <input
            v-model="busqueda"
            type="search"
            placeholder="Buscar clientes..."
            class="input pl-10"
          />
        </div>
        <button class="btn-primary">
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
              <span v-if="cliente.esVip" class="badge-vip">VIP</span>
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
        class="w-96 flex-shrink-0 card p-0 flex flex-col overflow-hidden animate-slide-in-right"
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
        <div v-if="tabActiva === 'info'" class="flex-1 overflow-y-auto p-5 space-y-4">

          <!-- Badges VIP y descuento -->
          <div class="flex items-center gap-2 flex-wrap">
            <button
              class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all"
              :class="clienteSeleccionado.esVip
                ? 'bg-vip-light text-vip border border-vip/30'
                : 'bg-surface-muted text-text-secondary border border-surface-border'"
              @click="toggleVip(clienteSeleccionado)"
            >
              <Star class="w-3.5 h-3.5" />
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

          <!-- Notas médicas (alergias, sensibilidades) -->
          <div v-if="clienteSeleccionado.notasMedicas">
            <label class="label">Notas médicas / Alergias</label>
            <p class="text-sm text-text-primary bg-surface-muted rounded-lg p-3">
              {{ clienteSeleccionado.notasMedicas }}
            </p>
          </div>

          <!-- Fórmula de tinte -->
          <div v-if="clienteSeleccionado.formulasTinte">
            <label class="label">Fórmula de tinte</label>
            <p class="text-sm text-text-primary bg-surface-muted rounded-lg p-3 font-mono">
              {{ clienteSeleccionado.formulasTinte }}
            </p>
          </div>

          <!-- Notas generales -->
          <div v-if="clienteSeleccionado.notas">
            <label class="label">Notas</label>
            <p class="text-sm text-text-secondary bg-surface-muted rounded-lg p-3">
              {{ clienteSeleccionado.notas }}
            </p>
          </div>

          <button class="btn-primary w-full">Guardar cambios</button>
        </div>

        <!-- ── TAB: Fotos del historial ─────────────────── -->
        <div v-if="tabActiva === 'fotos'" class="flex-1 overflow-y-auto p-5 space-y-4">

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
