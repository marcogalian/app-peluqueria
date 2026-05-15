<script setup lang="ts">
import InventarioAdminPage from '~/modules/inventario/pages/InventarioAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <InventarioAdminPage />
</template>
