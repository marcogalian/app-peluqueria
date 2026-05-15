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
  authorities?: Array<string | { authority?: string }>
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

  function normalizarRol(valor?: string): Rol | null {
    if (valor === 'ROLE_ADMIN' || valor === 'ADMIN') return 'ROLE_ADMIN'
    if (valor === 'ROLE_HAIRDRESSER' || valor === 'HAIRDRESSER') return 'ROLE_HAIRDRESSER'
    return null
  }

  function extraerRol(payload: JwtPayload): Rol | null {
    const candidates = [
      ...(payload.roles ?? []),
      payload.role,
      ...(payload.authorities ?? []).map((authority) =>
        typeof authority === 'string' ? authority : authority.authority,
      ),
    ]

    return candidates.map(normalizarRol).find((rol): rol is Rol => Boolean(rol)) ?? null
  }

  function resolverRolLegacy(payload: JwtPayload, usuarioPersistido: Usuario | null): Rol | null {
    const rolPersistido = usuarioPersistido?.username === payload.sub ? usuarioPersistido.rol : null
    if (rolPersistido) return rolPersistido

    return payload.sub === 'admin' ? 'ROLE_ADMIN' : null
  }

  function leerUsuarioPersistido(): Usuario | null {
    if (!import.meta.client) return null
    try {
      const raw = localStorage.getItem('auth_user')
      if (!raw) return null
      const parsed = JSON.parse(raw) as Usuario
      return normalizarRol(parsed.rol) ? parsed : null
    } catch {
      return null
    }
  }

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
    const usuarioPersistido = leerUsuarioPersistido()
    const rol = extraerRol(payload) ?? resolverRolLegacy(payload, usuarioPersistido)

    if (rol) {
      usuario.value = {
        id: payload.sub,
        username: payload.sub,
        email: payload.email ?? usuarioPersistido?.email ?? '',
        rol,
      }
      if (import.meta.client) {
        localStorage.setItem('auth_user', JSON.stringify(usuario.value))
      }
    } else {
      usuario.value = null
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
      localStorage.removeItem('auth_user')
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
