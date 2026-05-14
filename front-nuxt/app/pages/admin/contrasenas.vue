<script setup lang="ts">
import { KeyRound, Loader2, Eye, EyeOff, X } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'
import { api } from '~/infrastructure/http/api'

definePageMeta({ middleware: ['auth', 'admin'] })

const toast = useToast()

interface Empleado {
  id: string
  nombre: string
  especialidad: string
}

const empleados  = ref<Empleado[]>([])
const cargandoLista = ref(true)

onMounted(async () => {
  try {
    const { data } = await api.get<Empleado[]>('/peluqueros')
    empleados.value = data
  } finally {
    cargandoLista.value = false
  }
})

// ── Modal ────────────────────────────────────────────────────
const modalAbierto   = ref(false)
const empleadoActual = ref<Empleado | null>(null)
const form           = reactive({ password: '', confirm: '' })
const mostrarPass    = ref(false)
const mostrarConf    = ref(false)
const cargandoSave   = ref(false)
const errorModal     = ref('')

function abrirModal(emp: Empleado) {
  empleadoActual.value = emp
  form.password = ''
  form.confirm  = ''
  mostrarPass.value = false
  mostrarConf.value = false
  errorModal.value  = ''
  modalAbierto.value = true
}

function cerrarModal() {
  modalAbierto.value = false
  empleadoActual.value = null
}

async function guardar() {
  errorModal.value = ''

  if (form.password.length < 6) {
    errorModal.value = 'Mínimo 6 caracteres.'
    return
  }
  if (form.password !== form.confirm) {
    errorModal.value = 'Las contraseñas no coinciden.'
    return
  }

  cargandoSave.value = true
  try {
    const { authService } = await import('~/modules/auth/services/authService')
    await authService.cambiarPasswordEmpleado(empleadoActual.value!.id, form.password)
    toast.success(`Contraseña de ${empleadoActual.value!.nombre} actualizada`)
    cerrarModal()
  } catch {
    errorModal.value = 'No se pudo actualizar la contraseña. Inténtalo de nuevo.'
  } finally {
    cargandoSave.value = false
  }
}
</script>

<template>
  <div class="p-6 max-w-2xl">

    <div class="mb-6">
      <h1 class="text-xl font-bold text-text-primary flex items-center gap-2">
        <KeyRound class="w-5 h-5" />
        Contraseñas de empleados
      </h1>
      <p class="text-sm text-text-secondary mt-1">
        Cambia la contraseña de acceso de cualquier empleado.
      </p>
    </div>

    <div v-if="cargandoLista" class="flex justify-center py-12">
      <Loader2 class="w-6 h-6 animate-spin text-text-muted" />
    </div>

    <div v-else class="card divide-y divide-surface-border">
      <div
        v-for="emp in empleados"
        :key="emp.id"
        class="flex items-center justify-between px-4 py-3"
      >
        <div>
          <p class="font-medium text-text-primary text-sm">{{ emp.nombre }}</p>
          <p class="text-xs text-text-muted">{{ emp.especialidad }}</p>
        </div>
        <button
          class="btn-secondary text-sm py-1.5 px-3 flex items-center gap-1.5"
          @click="abrirModal(emp)"
        >
          <KeyRound class="w-3.5 h-3.5" />
          Cambiar contraseña
        </button>
      </div>

      <div v-if="empleados.length === 0" class="px-4 py-8 text-center text-text-muted text-sm">
        No hay empleados registrados.
      </div>
    </div>

  </div>

  <!-- ── Modal ──────────────────────────────────────────────── -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalAbierto"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="cerrarModal"
      >
        <div
          role="dialog"
          aria-modal="true"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >
          <div class="flex items-start justify-between mb-4">
            <div>
              <h3 class="text-base font-semibold text-text-primary">Cambiar contraseña</h3>
              <p class="text-sm text-text-secondary mt-0.5">{{ empleadoActual?.nombre }}</p>
            </div>
            <button class="text-text-muted hover:text-text-secondary transition-colors" @click="cerrarModal">
              <X class="w-4 h-4" />
            </button>
          </div>

          <div class="space-y-4">

            <div>
              <label class="label" for="modal-pass">Nueva contraseña</label>
              <div class="relative">
                <input
                  id="modal-pass"
                  v-model="form.password"
                  :type="mostrarPass ? 'text' : 'password'"
                  placeholder="Mínimo 6 caracteres"
                  class="input pr-10"
                  :disabled="cargandoSave"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
                  @click="mostrarPass = !mostrarPass"
                >
                  <Eye v-if="!mostrarPass" class="w-4 h-4" />
                  <EyeOff v-else class="w-4 h-4" />
                </button>
              </div>
            </div>

            <div>
              <label class="label" for="modal-conf">Confirmar contraseña</label>
              <div class="relative">
                <input
                  id="modal-conf"
                  v-model="form.confirm"
                  :type="mostrarConf ? 'text' : 'password'"
                  placeholder="Repite la contraseña"
                  class="input pr-10"
                  :disabled="cargandoSave"
                />
                <button
                  type="button"
                  class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
                  @click="mostrarConf = !mostrarConf"
                >
                  <Eye v-if="!mostrarConf" class="w-4 h-4" />
                  <EyeOff v-else class="w-4 h-4" />
                </button>
              </div>
            </div>

            <Transition name="modal-overlay">
              <p v-if="errorModal" role="alert" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
                {{ errorModal }}
              </p>
            </Transition>

            <div class="flex gap-3 pt-1">
              <button class="btn-secondary flex-1" :disabled="cargandoSave" @click="cerrarModal">
                Cancelar
              </button>
              <button class="btn-primary flex-1" :disabled="cargandoSave" @click="guardar">
                <Loader2 v-if="cargandoSave" class="w-4 h-4 animate-spin" />
                <span>{{ cargandoSave ? 'Guardando…' : 'Guardar' }}</span>
              </button>
            </div>

          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
