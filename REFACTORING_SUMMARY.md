# Spring-Security Project Refactoring Summary

## Overview
Refactorización completa del proyecto Spring-Security como base de autenticación y autorización escalable con Spring Boot 4.0.4 y Java 21.

## Technology Stack
- **Spring Boot**: 4.0.4
- **Java**: 21 (LTS)
- **Spring Security**: JWT-based stateless authentication
- **Build Tool**: Maven 3.9.11
- **ORM**: JPA/Hibernate with MapStruct mapper
- **Email**: SMTP + Brevo API dual-provider architecture

## Architecture Overview

### Controllers Organization by Functionality

#### 1. **RegistrationController** (`/api/v1/auth/registration`)
Gestiona el flujo de registro y verificación de email.

**Endpoints:**
- `POST /signup` - Registra nuevo usuario y envía código de verificación
- `POST /verify-email` - Verifica email con código de 6 dígitos
- `POST /resend-verification` - Reenvía código de verificación

**Request/Response DTOs:**
- `SignupRequest` → `SignupResponse`
- `VerifyEmailRequest` → `AuthResponse`
- `ResendVerificationRequest` → `MessageResponse`

---

#### 2. **PasswordRecoveryController** (`/api/v1/auth/password`)
Gestiona la recuperación de contraseña olvidada.

**Endpoints:**
- `POST /forgot` - Inicia proceso de recuperación (envía token por email)
- `POST /reset` - Establece nueva contraseña con token válido

**Request/Response DTOs:**
- `ForgotPasswordRequest` → `MessageResponse`
- `ResetPasswordRequest` → `MessageResponse`

---

#### 3. **InvitationsController** (`/api/v1/invitations`)
Gestiona la aceptación de invitaciones.

**Endpoints:**
- `GET /{token}/info` - Obtiene detalles de invitación sin aceptarla
- `POST /accept` - Acepta invitación y crea cuenta

**Request/Response DTOs:**
- `InviteInfoResponse`
- `AcceptInviteRequest` → `AuthResponse`

---

#### 4. **AuthController** (`/api/v1/auth`)
Core authentication - solo endpoints esenciales.

**Endpoints:**
- `POST /login` - Login con email/contraseña → tokens JWT
- `POST /logout` - Revoca refresh token actual
- `POST /logout-all` - Revoca todos los refresh tokens del usuario
- `POST /refresh` - Obtiene nuevo access token del refresh token
- `GET /me` - Obtiene perfil del usuario autenticado

**Request/Response DTOs:**
- `LoginRequest` → `AuthResponse`
- `RefreshTokenRequest` → `AuthResponse`
- `LogoutRequest` → `MessageResponse`

---

#### 5. **AdminInvitationController** (`/api/v1/admin/invitations`)
Admin operations para crear/enviar invitaciones.

**Security:** `@PreAuthorize("hasRole('ADMIN')")`

**Endpoints:**
- `POST /` - Crea y envía invitación a usuario

---

### Security Configuration

**Public Endpoints:**
```
# Swagger/API Docs
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**

# Health Check
/actuator/health

# Registration Flow
/api/v1/auth/registration/signup
/api/v1/auth/registration/verify-email
/api/v1/auth/registration/resend-verification

# Authentication
/api/v1/auth/login
/api/v1/auth/refresh
/api/v1/auth/logout

# Password Recovery
/api/v1/auth/password/forgot
/api/v1/auth/password/reset

# Invitations
/api/v1/invitations/*/info
/api/v1/invitations/accept
```

**Protected Endpoints:**
- `/api/v1/auth/logout-all` - Requiere autenticación
- `/api/v1/auth/me` - Requiere autenticación
- `/api/v1/admin/**` - Requiere role ADMIN

---

## Utility Classes

### 1. **NormalizationUtil**
Centraliza normalización de datos de entrada.

**Métodos Estáticos:**
```java
normalizeEmail(String email)        // Trim + lowercase + validar formato
normalizeFullName(String fullName)  // Trim + capitalize por palabra
isAlphanumeric(String text)         // Valida caracteres permitidos
isPasswordStrong(String password)   // Verifica requisitos mínimos de seguridad
```

### 2. **ValidationUtil**
Validación de reglas de negocio.

**Constantes:**
```java
MIN_PASSWORD_LENGTH = 8
MAX_PASSWORD_LENGTH = 72
VERIFICATION_CODE_LENGTH = 6
FULL_NAME_MAX_LENGTH = 120
EMAIL_MAX_LENGTH = 150
```

**Métodos Estáticos:**
```java
validatePassword(String password)                    // Requiere: mayús, minús, dígito, especial
validateFullName(String fullName)                    // Length + caracteres válidos
validateEmail(String email)                          // Formato + length
validateVerificationCode(String code)                // Exactamente 6 dígitos
validateEqualityOf(String value1, String value2, String context) // Compara valores
```

