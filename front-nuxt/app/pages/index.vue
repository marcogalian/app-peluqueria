<script setup lang="ts">
/**
 * Página raíz — solo redirige según el rol del usuario.
 * Admin → /admin/dashboard, Empleado → /agenda
 */
definePageMeta({ middleware: 'auth' })

const authStore = useAuthStore()

// En Nuxt 4, navigateTo en setup se ejecuta durante la hidratación
// Por eso usamos onMounted para esperar a que el store esté listo
onMounted(() => {
  if (authStore.isAdmin) {
    navigateTo('/admin/dashboard')
  } else {
    navigateTo('/agenda')
  }
})
</script>

<template>
  <!-- No se renderiza nada — esta página solo redirige -->
  <div />
</template>
