<script setup lang="ts">
/**
 * Página de login — diseño split 50/50.
 * Izquierda: foto peluquería con overlay oscuro azulado y texto de marca.
 * Derecha: formulario blanco con email, contraseña, recuérdame y recuperación admin.
 *
 * No requiere autenticación previa.
 */
import { Eye, EyeOff, AtSign, Lock, Scissors, ArrowRight, Loader2 } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

// ── Estado del formulario ─────────────────────────────────
const form = reactive({
  username:   '',
  password:   '',
  rememberMe: false,
})
const mostrarPassword = ref(false)
const cargando        = ref(false)
const error           = ref('')

// ── Modal de recuperación de contraseña ──────────────────
const modalRecuperar  = ref(false)
const emailRecuperar  = ref('')
const enviandoReset   = ref(false)

const authStore = useAuthStore()
const route     = useRoute()
const router    = useRouter()

// Si ya está autenticado al entrar a /login, redirige directamente
onMounted(() => {
  authStore.restaurarSesion()
  if (authStore.isAuthenticated) {
    redirigirSegunRol()
  }
})

/** Ruta de destino tras login según el rol del usuario */
function redirigirSegunRol() {
  const destino = (route.query.redirect as string) || (authStore.isAdmin ? '/admin/dashboard' : '/agenda')
  router.push(destino)
}

/** Envía las credenciales al backend */
async function handleLogin() {
  if (!form.username || !form.password) {
    error.value = 'Por favor introduce usuario y contraseña.'
    return
  }

  cargando.value = true
  error.value    = ''

  try {
    const { authService } = await import('~/modules/auth/services/authService')
    const respuesta = await authService.login({
      username:   form.username,
      password:   form.password,
      rememberMe: form.rememberMe,
    })

    authStore.guardarSesion(respuesta.token, respuesta.refreshToken)
    redirigirSegunRol()
  } catch (err: any) {
    // El backend puede devolver 401 o 403 cuando las credenciales son incorrectas
    if (err.response?.status === 401 || err.response?.status === 403) {
      error.value = 'Usuario o contraseña incorrectos.'
    } else {
      error.value = 'No se pudo conectar con el servidor. Inténtalo de nuevo.'
    }
  } finally {
    cargando.value = false
  }
}

/** Solicita el email de recuperación al backend */
const toast = useToast()

async function handleRecuperar() {
  if (!emailRecuperar.value) return
  enviandoReset.value = true
  try {
    const { authService } = await import('~/modules/auth/services/authService')
    await authService.solicitarResetPassword(emailRecuperar.value)
  } catch {
  // Silenciar: no revelar si el email existe ni si pertenece al admin.
  } finally {
    enviandoReset.value = false
    modalRecuperar.value = false
    toast.info('Si el email corresponde al admin, recibirás un enlace en breve')
  }
}
</script>

