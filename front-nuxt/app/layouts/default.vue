<template>
  <!--
    Layout principal autenticado.
    Sidebar fijo izquierda (gris claro) + contenido derecha (blanco/surface)
  -->
  <div class="flex h-screen overflow-hidden bg-surface">
    <AppSidebar />
    <div class="flex flex-col flex-1 overflow-hidden">
      <AppHeader />
      <main class="flex-1 overflow-y-auto p-8 bg-surface">
        <slot />
      </main>
    </div>
  </div>
</template>
