# INFORME - Arena Segura: login, bestiario y ranking con JDBC + MVC

Estudiante: Sneyder Steven Gomez Acuna | Taller Unidad 5.2 (ejercicio individual)
Rama: `feature/jdbc-mvc` | Base de datos: `rpg_arena_db` (MySQL en Docker)

## 1. Flujo View -> Controller -> DAO -> BD (login)

_Pendiente: se completa en la Parte D, cuando exista el login con MVC._

## 2. Inyeccion SQL: evidencia y defensa

_Pendiente: se completa en la Parte B (salida de consola de `LoginInseguroDemo` con `admin' OR 1=1 -- ` y la misma entrada fallando en `JugadorDAO.autenticar`)._

## 3. Statement vs PreparedStatement (pruebas propias)

_Pendiente: tabla con seguridad y tiempos de 1000 inserciones con cada uno (Parte E)._

## 4. executeQuery vs executeUpdate

| Metodo | Se usa con | Devuelve |
|---|---|---|
| `executeQuery()` | `SELECT` (leer) | un `ResultSet` con las filas, que se recorre con `while (rs.next())` |
| `executeUpdate()` | `INSERT`, `UPDATE`, `DELETE` (modificar) | un `int` con las filas afectadas (0 = nada cambio) |

## 5. Errores encontrados y como los resolvi

**5.1 Driver de MySQL faltante.** Al quitar `mysql-connector-j` del `pom.xml` y ejecutar `ProbarConexion`:

    No se pudo conectar: Falta el driver de MySQL: agrega mysql-connector-j al pom.xml y pulsa Load Maven Changes. (detalle tecnico: No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db)

Causa: `DriverManager` no encontro ningun driver que aceptara `jdbc:mysql:`. Solucion: agregar la dependencia en el `pom.xml` y pulsar *Load Maven Changes*.

**5.2 `config.txt` no encontrado.** Salida real de mi ejecucion:

    No se pudo conectar: No se encontro config.txt en C:\Users\steve\Downloads\rpg-base\config.txt. Copia config.example.txt como config.txt y escribe tus datos. No se intento conectar.

Causa: el archivo no existia (al crearlo despues, IntelliJ lo dejo como `config.txt.txt`). Solucion: crearlo en la raiz y renombrarlo a `config.txt`.

**5.3 Paquete `dao` mal creado.** Quedo un `com.rpg` dentro de `com.rpg` y otro paquete llamado `do`. Solucion: borrar el sobrante y renombrar `do` a `dao` con *Refactor > Rename*.
