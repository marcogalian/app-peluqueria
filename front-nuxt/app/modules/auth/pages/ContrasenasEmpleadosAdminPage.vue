<script setup lang="ts">
import { Eye, EyeOff, KeyRound, Loader2, Search, ShieldCheck, UserRound } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'
import { authService } from '~/modules/auth/services/authService'

interface EmpleadoPassword {
  id: string
  nombre: string
  email: string
  iniciales: string
  enBaja: boolean
}

const toast = useToast()
const empleados = ref<EmpleadoPassword[]>([])
const seleccionado = ref<EmpleadoPassword | null>(null)
const busqueda = ref('')
const nuevaPassword = ref('')
const repetirPassword = ref('')
const mostrarPassword = ref(false)
const mostrarRepetir = ref(false)
const cargando = ref(true)
const guardando = ref(false)
const error = ref('')

// --- Búsqueda y selección ---
const empleadosFiltrados = computed(() => {
  const q = busqueda.value.trim().toLowerCase()
  if (!q) return empleados.value
  return empleados.value.filter((empleado) =>
    empleado.nombre.toLowerCase().includes(q) || empleado.email.toLowerCase().includes(q),
  )
})

// --- Validación alineada con GestionarCredenciales ---
const passwordValida = computed(() => {
  const p = nuevaPassword.value
  return p.length >= 8 && /[A-Z]/.test(p) && /[a-z]/.test(p) && /[0-9]/.test(p)
})
const coincide = computed(() => nuevaPassword.value === repetirPassword.value)

const requisitos = computed(() => {
  const p = nuevaPassword.value
  return [
    { ok: p.length >= 8,    texto: 'Mínimo 8 caracteres' },
    { ok: /[A-Z]/.test(p),  texto: 'Una mayúscula' },
    { ok: /[a-z]/.test(p),  texto: 'Una minúscula' },
    { ok: /[0-9]/.test(p),  texto: 'Un número' },
  ]
})

const formularioListo = computed(() =>
  Boolean(seleccionado.value) && passwordValida.value && coincide.value,
)

onMounted(cargarEmpleados)

// --- Carga de datos ---
async function cargarEmpleados() {
  cargando.value = true
  try {
    const { api } = await import('~/infrastructure/http/api')
    const { data } = await api.get('/peluqueros')
    empleados.value = data.map((p: any) => {
      const nombre = String(p.nombre ?? '').trim()
      const iniciales = nombre.split(' ').map((parte) => parte[0]).slice(0, 2).join('').toUpperCase()
      return {
        id: p.id,
        nombre,
        email: p.user?.email ?? '',
        iniciales: iniciales || 'E',
        enBaja: Boolean(p.enBaja),
      }
    })
    seleccionado.value = empleados.value[0] ?? null
  } catch {
    toast.error('No se pudieron cargar los empleados')
  } finally {
    cargando.value = false
  }
}

// --- Acciones de pantalla ---
function seleccionar(empleado: EmpleadoPassword) {
  seleccionado.value = empleado
  nuevaPassword.value = ''
  repetirPassword.value = ''
  error.value = ''
}

async function guardarPassword() {
  error.value = ''
  if (!seleccionado.value) return
  if (!passwordValida.value) {
    error.value = 'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número.'
    return
  }
  if (!coincide.value) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }

  guardando.value = true
  try {
    await authService.cambiarPasswordEmpleado(seleccionado.value.id, {
      nuevaPassword: nuevaPassword.value,
      repetirPassword: repetirPassword.value,
    })
    nuevaPassword.value = ''
    repetirPassword.value = ''
    toast.success('Contraseña actualizada')
  } catch {
    error.value = 'No se pudo cambiar la contraseña.'
  } finally {
    guardando.value = false
  }
}
</script>

