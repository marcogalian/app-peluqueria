import { describe, it, expect, beforeEach, vi } from 'vitest'

// Mock del cliente HTTP. Se reemplaza el modulo entero antes de cada import dinamico
// que hace useChatbot dentro de sendMessage.
const apiPostMock = vi.fn()
vi.mock('~/infrastructure/http/api', () => ({
  api: { post: apiPostMock },
}))

// El composable usa estado modulo (singleton). Limpiamos cache entre tests.
async function importarFresco() {
  vi.resetModules()
  const modulo = await import('~/modules/chatbot/composables/useChatbot')
  return modulo.useChatbot()
}

describe('useChatbot', () => {
  beforeEach(() => {
    apiPostMock.mockReset()
  })

  it('sendMessage agrega el mensaje del usuario y la respuesta del bot', async () => {
    apiPostMock.mockResolvedValueOnce({
      data: { reply: 'Hola, soy el bot', suggestedQuestions: [] },
    })

    const { messages, sendMessage } = await importarFresco()
    await sendMessage('Hola')

    expect(messages.value).toHaveLength(2)
    expect(messages.value[0]).toEqual({ role: 'user', content: 'Hola' })
    expect(messages.value[1]).toEqual({ role: 'assistant', content: 'Hola, soy el bot' })
  })

  it('sendMessage envia el historial manteniendo user/assistant y recortando a 8 mensajes validos', async () => {
    apiPostMock.mockResolvedValue({
      data: { reply: 'segunda respuesta', suggestedQuestions: [] },
    })

    const { sendMessage } = await importarFresco()
    for (let i = 0; i < 8; i += 1) {
      await sendMessage(`mensaje ${i}`)
    }

    const ultimaLlamada = apiPostMock.mock.calls.at(-1)?.[1]
    expect(ultimaLlamada.message).toBe('mensaje 7')
    expect(ultimaLlamada.history).toHaveLength(8)
    expect(ultimaLlamada.history[0]).toEqual({ role: 'user', content: 'mensaje 3' })
    expect(ultimaLlamada.history[1]).toEqual({ role: 'assistant', content: 'segunda respuesta' })
    expect(ultimaLlamada.history.at(-1)).toEqual({ role: 'assistant', content: 'segunda respuesta' })
  })

  it('Si el backend devuelve error con status, se muestra "Error N: ..."', async () => {
    apiPostMock.mockRejectedValueOnce({
      response: { status: 500, data: { message: 'BD caida' } },
    })

    const { messages, sendMessage } = await importarFresco()
    await sendMessage('test')

    expect(messages.value[1].content).toBe('Error 500: BD caida')
  })

  it('Si el backend rechaza por historial largo, muestra un mensaje amable', async () => {
    apiPostMock.mockRejectedValueOnce({
      response: { status: 400, data: { message: 'El historial no puede superar 12 mensajes' } },
    })

    const { messages, sendMessage } = await importarFresco()
    await sendMessage('test')

    expect(messages.value[1].content).toContain('La conversación ya es demasiado larga')
  })

  it('Los errores locales no se reenvian como parte del historial', async () => {
    apiPostMock
      .mockRejectedValueOnce({
        response: { status: 400, data: { message: 'El historial no puede superar 12 mensajes' } },
      })
      .mockResolvedValueOnce({
        data: { reply: 'ok', suggestedQuestions: [] },
      })

    const { sendMessage } = await importarFresco()
    await sendMessage('primero')
    await sendMessage('segundo')

    const segundaLlamada = apiPostMock.mock.calls[1][1]
    expect(segundaLlamada.history).toEqual([{ role: 'user', content: 'primero' }])
  })

  it('Si no hay status (network error), se muestra mensaje generico', async () => {
    apiPostMock.mockRejectedValueOnce(new Error('Network down'))

    const { messages, sendMessage } = await importarFresco()
    await sendMessage('test')

    expect(messages.value[1].content).toContain('No se pudo conectar')
  })

  it('loading pasa a true durante la peticion y vuelve a false al terminar', async () => {
    let resolverPeticion: (valor: any) => void = () => {}
    apiPostMock.mockReturnValueOnce(
      new Promise((resolve) => {
        resolverPeticion = resolve
      }),
    )

    const { loading, sendMessage } = await importarFresco()
    const promesaSend = sendMessage('hola')

    expect(loading.value).toBe(true)
    resolverPeticion({ data: { reply: 'ok', suggestedQuestions: [] } })
    await promesaSend
    expect(loading.value).toBe(false)
  })

  it('suggestedQuestions se actualiza con las del backend', async () => {
    apiPostMock.mockResolvedValueOnce({
      data: { reply: 'respuesta', suggestedQuestions: ['Pregunta A', 'Pregunta B'] },
    })

    const { suggestedQuestions, sendMessage } = await importarFresco()
    await sendMessage('test')

    expect(suggestedQuestions.value).toEqual(['Pregunta A', 'Pregunta B'])
  })

  it('clearHistory vacia mensajes y sugerencias', async () => {
    apiPostMock.mockResolvedValueOnce({
      data: { reply: 'r', suggestedQuestions: ['A'] },
    })

    const { messages, suggestedQuestions, sendMessage, clearHistory } = await importarFresco()
    await sendMessage('hola')
    expect(messages.value.length).toBeGreaterThan(0)

    clearHistory()

    expect(messages.value).toHaveLength(0)
    expect(suggestedQuestions.value).toHaveLength(0)
  })

  it('toggle alterna isOpen', async () => {
    const { isOpen, toggle } = await importarFresco()
    expect(isOpen.value).toBe(false)

    toggle()
    expect(isOpen.value).toBe(true)

    toggle()
    expect(isOpen.value).toBe(false)
  })
})
