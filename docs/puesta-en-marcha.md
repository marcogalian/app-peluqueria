# Puesta en marcha

## Requisitos

Para ejecutar todo con Docker:

- Docker Desktop.
- Puertos libres: `3000`, `8080`, `5432`.

Para desarrollo local:

- Java 21.
- Node.js 22.
- npm.

## Variables de entorno

El archivo de entorno vive en:

```text
docker/.env
```

Existe un ejemplo en:

```text
docker/.env.example
```

Variables principales. El repositorio solo incluye nombres de variables y placeholders, no credenciales reales.

| Variable | Uso |
|---|---|
| `DB_USER` | Usuario PostgreSQL |
| `DB_PASSWORD` | Contrasena PostgreSQL |
| `DB_NAME` | Nombre de base de datos |
| `JWT_SECRET_KEY` | Clave para firmar JWT |
| `CHAT_AES_KEY` | Clave AES para chat interno |
| `MAILTRAP_USERNAME` | Usuario Mailtrap |
| `MAILTRAP_PASSWORD` | Contrasena Mailtrap |
| `APP_ADMIN_EMAIL` | Email del administrador para recuperacion de contrasena |
| `FRONTEND_BASE_URL` | URL publica del frontend para enlaces de recuperacion |
| `AI_PROVIDER` | Proveedor IA, recomendado `spring-ai` |
| `GEMINI_API_KEY` | Clave del asistente IA con Gemini |
| `OPENAI_API_KEY` | Clave del asistente IA con OpenAI |
| `OPENAI_MODEL` | Modelo OpenAI para Spring AI, por defecto `gpt-4.1-nano` |
| `OPENAI_MAX_TOKENS` | Limite de respuesta del asistente, recomendado `200` |
| `SPRING_AI_MODEL_CHAT` | Activacion de Spring AI. Usar `none` sin clave externa u `openai` con OpenAI |
| `APP_DEMO_PASSWORD` | Contrasena local para usuarios demo |

## Arranque con Docker

Desde la raiz del repo:

```bash
cd docker
docker compose up --build -d
```

Comprobar estado:

```bash
docker compose ps
```

Ver logs del backend:

```bash
docker compose logs api --tail 120
```

Ver logs del frontend:

```bash
docker compose logs front --tail 120
```

Parar servicios:

```bash
docker compose down
```

## URLs

| Servicio | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| PostgreSQL | localhost:5432 |

## Arranque backend local

```bash
cd backend-spring
./mvnw spring-boot:run
```

En Windows PowerShell:

```powershell
cd backend-spring
.\mvnw.cmd spring-boot:run
```

## Arranque frontend local

```bash
cd front-nuxt
npm install
npm run dev
```

## Tests

Backend:

```bash
cd backend-spring
./mvnw test
```

En Windows PowerShell:

```powershell
cd backend-spring
.\mvnw.cmd test
```

El backend usa JUnit 5 y Mockito para probar casos de uso, reglas de negocio y permisos sin levantar dependencias externas innecesarias.

Pruebas principales del backend:

| Test | Que comprueba |
|---|---|
| `PeluqueriaApiApplicationTests` | Que la aplicacion Spring arranca completa en entorno de test |
| `GestionarAgendaTest` | Que no se permiten citas solapadas y si se permiten citas consecutivas |
| `GestionarAusenciasTest` | Que las solicitudes concurrentes de vacaciones se serializan con semaforo |
| `AutenticarUsuarioTest` | Que el registro asigna rol seguro, hashea password y devuelve tokens |
| `AESCryptoUtilTest` | Que el cifrado AES descifra correctamente y usa IV aleatorio |
| `ResponderConsultasGestionTest` | Que el asistente IA respeta permisos, function calling y respuestas directas |
| `ChatFunctionExecutorTest` | Que las funciones del asistente devuelven datos correctos y bloquean funciones admin a empleados |

Frontend:

```bash
cd front-nuxt
npm test
```

El frontend usa Vitest para validar stores, servicios y composables, especialmente autenticacion, llamadas HTTP y comportamiento del asistente.

Pruebas principales del frontend:

| Test | Que comprueba |
|---|---|
| `auth.store.test.ts` | Sesion, token, restauracion, cierre y roles |
| `authService.test.ts` | Endpoints de login y recuperacion de contrasena |
| `useChatbot.test.ts` | Flujo del asistente, historial, errores, loading, sugerencias y limpiar chat |

## Usuarios demo

El backend crea usuarios demo en el primer arranque. Sus nombres de usuario son visibles en el seed, pero la contrasena se lee desde `APP_DEMO_PASSWORD` y no se documenta en el repositorio publico.

Usuarios generados en una base nueva:

| Usuario | Email | Rol |
|---|---|---|
| `admin` | `admin@email.com` | Administrador |
| `carmen` | `carmen@email.com` | Empleada |
| `lucia` | `lucia@email.com` | Empleada |
| `sofia` | `sofia@email.com` | Empleada |

Los empleados no tienen recuperacion de contrasena desde login. Si pierden acceso, el administrador cambia su contrasena desde `/admin/contrasenas`.

## Datos iniciales

Al arrancar por primera vez, el backend crea:

- Admin.
- Empleadas demo.
- Servicios.
- Productos.
- Ofertas.
- Clientes demo.

Si `randomuser.me` no esta disponible, usa clientes locales de respaldo.

## Problemas frecuentes

### El email de recuperacion no llega

Revisar credenciales Mailtrap:

```bash
cd docker
docker compose logs api --tail 160
```

Si aparece `Authentication failed`, el token de recuperacion puede haberse creado, pero Mailtrap ha rechazado el envio por credenciales SMTP incorrectas.

### El chat no responde

Revisar:

```bash
cd docker
docker compose logs api --tail 160
```

Para demo sin clave externa, dejar:

```env
AI_PROVIDER=spring-ai
SPRING_AI_MODEL_CHAT=none
```

Para probar integracion generativa con OpenAI:

```env
AI_PROVIDER=spring-ai
SPRING_AI_MODEL_CHAT=openai
OPENAI_API_KEY=sk-...
OPENAI_MODEL=gpt-4.1-nano
OPENAI_MAX_TOKENS=200
```

Spring AI queda desactivado con `SPRING_AI_MODEL_CHAT=none` para que los tests y entornos sin clave no intenten crear el cliente IA. Las consultas directas de clientes, citas y vacaciones deben seguir funcionando aunque el proveedor externo falle.

### Cambios de codigo no aparecen

Reconstruir contenedores:

```bash
cd docker
docker compose up --build -d api front
```

### Puerto ocupado

Comprobar procesos o cambiar puertos en `docker/docker-compose.yml`.

### Base de datos con datos antiguos

Si se quiere reiniciar la base por completo:

```bash
cd docker
docker compose down -v
docker compose up --build -d
```

Esto borra los volumenes de datos, asi que debe usarse con cuidado.
