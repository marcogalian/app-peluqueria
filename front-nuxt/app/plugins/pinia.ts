/**
 * Plugin de Pinia para Nuxt 4.
 * @pinia/nuxt 0.10.x no soporta Nuxt 4, así que registramos Pinia manualmente.
 * defineStore y storeToRefs se auto-importan via nuxt.config.ts imports.presets
 */
import { createPinia } from 'pinia'

export default defineNuxtPlugin((nuxtApp) => {
  const pinia = createPinia()
  nuxtApp.vueApp.use(pinia)

  // Exponemos pinia para SSR (hydration) si en algún momento activamos SSR
  return { provide: { pinia } }
})
