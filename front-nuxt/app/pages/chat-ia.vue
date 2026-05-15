<script setup lang="ts">
import AsistenteGestionPage from '~/modules/chatbot/pages/AsistenteGestionPage.vue'

definePageMeta({ middleware: 'auth' })
</script>

<template>
  <AsistenteGestionPage />
</template>
