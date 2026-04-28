<script setup lang="ts">
/**
 * Página raíz — solo redirige según el rol del usuario.
 * Admin → /admin/dashboard, Empleado → /agenda
 */
definePageMeta({
  middleware: [
    'auth',
    function (to, from) {
      const authStore = useAuthStore()
      if (authStore.isAdmin) {
        return navigateTo('/admin/dashboard')
      } else {
        return navigateTo('/agenda')
      }
    }
  ]
})
</script>

<template>
  <!-- No se renderiza nada — esta página solo redirige -->
  <div />
</template>
