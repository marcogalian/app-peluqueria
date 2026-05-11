<script setup lang="ts">
import ToastContainer from '~/modules/shared/components/ToastContainer.vue'
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-surface-container-low">
    <AppSidebar />
    <div class="flex flex-col flex-1 overflow-hidden">
      <AppHeader />
      <main class="flex-1 overflow-y-auto bg-surface">
        <div class="min-h-full px-8 pt-8 pb-14 lg:pb-16">
          <slot />
        </div>
      </main>
    </div>
  </div>
  <ToastContainer />
</template>
