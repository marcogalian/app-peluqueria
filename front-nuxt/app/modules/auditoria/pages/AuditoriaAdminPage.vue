<script setup lang="ts">
import { Activity, RefreshCw, ShieldCheck } from 'lucide-vue-next'
import { api } from '~/infrastructure/http/api'

interface RegistroActividad {
  id: string
  fechaHora: string
  usuario: string
  rol: string
  accion: 'CREACION' | 'MODIFICACION' | 'ELIMINACION' | string
  modulo: string
  detalle?: string | null
  entidadId?: string | null
  metodoHttp: string
  ruta: string
  estadoHttp: number
}

const registros = ref<RegistroActividad[]>([])
const cargando = ref(true)

const acciones = computed(() => ({
  total: registros.value.length,
  creaciones: registros.value.filter((r) => r.accion === 'CREACION').length,
  modificaciones: registros.value.filter((r) => r.accion === 'MODIFICACION').length,
  eliminaciones: registros.value.filter((r) => r.accion === 'ELIMINACION').length,
}))

async function cargarAuditoria() {
  cargando.value = true
  try {
    const { data } = await api.get('/v1/auditoria', { params: { limite: 100 } })
    registros.value = data
  } finally {
    cargando.value = false
  }
}

function formatearFecha(valor: string) {
  return new Intl.DateTimeFormat('es-ES', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(valor))
}

function rolVisible(rol: string) {
  if (rol === 'ROLE_ADMIN') return 'Admin'
  if (rol === 'ROLE_HAIRDRESSER') return 'Peluquero/a'
  return rol
}

function estiloAccion(accion: string) {
  if (accion === 'CREACION') return 'bg-emerald-50 text-emerald-700 border-emerald-100'
  if (accion === 'MODIFICACION') return 'bg-blue-50 text-blue-700 border-blue-100'
  if (accion === 'ELIMINACION') return 'bg-red-50 text-red-700 border-red-100'
  return 'bg-surface-container text-on-surface-variant border-outline-variant/40'
}

onMounted(cargarAuditoria)
</script>

<template>
  <main class="space-y-8">
    <header class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div>
        <p class="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant/70">
          Seguridad
        </p>
        <h2 class="mt-1 text-3xl font-extrabold tracking-tight text-primary">
          Registro de actividad
        </h2>
        <p class="mt-1 text-sm text-on-surface-variant">
          Acciones creadas, modificadas o eliminadas por el equipo.
        </p>
      </div>

      <button class="btn-secondary inline-flex items-center gap-2" :disabled="cargando" @click="cargarAuditoria">
        <RefreshCw :class="['h-4 w-4', cargando ? 'animate-spin' : '']" />
        Actualizar
      </button>
    </header>

    <section class="grid gap-4 md:grid-cols-4">
      <article class="rounded-lg border border-outline-variant/30 bg-white p-5 shadow-card">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70">Total</p>
        <p class="mt-2 text-3xl font-extrabold text-primary">{{ acciones.total }}</p>
      </article>
      <article class="rounded-lg border border-outline-variant/30 bg-white p-5 shadow-card">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70">Creaciones</p>
        <p class="mt-2 text-3xl font-extrabold text-emerald-600">{{ acciones.creaciones }}</p>
      </article>
      <article class="rounded-lg border border-outline-variant/30 bg-white p-5 shadow-card">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70">Cambios</p>
        <p class="mt-2 text-3xl font-extrabold text-blue-600">{{ acciones.modificaciones }}</p>
      </article>
      <article class="rounded-lg border border-outline-variant/30 bg-white p-5 shadow-card">
        <p class="text-[10px] font-bold uppercase tracking-widest text-on-surface-variant/70">Eliminaciones</p>
        <p class="mt-2 text-3xl font-extrabold text-red-500">{{ acciones.eliminaciones }}</p>
      </article>
    </section>

    <section class="overflow-hidden rounded-xl border border-outline-variant/30 bg-white shadow-card">
      <div class="flex items-center gap-3 border-b border-outline-variant/20 px-6 py-5">
        <div class="flex h-10 w-10 items-center justify-center rounded-full bg-primary/10 text-primary">
          <ShieldCheck class="h-5 w-5" />
        </div>
        <div>
          <h3 class="font-bold text-primary">Actividad reciente</h3>
          <p class="text-xs text-on-surface-variant">Últimos 100 movimientos auditados.</p>
        </div>
      </div>

      <div v-if="cargando" class="px-6 py-12 text-center text-sm text-on-surface-variant">
        Cargando actividad...
      </div>

      <div v-else-if="registros.length === 0" class="px-6 py-12 text-center">
        <Activity class="mx-auto mb-3 h-8 w-8 text-on-surface-variant/50" />
        <p class="font-bold text-primary">Todavía no hay actividad registrada</p>
        <p class="mt-1 text-sm text-on-surface-variant">Cuando alguien cree, modifique o elimine algo aparecerá aquí.</p>
      </div>

      <div v-else class="divide-y divide-outline-variant/20">
        <article
          v-for="registro in registros"
          :key="registro.id"
          class="grid gap-3 px-6 py-4 md:grid-cols-[8rem_1fr_auto] md:items-center"
        >
          <div class="text-sm font-bold text-primary">{{ formatearFecha(registro.fechaHora) }}</div>

          <div class="min-w-0">
            <div class="flex flex-wrap items-center gap-2">
              <span :class="['rounded-full border px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider', estiloAccion(registro.accion)]">
                {{ registro.accion }}
              </span>
              <span class="text-sm font-bold text-on-surface">
                {{ registro.usuario }}
              </span>
              <span class="text-xs text-on-surface-variant">
                {{ rolVisible(registro.rol) }} · {{ registro.modulo }}
              </span>
            </div>
            <p class="mt-1 text-sm font-semibold text-on-surface">
              {{ registro.detalle || `${registro.modulo} actualizado` }}
            </p>
            <p class="mt-1 truncate text-[11px] text-on-surface-variant">
              {{ registro.metodoHttp }} {{ registro.ruta }}
            </p>
          </div>

          <span class="w-fit rounded-full bg-surface-container px-2.5 py-1 text-[11px] font-bold text-on-surface-variant">
            {{ registro.modulo }}
          </span>
        </article>
      </div>
    </section>
  </main>
</template>
