<script setup lang="ts">
import ClientesAdminPage from '~/modules/clientes/pages/ClientesAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <ClientesAdminPage />
</template>
