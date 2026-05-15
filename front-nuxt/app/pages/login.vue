<script setup lang="ts">
import LoginPage from '~/modules/auth/pages/LoginPage.vue'

definePageMeta({
  layout: 'auth',
  auth: false,
})
</script>

<template>
  <LoginPage />
</template>
