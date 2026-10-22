# Estándares de Seguridad y Protección de Datos (ECC & ISO/IEC 27001:2022)

Este conjunto de directrices adopta los estándares del ecosistema **affaan-m/ECC** y los controles del **Anexo A de ISO/IEC 27001:2022** para el backend de SGS El Tabo.

---

## 1. Control de Acceso y Autorizaciones (A.5.15, A.8.2)
- **Principio de Mínimo Privilegio (Least Privilege)**: Todo endpoint o servicio debe exigir autenticación explícita salvo que esté debidamente justificado como público.
- **Prevención de Escalamiento de Privilegios**: En endpoints de auto-registro o creación de cuentas por usuarios no administradores, el rol asignado debe ser forzosamente `CIUDADANO`. La asignación de roles `OPERADOR`, `INSPECTOR` o `ADMINISTRADOR` requiere autorización previa de un usuario con rol `ADMINISTRADOR`.
- **Rutas de Evidencia y Multimedia**: Las descargas de archivos adjuntos (GridFS) no deben ser públicas; deben validar sesión y pertenencia/permisos de rol.

---

## 2. Validación Rigurosa de Entradas (A.8.26, A.8.28)
- **Validación en Backend Obligatoria**: Jamás confiar en las validaciones de cliente. Todo `@RequestBody` debe anotarse con `@Valid` o `@Validated`.
- **Validación de Identidad (RUN Chileno)**: Todo RUN recibido debe validarse mediante el algoritmo de Módulo 11 (dígito verificador) usando `@RutChileno`.
- **Política de Contraseñas Seguras**: Mínimo 8 caracteres, al menos una letra mayúscula, una minúscula, un dígito y un carácter especial.
- **Sanitización contra Inyecciones (XSS, NoSQL Injection)**: Escapar y sanitizar textos libres (descripciones, comentarios, observaciones de bitácora) antes de la persistencia.

---

## 3. Protección de Datos Personales (PII) y Privacidad (A.5.34, A.8.11)
- **Enmascaramiento de Datos (Data Masking)**:
  - En denuncias anónimas (`esAnonimo == true`), suprimir o enmascarar los datos personales del denunciante (`DenuncianteDTO`) en respuestas al cliente.
  - Ofuscar parcialmente RUNs (`12.***.***-K`), teléfonos (`+56 9 **** 1234`) y correos electrónicos (`u***@***.com`) cuando se expongan a perfiles operativos que no requieran contacto directo.
- **No registrar secretos ni PII en logs**: Prohibido imprimir contraseñas, tokens JWT o PII completa en logs de consola o trazas de excepción.

---

## 4. Gestión de Excepciones y Fuga de Información (A.8.28, A.8.15)
- **Manejo Centralizado**: `GlobalExceptionHandler` debe capturar `AccessDeniedException` y `AuthenticationException`, retornando códigos HTTP semánticos (401, 403) sin filtrar stack traces ni rutas internas de clases.
