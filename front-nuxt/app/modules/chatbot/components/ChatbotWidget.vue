<script setup lang="ts">
import { nextTick } from 'vue'
import { Bot, X, Trash2, Send, Loader2, Sparkles } from 'lucide-vue-next'

const { messages, isOpen, loading, suggestedQuestions, sendMessage, toggle, clearHistory } = useChatbot()
const authStore = useAuthStore()
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

const initialSuggestions = computed(() => {
  const base = [
    '¿Qué citas tengo hoy?',
    '¿Cuál es el horario del salón?',
    '¿Qué servicios ofrecemos?',
    '¿Qué productos tenemos?',
  ]
  if (authStore.isAdmin) {
    base.push('¿Cómo van las ganancias este mes?')
  }
  return base
})

const showInitialSuggestions = computed(() => messages.value.length === 0 && !loading.value)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  await sendMessage(text)
  await nextTick()
  scrollToBottom()
}

async function handleSuggestion(text: string) {
  await sendMessage(text)
  await nextTick()
  scrollToBottom()
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function handleClear() {
  clearHistory()
}

watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })
</script>

<template>
  <div v-if="authStore.isAuthenticated">
    <!-- Floating button -->
    <button
      class="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full bg-primary text-white shadow-lg
             hover:bg-primary-light transition-all duration-300 flex items-center justify-center
             hover:scale-105 active:scale-95"
      aria-label="Abrir asistente IA"
      @click="toggle"
    >
      <Bot v-if="!isOpen" class="w-6 h-6" />
      <X v-else class="w-6 h-6" />
    </button>

    <!-- Chat window -->
    <Transition
      enter-active-class="transition-all duration-300 ease-out"
      enter-from-class="opacity-0 translate-y-4 scale-95"
      enter-to-class="opacity-100 translate-y-0 scale-100"
      leave-active-class="transition-all duration-200 ease-in"
      leave-from-class="opacity-100 translate-y-0 scale-100"
      leave-to-class="opacity-0 translate-y-4 scale-95"
    >
      <div
        v-if="isOpen"
        class="fixed bottom-24 right-6 z-50 w-[380px] h-[520px] bg-white rounded-2xl shadow-xl
               border border-surface-border flex flex-col overflow-hidden"
      >
        <!-- Header -->
        <div class="flex items-center justify-between px-4 py-3 bg-primary text-white rounded-t-2xl">
          <div class="flex items-center gap-2">
            <Sparkles class="w-5 h-5" />
            <span class="font-semibold text-sm">Asistente IA</span>
          </div>
          <div class="flex items-center gap-1">
            <button
              class="p-1.5 rounded-lg hover:bg-white/20 transition-colors"
              title="Limpiar historial"
              @click="handleClear"
            >
              <Trash2 class="w-4 h-4" />
            </button>
            <button
              class="p-1.5 rounded-lg hover:bg-white/20 transition-colors"
              title="Cerrar"
              @click="toggle"
            >
              <X class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Messages area -->
        <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-3">
          <!-- Initial suggestions -->
          <div v-if="showInitialSuggestions" class="space-y-2">
            <p class="text-sm text-text-secondary text-center mb-3">
              ¡Hola! Soy el asistente de Peluquería Isabella. ¿En qué puedo ayudarte?
            </p>
            <div class="flex flex-wrap gap-2 justify-center">
              <button
                v-for="sug in initialSuggestions"
                :key="sug"
                class="text-xs px-3 py-1.5 rounded-full bg-surface-bg text-text-secondary
                       hover:bg-primary/10 hover:text-primary transition-colors border border-surface-border"
                @click="handleSuggestion(sug)"
              >
                {{ sug }}
              </button>
            </div>
          </div>

          <!-- Message bubbles -->
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="flex"
            :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <div
              class="max-w-[80%] px-3 py-2 rounded-2xl text-sm leading-relaxed"
              :class="msg.role === 'user'
                ? 'bg-primary text-white rounded-br-sm'
                : 'bg-surface-bg text-text-primary rounded-bl-sm'"
            >
              {{ msg.content }}
            </div>
          </div>

          <!-- Typing indicator -->
          <div v-if="loading" class="flex justify-start">
            <div class="bg-surface-bg rounded-2xl rounded-bl-sm px-4 py-3 flex items-center gap-1">
              <span class="w-2 h-2 bg-text-muted rounded-full animate-bounce" style="animation-delay: 0ms" />
              <span class="w-2 h-2 bg-text-muted rounded-full animate-bounce" style="animation-delay: 150ms" />
              <span class="w-2 h-2 bg-text-muted rounded-full animate-bounce" style="animation-delay: 300ms" />
            </div>
          </div>

          <!-- Suggested questions after bot reply -->
          <div v-if="suggestedQuestions.length > 0 && !loading" class="flex flex-wrap gap-1.5">
            <button
              v-for="q in suggestedQuestions"
              :key="q"
              class="text-xs px-2.5 py-1 rounded-full bg-primary/5 text-primary
                     hover:bg-primary/15 transition-colors border border-primary/20"
              @click="handleSuggestion(q)"
            >
              {{ q }}
            </button>
          </div>
        </div>

        <!-- Input area -->
        <div class="p-3 border-t border-surface-border">
          <form class="flex gap-2" @submit.prevent="handleSend">
            <input
              v-model="inputText"
              type="text"
              placeholder="Escribe tu pregunta..."
              class="flex-1 px-3 py-2 text-sm rounded-xl border border-surface-border bg-surface-bg
                     focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary
                     placeholder:text-text-muted"
              :disabled="loading"
            />
            <button
              type="submit"
              class="p-2 rounded-xl bg-primary text-white hover:bg-primary-light transition-colors
                     disabled:opacity-40 disabled:cursor-not-allowed"
              :disabled="!inputText.trim() || loading"
            >
              <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
              <Send v-else class="w-4 h-4" />
            </button>
          </form>
        </div>
      </div>
    </Transition>
  </div>
</template>
