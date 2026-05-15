<script setup lang="ts">
import { AlertTriangle, ArrowLeft, Home, LogIn } from 'lucide-vue-next'

const props = defineProps<{
  error: {
    statusCode?: number
    statusMessage?: string
    message?: string
  }
}>()

const statusCode = computed(() => props.error?.statusCode ?? 500)

const titulo = computed(() => {
  if (statusCode.value === 400) return 'Petición no válida'
  if (statusCode.value === 401) return 'Sesión necesaria'
  if (statusCode.value === 403) return 'Acceso restringido'
  if (statusCode.value === 404) return 'Página no encontrada'
  if (statusCode.value >= 500) return 'Error interno'
  return 'Algo no salió bien'
})

const descripcion = computed(() => {
  if (statusCode.value === 400) return 'La información enviada no es correcta o falta algún dato.'
  if (statusCode.value === 401) return 'Inicia sesión de nuevo para continuar trabajando.'
  if (statusCode.value === 403) return 'Tu usuario no tiene permisos para abrir esta zona.'
  if (statusCode.value === 404) return 'La pantalla que buscas no existe o ha cambiado de sitio.'
  if (statusCode.value >= 500) return 'El servidor no pudo completar la operación. Vuelve al panel e inténtalo de nuevo.'
  return props.error?.statusMessage || props.error?.message || 'La aplicación no pudo mostrar esta pantalla.'
})

function volver() {
  if (import.meta.client && window.history.length > 1) {
    clearError()
    window.history.back()
    return
  }
  clearError({ redirect: '/' })
}

function irInicio() {
  clearError({ redirect: '/' })
}

function irLogin() {
  clearError({ redirect: '/login' })
}
</script>

<template>
  <main class="min-h-screen bg-surface-container-lowest px-4 py-8 text-on-surface sm:px-6">
    <section class="mx-auto flex min-h-[calc(100vh-4rem)] w-full max-w-lg flex-col items-center justify-center text-center">
      <div class="mb-5 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-50 text-red-600">
        <AlertTriangle class="h-7 w-7" aria-hidden="true" />
      </div>

      <p class="text-xs font-black uppercase tracking-[0.24em] text-on-surface-variant">
        Error {{ statusCode }}
      </p>
      <h1 class="mt-3 text-3xl font-black text-primary sm:text-4xl">
        {{ titulo }}
      </h1>
      <p class="mt-3 max-w-md text-sm leading-6 text-on-surface-variant">
        {{ descripcion }}
      </p>

      <div class="mt-7 grid w-full gap-3 sm:grid-cols-2">
        <button
          type="button"
          class="btn-secondary justify-center"
          @click="volver"
        >
          <ArrowLeft class="h-4 w-4" aria-hidden="true" />
          Volver
        </button>
        <button
          type="button"
          class="btn-primary justify-center"
          @click="statusCode === 401 ? irLogin() : irInicio()"
        >
          <LogIn v-if="statusCode === 401" class="h-4 w-4" aria-hidden="true" />
          <Home v-else class="h-4 w-4" aria-hidden="true" />
          {{ statusCode === 401 ? 'Ir al login' : 'Ir al inicio' }}
        </button>
      </div>
    </section>
  </main>
</template>
