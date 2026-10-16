# Directrices de Rendimiento, Optimización y Buenas Prácticas de Desarrollo

Este documento define el estándar técnico obligatorio que cualquier agente de IA o desarrollador debe seguir al modificar o crear código en el proyecto **SGS El Tabo**. El objetivo es preservar la alta velocidad de carga, evitar deuda técnica, prevenir degradación del rendimiento y garantizar una arquitectura escalable.

---

## 1. Principios Generales y Arquitectura

1. **Principio DRY (Don't Repeat Yourself)**:
   - Ninguna lógica de validación, formateo, cálculo de fechas o carga de catálogos debe duplicarse entre controladores, servicios o componentes.
   - Toda función reutilizable debe residir en su módulo utilitario correspondiente (`utils/` o `helper/`).

2. **Rendimiento por Diseño (Performance-First)**:
   - Todo endpoint o función debe diseñarse pensando en el peor caso de volumen de datos (miles de registros, fotos pesadas, múltiples peticiones concurrentes).
   - Prohibido traer datasets completos a memoria para filtrar u ordenar si el motor de base de datos puede hacerlo indexadamente.

3. **Verificación Continua Obligatoria**:
   - Todo cambio en el backend debe verificarse ejecutando `./gradlew compileJava` y `./gradlew test`.
   - Todo cambio en el frontend debe verificarse ejecutando `npm run build` para validar empaquetado y code splitting.

---

## 2. Buenas Prácticas para el Backend (Java + Spring Boot + MongoDB)

### 2.1 Indexación Compuesta y Consultas MongoDB
- **Uso obligatorio de índices compuestos**: Cualquier consulta que combine filtros recurrentes (por ejemplo: `activo`, `estado`, `prioridad`, `fecha_creacion`, `categoria`) DEBE estar respaldada por un índice compuesto en la colección.
- **Migraciones versionadas con Mongock**:
  - Nunca crear índices directamente en caliente sin registrar la migración.
  - Toda modificación en la estructura de base de datos debe añadirse como una nueva clase en `com.conectatech.migration` con el siguiente número secuencial (ej. `V8__...`).
  - Implementar siempre el método `@RollbackExecution` correspondiente para garantizar reversibilidad.

### 2.2 Paginación del Lado del Servidor
- **Prohibido retornar colecciones no acotadas**:
  - Todo endpoint de búsqueda o bandeja de incidentes debe aceptar `Pageable` y retornar `Page<DTO>` o `Slice<DTO>`.
  - Configurar ordenamiento por defecto en `@PageableDefault` (ej. `sort = "fecha_creacion", direction = Sort.Direction.DESC`).
  - Para filtros dinámicos con `MongoTemplate`, ejecutar primero `count(query)` y luego `find(query.with(pageable))` para construir `PageImpl`.

### 2.3 Estrategia de Caché de Aplicación (Spring Cache)
- **Lecturas frecuentes e inmutables**:
  - Entidades de configuración o catálogos (`ORIGEN`, `PRIORIDAD`, `CATEGORIA`, `TIPO_EVENTO`) deben anotarse con `@Cacheable(value = "catalogos", key = "#tipo.toUpperCase()")`.
- **Evicción precisa de caché**:
  - Toda operación de mutación (`crear`, `editar`, `eliminar`, `activar/desactivar`) debe incluir `@CacheEvict(value = "catalogos", allEntries = true)`.
- **Aislamiento en tests**:
  - La anotación `@EnableCaching` debe residir en una clase `@Configuration` dedicada (`CacheConfig.java`), no en `@SpringBootApplication`, para evitar que contamine pruebas `@WebMvcTest`.

### 2.4 Manejo Multimedia y Tráfico de Red (GridFS)
- **Límites de tamaño estrictos**:
  - Respetar las propiedades `spring.servlet.multipart.max-file-size=10MB` y `spring.servlet.multipart.max-request-size=15MB`.
  - Validar programáticamente en el servicio:
    ```java
    if (archivo.getSize() > MAX_FILE_SIZE_BYTES) {
        throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido de 10MB.");
    }
    ```
- **Validación de tipo MIME real**:
  - Aceptar exclusivamente tipos multimedia permitidos (`contentType.startsWith("image/")`). Rechazar binarios desconocidos o potencialmente peligrosos.

### 2.5 Limpieza de Código y Eficiencia
- Evitar variables huérfanas o llamadas duplicadas a servicios/repositorios dentro del mismo método.
- Usar DTOs proyectados (`IncidenteResponseDTO`) en vez de exponer la entidad de dominio MongoDB completa.

---

## 3. Buenas Prácticas para el Frontend (React + Vite + Tailwind)

### 3.1 Modularización y Componentes UI
- **Componentes especializados y únicos**:
  - Para carga de fotos/evidencias: usar siempre `<EvidenciaUploader />`.
  - Para mapas, pines y georreferenciación: usar siempre `<UbicacionPicker />`.
  - Para feedback al usuario: usar siempre `<Toast />` provisto por `DashboardLayout` via `useOutletContext()`.
- **Prohibido duplicar modales o formularios**: Los modales de creación y edición deben reutilizar las utilidades consolidadas de validación (`validacionUtils.js`) y catálogos (`catalogoUtils.js`).

### 3.2 Code Splitting y Carga Perezosa (Lazy Loading)
- **Librerías pesadas en rutas bajo demanda**:
  - Módulos con dependencias de alto peso (ej: `leaflet` ~350KB en `Mapa`, `recharts` ~280KB en `Reportes`, librerías PDF/Excel) deben importarse con `React.lazy()`:
    ```javascript
    const Mapa = lazy(() => import('./pages/Mapa/Mapa'));
    const Reportes = lazy(() => import('./pages/Reportes/Reportes'));
    ```
  - Envolver las rutas siempre con `<Suspense fallback={<Loader />}>`.

### 3.3 Memoización y Control de Renders
- **Componentes Visuales Pesados**:
  - Envolver widgets, gráficos o tarjetas complejas con `React.memo(MiComponente)` para evitar renders cuando cambian estados ajenos en la vista padre.
- **useMemo**:
  - Usar para filtros, agrupaciones territoriales, cálculos de estadísticas o arrays ordenados (ej. `activosPorPrioridad`, `ultimosReportes`, `incidentesActivos`).
- **useCallback**:
  - Usar para callbacks y manejadores de eventos que se pasen como props a componentes hijos memoizados o que se utilicen como dependencias en `useEffect`.

### 3.4 Búsquedas con Debounce
- **No saturar la API en inputs de texto**:
  - Todo buscador en tiempo real debe usar el hook `useDebounce(filtros, 500)`.
  - El efecto de llamada al backend debe escuchar al valor debounced, nunca al evento `onChange` directo.

### 3.5 Consumo Eficiente de APIs y Catálogos
- **Prohibido hacer fetch manual de catálogos**:
  - Usar el hook global `useCatalogos()` provisto por `CatalogosContext`.
  - La app carga los catálogos una única vez en memoria al iniciar sesión; los componentes no deben volver a solicitarlos por HTTP.
- **Paginación en Servidor**:
  - No usar `Array.prototype.slice(inicio, fin)` en el frontend para paginar incidentes.
  - Enviar parámetros `page`, `size` y `sort` al backend (`/api/v1/incidentes/paginado`) y actualizar únicamente la página solicitada.

### 3.6 Optimización Multimedia en el Cliente
- **Compresión antes de enviar**:
  - Toda imagen seleccionada por el usuario debe pasar por `comprimirImagen(file)` (`imageCompressor.js`) antes de adjuntarse al `FormData`.
  - Parámetros estándar: Canvas API, calidad 0.7, resolución máxima 1920px en JPEG.
  - Validación de extensiones en inputs: `accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp"`.

---

## 4. Checklist Rápido para Todo Nuevo Requerimiento

Antes de dar por completada una tarea, verificar:
- [ ] ¿Hay código duplicado con alguna utilidad o componente existente?
- [ ] Si se agregó un filtro nuevo en base de datos, ¿cuenta con índice?
- [ ] Si se creó un listado, ¿está paginado en backend?
- [ ] Si se agregó una librería de más de 100KB, ¿se cargó con `lazy`?
- [ ] Si hay un input de búsqueda, ¿tiene debounce de 500ms?
- [ ] Si se suben imágenes, ¿están restringidas a 10MB y se comprimen con canvas?
- [ ] ¿Compilan `npm run build` y `./gradlew test` sin errores?
