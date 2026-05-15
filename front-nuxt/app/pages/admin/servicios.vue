<script setup lang="ts">
import ServiciosAdminPage from '~/modules/servicios/pages/ServiciosAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <ServiciosAdminPage />
</template>
