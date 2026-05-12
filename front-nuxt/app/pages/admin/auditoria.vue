<script setup lang="ts">
import AuditoriaAdminPage from '~/modules/auditoria/pages/AuditoriaAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <AuditoriaAdminPage />
</template>
