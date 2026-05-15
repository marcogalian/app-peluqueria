<script setup lang="ts">
import EmpleadosAdminPage from '~/modules/empleados/pages/EmpleadosAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <EmpleadosAdminPage />
</template>
