<script setup lang="ts">
import { Check, ChevronDown } from 'lucide-vue-next'

interface SelectOption {
  label: string
  value: string
}

const props = defineProps<{
  id?: string
  modelValue: string
  options: SelectOption[]
  ariaLabel?: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const abierto = ref(false)
const root = ref<HTMLElement | null>(null)

const selectedLabel = computed(() =>
  props.options.find(option => option.value === props.modelValue)?.label ?? '',
)

function seleccionar(value: string) {
  emit('update:modelValue', value)
  abierto.value = false
}

function onDocumentClick(event: MouseEvent) {
  if (!root.value?.contains(event.target as Node)) {
    abierto.value = false
  }
}

onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))
</script>

<template>
  <div ref="root" class="relative">
    <button
      :id="id"
      class="app-select-trigger"
      type="button"
      :aria-label="ariaLabel"
      :aria-expanded="abierto"
      aria-haspopup="listbox"
      @click="abierto = !abierto"
    >
      <span class="truncate">{{ selectedLabel }}</span>
      <ChevronDown class="h-4 w-4 text-on-surface-variant transition-transform" :class="{ 'rotate-180': abierto }" />
    </button>

    <Transition name="app-select-menu">
      <div
        v-if="abierto"
        class="app-select-menu"
        role="listbox"
        :aria-labelledby="id"
      >
        <button
          v-for="option in options"
          :key="option.value"
          class="app-select-option"
          :class="{ 'app-select-option-active': option.value === modelValue }"
          type="button"
          role="option"
          :aria-selected="option.value === modelValue"
          @click="seleccionar(option.value)"
        >
          <span class="truncate">{{ option.label }}</span>
          <Check v-if="option.value === modelValue" class="h-4 w-4" />
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.app-select-trigger {
  display: flex;
  min-height: 2.5rem;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  border-radius: 0.375rem;
  border: 1px solid rgb(104 111 122 / 0.5);
  background: white;
  padding: 0.625rem 0.875rem;
  color: rgb(26 28 30);
  font-size: 0.875rem;
  font-weight: 500;
  line-height: 1.25rem;
  box-shadow: inset 0 0 0 1px rgb(104 111 122 / 0.12);
  transition: border-color 160ms ease, box-shadow 160ms ease, background-color 160ms ease;
}

.app-select-trigger:focus-visible {
  outline: none;
  border-color: rgb(0 32 69);
  box-shadow: 0 0 0 3px rgb(0 32 69 / 0.14);
}

.app-select-menu {
  position: absolute;
  z-index: 50;
  top: calc(100% + 0.25rem);
  left: 0;
  width: 100%;
  overflow: hidden;
  border-radius: 0.375rem;
  border: 1px solid rgb(104 111 122 / 0.18);
  background: white;
  box-shadow: 0 10px 24px rgb(15 23 42 / 0.12);
}

.app-select-option {
  display: flex;
  min-height: 2.25rem;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.5rem 0.875rem;
  text-align: left;
  color: rgb(26 28 30);
  font-size: 0.875rem;
  font-weight: 600;
}

.app-select-option:hover {
  background: rgb(245 246 249);
}

.app-select-option-active {
  background: rgb(237 242 248);
  color: rgb(0 32 69);
  font-weight: 800;
}

.app-select-menu-enter-active,
.app-select-menu-leave-active {
  transition: opacity 120ms ease, transform 120ms ease;
}

.app-select-menu-enter-from,
.app-select-menu-leave-to {
  opacity: 0;
  transform: translateY(-0.25rem);
}
</style>
