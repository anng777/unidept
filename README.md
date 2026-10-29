# UniDept — Spring Boot + Thymeleaf + Bootstrap (v2.0)

Plataforma tipo Airbnb para estudiantes universitarios de Lima: alquiler de departamentos y habitaciones, perfiles verificados con RENIEC (Decolecta), distancia a la universidad, filtros, chat, calificaciones, reportes y panel de administración.

> **Aviso honesto de verificación.** Esta versión se escribió en un entorno donde Maven Central está bloqueado, así que **el proyecto Spring NO se compiló ni se ejecutó aquí**. Revisé estructura de plantillas, ids de JS, fragmentos y referencias entre clases, pero la primera compilación real será en tu máquina (`mvn spring-boot:run`). Si algo falla al compilar, envíame el error exacto y lo corrijo.

## Qué cambió respecto al avance anterior

| Área | Antes | Ahora |
|---|---|---|
| Framework | Servlets + JS vanilla | **Spring Boot 3.3 + Thymeleaf + Bootstrap 5.3** |
| Datos | JDBC manual (DAO) | **Spring Data JPA** (entidades, repositorios, `Specification` para la búsqueda) |
| Sesión | HttpSession | **Spring Security 6 sin sesión + JWT** (cookie HttpOnly `UNIDEPT_JWT` o `Authorization: Bearer`) |
| Roles | tipo de usuario en sesión | Roles reales `ESTUDIANTE`, `ARRENDADOR`, `ADMINISTRADOR` (`/admin/**` solo administrador) |

## Las 4 mejoras pedidas

1. **Ubicación y distancia coherentes.** Las coordenadas son la única fuente de verdad: el enlace de Google Maps abre `lat,lng` exactas; el detalle muestra un mapa con el alojamiento y la universidad; al publicar se marca el punto en el mapa (clic/arrastre, búsqueda de dirección o pegando un enlace de Google Maps) y se valida que esté en Lima/Callao. La distancia se calcula con esas coordenadas (Google Distance Matrix si hay clave; si no, línea recta ×1,3 a 80 m/min) y **solo se dice "a pie" cuando son ≤ 30 min**; si no, "lejos para ir a pie". Se quitaron los minutos escritos a mano en las descripciones y se regeneraron las coordenadas de ejemplo respecto a su universidad. Para quien no está verificado la distancia se redondea a 100 m.
2. **Disponible / Ocupado.** Insignia en tarjetas y detalle. El arrendador lo cambia en *Mis alojamientos* (botón) y en *Fechas*. Un alojamiento figura ocupado si está marcado así **o** si hoy cae dentro de un rango ocupado. Filtro "Solo disponibles".
3. **Fechas.** Al solicitar se elige *desde–hasta* en un calendario (flatpickr). Las fechas ocupadas aparecen con un **círculo rojo suave** y no se pueden elegir; si el rango incluye alguna, se rechaza (también en el servidor). El arrendador puede marcar/liberar rangos y, al **aceptar** una solicitud, sus fechas pasan a ocupadas automáticamente (tabla `ocupaciones`).
4. **Tarjetas uniformes.** Todas comparten estructura: foto de alto fijo, título de 2 líneas, filas de distancia y etiquetas con altura reservada y botón "Ver detalle" anclado abajo (`h-100` + `mt-auto`), con o sin roommates/mascotas.

Se mantienen las funciones anteriores: datos sensibles ocultos para invitados/no verificados, registro con DNI verificado por RENIEC (un DNI = una cuenta), usuario editable único, login por correo o usuario, correo institucional para estudiantes, celular 9 dígitos que empieza en 9, precio ≥ 0, filtros de roommates y mascotas, varias fotos, 15 universidades (incl. sedes UTP), chat con primer mensaje de la solicitud, notificaciones, calificaciones, reportes y panel de administración.

## Requisitos

- JDK 17+, Maven 3.9+, MySQL 8.
- (Opcional pero necesario para registrarse) token de Decolecta.

