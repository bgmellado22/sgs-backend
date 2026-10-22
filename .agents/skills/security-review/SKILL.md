---
name: security-review
description: Skill de revisión de seguridad y análisis estático basado en affaan-m/ECC y OWASP Top 10 para SGS El Tabo.
---

# Skill de Revisión de Seguridad (ECC Security Review)

Esta skill permite ejecutar auditorías automatizadas y revisiones manuales de seguridad en el código de SGS El Tabo.

## Capacidades
1. **Ejecución de AgentShield**:
   ```powershell
   npx --yes ecc-agentshield scan -p . -f markdown -o security-audit-backend.md --compliance iso
   ```
2. **Chequeo de Broken Access Control (OWASP A01)**:
   - Validar que `@PreAuthorize` o `SecurityConfig.java` proteja cada endpoint según el rol mínimo requerido.
   - Revisar que no existan accesos directos a objetos inseguros (IDOR).
3. **Chequeo de Validaciones Jakarta (OWASP A03 / ISO A.8.28)**:
   - Verificar presencia de `@Valid` en `@RequestBody`.
   - Verificar validación de formato con `@RutChileno`, `@Email`, `@Pattern`.
4. **Chequeo de Protección PII (ISO A.8.11 / Ley 19.628)**:
   - Verificar enmascaramiento de datos personales en DTOs de salida.
