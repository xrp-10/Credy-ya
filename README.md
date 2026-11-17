# CrediYa

API reactiva para solicitudes de préstamo, hecha con Spring WebFlux, R2DBC y PostgreSQL sobre arquitectura hexagonal (scaffold Clean Architecture de Bancolombia). Implementa las historias de usuario 1 a 4 del bootcamp.

## Cómo correrla en local

1. Crear la base en PostgreSQL: `CREATE DATABASE crediya;`. Al arrancar, la app crea las tablas y los catálogos con `schema.sql` y `data.sql` (no duplica nada si ya existen).
2. Configurar las variables de entorno:

| Variable | Por defecto | Para qué |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | `localhost`, `5432`, `crediya` | Conexión a PostgreSQL |
| `DB_USERNAME`, `DB_PASSWORD` | `postgres`, vacío | Usuario de la base |
| `JWT_SECRET` | llave temporal | Firma de los tokens, mínimo 32 caracteres. Sin ella, los tokens dejan de servir al reiniciar |
| `JWT_EXPIRATION_MINUTES` | `60` | Minutos de vigencia del token |

3. Arrancar: `./gradlew :app-service:bootRun`. La API queda en `http://localhost:8080`.

### Primer administrador

Registrar usuarios exige un ADMIN o un ASESOR, así que el primer administrador se crea directo en la base. La clave queda cifrada con BCrypt:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
INSERT INTO usuario (nombre, apellido, email, documento_identidad, salario_base, id_rol, contrasena)
VALUES ('Nombre', 'Apellido', 'admin@tu-correo.com', 'DOCUMENTO', 0, 1, crypt('TU-CLAVE', gen_salt('bf', 10)));
```

## Endpoints

| Método y ruta | HU | Quién puede | Qué hace |
|---|---|---|---|
| `POST /api/v1/login` | 3 | Cualquiera | Recibe `correo_electronico` y `clave`; devuelve `{"token", "tipo": "Bearer"}` |
| `POST /api/v1/usuarios` | 1 | ADMIN, ASESOR | Registra un usuario |
| `POST /api/v1/solicitud` | 2 | CLIENTE | Registra una solicitud a nombre de quien llama, en "Pendiente de revisión" |
| `GET /api/v1/solicitud` | 4 | ASESOR | Lista paginada de solicitudes pendientes, rechazadas y en revisión manual |

Los endpoints protegidos llevan la cabecera `Authorization: Bearer <token>`. Todo error responde JSON con `estado`, `error` y `mensaje`.

- **Registrar usuario**: `nombres`, `apellidos`, `correo_electronico`, `salario_base` (0 a 15000000), `documento_identidad` y `clave` (8 a 64 caracteres) son obligatorios. Son opcionales `fecha_nacimiento` (AAAA-MM-DD), `direccion`, `telefono` e `id_rol` (1 ADMIN, 2 ASESOR, 3 CLIENTE; por defecto 3).
- **Registrar solicitud**: `documento_identidad`, `monto`, `plazo` (meses) e `id_tipo_prestamo`. El monto debe estar dentro del rango del tipo de préstamo.
- **Listar solicitudes**: los parámetros son opcionales: `estado` (1, 2 o 3), `email`, `tipo_prestamo`, `pagina` (desde 0) y `tamano` (1 a 50, por defecto 10). `deuda_total_mensual_solicitudes_aprobadas` es la suma de las cuotas de las solicitudes aprobadas del cliente: monto × i / (1 − (1 + i)^−plazo), con i = tasa_interes / 100 (tasa mensual).

## Pruebas

`./gradlew test` ejecuta las pruebas unitarias con pitest (mutation testing) y `./gradlew validateStructure` valida las dependencias entre capas.

---

# Proyecto Base Implementando Clean Architecture

## Antes de Iniciar

Empezaremos por explicar los diferentes componentes del proyectos y partiremos de los componentes externos, continuando con los componentes core de negocio (dominio) y por último el inicio y configuración de la aplicación.

Lee el artículo [Clean Architecture — Aislando los detalles](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a)

# Arquitectura

![Clean Architecture](https://miro.medium.com/max/1400/1*ZdlHz8B0-qu9Y-QO3AXR_w.png)

## Domain

Es el módulo más interno de la arquitectura, pertenece a la capa del dominio y encapsula la lógica y reglas del negocio mediante modelos y entidades del dominio.

## Usecases

Este módulo gradle perteneciente a la capa del dominio, implementa los casos de uso del sistema, define lógica de aplicación y reacciona a las invocaciones desde el módulo de entry points, orquestando los flujos hacia el módulo de entities.

## Infrastructure

### Helpers

En el apartado de helpers tendremos utilidades generales para los Driven Adapters y Entry Points.

Estas utilidades no están arraigadas a objetos concretos, se realiza el uso de generics para modelar comportamientos
genéricos de los diferentes objetos de persistencia que puedan existir, este tipo de implementaciones se realizan
basadas en el patrón de diseño [Unit of Work y Repository](https://medium.com/@krzychukosobudzki/repository-design-pattern-bc490b256006)

Estas clases no puede existir solas y debe heredarse su compartimiento en los **Driven Adapters**

### Driven Adapters

Los driven adapter representan implementaciones externas a nuestro sistema, como lo son conexiones a servicios rest,
soap, bases de datos, lectura de archivos planos, y en concreto cualquier origen y fuente de datos con la que debamos
interactuar.

### Entry Points

Los entry points representan los puntos de entrada de la aplicación o el inicio de los flujos de negocio.

## Application

Este módulo es el más externo de la arquitectura, es el encargado de ensamblar los distintos módulos, resolver las dependencias y crear los beans de los casos de use (UseCases) de forma automática, inyectando en éstos instancias concretas de las dependencias declaradas. Además inicia la aplicación (es el único módulo del proyecto donde encontraremos la función “public static void main(String[] args)”.

**Los beans de los casos de uso se disponibilizan automaticamente gracias a un '@ComponentScan' ubicado en esta capa.**
