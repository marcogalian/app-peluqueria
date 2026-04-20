/**
 * Composable singleton para notificaciones de chat en tiempo real.
 * Se conecta al WS y cuenta mensajes entrantes de otros usuarios.
 * El conteo se resetea al navegar a /mensajes.
 */

interface NotifMensaje {
  emisor: string
  timestamp: number
}

// Singleton — refs a nivel de módulo, shared entre todos los componentes
const noLeidos = ref<NotifMensaje[]>([])
const conteo    = ref(0)
let   _client: any = null

export function useNotificacionesChat() {
  const authStore = useAuthStore()
  const config    = useRuntimeConfig()

  async function conectar() {
    if (!import.meta.client) return
    if (_client?.active)     return

    try {
      const [{ Client }, SockJS] = await Promise.all([
        import('@stomp/stompjs'),
        import('sockjs-client').then(m => m.default),
      ])

      const token = localStorage.getItem('access_token')
      if (!token) return

      _client = new Client({
        webSocketFactory: () => new (SockJS as any)(config.public.wsBase),
        connectHeaders:   { Authorization: `Bearer ${token}` },
        reconnectDelay:   5000,

        onConnect: () => {
          _client.subscribe('/topic/public', (frame: any) => {
            const msg = JSON.parse(frame.body)

            if (msg.tipo !== 'CHAT') return
            if (msg.emisor === authStore.usuario?.username) return

            noLeidos.value.unshift({ emisor: msg.emisor, timestamp: Date.now() })
            if (noLeidos.value.length > 5) noLeidos.value.pop()
            conteo.value++
          })
        },

        onStompError: () => { /* silently reconnect */ },
      })

      _client.activate()
    } catch { /* WS no disponible, funciona sin notificaciones */ }
  }

  function limpiarConteo() {
    conteo.value   = 0
    noLeidos.value = []
  }

  function desconectar() {
    _client?.deactivate()
    _client = null
  }

  return { noLeidos, conteo, conectar, limpiarConteo, desconectar }
}