## Puesta en marcha

1. En MySQL ejecuta `database/schema.sql` (**borra y recrea la BD `unidept`**; ya trae datos de ejemplo).
2. Edita usuario/clave de MySQL en `src/main/resources/application.properties` (por defecto `root`/`root`).
3. Variables de entorno:
   - `RENIEC_API_TOKEN` — token de Decolecta (sin él no se pueden crear cuentas nuevas).
   - `JWT_SECRET` — secreto largo (≥ 32 caracteres) para firmar los JWT. **Cámbialo** fuera de pruebas.
   - `GOOGLE_MAPS_API_KEY` — opcional (Distance Matrix).
4. `mvn spring-boot:run` y abre **http://localhost:8080/unidept/**.

## Usuarios de prueba (contraseña `Password123`)

| Usuario | Rol |
|---|---|
| ana_ramirez, valeria_soto | Estudiante |
| carlos_mendoza | Arrendador verificado |
| lucia_flores | Arrendador no verificado |
| admin | Administrador — ingresa por `/unidept/admin/login` |

Nota: el hash BCrypt del seed solo verifiqué que fuera autoconsistente; si el login de prueba falla, genera uno nuevo con `new BCryptPasswordEncoder().encode("Password123")` y reemplázalo en `schema.sql`.

## Rutas principales

- Públicas: `/`, `/alojamientos`, `/alojamientos/{id}`, `/login`, `/registro`, `/admin/login`, `GET /api/dni`, `GET /api/alojamientos/{id}/ocupadas`, `POST /api/auth/login` (devuelve el JWT).
- Estudiante: `POST /solicitudes/crear`, `/mis-solicitudes`.
- Arrendador: `/alojamientos/nuevo|guardar|{id}/editar|{id}/disponibilidad`, `/mis-alojamientos` (aceptar/rechazar, disponible/ocupado).
- Autenticados: `/chat/{id}`, `/perfil`, `/reportes`, `/api/notificaciones`.
- Administrador: `/admin`, `/admin/usuarios`, `/admin/publicaciones`, `/admin/reportes`.

Seguridad: el JWT se revalida contra la BD en cada petición (una cuenta suspendida pierde acceso al instante); formularios protegidos con CSRF; contraseñas con BCrypt.

## Estructura

```
database/schema.sql              esquema + datos de ejemplo (incluye `ocupaciones`)
src/main/java/com/unihousing/
  model/        entidades JPA y enums
  repository/   Spring Data JPA
  service/      reglas de negocio (RENIEC, distancia, solicitudes, disponibilidad…)
  security/     JWT (JwtService, JwtAuthFilter), UserDetails
  config/       SecurityConfig
  web/          controladores MVC y API
src/main/resources/templates/    Thymeleaf (fragments/layout.html = cabecera, navbar, pie)
src/main/resources/static/       css/app.css y js/*.js
```

## Limitaciones y pendientes (honestos)

- **No compilado/ejecutado en el entorno de desarrollo** (ver aviso arriba).
- **Coordenadas de universidades y alojamientos de ejemplo son aproximadas** (de mi mejor conocimiento; no pude consultar un geocodificador). Corrígelas con Google Maps si necesitas precisión; los alojamientos de ejemplo se ubicaron a una distancia/rumbo razonables de su universidad de referencia.
- Mapas con Leaflet/OpenStreetMap; la búsqueda de dirección usa Nominatim desde el navegador del usuario (sujeta a sus límites de uso). Los enlaces cortos `maps.app.goo.gl` no contienen coordenadas: usa el enlace largo o marca el punto.
- El tiempo "a pie" es una estimación (línea recta ×1,3); con `GOOGLE_MAPS_API_KEY` se usa Distance Matrix (nunca probado aquí).
- El calendario bloquea fechas por rangos ocupados; no hay pagos, depósito ni contratos.
- No hay pruebas automatizadas todavía.
