<script setup lang="ts">
import ToastContainer from '~/modules/shared/components/ToastContainer.vue'
</script>

<template>
  <div class="flex h-dvh overflow-hidden bg-surface-container-low">
    <AppSidebar />
    <div class="flex min-w-0 flex-1 flex-col overflow-hidden">
      <AppHeader />
      <main class="min-h-0 flex-1 overflow-y-auto overflow-x-hidden bg-surface">
        <div class="min-h-full px-4 pt-5 pb-12 sm:px-6 sm:pt-6 lg:px-8 lg:pt-8 lg:pb-16">
          <slot />
        </div>
      </main>
    </div>
  </div>
  <ToastContainer />
</template>
