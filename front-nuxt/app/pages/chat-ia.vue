<script setup lang="ts">
import { nextTick } from 'vue'
import { Trash2, Send, Loader2, Sparkles, Bot } from 'lucide-vue-next'

definePageMeta({ middleware: 'auth' })

const { messages, loading, suggestedQuestions, sendMessage, clearHistory } = useChatbot()
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)

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

watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })
</script>

<template>
  <div class="max-w-3xl mx-auto h-full flex flex-col gap-4">

    <!-- Cabecera -->
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <div class="w-10 h-10 rounded-2xl bg-primary flex items-center justify-center">
          <Sparkles class="w-5 h-5 text-white" />
        </div>
        <div>
          <h1 class="text-xl font-bold text-on-surface">Asistente IA</h1>
        </div>
      </div>
      <button
        v-if="messages.length > 0"
        class="flex items-center gap-2 px-3 py-2 rounded-xl text-sm text-on-surface-variant
               hover:bg-surface-container transition-colors"
        @click="clearHistory"
      >
        <Trash2 class="w-4 h-4" />
        Limpiar chat
      </button>
    </div>

    <!-- Área de chat -->
    <div class="flex-1 flex flex-col bg-white rounded-2xl border border-outline-variant/20 overflow-hidden min-h-0">

      <!-- Mensajes -->
      <div ref="messagesContainer" class="flex-1 overflow-y-auto p-6 space-y-4">

        <!-- Estado vacío -->
        <div v-if="messages.length === 0 && !loading" class="flex flex-col items-center gap-3 pt-12 text-center">
          <div class="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center">
            <Bot class="w-8 h-8 text-primary" />
          </div>
          <p class="font-semibold text-on-surface">Asistente IA</p>
          <p class="text-sm text-on-surface-variant">Escribe tu pregunta para empezar</p>
        </div>

        <!-- Burbujas de mensajes -->
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[75%] px-4 py-3 rounded-2xl text-sm leading-relaxed"
            :class="msg.role === 'user'
              ? 'bg-primary text-white rounded-br-sm'
              : 'bg-surface-container text-on-surface rounded-bl-sm'"
          >
            {{ msg.content }}
          </div>
        </div>

        <!-- Indicador de escritura -->
        <div v-if="loading" class="flex justify-start">
          <div class="bg-surface-container rounded-2xl rounded-bl-sm px-5 py-3 flex items-center gap-1.5">
            <span class="w-2 h-2 bg-on-surface-variant/50 rounded-full animate-bounce" style="animation-delay: 0ms" />
            <span class="w-2 h-2 bg-on-surface-variant/50 rounded-full animate-bounce" style="animation-delay: 150ms" />
            <span class="w-2 h-2 bg-on-surface-variant/50 rounded-full animate-bounce" style="animation-delay: 300ms" />
          </div>
        </div>

        <!-- Sugerencias tras respuesta -->
        <div v-if="suggestedQuestions.length > 0 && !loading" class="flex flex-wrap gap-2 pl-1">
          <button
            v-for="q in suggestedQuestions"
            :key="q"
            class="text-xs px-3 py-1.5 rounded-full bg-primary/5 text-primary
                   hover:bg-primary/15 transition-colors border border-primary/20"
            @click="handleSuggestion(q)"
          >
            {{ q }}
          </button>
        </div>
      </div>

      <!-- Input -->
      <div class="p-4 border-t border-outline-variant/20">
        <form class="flex gap-3" @submit.prevent="handleSend">
          <input
            v-model="inputText"
            type="text"
            placeholder="Escribe tu pregunta..."
            class="flex-1 px-4 py-3 text-sm rounded-xl border border-outline-variant/30 bg-surface-container-low
                   focus:outline-none focus:ring-2 focus:ring-primary/30 focus:border-primary
                   placeholder:text-on-surface-variant/50"
            :disabled="loading"
          />
          <button
            type="submit"
            class="px-4 py-3 rounded-xl bg-primary text-white hover:bg-primary/90 transition-colors
                   disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-2"
            :disabled="!inputText.trim() || loading"
          >
            <Loader2 v-if="loading" class="w-4 h-4 animate-spin" />
            <Send v-else class="w-4 h-4" />
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
