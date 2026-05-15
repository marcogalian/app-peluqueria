<script setup lang="ts">
import MensajesPage from '~/modules/mensajes/pages/MensajesPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <MensajesPage />
</template>
