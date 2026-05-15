interface ChatMsg {
  role: 'user' | 'assistant'
  content: string
}

const messages = ref<ChatMsg[]>([])
const isOpen = ref(false)
const loading = ref(false)
const suggestedQuestions = ref<string[]>([])

export function useChatbot() {
  async function sendMessage(text: string) {
    messages.value.push({ role: 'user', content: text })
    loading.value = true
    try {
      const { api } = await import('~/infrastructure/http/api')
      const { data } = await api.post('/chat', {
        message: text,
        history: messages.value.slice(0, -1).map(m => ({
          role: m.role === 'assistant' ? 'model' : 'user',
          content: m.content,
        })),
      })
      messages.value.push({ role: 'assistant', content: data.reply })
      suggestedQuestions.value = data.suggestedQuestions || []
    } catch (err: unknown) {
      const isAxiosErr = (e: unknown): e is { response?: { status?: number; data?: { message?: string } }; message?: string } =>
        typeof e === 'object' && e !== null && 'response' in e
      const status = isAxiosErr(err) ? err.response?.status : undefined
      const detail = isAxiosErr(err)
        ? err.response?.data?.message ?? err.message ?? 'sin detalles'
        : 'sin detalles'
      console.error('[Chatbot] Error:', status, detail)
      messages.value.push({
        role: 'assistant',
        content: status
          ? `Error ${status}: ${detail}`
          : 'No se pudo conectar con el servidor. ¿Está arrancado el backend?',
      })
    } finally {
      loading.value = false
    }
  }

  function toggle() {
    isOpen.value = !isOpen.value
  }

  function clearHistory() {
    messages.value = []
    suggestedQuestions.value = []
  }

  return { messages, isOpen, loading, suggestedQuestions, sendMessage, toggle, clearHistory }
}
