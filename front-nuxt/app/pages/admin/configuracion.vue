<script setup lang="ts">
import ConfiguracionAdminPage from '~/modules/configuracion/pages/ConfiguracionAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <ConfiguracionAdminPage />
</template>
