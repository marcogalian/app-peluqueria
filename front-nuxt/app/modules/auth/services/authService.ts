/**
 * Servicio de autenticación.
 * Encapsula las llamadas HTTP al backend — los componentes no saben que existe Axios (D de SOLID).
 */
import { api } from '~/infrastructure/http/api'
import type { LoginRequest, AuthResponse } from '../types/auth.types'

export const authService = {
  /**
   * Envía credenciales al backend y recibe los tokens JWT.
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    const { data } = await api.post<AuthResponse>('/auth/login', request)
    return data
  },

  /**
   * Solicita el envío de un email de recuperación de contraseña.
   */
  async solicitarResetPassword(email: string): Promise<void> {
    await api.post('/auth/forgot-password', { email })
  },

  /**
   * Establece una nueva contraseña usando el token recibido por email.
   */
  async resetPassword(token: string, nuevaPassword: string): Promise<void> {
    await api.post('/auth/reset-password', { token, nuevaPassword })
  },

  /**
   * Admin cambia la contraseña de un empleado por su ID de peluquero.
   */
  async cambiarPasswordEmpleado(peluqueroId: string, nuevaPassword: string): Promise<void> {
    await api.post(`/credenciales/empleado/${peluqueroId}/contrasena`, { nuevaPassword })
  },
}
