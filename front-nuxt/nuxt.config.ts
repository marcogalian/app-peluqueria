/**
 * Configuración Nuxt 4.
 * En Nuxt 4 el directorio fuente es app/ — todos los paths son relativos a él.
 */
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },

  modules: [
    '@nuxtjs/tailwindcss',
    '@vueuse/nuxt',
    // Nota: @pinia/nuxt 0.10.x no soporta Nuxt 4 — se usa plugin manual en app/plugins/pinia.ts
  ],

  // Auto-import de componentes de módulos de negocio (screaming architecture)
  // En Nuxt 4 estos paths son relativos al srcDir (app/)
  components: [
    { path: './modules', pathPrefix: false },
    { path: './components', pathPrefix: false },
  ],

  // Auto-import de composables y stores de cada módulo de negocio
  imports: {
    presets: [
      { from: 'pinia', imports: ['defineStore', 'storeToRefs', 'acceptHMRUpdate'] },
    ],
    dirs: [
      'modules/*/composables',
      'modules/*/store',
      'shared/composables',
      'shared/store',
      'infrastructure/**',
    ],
  },

  // Variables de entorno accesibles en el cliente
  runtimeConfig: {
    public: {
      apiBase:     process.env.NUXT_PUBLIC_API_BASE     || 'http://localhost:8080/api',
      wsBase:      process.env.NUXT_PUBLIC_WS_BASE      || 'http://localhost:8080/chat-websocket',
      uploadsBase: process.env.NUXT_PUBLIC_UPLOADS_BASE || 'http://localhost:8080/uploads',
    },
  },

  // CSS global con el design system
  css: ['~/assets/css/main.css'],

  // En desarrollo queremos usar siempre el 3000 y fallar si está ocupado,
  // para no acabar en 3001 sin darnos cuenta.
  vite: {
    server: {
      host: 'localhost',
      port: 3000,
      strictPort: true,
    },
  },

  // Alias útiles para imports
  alias: {
    '~modules': '/app/modules',
    '~shared':  '/app/shared',
    '~infra':   '/app/infrastructure',
  },

  // Configuración de la app sin transiciones de página para evitar
  // el bug de "pantalla en blanco" al navegar entre layouts en Nuxt
  app: {
    head: {
      title: 'Peluquería Isabella',
      meta: [
        { name: 'description', content: 'Sistema de gestión de peluquería' },
        { name: 'viewport',    content: 'width=device-width, initial-scale=1' },
      ],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Manrope:wght@300;400;500;600;700;800&display=swap',
        },
      ],
    },
  },

  routeRules: {
    '/_nuxt/**': { headers: { 'cache-control': 'public, max-age=31536000, immutable' } },
  },

  ssr: false,

  typescript: {
    strict: true,
    typeCheck: false,
  },
})
