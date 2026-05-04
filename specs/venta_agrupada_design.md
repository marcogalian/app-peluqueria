# Venta agrupada

## Objetivo

Guardar una compra de productos como una sola operacion de venta, con sus lineas, total, metodo de pago y usuario que la realiza.

## Backend

- Endpoint nuevo: `POST /api/v1/productos/ventas`
- Acceso: `ROLE_ADMIN` y `ROLE_HAIRDRESSER`.
- Entrada:
  - `metodoPago`: `EFECTIVO`, `TARJETA`, `BIZUM` u `OTRO`.
  - `lineas`: lista de `{ productoId, cantidad }`.
- Salida:
  - id de venta agrupada
  - numero legible
  - vendedor
  - lineas vendidas
  - total
  - resumen de ventas

## Persistencia

- `ventas`: cabecera de la operacion.
- `ventas_producto`: se conserva como tabla de lineas, ahora enlazada opcionalmente a `ventas`.
- Las ventas antiguas sin cabecera siguen participando en estadisticas.

## Seguridad

- El backend toma el vendedor desde el token autenticado, no desde el cliente.
- Las cantidades se validan en servidor.
- Se comprueba stock dentro de una transaccion antes de guardar.
- La respuesta no expone password, tokens ni datos sensibles.