### 3. **CodeGeneratorUtil**
Generación segura de códigos y tokens.

**Métodos Estáticos:**
```java
generateVerificationCode()           // 6 dígitos numéricos
generateSecureToken(int length)      // Alfanumérico de longitud variable
generateSecureToken()                // Token base64 URL-safe de 32 bytes
```

---

## Data Models

### Core Entities
- **User**: id (UUID), fullName, email (UNIQUE), role (ENUM), status (ENUM), timestamps
- **SecurityAccount**: password hash, email verified, enabled, locked flags
- **RefreshToken**: token (UNIQUE), expiry, revocation status
- **EmailVerificationToken**: 6-digit code, expiry, usage tracking
- **PasswordResetToken**: secure token, expiry, usage tracking
- **InvitationToken**: secure token, email target, assigned role, expiry

### Enums
- **Role**: ADMIN, USER
- **UserStatus**: PENDING_VERIFICATION, ACTIVE, INVITED, BLOCKED, INACTIVE

---

## Configuration Profiles

### Development (`application-dev.properties`)
- H2 in-memory database with web console
- SMTP email provider
- Relaxed CORS: `http://localhost:3000`, `http://localhost:4200`

### Production (`application-prod.properties`)
- PostgreSQL database with connection pooling
- Brevo API email provider
- Strict CORS configuration

### Test (`application-test.properties`)
- H2 in-memory database
- Disabled email sending
- Single origin CORS

---

## Build & Test Results

**Compilation Status**: ✅ BUILD SUCCESS

**Test Results**: 
```
Tests run: 2
Failures: 0
Errors: 0
Skipped: 0
```

**Key Fixes Applied:**
1. Fixed Java 21 compatibility: Replaced `_` with `ignored` in catch blocks
2. Removed @Component annotation from utility classes (@UtilityClass already handles instantiation prevention)
3. Fixed PasswordRecoveryController field access: `newPassword()` and `confirmPassword()` methods
4. Updated SecurityConfig public endpoints to match new controller routes

---

## Email System Integration

### Dual Provider Architecture
```
EmailService Interface
  ├─ sendPasswordResetEmail()
  ├─ sendEmailVerificationCode()
  └─ sendInvitationEmail()
      ↓
EmailServiceImpl (Strategy Pattern)
  ├─ SMTP Provider: Standard email delivery
  └─ Brevo API Provider: Production-ready transactional emails
```

### Configuration
- Provider selection: `app.mail.provider` (smtp/brevo)
- Brevo credentials: `app.mail.brevo.api-url`, `app.mail.brevo.api-key`
- Timeout configuration: `app.mail.brevo.timeout-seconds`

---

## JWT Token Management

### Access Token
- TTL: 900 seconds (15 minutes)
- Contains: user ID, email, roles
- Validation: Spring Security's JwtAuthenticationProvider

### Refresh Token
- TTL: 7 days
- Storage: Database table (RefreshToken entity)
- Revocation support: flags and cleanup on logout

---

## Next Steps for Production

1. **Database Constraints Optimization**
   - Add @UniqueConstraint and @Index on entity fields
   - Configure cascading behaviors

2. **Administrative User Controller**
   - List users with pagination
   - View user details
   - Lock/unlock accounts
   - Assign roles

3. **Security Hardening**
   - Implement rate limiting on auth endpoints
   - Add account lockout after failed attempts
   - CSRF tokens for state-changing operations

4. **Monitoring & Logging**
   - Structured logging for auth events
   - Metrics for token generation/validation
   - Audit trail for admin operations

5. **Documentation**
   - OpenAPI/Swagger specifications (auto-generated)
   - API endpoint documentation in code
   - Deployment guide for production use

---

## Deployment Notes

**Docker Support:**
- Dockerfile included for containerization
- docker-compose.yml available for local development with PostgreSQL

**Database Support:**
- Development: H2 (automatic setup)
- Production: PostgreSQL 12+ required

**Required Environment Variables (Production):**
```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/db
SPRING_DATASOURCE_USERNAME=user
SPRING_DATASOURCE_PASSWORD=password
APP_SECURITY_JWT_SECRET=<base64-encoded-secret>
APP_MAIL_BREVO_API_KEY=<brevo-api-key>
APP_SECURITY_CORS_ALLOWED_ORIGINS=<comma-separated-origins>
```

---

## Quality Metrics

- **Code Coverage**: Utility class testing available
- **Security**: JWT + BCrypt password hashing + CORS protection
- **Performance**: Connection pooling (HikariCP), stateless authentication
- **Scalability**: Horizontal scaling ready (stateless sessions, JWT tokens)
