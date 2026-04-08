import { api } from '~/infrastructure/http/api'
import type { ResumenDia, NuevaCitaPayload } from '../types/calendario.types'
import { format } from 'date-fns'

export const calendarioService = {
  /** Obtiene el resumen de citas de todos los días del mes indicado */
  async getResumenMes(anio: number, mes: number): Promise<ResumenDia[]> {
    const { data } = await api.get<ResumenDia[]>('/citas/resumen-mes', {
      params: { anio, mes },
    })
    return data
  },

  /** Crea una nueva cita desde el popover del calendario */
  async crearCita(payload: NuevaCitaPayload): Promise<void> {
    await api.post('/citas', payload)
  },
}
