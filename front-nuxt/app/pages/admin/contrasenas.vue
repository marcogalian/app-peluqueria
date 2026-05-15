<script setup lang="ts">
import ContrasenasEmpleadosAdminPage from '~/modules/auth/pages/ContrasenasEmpleadosAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <ContrasenasEmpleadosAdminPage />
</template>
