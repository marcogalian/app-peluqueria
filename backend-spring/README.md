# API de Gestión de Peluquería

Backend Spring Boot 3 para la gestión integral de una peluquería. Expone una API REST con autenticación JWT, módulos de negocio separados por dominio y soporte para chat vía WebSocket/STOMP, notificaciones y seed de datos demo.

## Stack

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA + Hibernate
- PostgreSQL
- WebSocket STOMP
- Mailtrap / Twilio
- MapStruct + Lombok

## Módulos principales

- `security`: login, JWT y autorización por rol
- `clientes`: CRUD, historial y consentimiento de fotos
- `citas`: agenda, validación de solapes y cancelaciones
- `peluqueros`: empleados, bajas, reactivación y foto
- `ausencias`: solicitudes, aprobación y cancelación
- `servicios`: catálogo con categoría, precio y duración
- `productos`: inventario y control de stock
- `ofertas`: promociones y días especiales
- `finanzas`: dashboard y resultados
- `chat`: mensajería interna con STOMP
- `shared/notification`: notificaciones transversales

## Ejecución local

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw test
```

Los tests usan H2 en memoria para no depender de una base PostgreSQL local.
