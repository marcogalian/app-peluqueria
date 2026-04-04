import type { Config } from 'tailwindcss'

/**
 * Design system exacto del diseño Atelier Sapphire.
 * Colores extraídos directamente del stitch/code.html.
 * Fuente: Manrope (igual que el diseño de referencia).
 */
export default {
  content: [
    './app/**/*.{vue,ts,js}',
  ],
  theme: {
    extend: {
      colors: {
        // ── Colores primarios (azul marino profesional) ──────
        primary: {
          DEFAULT: '#002045',      // azul marino más oscuro — textos de título
          container: '#1a365d',    // azul marino medio — botones principales, borde activo sidebar
          fixed:     '#d6e3ff',    // azul muy claro — fondos de iconos
          'fixed-dim': '#adc7f7',  // azul claro
        },
        // ── Superficies y fondos ─────────────────────────────
        surface: {
          DEFAULT:  '#faf9fd',     // fondo de página
          dim:      '#dad9dd',
          bright:   '#faf9fd',
          container: {
            lowest:  '#ffffff',    // blanco puro — cards y drawers
            low:     '#f4f3f7',    // gris muy claro — sidebar, fondos de sección
            DEFAULT: '#efedf1',    // gris claro — chips, tags
            high:    '#e9e7eb',    // gris medio claro
            highest: '#e3e2e6',    // gris medio — inputs
          },
        },
        // ── Texto ────────────────────────────────────────────
        'on-surface':         '#1a1c1e',   // texto principal
        'on-surface-variant': '#43474e',   // texto secundario
        // ── Bordes ───────────────────────────────────────────
        'outline':         '#74777f',
        'outline-variant': '#c4c6cf',
        // ── Secundario ───────────────────────────────────────
        secondary: {
          DEFAULT:   '#555f70',
          container: '#d9e3f8',
          'fixed':   '#d9e3f8',
        },
        // ── Terciario (dorado/ámbar para VIP, ofertas) ───────
        tertiary: {
          DEFAULT:   '#321b00',
          container: '#4f2e00',
          fixed:     '#ffddba',
          'fixed-dim': '#f2bc82',
        },
        // ── Estados ──────────────────────────────────────────
        error: '#ba1a1a',
        'error-container': '#ffdad6',
        success: '#1d7240',
      },

      // Manrope — misma fuente que el diseño de referencia
      fontFamily: {
        sans:     ['Manrope', 'system-ui', 'sans-serif'],
        headline: ['Manrope', 'system-ui', 'sans-serif'],
      },

      fontSize: {
        '2xs': ['0.625rem', { lineHeight: '0.875rem' }],
      },

      // Border radius más redondeados como en el diseño
      borderRadius: {
        DEFAULT: '0.5rem',
        lg:      '1rem',
        xl:      '1.5rem',
        '2xl':   '2rem',
        full:    '9999px',
      },

      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.07), 0 1px 2px rgba(0,0,0,0.05)',
        'card-md': '0 4px 12px rgba(0,0,0,0.08)',
        'card-lg': '0 10px 30px rgba(0,0,0,0.1)',
        'card-xl': '0 20px 40px rgba(0,0,0,0.12)',
      },

      width: { sidebar: '256px' },    // w-64 del diseño original
    },
  },
  plugins: [],
} satisfies Config
