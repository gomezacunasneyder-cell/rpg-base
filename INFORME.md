# INFORME - Arena Segura (JDBC + MVC)

Sneyder Steven Gomez Acuna | Taller Unidad 5.2 (individual) | Rama `main` | BD `rpg_arena_db`

## 1. Flujo del login (View -> Controller -> DAO -> BD)

```
 LoginUI (View) --> LoginController  --> JugadorDAO            --> MySQL
 pide y muestra     3 intentos max.;     PreparedStatement         rpg_arena_db
 el mensaje         clave -> SHA-256     usuario=? AND clave=?     tabla jugadores
 <-- vuelve igual: Optional<Jugador> -> Resultado -> mensaje (la View no ve la BD)
```

## 2. Inyeccion SQL: evidencia y defensa

```
# Extracto de la salida de LoginInseguroDemo (... = omitido)
Entrada -> usuario="admin' OR 1=1 -- "  clave="no_la_se"   (espacio al final)
[INSEGURO] ... SELECT ... WHERE usuario = 'admin' OR 1=1 -- ' AND clave = '8773...'
  RESULTADO: ENTRO como 'admin' ... <<< VULNERABLE: entro SIN conocer la clave
[SEGURO] ...
  RESULTADO: login RECHAZADO ... <<< DEFENDIDO: la entrada se trato como un dato...
```

- **`Statement`:** el texto se pega en el SQL: `1=1` siempre es verdadero y `-- ` comenta la clave, asi que la entrada se ejecuta como codigo.
- **`PreparedStatement` (diapositiva "El Escudo"):** los `?` solo reciben datos, nunca logica: la base busca un usuario llamado `admin' OR 1=1 -- `, no existe y la consulta queda intacta.

## 3. Statement vs PreparedStatement (pruebas propias)

| Aspecto | `Statement` | `PreparedStatement` |
|---|---|---|
| Seguridad | Vulnerable | Segura: la entrada es un dato |
| Repeticion de consultas | SQL nuevo cada vez | Un solo SQL; cambia el `?` |
| 1000 inserciones, promedio de 5 rondas (`BenchmarkInserciones`) | 3672 ms | 3644 ms (dif. < 1 %: ruido) |

## 4. executeQuery vs executeUpdate

- **`executeQuery()`**: para `SELECT`; devuelve un `ResultSet` (ej. `JugadorDAO.autenticar`).
- **`executeUpdate()`**: para `INSERT`, `UPDATE`, `DELETE`; devuelve un `int`, las filas afectadas.

## 5. Errores encontrados y como los resolvi

1. **Driver faltante:** `No suitable driver found for jdbc:mysql://localhost:3306/rpg_arena_db`; agregue `mysql-connector-j` al `pom.xml`.
2. **`config.txt.txt`** (IntelliJ agrego otra extension): lo renombre a `config.txt`.
3. **Paquete `do`** (palabra reservada) y `com.rpg` anidado: renombre a `dao` y borre el sobrante.

## 6. Reto extra y autoevaluacion

- **Hash SHA-256** (`MessageDigest`): la clave real no queda en la base, pero **sigue haciendo falta `PreparedStatement`**: el hash solo protege `clave` y `usuario` seguiria siendo inyectable.
- **Autoevaluacion:** `Statement` sin `Prepared` y `java.sql` fuera de `dao`: solo en `LoginInseguroDemo`.
