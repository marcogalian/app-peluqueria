<script setup lang="ts">
import AgendaPage from '~/modules/agenda/pages/AgendaPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <AgendaPage />
</template>
