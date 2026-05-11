# Documentacion del proyecto

Esta carpeta contiene la documentacion principal de Peluqueria Isabella.

## Lectura recomendada

1. [Guia funcional](guia-funcional.md)
2. [Arranque y configuracion](arranque-y-configuracion.md)
3. [Guia tecnica](guia-tecnica.md)
4. [Pruebas y validacion](pruebas-y-validacion.md)
5. [Javadoc y documentacion de codigo](javadoc.md)

## Documentos relacionados

La carpeta `specs/` contiene documentos de arquitectura y decisiones:

- `specs/arquitectura-backend-hexagonal.md`
- `specs/arquitectura-frontend-vertical-slicing.md`
- `specs/memoria_progreso_2026-05-09.md`
- `specs/venta_agrupada_design.md`

## Para presentacion

Para explicar el proyecto, el orden mas natural es:

1. Problema: una peluqueria necesita centralizar citas, clientes, ventas y empleados.
2. Solucion: panel web con roles admin/empleado.
3. Arquitectura: backend modular por dominio y frontend por vertical slicing.
4. Demo: login admin, dashboard, agenda, clientes, inventario, ausencias y asistente.
5. Calidad: tests, Swagger, Javadoc, Docker y documentacion.
