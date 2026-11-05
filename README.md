# producto-service-hib (arquitectura híbrida: cebolla + hexagonal)

API REST de productos con Spring Boot y PostgreSQL que **combina** las dos arquitecturas vistas en clase:

- De la **cebolla** (`cebolla/app-productos-ceb`) toma los **anillos**: `domain` → `application` → `infrastructure`, con la regla de que las dependencias siempre apuntan hacia el centro.
- Del **hexagonal** (`hexagonal/app-productos`) toma los **puertos y adaptadores**: puertos de entrada (`PortIn`) que implementan los casos de uso, puertos de salida (`PortOut`) pequeños y separados, y adaptadores que se conectan a ellos.

Mismos endpoints, mismas reglas de negocio y misma base de datos que las otras dos versiones. Solo cambia la organización.

## Arquitectura

Diagrama completo en [`docs/arquitectura.drawio`](docs/arquitectura.drawio). Se abre en [app.diagrams.net](https://app.diagrams.net) (*File → Open from → Device*) o en VS Code con la extensión **Draw.io Integration** (`hediet.vscode-drawio`).

```
 ┌──────────────────────────── Infraestructura (cebolla) ─────────────────────────────┐
 │  adapters/in/web  (adaptador de ENTRADA)        adapters/out/persistence (SALIDA)  │
 │  ProductoController ──┐                    ┌──► ProductoRepositoryAdapter (JPA)    │
 │  config/ BeanConfig   │                    │                                       │
 │  ┌────────────────────┼─ Aplicación ───────┼───────────────────────────────────┐   │
 │  │   ports/in  ◄──────┘                    │                                   │   │
 │  │   CrearProductoPortIn, ObtenerProductoPortIn, ...  (puertos de ENTRADA)     │   │
 │  │      ▲ implementan                      │                                   │   │
 │  │   usecase/ CrearProducto, ObtenerProducto, CambiarPrecio, Eliminar          │   │
 │  │  ┌──────────────── Servicios de dominio ┼────────────────────────────────┐  │   │
 │  │  │   ports/out  ◄───────────────────────┘ (implementados por el adaptador)│  │   │
 │  │  │   BuscarProductoPortOut, GuardarProductoPortOut, EliminarProductoPortOut│ │   │
 │  │  │  ┌─────────────── Modelo de dominio ─────────────────┐                 │  │   │
 │  │  │  │   Producto, Categoria, excepciones del dominio    │                 │  │   │
 │  │  │  └───────────────────────────────────────────────────┘                 │  │   │
 │  │  └────────────────────────────────────────────────────────────────────────┘  │   │
 │  └──────────────────────────────────────────────────────────────────────────────┘   │
 └─────────────────────────────────────────────────────────────────────────────────────┘
                    Las dependencias siempre apuntan hacia el centro
```

### Estructura de carpetas

```
src/main/java/com/ejemplo/producto/
├─ domain/                              ← anillos internos de la cebolla (Java puro)
│  ├─ model/          Producto, Categoria
│  ├─ exception/      PrecioInvalidoException, ProductoNoEncontradoException
│  └─ ports/out/      ← PUERTOS DE SALIDA (hexagonal) en el anillo "servicios de dominio"
│     ├─ BuscarProductoPortOut
│     ├─ GuardarProductoPortOut
│     └─ EliminarProductoPortOut
├─ application/                         ← anillo de servicios de aplicación
│  ├─ ports/in/       ← PUERTOS DE ENTRADA (hexagonal)
│  │  ├─ CrearProductoPortIn + CrearProductoCommand
│  │  ├─ ObtenerProductoPortIn
│  │  ├─ CambiarPrecioProductoPortIn
│  │  └─ EliminarProductoPortIn
│  └─ usecase/        ← casos de uso que implementan los puertos de entrada
│     ├─ CrearProducto, ObtenerProducto
│     └─ CambiarPrecioProducto, EliminarProducto
└─ infrastructure/                      ← anillo externo
   ├─ adapters/
   │  ├─ in/web/      ← ADAPTADOR DE ENTRADA: ProductoController, GlobalExceptionHandler, dto/, doc/
   │  └─ out/persistence/ ← ADAPTADOR DE SALIDA: ProductoRepositoryAdapter, ProductoEntity,
   │                        ProductoJpaRepository, ProductoMapper
   └─ config/         BeanConfig (ensambla puertos ↔ casos de uso ↔ adaptadores), OpenApiConfig
```

### ¿Qué se tomó de cada arquitectura?

| Concepto | Cebolla | Hexagonal | Híbrida |
|---|---|---|---|
| Organización principal | Anillos `domain / application / infrastructure` | `core / adapters` | **Anillos de la cebolla** |
| Contrato de persistencia | `IProductoRepository` (una interfaz) en `domain.services` | 3 `PortOut` en `core.ports.out` | **3 `PortOut`** en `domain.ports.out` (anillo servicios de dominio) |
| Contrato de los casos de uso | No hay: el controlador usa la clase concreta | `PortIn` en `core.ports.in` | **`PortIn`** en `application.ports.in` |
| Casos de uso | `application/` | `core.usecase` | `application.usecase` (implementan `PortIn`) |
| HTTP y JPA | `infrastructure.web`, `infrastructure.persistence` | `adapters.in.web`, `adapters.out.persistence` | `infrastructure.adapters.in.web`, `infrastructure.adapters.out.persistence` |

### Reglas de dependencia

| Capa | Paquete | Puede depender de |
|---|---|---|
| Modelo de dominio | `domain.model`, `domain.exception` | Nadie |
| Servicios de dominio (puertos de salida) | `domain.ports.out` | Modelo |
| Aplicación (puertos de entrada + casos de uso) | `application` | Dominio |
| Infraestructura (adaptadores + config) | `infrastructure` | Todas las internas |

Reglas extra del hexagonal:
- El controlador solo conoce **puertos de entrada** (`PortIn`), nunca los casos de uso concretos.
- Los casos de uso solo conocen **puertos de salida** (`PortOut`), nunca los adaptadores.
- `domain` y `application` no importan Spring, JPA ni HTTP; `BeanConfig` hace el cableado.

Estas reglas **se verifican automáticamente** en
[`ArquitecturaHibridaTest`](src/test/java/com/ejemplo/producto/arquitectura/ArquitecturaHibridaTest.java) con ArchUnit: si alguien rompe una, `mvnw test` falla.

### Flujo de una petición

```
PATCH /api/productos/1/precio
  └─► ProductoController                    (infrastructure/adapters/in/web)
        └─► CambiarPrecioProductoPortIn     (application/ports/in)        ← puerto de entrada
              = CambiarPrecioProducto       (application/usecase)
                ├─► BuscarProductoPortOut   (domain/ports/out)            ← puerto de salida
                │     = ProductoRepositoryAdapter (infrastructure/adapters/out/persistence)
                ├─► Producto.cambiarPrecio  (domain/model)                ← regla de negocio
                └─► GuardarProductoPortOut  (domain/ports/out)
                      = ProductoRepositoryAdapter
```

## Ejecutar

Requisitos: Java 21 y PostgreSQL con la base `topicos_db` (ver `src/main/resources/application.yml`).

```bash
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
./mvnw test
```

- API: `http://localhost:8090/api/productos`
- Swagger UI: `http://localhost:8090/swagger-ui.html`

> **DevTools** está activo: al recompilar una clase (Ctrl+S en VS Code, *Build* en IntelliJ) la app se reinicia sola, sin parar `spring-boot:run`. No se incluye en el `.jar` final.

> Usa el mismo puerto (8090) que las versiones hexagonal y cebolla: ejecuta solo una a la vez.

## Integración continua (GitHub Actions)

El workflow [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre `./mvnw verify` (las 30 pruebas, incluidas las de arquitectura y persistencia) en cada push a `main`/`master`, en cada pull request o a mano desde la pestaña *Actions*. Las pruebas usan la base PostgreSQL de **Render**.

Explicación paso a paso y lo que falta para desplegar: [`docs/ci.md`](docs/ci.md).

Las credenciales **no** van en el repositorio. Créalas en *Settings → Secrets and variables → Actions → New repository secret*:

| Secret | Valor |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-externo>.oregon-postgres.render.com:5432/<base>?sslmode=require` |
| `DB_USERNAME` | usuario de la base en Render |
| `DB_PASSWORD` | contraseña de la base en Render |

Notas:
- Usa el **hostname externo** (*External Database URL* en el panel de Render). El hostname corto `dpg-...-a` solo funciona desde servicios dentro de Render.
- Render exige SSL para conexiones externas, por eso la URL lleva `?sslmode=require`.
- Spring Boot toma `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` y reemplaza los valores de `application.yml`, así que en local se sigue usando `localhost`.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/productos` | Crear un producto |
| GET | `/api/productos` | Listar productos |
| GET | `/api/productos/{id}` | Obtener un producto |
| PATCH | `/api/productos/{id}/precio` | Cambiar el precio |
| DELETE | `/api/productos/{id}` | Eliminar un producto |
