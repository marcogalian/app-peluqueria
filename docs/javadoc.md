# Javadoc y documentacion de codigo

## Objetivo

El Javadoc debe explicar las piezas importantes del backend sin llenar el codigo de comentarios obvios.

Se recomienda documentar:

- Casos de uso en `application`.
- Contratos de dominio en `domain`.
- Controladores REST principales.
- Integraciones externas.
- Reglas de negocio delicadas.

No hace falta documentar getters, setters, entidades JPA simples o DTOs evidentes.

## Generar Javadoc

Desde `backend-spring`:

```bash
./mvnw javadoc:javadoc
```

En Windows PowerShell:

```powershell
.\mvnw.cmd javadoc:javadoc
```

Salida generada:

```text
backend-spring/target/site/apidocs/index.html
```

## Criterio de escritura

Un buen Javadoc debe responder:

- Que responsabilidad tiene esta clase.
- Que regla de negocio protege.
- Que dependencia externa toca, si toca alguna.
- Que permisos o roles aplica.
- Que errores funcionales pueden ocurrir.

Ejemplo:

```java
/**
 * Caso de uso principal para gestionar la agenda del salon.
 *
 * Centraliza altas, modificaciones, cancelaciones y finalizacion de citas.
 * Tambien protege la regla de negocio que impide solapar citas del mismo
 * empleado en el mismo tramo horario.
 */
public class GestionarAgenda {
}
```

## Clases prioritarias

### Citas

- `GestionarAgenda`
- `GenerarTicketCita`
- `EnviarRecordatoriosCitas`
- `CitaController`
- `CitaRepository`
- `NotificadorCitas`

### Clientes

- `RegistrarClienteEnSistema`
- `ConsultarClientes`
- `ActualizarCliente`
- `ClienteController`
- `ClienteRepository`

### Productos e inventario

- `GestionarInventario`
- `DetectarStockBajoProgramado`
- `ProductoController`
- `ProductoRepository`

### Ausencias

- `GestionarAusencias`
- `GestionarDiasBloqueados`
- `AusenciaController`
- `DiaBloqueadoController`
- `AusenciaRepository`
- `DiaBloqueadoRepository`

### Chat y asistente

- `ResponderConsultasGestion`
- `ConsultasGestionPeluqueriaPostgres`
- `GeminiModeloLenguaje`
- `ArchivoContextoNegocio`
- `MensajesController`

### Seguridad

- `AutenticarUsuario`
- `GestionarSesion`
- `JwtService`
- `SecurityConfig`

## Convenciones

- Usar espanol claro.
- No usar frases vacias como "Clase que gestiona X" si no aporta nada.
- Explicar reglas de negocio, no sintaxis.
- Si una clase existe por arquitectura, explicar donde encaja.
- Si hay una limitacion conocida, documentarla.

## Relacion con Swagger

Javadoc documenta codigo Java.

Swagger documenta API REST para usuarios tecnicos externos.

Ambas cosas se complementan:

- Javadoc: para entender el backend por dentro.
- Swagger: para probar endpoints y ver contratos HTTP.
