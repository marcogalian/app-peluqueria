export type CategoriaGasto =
  | 'LUZ'
  | 'AGUA'
  | 'ALQUILER'
  | 'SUMINISTROS_PRODUCTOS'
  | 'MARKETING'
  | 'SALARIOS'
  | 'OTROS'

export interface Gasto {
  id?: string
  concepto: string
  importe: number
  fecha: string       // YYYY-MM-DD
  categoria: CategoriaGasto
}

export const CATEGORIAS: CategoriaGasto[] = [
  'LUZ',
  'AGUA',
  'ALQUILER',
  'SUMINISTROS_PRODUCTOS',
  'MARKETING',
  'SALARIOS',
  'OTROS',
]

export const CATEGORIAS_LABEL: Record<CategoriaGasto, string> = {
  LUZ:                  'Luz',
  AGUA:                 'Agua',
  ALQUILER:             'Alquiler local',
  SUMINISTROS_PRODUCTOS:'Productos / suministros',
  MARKETING:            'Marketing',
  SALARIOS:             'Nóminas',
  OTROS:                'Otros',
}

export const CATEGORIAS_ICONO: Record<CategoriaGasto, string> = {
  LUZ:                  '⚡',
  AGUA:                 '💧',
  ALQUILER:             '🏠',
  SUMINISTROS_PRODUCTOS:'📦',
  MARKETING:            '📣',
  SALARIOS:             '👩',
  OTROS:                '📋',
}
