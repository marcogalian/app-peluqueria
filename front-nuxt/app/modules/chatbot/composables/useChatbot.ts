interface ChatMsg {
  role: 'user' | 'assistant'
  content: string
  localOnly?: boolean
}

const MAX_HISTORY_MESSAGES = 8
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
      const history = messages.value
        .slice(0, -1)
        .filter(m => !m.localOnly)
        .slice(-MAX_HISTORY_MESSAGES)
        .map(m => ({
          role: m.role,
          content: m.content,
        }))

      const { data } = await api.post('/chat', {
        message: text,
        history,
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
      const friendlyMessage = status === 400 && detail.includes('historial')
        ? 'La conversación ya es demasiado larga. Limpia el chat o sigue con una pregunta nueva más breve.'
        : status
          ? `Error ${status}: ${detail}`
          : 'No se pudo conectar con el servidor. ¿Está arrancado el backend?'
      suggestedQuestions.value = []
      messages.value.push({
        role: 'assistant',
        content: friendlyMessage,
        localOnly: true,
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
