package com.rpg.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * RETO EXTRA: las claves no se guardan en texto plano sino como hash SHA-256 (MessageDigest).
 *
 * El hash es de una sola via: de "admin123" se obtiene siempre el mismo texto de 64 caracteres
 * hexadecimales, pero desde ese texto no se puede volver a la clave original. Para comprobar un
 * login se calcula el hash de lo que escribio el usuario y se compara con el guardado. El
 * resultado coincide con la funcion SHA2(clave, 256) de MySQL usada en schema.sql.
 *
 * Limite: SHA-256 sin sal es rapido y dos usuarios con la misma clave tendrian el mismo hash;
 * en un sistema real se usaria un algoritmo lento con sal (bcrypt, PBKDF2, Argon2). Aqui se
 * usa SHA-256 porque es lo que pide el taller. Ver INFORME.md.
 */
public final class ClaveHash {

    private ClaveHash() {
    }

    public static String sha256(String clave) {
        Objects.requireNonNull(clave, "La clave no puede ser null.");
        try {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bytes = resumen.digest(clave.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM; si faltara, la aplicacion no puede funcionar.
            throw new IllegalStateException("Esta JVM no soporta SHA-256: " + e.getMessage(), e);
        }
    }
}