<template>
  <!--
    Layout split: mitad izquierda = imagen + overlay, mitad derecha = formulario.
    En móvil la imagen se oculta y el formulario ocupa toda la pantalla.
  -->
  <div class="flex h-screen">

    <!-- ══════════════════════════════════════════════════════
         LADO IZQUIERDO — Imagen + overlay + texto de marca
         ════════════════════════════════════════════════════ -->
    <div class="hidden lg:flex lg:w-1/2 relative overflow-hidden">

      <!--
        Imagen de fondo — reemplaza con la foto real del salón:
        Copia la imagen en front-nuxt/public/images/salon-login.jpg
        y descomenta la etiqueta <img> de abajo.
      -->
      <img src="/images/foto-admin.jpeg" alt="Peluquería" class="absolute inset-0 w-full h-full object-cover" />

      <!-- Overlay oscuro con tono azulado — mismo efecto que el diseño de referencia -->
      <div class="absolute inset-0 bg-gradient-to-b from-[#1a2d5a]/70 via-[#1a2d5a]/50 to-[#0f1c3a]/85" />

      <!-- Contenido sobre el overlay -->
      <div class="relative z-10 flex flex-col justify-between p-10 w-full">

        <!-- Logo/Foto en la esquina superior izquierda -->
        <div class="flex items-center gap-2.5">
          <img src="/images/propietaria.jpeg" alt="Propietaria" class="w-9 h-9 rounded-full object-cover border-2 border-white/20 shadow-sm" />
          <span class="text-white font-semibold text-lg">Peluquería Isabella</span>
        </div>

        <!-- Texto principal en la parte inferior -->
        <div class="animate-slide-in-up">
          <h2 class="text-white font-bold text-4xl leading-tight mb-3">
            El arte de tu belleza,<br />en manos expertas.
          </h2>
          <p class="text-white/70 text-base leading-relaxed max-w-sm">
            Vive una experiencia sublime con servicios diseñados para alcanzar la excelencia estética.
          </p>
        </div>

      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         LADO DERECHO — Formulario de login
         ════════════════════════════════════════════════════ -->
    <div class="w-full lg:w-1/2 flex items-center justify-center bg-white px-4 sm:px-8 py-8 sm:py-12">
      <div class="w-full max-w-sm animate-fade-scale-in">

        <!-- Encabezado del formulario -->
        <div class="mb-8">
          <!-- En móvil mostramos la foto/logo aquí -->
          <div class="flex items-center gap-2 mb-6 lg:hidden">
            <img src="/images/propietaria.jpeg" alt="Propietaria" class="w-8 h-8 rounded-full object-cover border border-gray-200 shadow-sm" />
            <span class="text-text-primary font-semibold">Peluquería Isabella</span>
          </div>

          <h1 class="text-2xl font-bold text-text-primary">Bienvenido de nuevo</h1>
          <p class="text-text-secondary text-sm mt-1">
            Ingresa tus credenciales para acceder al sistema
          </p>
        </div>

        <!-- ── Formulario ──────────────────────────────────── -->
        <form class="space-y-5" aria-label="Iniciar sesión" @submit.prevent="handleLogin">

          <!-- Campo usuario -->
          <div>
            <label class="label" for="login-username">Usuario</label>
            <div class="relative">
              <AtSign class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" aria-hidden="true" />
              <input
                id="login-username"
                v-model="form.username"
                type="text"
                placeholder="nombre.usuario"
                autocomplete="username"
                class="input pl-10"
                :disabled="cargando"
              />
            </div>
          </div>

          <!-- Campo contraseña con toggle de visibilidad -->
          <div>
            <label class="label" for="login-password">Contraseña</label>
            <div class="relative">
              <Lock class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-text-muted" aria-hidden="true" />
              <input
                id="login-password"
                v-model="form.password"
                :type="mostrarPassword ? 'text' : 'password'"
                placeholder="••••••••••••"
                autocomplete="current-password"
                class="input pl-10 pr-10"
                :disabled="cargando"
                @keyup.enter="handleLogin"
              />
              <!-- Botón ojo para mostrar/ocultar contraseña -->
              <button
                type="button"
                :aria-label="mostrarPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'"
                :aria-pressed="mostrarPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 text-text-muted hover:text-text-secondary transition-colors"
                @click="mostrarPassword = !mostrarPassword"
              >
                <Eye v-if="!mostrarPassword" class="w-4 h-4" aria-hidden="true" />
                <EyeOff v-else class="w-4 h-4" aria-hidden="true" />
              </button>
            </div>
          </div>

          <!-- Recuérdame + Olvidé contraseña -->
          <div class="flex items-center justify-between">
            <label class="flex items-center gap-2 cursor-pointer group" for="login-remember">
              <input
                id="login-remember"
                v-model="form.rememberMe"
                type="checkbox"
                class="w-4 h-4 rounded border-surface-border text-primary focus:ring-primary/30 cursor-pointer"
              />
              <span class="text-sm text-text-secondary group-hover:text-text-primary transition-colors">
                Recordarme
              </span>
            </label>
            <button
              type="button"
              class="text-sm text-primary hover:text-primary-light font-medium transition-colors"
              @click="modalRecuperar = true"
            >
              ¿Eres admin y olvidaste tu contraseña?
            </button>
          </div>

          <!-- Mensaje de error -->
          <Transition name="modal-overlay">
            <p v-if="error" role="alert" class="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
              {{ error }}
            </p>
          </Transition>

          <!-- Botón de login -->
          <button
            type="submit"
            class="btn-primary w-full py-3 text-base"
            :disabled="cargando"
          >
            <Loader2 v-if="cargando" class="w-4 h-4 animate-spin" />
            <span>{{ cargando ? 'Entrando...' : 'Iniciar Sesión' }}</span>
            <ArrowRight v-if="!cargando" class="w-4 h-4" />
          </button>

        </form>

        <!-- Pie del formulario -->
        <p class="text-center text-sm text-text-muted mt-6">
          ¿No tienes acceso?
          <a href="mailto:admin@email.com" class="text-primary hover:text-primary-light font-medium transition-colors">
            Contacta con administración
          </a>
        </p>

      </div>
    </div>

  </div>

  <!-- ══════════════════════════════════════════════════════
       MODAL — Recuperar contraseña
       ════════════════════════════════════════════════════ -->
  <Teleport to="body">
    <Transition name="modal-overlay">
      <div
        v-if="modalRecuperar"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm p-4"
        @click.self="modalRecuperar = false"
      >
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="modal-recuperar-titulo"
          class="bg-white rounded-card shadow-card-lg w-full max-w-sm p-6 animate-fade-scale-in"
        >

          <h3 id="modal-recuperar-titulo" class="text-lg font-semibold text-text-primary mb-1">Recuperar contraseña admin</h3>
          <p class="text-sm text-text-secondary mb-5">
            Introduce el email del administrador para recibir un enlace temporal.
          </p>

          <div class="mb-4">
            <label class="label" for="recuperar-email">Email</label>
            <input
              id="recuperar-email"
              v-model="emailRecuperar"
              type="email"
              placeholder="nombre@ejemplo.com"
              class="input"
            />
          </div>
          <div class="flex gap-3">
            <button class="btn-secondary flex-1" @click="modalRecuperar = false">Cancelar</button>
            <button class="btn-primary flex-1" :disabled="enviandoReset || !emailRecuperar" @click="handleRecuperar">
              <Loader2 v-if="enviandoReset" class="w-4 h-4 animate-spin" />
              <span>{{ enviandoReset ? 'Enviando...' : 'Enviar enlace' }}</span>
            </button>
          </div>


        </div>
      </div>
    </Transition>
  </Teleport>
</template>