<template>
  <div class="mx-auto flex w-full max-w-6xl flex-col gap-5">
    <!-- Cabecera -->
    <header class="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <div class="mb-2 inline-flex items-center gap-2 rounded-full bg-primary-container/10 px-3 py-1 text-xs font-bold text-primary">
          <ShieldCheck class="h-4 w-4" aria-hidden="true" />
          Administración
        </div>
        <h1 class="text-2xl font-extrabold tracking-tight text-primary sm:text-3xl">Contraseñas de empleados</h1>
        <p class="mt-1 text-sm text-on-surface-variant">Gestión de acceso del personal</p>
      </div>
      <div class="rounded-full bg-surface-container-high px-4 py-2 text-sm font-bold text-primary">
        {{ empleados.length }} empleados
      </div>
    </header>

    <section class="grid gap-5 lg:grid-cols-[minmax(0,0.95fr)_minmax(360px,1.05fr)]">
      <!-- Listado y búsqueda de empleados -->
      <div class="card overflow-hidden">
        <div class="border-b border-outline-variant/20 p-4">
          <label class="sr-only" for="buscar-empleado-password">Buscar empleado</label>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" aria-hidden="true" />
            <input
              id="buscar-empleado-password"
              v-model="busqueda"
              type="search"
              class="input pl-10"
              placeholder="Buscar por nombre o email"
            />
          </div>
        </div>

        <div v-if="cargando" class="flex items-center justify-center gap-2 px-4 py-12 text-sm font-semibold text-on-surface-variant">
          <Loader2 class="h-4 w-4 animate-spin" aria-hidden="true" />
          Cargando empleados
        </div>

        <div v-else class="divide-y divide-outline-variant/15">
          <button
            v-for="empleado in empleadosFiltrados"
            :key="empleado.id"
            type="button"
            class="flex w-full items-center gap-3 px-4 py-3 text-left transition-colors hover:bg-surface-container-low"
            :class="seleccionado?.id === empleado.id ? 'bg-primary-container/5' : ''"
            @click="seleccionar(empleado)"
          >
            <span class="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-primary-container/10 text-sm font-extrabold text-primary">
              {{ empleado.iniciales }}
            </span>
            <span class="min-w-0 flex-1">
              <span class="block truncate text-sm font-extrabold text-on-surface">{{ empleado.nombre }}</span>
              <span class="block truncate text-xs text-on-surface-variant">{{ empleado.email }}</span>
            </span>
            <span
              v-if="empleado.enBaja"
              class="rounded-full bg-red-50 px-2 py-1 text-[10px] font-bold text-red-700"
            >
              Baja
            </span>
          </button>

          <p v-if="empleadosFiltrados.length === 0" class="px-4 py-10 text-center text-sm text-on-surface-variant">
            Sin resultados
          </p>
        </div>
      </div>

      <!-- Formulario de cambio de contraseña -->
      <form class="card p-5 sm:p-6" @submit.prevent="guardarPassword">
        <div class="mb-6 flex items-start gap-4">
          <div class="flex h-12 w-12 flex-shrink-0 items-center justify-center rounded-full bg-primary-container text-white">
            <KeyRound class="h-5 w-5" aria-hidden="true" />
          </div>
          <div class="min-w-0">
            <p class="text-xs font-bold uppercase tracking-[0.15em] text-on-surface-variant">Empleado seleccionado</p>
            <h2 class="mt-1 truncate text-xl font-extrabold text-primary">
              {{ seleccionado?.nombre || 'Selecciona un empleado' }}
            </h2>
            <p class="truncate text-sm text-on-surface-variant">{{ seleccionado?.email || 'Sin empleado activo' }}</p>
          </div>
        </div>

        <div v-if="!seleccionado" class="rounded-xl border border-dashed border-outline-variant/50 px-4 py-10 text-center">
          <UserRound class="mx-auto mb-3 h-8 w-8 text-on-surface-variant/60" aria-hidden="true" />
          <p class="text-sm font-semibold text-on-surface-variant">Elige un empleado para continuar</p>
        </div>

        <div v-else class="space-y-5">
          <div>
            <label class="label" for="empleado-password">Nueva contraseña</label>
            <div class="relative">
              <input
                id="empleado-password"
                v-model="nuevaPassword"
                :type="mostrarPassword ? 'text' : 'password'"
                class="input pr-10"
                autocomplete="new-password"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant transition-colors hover:text-on-surface"
                :aria-label="mostrarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                @click="mostrarPassword = !mostrarPassword"
              >
                <EyeOff v-if="mostrarPassword" class="h-4 w-4" aria-hidden="true" />
                <Eye v-else class="h-4 w-4" aria-hidden="true" />
              </button>
            </div>
            <ul v-if="nuevaPassword" class="mt-2 space-y-1">
              <li v-for="r in requisitos" :key="r.texto" class="flex items-center gap-1.5 text-xs"
                  :class="r.ok ? 'text-green-600' : 'text-red-500'">
                <span>{{ r.ok ? '✓' : '✗' }}</span>
                <span>{{ r.texto }}</span>
              </li>
            </ul>
          </div>

          <div>
            <label class="label" for="empleado-password-repeat">Repetir contraseña</label>
            <div class="relative">
              <input
                id="empleado-password-repeat"
                v-model="repetirPassword"
                :type="mostrarRepetir ? 'text' : 'password'"
                class="input pr-10"
                autocomplete="new-password"
              />
              <button
                type="button"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant transition-colors hover:text-on-surface"
                :aria-label="mostrarRepetir ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                @click="mostrarRepetir = !mostrarRepetir"
              >
                <EyeOff v-if="mostrarRepetir" class="h-4 w-4" aria-hidden="true" />
                <Eye v-else class="h-4 w-4" aria-hidden="true" />
              </button>
            </div>
          </div>

          <p v-if="error" role="alert" class="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {{ error }}
          </p>

          <button type="submit" class="btn-primary w-full" :disabled="guardando || !formularioListo">
            <Loader2 v-if="guardando" class="h-4 w-4 animate-spin" aria-hidden="true" />
            <span>{{ guardando ? 'Guardando...' : 'Cambiar contraseña' }}</span>
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
