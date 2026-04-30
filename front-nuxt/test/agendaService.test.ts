import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('~/infrastructure/http/api', () => ({
  api: { get: vi.fn(), patch: vi.fn() },
}))

vi.mock('date-fns', () => ({
  format: vi.fn((date: Date, _fmt: string) => {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    const d = String(date.getDate()).padStart(2, '0')
    return `${y}-${m}-${d}`
  }),
}))

import { agendaService } from '~/modules/agenda/services/agendaService'
import { api } from '~/infrastructure/http/api'

describe('agendaService', () => {
  beforeEach(() => vi.clearAllMocks())

  it('getCitasDelDia() llama a /citas/agenda con la fecha formateada', async () => {
    const mockCitas = [{ id: '1', clienteNombre: 'Ana', hora: '10:00' }]
    vi.mocked(api.get).mockResolvedValue({ data: mockCitas })

    const fecha = new Date(2026, 3, 29) // 29 abr 2026
    const result = await agendaService.getCitasDelDia(fecha)

    expect(api.get).toHaveBeenCalledWith('/citas/agenda', { params: { fecha: '2026-04-29' } })
    expect(result).toEqual(mockCitas)
  })

  it('actualizarComentario() llama al endpoint PATCH con el comentario', async () => {
    vi.mocked(api.patch).mockResolvedValue({})

    await agendaService.actualizarComentario('cita-42', 'Color aplicado correctamente')

    expect(api.patch).toHaveBeenCalledWith('/citas/cita-42/comentarios', {
      comentarios: 'Color aplicado correctamente',
    })
  })

  it('cambiarEstado() llama al endpoint PATCH con el estado', async () => {
    vi.mocked(api.patch).mockResolvedValue({})

    await agendaService.cambiarEstado('cita-42', 'EN_CURSO')

    expect(api.patch).toHaveBeenCalledWith('/citas/cita-42/estado', { estado: 'EN_CURSO' })
  })

  it('cancelar() envía el motivo de cancelación al endpoint correcto', async () => {
    vi.mocked(api.patch).mockResolvedValue({})

    await agendaService.cancelar('cita-42', 'Cliente no se presentó')

    expect(api.patch).toHaveBeenCalledWith('/citas/cita-42/cancelar', {
      motivoCancelacion: 'Cliente no se presentó',
    })
  })
})
