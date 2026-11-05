# Integración continua y despliegue

Este documento explica, paso a paso, qué ocurre desde que se hace `git push` hasta que el código queda verificado, y qué falta para llegar a producción.

> **Estado actual:** el proyecto tiene **integración continua (CI)**, pero todavía **no tiene despliegue automático (CD)**. El workflow compila y prueba el código; no publica la aplicación en ningún servidor. La base de datos de Render ya existe, pero la API no está desplegada.

## CI y CD: la diferencia

| | Pregunta que responde | En este proyecto |
|---|---|---|
| **CI** (integración continua) | ¿El código compila y pasa las pruebas? | Implementado en [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) |
| **CD** (despliegue continuo) | ¿La nueva versión ya está publicada para los usuarios? | Pendiente (ver [Qué falta para desplegar](#qué-falta-para-desplegar)) |

## Flujo completo

```mermaid
flowchart LR
    A[git push a main] --> B[GitHub Actions]
    B --> C[Descargar código]
    C --> D[Instalar Java 21]
    D --> E[Verificar secrets]
    E --> F[mvnw verify]
    F <--> G[(PostgreSQL en Render)]
    F --> H[Guardar reportes]
    H --> I{¿Pruebas OK?}
    I -- Sí --> J[Check verde]
    I -- No --> K[Check rojo]
    J -.-> L[Desplegar: pendiente]
```

## Cuándo se ejecuta

| Evento | Cuándo ocurre |
|---|---|
| `push` | Al subir commits a `main` o `master` |
| `pull_request` | Al abrir o actualizar un pull request |
| `workflow_dispatch` | A mano, en *Actions → CI → Run workflow* |

Las ejecuciones van **una a la vez** (`concurrency`), porque todas comparten la misma base de datos. Si llega otra mientras una está corriendo, espera su turno.

## Paso a paso

Todo corre en una máquina virtual Ubuntu limpia que GitHub crea para la ejecución y elimina al terminar. El job tiene un límite de 20 minutos.

### 1. Descargar código

```yaml
uses: actions/checkout@v5
```

Trae al servidor el código del commit que disparó la ejecución. **Solo llega lo que está en git**: si un archivo está ignorado por `.gitignore`, el CI no lo ve aunque exista en tu PC.

### 2. Instalar Java 21

```yaml
uses: actions/setup-java@v5
with:
  distribution: temurin
  java-version: '21'
  cache: maven
```

Instala la misma versión de Java que declara el `pom.xml`. Con `cache: maven` guarda las dependencias descargadas, así las siguientes ejecuciones son más rápidas.

### 3. Verificar que existan los secrets

Revisa que las tres variables de conexión tengan valor. Si falta alguna, se detiene aquí con un mensaje claro, en lugar de fallar más adelante con un error de conexión difícil de entender.

### 4. Ejecutar las pruebas

```bash
chmod +x mvnw
./mvnw -B verify
```

- `chmod +x` le da permiso de ejecución a `mvnw`. En Windows no hace falta, pero en Linux sí.
- `-B` (*batch*) quita las preguntas interactivas y simplifica la salida.
- `verify` compila el código, corre las pruebas y genera el `.jar`.

Si una sola prueba falla, la ejecución queda en rojo.

| Pruebas | Clase | Cantidad | ¿Usa la base de Render? |
|---|---|---|---|
| Dominio | `ProductoTest` | 7 | No |
| Caso de uso | `CambiarPrecioProductoTest` | 3 | No (repositorio simulado con Mockito) |
| Arquitectura | `ArquitecturaHibridaTest` | 4 | No |
| API web | `ProductoControllerTest` | 11 | No (`@WebMvcTest`) |
| Persistencia | `ProductoRepositoryAdapterTest` | 5 | **Sí** (`@DataJpaTest`) |
| **Total** | | **30** | |

Solo las pruebas de persistencia se conectan a Render. Al arrancar, Flyway revisa el esquema de la base. Cada prueba corre dentro de una transacción que `@DataJpaTest` revierte al terminar, de modo que los datos de prueba no quedan guardados.

#### Ciclo de vida de `verify`

`verify` no es una tarea suelta: es una **fase** del ciclo de vida de Maven. Al pedirla, Maven ejecuta en orden todas las fases anteriores. Cada fase solo hace algo si tiene un plugin asociado.

```text
./mvnw verify

  validate                 revisa que el pom.xml sea correcto
     │
  process-resources        resources:resources      copia application.yml y las migraciones
     │
  compile                  compiler:compile         compila src/main/java
     │
  process-test-resources   resources:testResources  copia los recursos de prueba (aquí no hay)
     │
  test-compile             compiler:testCompile     compila src/test/java
     │
  test                     surefire:test            corre las 30 pruebas  ◄── si fallan, se detiene aquí
     │
  package                  jar:jar                  crea el .jar con las clases del proyecto
     │                     spring-boot:repackage    lo convierte en .jar ejecutable (~60 MB)
     │
  verify                   (sin plugin asociado)    última fase de la cadena
```

Las fases `pre-integration-test`, `integration-test` y `post-integration-test` también van antes de `verify`, pero aquí no hacen nada porque el proyecto no usa Failsafe. Por eso, en este proyecto, `verify` hace lo mismo que `package`.

Salida real de la ejecución (`./mvnw -B clean verify`), ya filtrada:

```text
--- clean:3.4.1:clean (default-clean) @ producto-service-hib ---
--- resources:3.3.1:resources (default-resources) @ producto-service-hib ---
--- compiler:3.14.1:compile (default-compile) @ producto-service-hib ---
--- resources:3.3.1:testResources (default-testResources) @ producto-service-hib ---
--- compiler:3.14.1:testCompile (default-testCompile) @ producto-service-hib ---
--- surefire:3.5.4:test (default-test) @ producto-service-hib ---
Tests run: 30, Failures: 0, Errors: 0, Skipped: 0
--- jar:3.4.2:jar (default-jar) @ producto-service-hib ---
--- spring-boot:3.5.7:repackage (repackage) @ producto-service-hib ---
BUILD SUCCESS
```

Cada línea sigue el formato `plugin:versión:goal (ejecución) @ proyecto`. `clean` aparece solo porque se pidió en este comando. Pertenece a otro ciclo de vida y el CI no lo necesita, ya que siempre empieza en una máquina limpia.

Al terminar, `target/` contiene:

| Archivo | Qué es |
|---|---|
| `producto-service-hib-0.0.1-SNAPSHOT.jar` | Ejecutable, con todas las dependencias (~60 MB). Es el que se desplegaría |
| `producto-service-hib-0.0.1-SNAPSHOT.jar.original` | Solo las clases del proyecto (~45 KB) |

### 5. Publicar los reportes

```yaml
if: always()
uses: actions/upload-artifact@v5
```

Guarda `target/surefire-reports/` durante 7 días, y lo hace **aunque las pruebas fallen** (`if: always()`), que es justo cuando más se necesita. Se descargan desde la página de la ejecución, en *Artifacts*.

Si la compilación falla, no hay reportes: las pruebas nunca llegaron a correr.

## Secrets

Las credenciales no se escriben en el repositorio. Se guardan en GitHub, en *Settings → Secrets and variables → Actions → Repository secrets*:

| Secret | Contenido |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host-externo>:5432/<base>?sslmode=require` |
| `DB_USERNAME` | Usuario de la base |
| `DB_PASSWORD` | Contraseña de la base |

El workflow las pasa a Spring Boot así:

```yaml
SPRING_DATASOURCE_URL: ${{ secrets.DB_URL }}
SPRING_DATASOURCE_USERNAME: ${{ secrets.DB_USERNAME }}
SPRING_DATASOURCE_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

Spring Boot da prioridad a estas variables de entorno sobre `application.yml`. Por eso en el CI se usa Render y en tu PC se sigue usando `localhost`, sin cambiar el código.

Tres detalles de la conexión a Render:

- **Hostname externo:** desde GitHub hay que usar el host completo, `dpg-...-a.<región>-postgres.render.com`. El corto, `dpg-...-a`, solo funciona dentro de Render.
- **SSL:** las conexiones externas lo necesitan; de ahí el `?sslmode=require`.
- **Formato JDBC:** Java necesita la URL con `jdbc:postgresql://`. La URL que muestra Render (`postgresql://usuario:clave@host/base`) no sirve tal cual: el usuario y la contraseña van aparte.

## Cómo revisar el resultado

1. Entra al repositorio en GitHub y abre la pestaña **Actions**.
2. Selecciona la ejecución. El check verde indica que pasó y la X roja que falló.
3. Abre el job y luego el paso que falló para ver su log.

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| Falla en "Verificar que existan los secrets" | El secret no existe o tiene otro nombre | Créalo en **Repository secrets** con el nombre exacto |
| `cannot find symbol` o `package does not exist` | Hay archivos que no se subieron a git | Compara `git ls-files` con tu carpeta y revisa `.gitignore` |
| Error de conexión o `UnknownHostException` | Se usó el hostname interno, o falta `sslmode=require` | Corrige el secret `DB_URL` |
| `password authentication failed` | La contraseña cambió | Actualiza el secret `DB_PASSWORD` |
| Pasa en local y falla en el CI | Diferencia entre Windows y Linux | Revisa mayúsculas en nombres de archivo y rutas |

> **Caso real de este proyecto:** la primera ejecución falló porque la regla `out/` del `.gitignore` también ignoraba `domain/ports/out/` y `adapters/out/`. Esos archivos existían en local pero nunca llegaron a GitHub. Se corrigió cambiándola por `/out/`, que solo afecta a la carpeta de la raíz.

## Qué falta para desplegar

Estos pasos **todavía no están implementados**. Son el camino propuesto para publicar la API en Render.

1. **Crear un `Dockerfile`.** Render no tiene un entorno nativo para Java, así que la aplicación se despliega como imagen Docker.
2. **Crear un Web Service en Render**, conectado a este repositorio y en la misma región que la base de datos (oregon).
3. **Configurar sus variables de entorno en Render:**
   - `SPRING_DATASOURCE_URL`: aquí sí conviene el hostname **interno**, porque la app y la base estarían en la misma red.
   - `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.
   - `PORT=8090`, el puerto en el que escucha la aplicación.
4. **Desactivar el auto-deploy** de Render y copiar la URL del *Deploy Hook* del servicio.
5. **Guardar esa URL** como secret de GitHub, por ejemplo `RENDER_DEPLOY_HOOK`.
6. **Agregar un job `deploy` a `ci.yml`** que corra solo en `main` y solo si las pruebas pasaron:

   ```yaml
   deploy:
     needs: test
     if: github.ref == 'refs/heads/main' && github.event_name == 'push'
     runs-on: ubuntu-latest
     steps:
       - name: Desplegar en Render
         run: curl -fsS -X POST "${{ secrets.RENDER_DEPLOY_HOOK }}"
   ```

   `needs: test` es la parte importante: si las pruebas fallan, no se despliega.

Antes de desplegar conviene separar las bases de datos. Hoy el CI prueba contra la base de Render; si la API de producción usa esa misma base, las pruebas correrían sobre los datos reales. Lo recomendable es que las pruebas usen una base aparte, por ejemplo un contenedor PostgreSQL dentro del propio workflow.
