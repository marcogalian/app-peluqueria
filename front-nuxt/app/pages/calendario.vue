<script setup lang="ts">
import CalendarioPage from '~/modules/calendario/pages/CalendarioPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <CalendarioPage />
</template>
