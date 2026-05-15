<script setup lang="ts">
import VentasPage from '~/modules/ventas/pages/VentasPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <VentasPage />
</template>
