# INFORME - Arena Segura: login, bestiario y ranking con JDBC + MVC

Estudiante: Sneyder Steven Gomez Acuna | Taller Unidad 5.2 (individual) | Rama `feature/jdbc-mvc` | BD `rpg_arena_db` (MySQL en Docker)

## 1. Flujo del login (View -> Controller -> DAO -> BD)

```
 Teclado --> LoginUI (View)       solo pide usuario y clave y muestra el mensaje
                | iniciarSesion(usuario, clave)
                v
             LoginController      max. 3 intentos; convierte la clave a SHA-256
                | autenticar(usuario, claveHash)
                v
             JugadorDAO           SELECT ... WHERE usuario = ? AND clave = ?  (PreparedStatement)
                | JDBC
                v
             MySQL  rpg_arena_db.jugadores
```

La respuesta vuelve igual: `Optional<Jugador>` -> `Resultado` -> mensaje en pantalla. La View nunca ve la base de datos.

## 2. Inyeccion SQL: evidencia y defensa

Ataque: usuario `admin' OR 1=1 -- ` (con un espacio al final) y cualquier clave. Salida de `LoginInseguroDemo`, caso 3:

```
--- Caso 3: ATAQUE: OR 1=1 y comentario ---
Entrada -> usuario="admin' OR 1=1 -- "  clave="no_la_se"
[INSEGURO] Statement + concatenacion
  SQL armado: SELECT id, usuario, rol FROM jugadores WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '87733d73aaf1c235d4db60e6810a2d2558f7c8ef68cf67c8239a8c74eb6cb20e'
  Filas devueltas por la consulta: 2
  RESULTADO: ENTRO como 'admin' (rol admin)  <<< VULNERABLE: entro SIN conocer la clave
[SEGURO] PreparedStatement + comodines ?
  RESULTADO: login RECHAZADO (usuario o clave incorrectos)  <<< DEFENDIDO: la entrada se trato como un dato, no como codigo
```

- **Por que entra con `Statement`:** el texto se pega dentro del SQL y queda `... WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '...'`. `1=1` siempre es verdadero y `-- ` comenta el resto, clave incluida: la base ejecuta la entrada como codigo.
- **Por que falla con `PreparedStatement` (diapositiva "El Escudo"):** el SQL lleva comodines (`WHERE usuario = ? AND clave = ?`) y los valores se entregan aparte con `setString`. Los `?` reservan espacio solo para datos, nunca para logica: la base busca un usuario llamado literalmente `admin' OR 1=1 -- `, no existe y la consulta queda intacta.

## 3. Statement vs PreparedStatement (pruebas propias)

| Aspecto | `Statement` | `PreparedStatement` |
|---|---|---|
| Seguridad | Vulnerable: `admin' OR 1=1 -- ` entra sin clave | Segura: la entrada es un dato y el login falla |
| Repeticion de consultas | Arma un SQL distinto en cada vuelta | Se escribe una vez; solo cambia el valor del `?` |
| Comillas y tipos | Se escapan a mano (`O'Brien` rompe el SQL) | `setString`, `setInt`... lo resuelven |
| Tiempo de 1000 inserciones (promedio de 5 rondas) | 3672 ms | 3644 ms |

Prueba: 1000 `INSERT` con cada uno, misma conexion, 5 rondas con calentamiento (`BenchmarkInserciones`, fuera del proyecto). Resultado: 3672 ms contra 3644 ms, menos de 1 % de diferencia y dentro del ruido (cada uno gano en algunas rondas). El tiempo parece dominado por el envio y el guardado de cada fila (autocommit), no por el analisis del SQL: la ventaja real del `PreparedStatement` es la seguridad, no la velocidad.

## 4. executeQuery vs executeUpdate

| Metodo | Se usa con | Devuelve | Ejemplo |
|---|---|---|---|
| `executeQuery()` | `SELECT` | un `ResultSet` (se recorre con `while (rs.next())`) | `JugadorDAO.autenticar` |
| `executeUpdate()` | `INSERT`, `UPDATE`, `DELETE` | un `int`: filas afectadas (0 = nada cambio) | `VictoriaDAO.registrar` |

## 5. Errores encontrados y como los resolvi

1. **Driver faltante:** sin `mysql-connector-j` en el `pom.xml` salio `No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db` (`DriverManager` no halla driver para `jdbc:mysql:`). Solucion: agregar la dependencia y *Load Maven Changes*.
2. **`config.txt` no encontrado:** IntelliJ lo habia creado como `config.txt.txt`. Solucion: renombrarlo a `config.txt` en la raiz.
3. **Paquete mal creado:** un `com.rpg` dentro de `com.rpg` y un paquete `do` (palabra reservada). Solucion: borrar el sobrante y renombrar `do` a `dao` con *Refactor > Rename*.

## 6. Reto extra y autoevaluacion

- **Hash SHA-256** (`MessageDigest`): el Controller convierte la clave y en la base nunca queda la real. **Sigue haciendo falta `PreparedStatement`** porque el hash solo protege el campo `clave`; `usuario` viaja tal cual y seria inyectable. (Sin sal es debil: en produccion se usa bcrypt, PBKDF2 o Argon2.)
- **Busquedas de la guia:** `Statement` sin `Prepared` y `java.sql` fuera de `dao` aparecen solo en `LoginInseguroDemo`, que necesita `Statement` para mostrar el ataque; es la unica excepcion, aislada en el paquete `demo`.
