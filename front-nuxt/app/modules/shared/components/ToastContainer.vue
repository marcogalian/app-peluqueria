<script setup lang="ts">
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-vue-next'
import { useToast } from '~/modules/shared/composables/useToast'

const { toasts, dismiss } = useToast()

const icon = { success: CheckCircle2, error: AlertCircle, info: Info } as const
const colors = {
  success: 'bg-green-50 text-green-800 border-green-200',
  error:   'bg-red-50 text-red-800 border-red-200',
  info:    'bg-blue-50 text-blue-800 border-blue-200',
} as const
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2 pointer-events-none">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        leave-active-class="transition-all duration-200 ease-in"
        enter-from-class="opacity-0 translate-x-8"
        leave-to-class="opacity-0 translate-x-8"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="colors[toast.type]"
          class="pointer-events-auto flex items-center gap-3 px-4 py-3 rounded-xl border shadow-lg min-w-[280px] max-w-[400px]"
          role="alert"
        >
          <component :is="icon[toast.type]" class="w-5 h-5 flex-shrink-0" />
          <span class="text-sm font-medium flex-1">{{ toast.message }}</span>
          <button
            class="p-0.5 rounded-full hover:bg-black/5 transition-colors flex-shrink-0"
            aria-label="Cerrar"
            @click="dismiss(toast.id)"
          >
            <X class="w-3.5 h-3.5" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
