/**
 * Middleware global de autenticación.
 * Se ejecuta antes de cada navegación.
 * Las páginas públicas usan definePageMeta({ auth: false }) para saltárselo.
 */
export default defineNuxtRouteMiddleware((to) => {
  if (to.meta.auth === false) return

  const authStore = useAuthStore()
  authStore.restaurarSesion()

  if (!authStore.isAuthenticated) {
    return navigateTo({ path: '/login', query: { redirect: to.fullPath } })
  }
})
