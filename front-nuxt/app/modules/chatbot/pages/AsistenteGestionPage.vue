<script setup lang="ts">
import { nextTick } from 'vue'
import { Trash2, Send, Loader2, Sparkles, Scissors } from 'lucide-vue-next'

definePageMeta({ middleware: 'auth' })

const { messages, loading, suggestedQuestions, sendMessage, clearHistory } = useChatbot()
const inputText = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const questionInput = ref<HTMLInputElement | null>(null)

async function handleSend() {
  const text = inputText.value.trim()
  if (!text || loading.value) return
  inputText.value = ''
  await sendMessage(text)
  await nextTick()
  scrollToBottom()
  focusQuestionInput()
}

async function handleSuggestion(text: string) {
  await sendMessage(text)
  await nextTick()
  scrollToBottom()
  focusQuestionInput()
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

function focusQuestionInput() {
  requestAnimationFrame(() => questionInput.value?.focus())
}

watch(messages, () => {
  nextTick(() => scrollToBottom())
}, { deep: true })
</script>

<template>
  <div class="max-w-5xl mx-auto h-full flex flex-col gap-4">

    <!-- Banner -->
    <div class="relative overflow-hidden rounded-xl border border-primary-container/15 bg-primary-container text-white shadow-sm">
      <img
        src="/images/foto-admin.jpeg"
        alt="Interior de Peluquería Isabella"
        class="absolute inset-0 h-full w-full object-cover"
      />
      <div class="absolute inset-0 bg-gradient-to-r from-[#071a35]/95 via-[#1a365d]/82 to-[#1a365d]/42" />
      <div class="relative flex flex-col gap-5 p-6 sm:flex-row sm:items-end sm:justify-between">
        <div class="max-w-2xl">
          <div class="mb-3 inline-flex items-center gap-2 rounded-full border border-white/20 bg-white/10 px-3 py-1 text-[11px] font-bold uppercase tracking-widest text-white/80">
            <Sparkles class="h-3.5 w-3.5" />
            Chat interno
          </div>
          <h1 class="text-2xl font-extrabold tracking-tight sm:text-3xl">Asistente de Gestión</h1>
          <p class="mt-2 max-w-xl text-sm leading-relaxed text-white/78">
            Consulta datos, organiza tareas y resuelve dudas operativas del salón sin salir del panel.
          </p>
        </div>
        <button
          v-if="messages.length > 0"
          class="chat-clear-button inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-extrabold"
          @click="clearHistory"
        >
          <Trash2 class="w-4 h-4" />
          Limpiar chat
        </button>
      </div>
      <div class="relative h-1 w-full bg-white/12">
        <div class="h-full w-44 bg-white/70" />
      </div>
    </div>

    <!-- Área de chat -->
    <div class="flex-1 flex flex-col bg-white rounded-xl border border-outline-variant/30 overflow-hidden min-h-0 shadow-sm">

      <!-- Mensajes -->
      <div ref="messagesContainer" class="flex-1 overflow-y-auto p-6 space-y-4">

        <!-- Estado vacío -->
        <div v-if="messages.length === 0 && !loading" class="flex flex-col items-center gap-3 pt-12 text-center">
          <div class="w-16 h-16 rounded-full border border-primary-container/20 bg-primary-container/8 flex items-center justify-center">
            <Scissors class="w-7 h-7 text-primary-container" />
          </div>
          <p class="font-semibold text-on-surface">Asistente de gestión de peluquería</p>
          <p class="max-w-md text-sm text-on-surface-variant">
            Pregunta por citas, clientes, empleados, inventario, ventas o cualquier duda diaria del salón.
          </p>
        </div>

        <!-- Burbujas de mensajes -->
        <div
          v-for="(msg, i) in messages"
          :key="i"
          class="flex"
          :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
        >
          <div
            class="max-w-[75%] whitespace-pre-line px-4 py-3 rounded-2xl text-sm leading-relaxed"
            :class="msg.role === 'user'
              ? 'bg-primary-container text-white rounded-br-sm'
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
            class="text-xs px-3 py-1.5 rounded-md bg-primary-container/5 text-primary-container
                   hover:bg-primary-container/12 transition-colors border border-primary-container/20"
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
            ref="questionInput"
            v-model="inputText"
            type="text"
            placeholder="Escribe una consulta de gestión..."
            class="flex-1 px-4 py-3 text-sm rounded-md border border-outline-variant/50 bg-white
                   focus:outline-none focus:ring-2 focus:ring-primary-container/20 focus:border-primary-container
                   placeholder:text-on-surface-variant/50"
            :disabled="loading"
          />
          <button
            type="submit"
            class="px-4 py-3 rounded-md bg-primary-container text-white hover:bg-primary-container/90 transition-colors
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

<style scoped>
.chat-clear-button {
  color: #1a365d;
  background: #ffffff;
  border: 1px solid rgb(255 255 255 / 0.95);
  box-shadow: 0 2px 6px rgb(0 0 0 / 0.18);
}

.chat-clear-button:hover {
  background: #f4f3f7;
}
</style>
