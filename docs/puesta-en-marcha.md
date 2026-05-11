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
| `GEMINI_API_KEY` | Clave del asistente IA |
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

## Usuarios demo

El backend crea usuarios demo en el primer arranque. Sus nombres de usuario son visibles en el seed, pero la contrasena se lee desde `APP_DEMO_PASSWORD` y no se documenta en el repositorio publico.

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

### El chat no responde

Revisar:

```bash
cd docker
docker compose logs api --tail 160
```

Si aparecen errores `403` o `429` de Gemini, la clave o la cuota de Gemini no estan disponibles. Las consultas directas de clientes deben seguir funcionando.

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
