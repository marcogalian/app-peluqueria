<script setup lang="ts">
import ResultadosAdminPage from '~/modules/resultados/pages/ResultadosAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <ResultadosAdminPage />
</template>
