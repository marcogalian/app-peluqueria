<script setup lang="ts">
import { ArrowLeft, ArrowRight, Eye, EyeOff, Loader2, Lock, ShieldCheck } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'
import { authService } from '~/modules/auth/services/authService'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const token = computed(() => String(route.query.token ?? ''))
const nuevaPassword = ref('')
const repetirPassword = ref('')
const mostrarPassword = ref(false)
const mostrarRepetir = ref(false)
const guardando = ref(false)
const completado = ref(false)
const error = ref('')

// --- Validación compartida con el backend ---
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

async function guardarPassword() {
  error.value = ''
  if (!token.value) {
    error.value = 'El enlace de recuperación no es válido.'
    return
  }
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
    await authService.resetPassword(token.value, nuevaPassword.value)
    completado.value = true
    toast.success('Contraseña actualizada')
  } catch {
    error.value = 'El enlace ha caducado o ya fue utilizado.'
  } finally {
    guardando.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center bg-surface-container-low px-4 py-8">
    <section class="w-full max-w-md rounded-2xl border border-outline-variant/20 bg-white p-6 shadow-card-lg sm:p-8">
      <!-- Cabecera -->
      <div class="mb-7 flex items-start gap-4">
        <div class="flex h-12 w-12 items-center justify-center rounded-full bg-primary-container/10 text-primary">
          <ShieldCheck class="h-6 w-6" aria-hidden="true" />
        </div>
        <div>
          <h1 class="text-2xl font-extrabold tracking-tight text-primary">Nueva contraseña</h1>
          <p class="mt-1 text-sm text-on-surface-variant">Cuenta de administrador</p>
        </div>
      </div>

      <!-- Confirmación final -->
      <div v-if="completado" class="space-y-5">
        <div class="rounded-xl border border-green-200 bg-green-50 px-4 py-3 text-sm font-semibold text-green-700">
          La contraseña del administrador se ha cambiado correctamente.
        </div>
        <button class="btn-primary w-full" @click="router.push('/login')">
          Ir al login
          <ArrowRight class="h-4 w-4" aria-hidden="true" />
        </button>
      </div>

      <!-- Formulario de nueva contraseña -->
      <form v-else class="space-y-5" @submit.prevent="guardarPassword">
        <div>
          <label class="label" for="reset-password">Nueva contraseña</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" aria-hidden="true" />
            <input
              id="reset-password"
              v-model="nuevaPassword"
              :type="mostrarPassword ? 'text' : 'password'"
              autocomplete="new-password"
              class="input pl-10 pr-10"
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
          <label class="label" for="reset-password-repeat">Repetir contraseña</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-on-surface-variant" aria-hidden="true" />
            <input
              id="reset-password-repeat"
              v-model="repetirPassword"
              :type="mostrarRepetir ? 'text' : 'password'"
              autocomplete="new-password"
              class="input pl-10 pr-10"
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

        <div class="flex flex-col-reverse gap-3 sm:flex-row">
          <button type="button" class="btn-secondary flex-1" @click="router.push('/login')">
            <ArrowLeft class="h-4 w-4" aria-hidden="true" />
            Volver
          </button>
          <button type="submit" class="btn-primary flex-1" :disabled="guardando || !passwordValida || !coincide">
            <Loader2 v-if="guardando" class="h-4 w-4 animate-spin" aria-hidden="true" />
            <span>{{ guardando ? 'Guardando...' : 'Guardar' }}</span>
          </button>
        </div>
      </form>
    </section>
  </div>
</template>
