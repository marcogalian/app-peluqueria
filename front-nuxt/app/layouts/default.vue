<template>
  <!--
    Layout principal autenticado.
    Sidebar fijo izquierda (gris claro) + contenido derecha (blanco/surface)
  -->
  <div class="flex h-screen overflow-hidden bg-surface-container-low">
    <AppSidebar />
    <div class="flex flex-col flex-1 overflow-hidden bg-surface rounded-tl-[28px]">
      <AppHeader />
      <main class="flex-1 overflow-y-auto p-8">
        <slot />
      </main>
    </div>
  </div>
</template>
