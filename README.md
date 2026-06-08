# Misión Psicosocial - Simulador Académico

Aplicación full-stack para simulación de casos de psicología social.

## Stack

- **Backend:** Spring Boot 3.2 + JPA + H2 + JWT Security
- **Frontend:** Angular 19 + Tailwind CSS

## Pantallas

1. **Login** - Iniciar sesión
2. **Dashboard** - Panel de estudiante con estadísticas y casos
3. **Simulador** - Caso en curso con preguntas interactivas
4. **Reportes** - Evaluación de intentos con métricas clínicas, éticas y normativas

## Usuarios de prueba

| Rol | Email | Contraseña |
|-----|-------|------------|
| Estudiante | estudiante@simulador.com | estudiante123 |
| Admin | admin@simulador.com | admin123 |

## Ejecutar

### Backend (puerto 8080)

```bash
cd backend
./mvnw spring-boot:run
```

En Windows:
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Frontend (puerto 4200)

```bash
cd frontend
npm install
npm start
```

Abrir http://localhost:4200

## Docker

Requisitos: [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución.

```bash
# Desde la raíz del proyecto
cp .env.example .env   # opcional: correo, OAuth, JWT
docker compose up --build
```

- **Frontend:** http://localhost
- **Backend API:** http://localhost:8080/api (también accesible vía nginx en `/api`)

Los datos (H2) y los avatares subidos se guardan en volúmenes Docker y persisten entre reinicios.

```bash
docker compose down          # detener
docker compose down -v       # detener y borrar datos
```

## API Endpoints

- `POST /api/auth/login` - Autenticación
- `GET /api/dashboard?search=` - Panel estudiante
- `GET /api/cases/{id}` - Detalle del caso
- `POST /api/cases/{id}/start` - Iniciar intento
- `POST /api/cases/{id}/answer` - Enviar respuesta
- `POST /api/cases/{id}/reset-request` - Solicitar reinicio
- `GET /api/reports?search=` - Reportes
- `GET /api/reports/attempts/{id}` - Detalle de intento
