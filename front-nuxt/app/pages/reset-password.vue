<script setup lang="ts">
import { Lock, Eye, EyeOff, Loader2, CheckCircle, AlertCircle } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

definePageMeta({
  layout: 'auth',
  auth: false,
})

const route  = useRoute()
const router = useRouter()
const toast  = useToast()

const token = computed(() => route.query.token as string | undefined)

const form = reactive({
  password: '',
  confirm:  '',
})
const mostrarPassword  = ref(false)
const mostrarConfirm   = ref(false)
const cargando         = ref(false)
const error            = ref('')
const exito            = ref(false)

async function handleReset() {
  error.value = ''

  if (!token.value) {
    error.value = 'El enlace no contiene un token válido.'
    return
  }
  if (form.password.length < 6) {
    error.value = 'La contraseña debe tener al menos 6 caracteres.'
    return
  }
  if (form.password !== form.confirm) {
    error.value = 'Las contraseñas no coinciden.'
    return
  }

  cargando.value = true
  try {
    const { authService } = await import('~/modules/auth/services/authService')
    await authService.resetPassword(token.value, form.password)
    exito.value = true
    toast.success('Contraseña actualizada correctamente')
    setTimeout(() => router.push('/login'), 2000)
  } catch {
    error.value = 'El enlace no es válido o ha expirado. Solicita uno nuevo.'
  } finally {
    cargando.value = false
  }
}
</script>

<template>
  <div class="flex h-screen items-center justify-center bg-white px-4">
    <div class="w-full max-w-sm">

      <div class="mb-8 text-center">
        <h1 class="text-2xl font-bold text-text-primary">Nueva contraseña</h1>
        <p class="text-text-secondary text-sm mt-1">
          Introduce la nueva contraseña para tu cuenta de administrador.
        </p>
      </div>

      <div v-if="exito" class="flex flex-col items-center gap-3 py-8">
        <CheckCircle class="w-12 h-12 text-green-500" />
        <p class="text-text-primary font-medium">Contraseña actualizada</p>
        <p class="text-sm text-text-secondary">Redirigiendo al login…</p>
      </div>

      <form v-else class="space-y-5" @submit.prevent="handleReset">

        <div>
          <label class="label" for="reset-password">Nueva contraseña</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
            <input
              id="reset-password"
              v-model="form.password"
              :type="mostrarPassword ? 'text' : 'password'"
              placeholder="Mínimo 6 caracteres"
              class="input pl-10 pr-10"
              :disabled="cargando"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
              @click="mostrarPassword = !mostrarPassword"
            >
              <Eye v-if="!mostrarPassword" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
        </div>

        <div>
          <label class="label" for="reset-confirm">Confirmar contraseña</label>
          <div class="relative">
            <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" />
            <input
              id="reset-confirm"
              v-model="form.confirm"
              :type="mostrarConfirm ? 'text' : 'password'"
              placeholder="Repite la contraseña"
              class="input pl-10 pr-10"
              :disabled="cargando"
            />
            <button
              type="button"
              class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
              @click="mostrarConfirm = !mostrarConfirm"
            >
              <Eye v-if="!mostrarConfirm" class="w-4 h-4" />
              <EyeOff v-else class="w-4 h-4" />
            </button>
          </div>
        </div>

        <Transition name="modal-overlay">
          <div v-if="error" role="alert" class="flex items-start gap-2 text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
            <AlertCircle class="w-4 h-4 mt-0.5 shrink-0" />
            <span>{{ error }}</span>
          </div>
        </Transition>

        <button
          type="submit"
          class="btn-primary w-full py-3 text-base"
          :disabled="cargando"
        >
          <Loader2 v-if="cargando" class="w-4 h-4 animate-spin" />
          <span>{{ cargando ? 'Guardando…' : 'Establecer contraseña' }}</span>
        </button>

        <p class="text-center text-sm text-text-muted">
          <NuxtLink to="/login" class="text-primary hover:text-primary-light font-medium transition-colors">
            Volver al login
          </NuxtLink>
        </p>

      </form>

    </div>
  </div>
</template>
