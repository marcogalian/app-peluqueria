# Frontend Peluquería

Frontend del sistema de gestión de peluquería construido con Nuxt 4, Vue 3, TypeScript, Pinia y Tailwind CSS.

## Stack

- Nuxt 4
- Vue 3
- TypeScript
- Pinia
- Tailwind CSS
- Axios
- Vitest + Vue Test Utils

## Estructura

- `app/pages`: rutas y pantallas
- `app/components`: layout y componentes reutilizables
- `app/modules`: código organizado por dominio
- `app/middleware`: protección por autenticación y rol
- `app/infrastructure/http`: cliente API centralizado

## Comandos

```bash
npm install
npm run dev
npm run build
npm test
```

## Integración con backend

La URL base de la API se resuelve desde runtime config de Nuxt y, en local, usa `http://localhost:8080/api`.
