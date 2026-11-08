# INFORME - Arena Segura: login, bestiario y ranking con JDBC + MVC

Estudiante: Sneyder Steven Gomez Acuna | Taller Unidad 5.2 (ejercicio individual)
Rama: `feature/jdbc-mvc` | Base de datos: `rpg_arena_db` (MySQL en Docker)

## 1. Flujo View -> Controller -> DAO -> BD (login)

_Pendiente: se completa en la Parte D, cuando exista el login con MVC._

## 2. Inyeccion SQL: evidencia y defensa

Entrada del ataque (lleva **un espacio al final**, MySQL lo exige despues de `--`): `admin' OR 1=1 -- ` con cualquier clave.

**Evidencia** (salida de consola de `LoginInseguroDemo`, caso 3):

```
PEGAR AQUI LA SALIDA REAL DE TU EJECUCION (caso 3: ATAQUE OR 1=1 y comentario)

--- Caso 3: ATAQUE: OR 1=1 y comentario ---
Entrada -> usuario="admin' OR 1=1 -- "  clave="no_la_se"
[INSEGURO] Statement + concatenacion
  SQL armado: SELECT id, usuario, rol FROM jugadores WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '87733d73aaf1c235d4db60e6810a2d2558f7c8ef68cf67c8239a8c74eb6cb20e'
  Filas devueltas por la consulta: 2
  RESULTADO: ENTRO como 'admin' (rol admin)  <<< VULNERABLE: entro SIN conocer la clave
[SEGURO] PreparedStatement + comodines ?
  RESULTADO: login RECHAZADO (usuario o clave incorrectos)  <<< DEFENDIDO: la entrada se trato como un dato, no como codigo
```

**Por que entra con `Statement` y concatenacion.** El texto del usuario se pega dentro del SQL y queda:
`... WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '...'`. La condicion `1=1` siempre es verdadera y `-- ` comenta el resto, incluida la comparacion de la clave. La base ejecuta la entrada como si fuera codigo.

**Por que falla con `PreparedStatement` (diapositiva "El Escudo").** El SQL se escribe con comodines (`WHERE usuario = ? AND clave = ?`) y los valores se entregan aparte con `setString`. Los `?` reservan espacios solo para datos, nunca para logica ejecutable: la base busca un usuario cuyo nombre literal sea exactamente `admin' OR 1=1 -- `, no existe, y la logica de la consulta permanece intacta. No hay inyeccion posible.

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
