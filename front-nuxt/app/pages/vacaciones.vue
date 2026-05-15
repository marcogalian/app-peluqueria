<script setup lang="ts">
import VacacionesPage from '~/modules/ausencias/pages/VacacionesPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <VacacionesPage />
</template>
