/**
 * Tipos genéricos para respuestas de la API.
 * Todos los servicios usan estos contratos para garantizar consistencia (L de SOLID).
 */

/** Respuesta paginada — la API devuelve este formato en listados */
export interface PaginatedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number       // página actual (0-based)
  size: number
  first: boolean
  last: boolean
}

/** Respuesta simple de error del backend */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
}

/** Estado de carga — todos los composables siguen este contrato */
export interface AsyncState<T> {
  data: T | null
  loading: boolean
  error: string | null
}
