# Spring Security — Authentication & Authorization API

REST API completa de autenticación y autorización construida con **Spring Boot 4** y **Java 21**. Implementa un sistema de seguridad stateless basado en JWT con soporte para registro, verificación de email, recuperación de contraseña e invitaciones de usuarios.

---

## Características

- **Registro de usuarios** con verificación de email por código de 6 dígitos
- **Autenticación JWT** con access token y refresh token
- **Recuperación de contraseña** mediante link enviado por email
- **Sistema de invitaciones** — un administrador puede invitar usuarios directamente
- **Logout por dispositivo** y **logout global** (revoca todos los refresh tokens)
- **Control de acceso por roles** — `ADMIN` y `USER`
- **Documentación Swagger/OpenAPI** integrada
- **Global exception handling** con respuestas de error estructuradas
- **CORS configurable** por variables de entorno
- Soporte de **perfiles** `dev`, `prod` y `test`

---

## Stack tecnológico

| Tecnología | Versión |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.4 |
| Spring Security | (incluido en Boot) |
| JJWT | 0.12.7 |
| MapStruct | 1.6.3 |
| Lombok | 1.18.42 |
| SpringDoc OpenAPI | 3.0.2 |
| H2 Database | (dev/test) |
| Spring Actuator | — |

---

## Requisitos previos

- Java 21+
- Maven 3.9+
- (Opcional) Servidor de base de datos para perfil `prod`
- (Opcional) Servicio de email SMTP para envío de verificaciones

---

## Configuración

La aplicación usa **Spring Profiles** y carga variables desde un archivo `.env` en la raíz del proyecto.

### Variables de entorno requeridas

Crea un archivo `.env` en la raíz del proyecto:

```env
# JWT
jwt.secret_key=<clave-base64-segura-mínimo-256-bits>
jwt.expiration=900000
jwt.refresh-expiration=604800000

# Email SMTP
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your@email.com
spring.mail.password=your-password

# URLs del frontend
RESET_PASSWORD_BASE_URL=http://localhost:5173/reset-password
ACCEPT_INVITE_BASE_URL=http://localhost:5173/accept-invite

# CORS (opcional, separado por comas)
app.security.cors.allowed-origins=http://localhost:3000,http://localhost:4200
```

### Perfiles disponibles

| Perfil | Base de datos | Descripción |
|---|---|---|
| `dev` (default) | H2 en memoria | Desarrollo local con logs detallados |
| `test` | H2 en memoria | Ejecución de tests |
| `prod` | Configurable | Producción |

Para cambiar de perfil:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

---

## Ejecución

```bash
# Clonar el repositorio
git clone https://github.com/crishof/spring-security.git
cd spring-security

# Ejecutar en modo desarrollo
./mvnw spring-boot:run

# O compilar y ejecutar el JAR
./mvnw clean package
java -jar target/Spring-Security-0.0.1.jar
```

La API estará disponible en `http://localhost:8080`.

---

## Documentación API

Con la aplicación corriendo, accede a la UI de Swagger:

```
http://localhost:8080/swagger-ui.html
```

---

## Endpoints

### Registro (`/api/v1/auth/registration`)

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/signup` | Registra un nuevo usuario | Público |
| `POST` | `/verify-email` | Verifica el email con el código recibido | Público |
| `POST` | `/resend-verification` | Reenvía el código de verificación | Público |

### Autenticación (`/api/v1/auth`)

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/login` | Inicia sesión, devuelve access + refresh token | Público |
| `POST` | `/logout` | Cierra sesión (revoca el refresh token) | Público |
| `POST` | `/logout-all` | Cierra todas las sesiones del usuario | Requerida |
| `POST` | `/refresh` | Genera nuevo access token con el refresh token | Público |
| `GET` | `/me` | Devuelve la información del usuario autenticado | Requerida |

### Recuperación de contraseña (`/api/v1/auth/password`)

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/forgot` | Envía un link de reseteo al email | Público |
| `POST` | `/reset` | Resetea la contraseña con el token recibido | Público |

### Invitaciones (`/api/v1/invitations`)

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `GET` | `/{token}/info` | Obtiene información de la invitación | Público |
| `POST` | `/accept` | Acepta la invitación y crea la cuenta | Público |

### Administración (`/api/v1/admin`)

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| `POST` | `/invitations` | Crea una invitación para un nuevo usuario | `ADMIN` |

---

## Flujo de autenticación

```
1. POST /signup          → Se crea la cuenta con estado PENDING_VERIFICATION
2. POST /verify-email    → Se activa la cuenta, devuelve access + refresh token
3. POST /login           → Devuelve access + refresh token
4. GET  /api/...         → Authorization: Bearer <access_token>
5. POST /refresh         → Cuando el access token expira, usa el refresh token
6. POST /logout          → Revoca el refresh token
```

---

## Modelo de datos

```
User
 ├── id (UUID)
 ├── fullName
 ├── email (único)
 ├── role (ADMIN | USER)
 ├── status (PENDING_VERIFICATION | ACTIVE | ...)
 ├── createdAt
 └── updatedAt

SecurityAccount
 ├── passwordHash (BCrypt)
 ├── emailVerified
 ├── enabled
 └── locked

RefreshToken
 ├── token
 ├── user → User
 ├── expiresAt
 └── revoked
```

---

## Seguridad

- Contraseñas hasheadas con **BCrypt**
- Tokens JWT firmados con **HMAC-SHA256** (clave configurable)
- Sesiones **stateless** — sin estado en servidor
- Claims del access token: `uid`, `role`, `status`
- Refresh tokens almacenados en base de datos con soporte de revocación
- Endpoints de administración protegidos con `hasRole("ADMIN")`
- CORS configurable por variable de entorno

---

## Tests

```bash
./mvnw test
```

---

## Estructura del proyecto

```
src/main/java/com/crishof/springsecurity/
├── config/          # WebClient y configuración de beans
├── controller/      # AuthController, RegistrationController, InvitationsController,
│                    # PasswordRecoveryController, AdminInvitationController
├── dto/             # Request y Response DTOs
├── exception/       # Excepciones de dominio + GlobalExceptionHandler
├── model/           # Entidades JPA (User, SecurityAccount, RefreshToken, ...)
├── repository/      # Repositorios Spring Data JPA
├── security/
│   ├── config/      # SecurityConfig (FilterChain, CORS, BCrypt)
│   ├── jwt/         # JwtService, JwtFilter
│   ├── principal/   # SecurityUser, SecurityUserDetailsService
│   └── web/         # RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── service/         # AuthService + AuthServiceImpl, EmailService
└── util/            # ValidationUtil
```

---

## Licencia

Este proyecto es de uso personal y educativo.
