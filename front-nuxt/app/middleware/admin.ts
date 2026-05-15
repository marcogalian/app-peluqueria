/**
 * Middleware de autorización para rutas /admin/*.
 * Si el usuario no es admin, lo redirige a su agenda.
 */
export default defineNuxtRouteMiddleware(() => {
  const authStore = useAuthStore()
  authStore.restaurarSesion()

  if (!authStore.isAdmin) {
    return navigateTo('/agenda')
  }
})
