/**
 * Cliente HTTP centralizado con Axios.
 *
 * Por qué aquí y no en cada módulo:
 * - Un único punto de configuración (base URL, headers, interceptors)
 * - Los módulos dependen de esta abstracción, no de Axios directamente (D de SOLID)
 * - Si mañana cambiamos de Axios a fetch, solo tocamos este archivo
 */
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

const getBaseURL = (): string => {
  if (typeof window !== 'undefined' && (window as any).__NUXT__?.config?.public?.apiBase) {
    return (window as any).__NUXT__.config.public.apiBase
  }
  return 'http://localhost:8080/api'
}

export const api: AxiosInstance = axios.create({
  baseURL: getBaseURL(),
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

// ── Interceptor de REQUEST ───────────────────────────────────
// Añade el token JWT en cada petición automáticamente
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // El token se guarda en localStorage por el auth store
  const token = localStorage.getItem('access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// ── Interceptor de RESPONSE ──────────────────────────────────
// Si el servidor devuelve 401, intenta refrescar el token una vez
// Si falla, limpia la sesión y redirige al login
api.interceptors.response.use(
  // Respuesta correcta — la deja pasar sin tocarla
  (response) => response,

  // Error — maneja el 401 con refresh token
  async (error) => {
    const originalRequest = error.config

    // Solo intentamos refresh si es un 401 y no es la propia llamada de refresh
    const is401 = error.response?.status === 401
    const isRefreshCall = originalRequest.url?.includes('/auth/refresh')
    const alreadyRetried = originalRequest._retry

    if (is401 && !isRefreshCall && !alreadyRetried) {
      originalRequest._retry = true

      try {
        const refreshToken = localStorage.getItem('refresh_token')
        if (!refreshToken) throw new Error('No hay refresh token')

        // Pide un nuevo access token al backend
        const { data } = await axios.post(`${getBaseURL()}/auth/refresh`, {
          refreshToken,
        })

        // Guarda el nuevo token y reintenta la petición original
        localStorage.setItem('access_token', data.token)
        originalRequest.headers.Authorization = `Bearer ${data.token}`
        return api(originalRequest)
      } catch {
        // El refresh falló — limpia sesión y fuerza login
        localStorage.removeItem('access_token')
        localStorage.removeItem('refresh_token')
        window.location.href = '/login'
      }
    }

    return Promise.reject(error)
  },
)
