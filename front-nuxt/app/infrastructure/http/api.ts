/**
 * Cliente HTTP centralizado con Axios.
 *
 * Por qué aquí y no en cada módulo:
 * - Un único punto de configuración (base URL, headers, interceptors)
 * - Los módulos dependen de esta abstracción, no de Axios directamente (D de SOLID)
 * - Si mañana cambiamos de Axios a fetch, solo tocamos este archivo
 */
import axios, { type AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'

interface ApiErrorBody {
  message?: string
  detail?: string
  error?: string
  status?: number
}

export interface NormalizedApiError extends AxiosError<ApiErrorBody> {
  userMessage?: string
  statusCode?: number
}

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
  timeout: 40000,
})

export function getApiErrorMessage(error: unknown, fallback = 'No se pudo completar la operación.'): string {
  if (!axios.isAxiosError<ApiErrorBody>(error)) return fallback

  const data = error.response?.data
  if (typeof data === 'string' && data.trim()) return data
  if (data?.message) return data.message
  if (data?.detail) return data.detail
  if (data?.error) return data.error

  const status = error.response?.status
  if (status === 400) return 'Revisa los datos enviados.'
  if (status === 401) return 'Tu sesión ha caducado. Inicia sesión de nuevo.'
  if (status === 403) return 'No tienes permisos para realizar esta acción.'
  if (status === 404) return 'No se encontró el recurso solicitado.'
  if (status === 409) return 'La operación entra en conflicto con el estado actual.'
  if (status && status >= 500) return 'Error interno del servidor. Inténtalo de nuevo.'

  return fallback
}

// ── Interceptor de REQUEST ───────────────────────────────────
// Añade el token JWT en cada petición automáticamente
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  // localStorage solo existe en el cliente — guard necesario aunque SSR esté desactivado
  if (import.meta.client) {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
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

      // localStorage y window solo existen en el cliente
      if (!import.meta.client) return Promise.reject(error)

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

    const normalizedError = error as NormalizedApiError
    normalizedError.statusCode = error.response?.status
    normalizedError.userMessage = getApiErrorMessage(error)

    return Promise.reject(error)
  },
)
