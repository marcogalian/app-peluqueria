/**
 * Store de autenticación con Pinia.
 * Responsabilidad única (S de SOLID): gestionar el estado de sesión del usuario.
 * No tiene lógica de UI ni de navegación — eso es tarea de los componentes.
 */
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import type { Usuario, Rol } from '../types/auth.types'

interface JwtPayload {
  sub: string          // username
  email?: string
  roles?: string[]     // ['ROLE_ADMIN'] o ['ROLE_HAIRDRESSER']
  role?: string        // alternativa singular del backend
  exp: number
}

export const useAuthStore = defineStore('auth', () => {
  // ── Estado ────────────────────────────────────────────────
  const accessToken = ref<string | null>(null)
  const usuario = ref<Usuario | null>(null)

  // ── Getters ───────────────────────────────────────────────

  /** ¿Hay sesión activa con token válido? */
  const isAuthenticated = computed((): boolean => {
    if (!accessToken.value) return false
    try {
      const payload = jwtDecode<JwtPayload>(accessToken.value)
      // Comprobamos que el token no haya expirado
      return payload.exp * 1000 > Date.now()
    } catch {
      return false
    }
  })

  /** ¿Es administrador? */
  const isAdmin = computed((): boolean => usuario.value?.rol === 'ROLE_ADMIN')

  /** ¿Es empleado (peluquero)? */
  const isEmpleado = computed((): boolean => usuario.value?.rol === 'ROLE_HAIRDRESSER')

  // ── Acciones ──────────────────────────────────────────────

  /**
   * Guarda el token en el store y en localStorage.
   * Decodifica el JWT para extraer los datos del usuario sin llamar al backend.
   */
  function guardarSesion(token: string, refreshToken?: string): void {
    accessToken.value = token
    if (import.meta.client) {
      localStorage.setItem('access_token', token)
      if (refreshToken) {
        localStorage.setItem('refresh_token', refreshToken)
      }
    }

    // Extraemos los datos del usuario directamente del payload JWT
    const payload = jwtDecode<JwtPayload>(token)
    const rol = (payload.roles?.[0] ?? payload.role ?? 'ROLE_HAIRDRESSER') as Rol

    usuario.value = {
      id: payload.sub,
      username: payload.sub,
      email: payload.email ?? '',
      rol,
    }
  }

  /**
   * Intenta restaurar la sesión desde localStorage al recargar la app.
   * Si el token guardado ha expirado, limpia la sesión.
   */
  function restaurarSesion(): void {
    if (!import.meta.client) return

    const tokenGuardado = localStorage.getItem('access_token')
    if (!tokenGuardado) return

    try {
      const payload = jwtDecode<JwtPayload>(tokenGuardado)
      if (payload.exp * 1000 > Date.now()) {
        guardarSesion(tokenGuardado, localStorage.getItem('refresh_token') ?? undefined)
      } else {
        // Token expirado — limpiamos para forzar login
        cerrarSesion()
      }
    } catch {
      cerrarSesion()
    }
  }

  /** Cierra la sesión y limpia todo el estado */
  function cerrarSesion(): void {
    accessToken.value = null
    usuario.value = null
    if (import.meta.client) {
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
    }
  }

  return {
    accessToken,
    usuario,
    isAuthenticated,
    isAdmin,
    isEmpleado,
    guardarSesion,
    restaurarSesion,
    cerrarSesion,
  }
})
