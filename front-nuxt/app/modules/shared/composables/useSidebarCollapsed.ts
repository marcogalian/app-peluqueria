/**
 * Estado compartido del sidebar colapsado.
 *
 * Persistido en localStorage para que el usuario mantenga su preferencia
 * entre sesiones. Lo usa AppSidebar (para cambiar ancho/labels) y
 * AppHeader (para el boton de toggle).
 */
import { useStorage } from '@vueuse/core'

const collapsed = useStorage<boolean>('sidebar:collapsed', false)

export function useSidebarCollapsed() {
  function toggle() {
    collapsed.value = !collapsed.value
  }

  return { collapsed, toggle }
}
