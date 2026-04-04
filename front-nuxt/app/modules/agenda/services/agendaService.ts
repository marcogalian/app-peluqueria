/**
 * Servicio de agenda — encapsula las llamadas HTTP del módulo.
 */
import { api } from '~/infrastructure/http/api'
import type { CitaAgenda } from '../types/agenda.types'
import { format } from 'date-fns'

export const agendaService = {
  /** Obtiene las citas del día indicado para el empleado autenticado */
  async getCitasDelDia(fecha: Date): Promise<CitaAgenda[]> {
    const fechaStr = format(fecha, 'yyyy-MM-dd')
    const { data } = await api.get<CitaAgenda[]>('/citas/agenda', {
      params: { fecha: fechaStr },
    })
    return data
  },

  /** Actualiza el comentario de una cita (solo el empleado asignado) */
  async actualizarComentario(citaId: string, comentarios: string): Promise<void> {
    await api.patch(`/citas/${citaId}/comentarios`, { comentarios })
  },

  /** Cambia el estado de una cita (ej. marcar como EN_CURSO) */
  async cambiarEstado(citaId: string, estado: string): Promise<void> {
    await api.patch(`/citas/${citaId}/estado`, { estado })
  },

  /** Cancela una cita con un motivo obligatorio */
  async cancelar(citaId: string, motivo: string): Promise<void> {
    await api.patch(`/citas/${citaId}/cancelar`, { motivoCancelacion: motivo })
  },
}
