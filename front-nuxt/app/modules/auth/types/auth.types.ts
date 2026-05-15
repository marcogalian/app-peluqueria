/** Roles que puede tener un usuario en el sistema */
export type Rol = 'ROLE_ADMIN' | 'ROLE_HAIRDRESSER'

/** Datos del usuario autenticado */
export interface Usuario {
  id: string
  username: string
  email: string
  rol: Rol
}

/** Petición de login al backend */
export interface LoginRequest {
  username: string
  password: string
  rememberMe?: boolean
}

/** Respuesta del backend tras login exitoso — coincide con AuthResponse.java del backend */
export interface AuthResponse {
  token: string        // el backend usa "token", no "accessToken"
  refreshToken?: string
}

export interface CambiarPasswordEmpleadoRequest {
  nuevaPassword: string
  repetirPassword: string
}
