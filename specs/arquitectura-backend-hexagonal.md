# Arquitectura backend

El backend sigue una combinacion pragmatica de Vertical Slicing, Screaming Architecture y Hexagonal implicita.

## Como leer un modulo

Cada carpeta principal representa una parte del negocio:

- `clientes`
- `citas`
- `productos`
- `servicios`
- `peluqueros`
- `ausencias`
- `ofertas`
- `finanzas`
- `fotos`
- `chat`
- `chatbot`
- `configuracion`
- `security`

Dentro de cada modulo, la lectura es siempre la misma:

```text
modulo/
  application/      Casos de uso: lo que el sistema hace
  domain/           Modelo de negocio y contratos propios del negocio
  infrastructure/   Web, base de datos, email, Gemini, seguridad tecnica
```

## Regla mental

- Si quieres saber que puede hacer el sistema, entra en `application`.
- Si quieres saber que conceptos existen, entra en `domain`.
- Si quieres saber como se expone o se guarda, entra en `infrastructure`.

## Nombres

Se elimino la nomenclatura ruidosa:

- Ya no se usan carpetas `domain/port/in`, `domain/port/out`.
- Ya no se usan carpetas `infrastructure/in`, `infrastructure/out`.
- Ya no se usan clases `*Port`, `*Adapter` ni `*UseCase`.
- Las implementaciones de persistencia llevan nombre tecnologico: `PostgresClienteRepository`, `PostgresCitaRepository`, etc.
- Los contratos de negocio viven en `domain`: `ClienteRepository`, `CitaRepository`, `ProductoRepository`.
- Los casos de uso viven en `application`: `GestionarAgenda`, `GestionarInventario`, `ResponderConsultasGestion`.

## Ejemplo

```text
clientes/
  application/
    RegistrarCliente.java
    RegistrarClienteEnSistema.java
    ConsultarClientes.java
    ActualizarCliente.java

  domain/
    Cliente.java
    ClienteRepository.java
    Genero.java
    HistorialClinicoDTO.java

  infrastructure/
    web/
      ClienteController.java
    persistence/
      ClienteEntity.java
      ClienteMapper.java
      JpaClienteRepository.java
      PostgresClienteRepository.java
```

## Hexagonal, sin ruido

La arquitectura hexagonal sigue estando, pero sin nombrarla en cada archivo:

- `application` depende de `domain`.
- `domain` no depende de Spring ni de JPA.
- `infrastructure` implementa detalles externos: controladores, repositorios JPA, entidades, mappers, email, Gemini.

Asi el proyecto grita negocio primero y tecnologia despues.
