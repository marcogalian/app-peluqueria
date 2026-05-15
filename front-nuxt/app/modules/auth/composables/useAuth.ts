/**
 * Composable de autenticación.
 * Orquesta authService + authStore para que los componentes
 * no dependan directamente de ninguno de los dos.
 */
import axios from 'axios'
import { authService } from '../services/authService'
import type { LoginRequest } from '../types/auth.types'

export function useAuth() {
  const store  = useAuthStore()
  const router = useRouter()

  const cargando = ref(false)
  const error    = ref('')

  async function login(request: LoginRequest) {
    cargando.value = true
    error.value    = ''
    try {
      const respuesta = await authService.login(request)
      store.guardarSesion(respuesta.token, respuesta.refreshToken)
      // Redirige según rol
      await router.push(store.isAdmin ? '/admin/dashboard' : '/agenda')
    } catch (e: unknown) {
      const es401 = axios.isAxiosError(e) && e.response?.status === 401
      error.value = es401
        ? 'Usuario o contraseña incorrectos.'
        : 'No se pudo conectar con el servidor.'
    } finally {
      cargando.value = false
    }
  }

  function logout() {
    store.cerrarSesion()
    router.push('/login')
  }

  return { cargando, error, login, logout, store }
}
