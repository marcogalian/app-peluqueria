import { defineConfig } from 'vitest/config'
import { resolve } from 'path'

export default defineConfig({
  plugins: [
    {
      name: 'nuxt-client-stub',
      enforce: 'pre',
      transform(code: string, id: string) {
        if (id.includes('node_modules')) return
        return { code: code.replace(/import\.meta\.client/g, 'true'), map: null }
      },
    },
  ],
  test: {
    environment: 'happy-dom',
    globals: true,
    setupFiles: ['./test/setup.ts'],
  },
  resolve: {
    alias: {
      '~': resolve(__dirname, 'app'),
    },
  },
})
