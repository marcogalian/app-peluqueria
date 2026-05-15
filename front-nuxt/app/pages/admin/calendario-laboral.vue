<script setup lang="ts">
import CalendarioLaboralAdminPage from '~/modules/calendario/pages/CalendarioLaboralAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <CalendarioLaboralAdminPage />
</template>
