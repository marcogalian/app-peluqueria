<script setup lang="ts">
import PanelControlAdminPage from '~/modules/dashboard/pages/PanelControlAdminPage.vue'

definePageMeta({ middleware: ['auth', 'admin'] })
</script>

<template>
  <PanelControlAdminPage />
</template>
