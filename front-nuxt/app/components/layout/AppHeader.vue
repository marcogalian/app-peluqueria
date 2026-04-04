<script setup lang="ts">
/**
 * Header superior — diseño Atelier Sapphire.
 *
 * Izquierda: búsqueda global (rounded-full, fondo gris)
 * Derecha: notificaciones (punto rojo), ajustes, divisor, nombre usuario + avatar
 *
 * Fondo semitransparente con blur (glass effect del diseño)
 */
import { Bell, Settings } from 'lucide-vue-next'

const authStore = useAuthStore()
const route = useRoute()

// Placeholder del buscador según la sección activa
const placeholderBusqueda = computed(() => {
  if (route.path.includes('servicios'))  return 'Buscar servicios o categorías...'
  if (route.path.includes('clientes'))   return 'Buscar clientes...'
  if (route.path.includes('empleados'))  return 'Buscar empleados...'
  if (route.path.includes('inventario')) return 'Buscar productos, ventas...'
  if (route.path.includes('resultados')) return 'Buscar métrica...'
  return 'Buscar...'
})

const iniciales = computed(() => {
  const u = authStore.usuario?.username ?? ''
  return u.slice(0, 2).toUpperCase()
})
</script>

<template>
  <!--
    Header con glass effect: fondo #FAF9FD semitransparente + backdrop-blur.
    Posición sticky para que quede sobre el contenido al hacer scroll.
  -->
  <header class="h-16 flex-shrink-0 flex items-center justify-between px-8
                 bg-surface/70 backdrop-blur-md sticky top-0 z-10
                 border-b border-outline-variant/10 transition-all">

    <!-- ── Búsqueda global ──────────────────────────────── -->
    <div class="flex items-center gap-4 flex-1 max-w-md">
      <div class="relative w-full">
        <!-- Icono de búsqueda -->
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-on-surface-variant/60"
             fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          type="search"
          :placeholder="placeholderBusqueda"
          class="w-full pl-10 pr-4 py-2 rounded-full border-none
                 bg-surface-container-highest text-sm text-on-surface
                 placeholder:text-on-surface-variant/50
                 focus:outline-none focus:ring-2 focus:ring-primary-container/20
                 focus:bg-white transition-all"
        />
      </div>
    </div>

    <!-- ── Acciones + Usuario ───────────────────────────── -->
    <div class="flex items-center gap-2">

      <!-- Notificaciones con punto rojo -->
      <button class="relative p-2 rounded-lg text-on-surface-variant
                     hover:text-primary-container hover:bg-surface-container-low
                     transition-colors">
        <Bell class="w-5 h-5" />
        <span class="absolute top-2 right-2 w-2 h-2 bg-error rounded-full" />
      </button>

      <!-- Ajustes rápidos -->
      <button class="p-2 rounded-lg text-on-surface-variant
                     hover:text-primary-container hover:bg-surface-container-low
                     transition-colors">
        <Settings class="w-5 h-5" />
      </button>

      <!-- Divisor vertical -->
      <div class="h-8 w-px bg-outline-variant/30 mx-2" />

      <!-- Nombre + rol + avatar -->
      <div class="flex items-center gap-3">
        <div class="text-right">
          <p class="text-sm font-bold text-on-surface leading-none">
            {{ authStore.usuario?.username ?? 'Usuario' }}
          </p>
          <p class="text-[10px] text-on-surface-variant mt-0.5">
            {{ authStore.isAdmin ? 'Administrador' : 'Empleado' }}
          </p>
        </div>
        <!-- Avatar — placeholder con iniciales -->
        <div class="w-9 h-9 rounded-full bg-primary-container flex items-center justify-center
                    text-white text-xs font-bold border-2 border-primary-fixed shadow-sm flex-shrink-0">
          {{ iniciales }}
        </div>
      </div>

    </div>
  </header>
</template>
