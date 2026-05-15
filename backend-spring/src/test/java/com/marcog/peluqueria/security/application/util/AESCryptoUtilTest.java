package com.marcog.peluqueria.security.application.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del cifrado AES con IV aleatorio.
 * Verifica el round-trip y que dos cifrados del mismo texto produzcan ciphertexts distintos.
 */
class AESCryptoUtilTest {

    private static final String CLAVE_TEST =
            "7d3f9b1e8c4a2f6d5b9e3c7a1d4f8b2e6c9a5d3f7b1e4c8a2d6f9b3e5c7a1d4f";

    private AESCryptoUtil aesCryptoUtil;

    @BeforeEach
    void setUp() {
        aesCryptoUtil = new AESCryptoUtil(CLAVE_TEST);
    }

    @Test
    @DisplayName("encrypt + decrypt devuelve el texto original")
    void encryptDecrypt_devuelveOriginal() {
        String original = "Mensaje secreto del chat interno";

        String cifrado = aesCryptoUtil.encrypt(original);
        String descifrado = aesCryptoUtil.decrypt(cifrado);

        assertEquals(original, descifrado);
    }

    @Test
    @DisplayName("Dos cifrados del mismo texto producen ciphertexts distintos (IV aleatorio)")
    void encrypt_dosLlamadasMismoTexto_producenCiphertextsDistintos() {
        String original = "Texto repetido";

        String primeroCifrado = aesCryptoUtil.encrypt(original);
        String segundoCifrado = aesCryptoUtil.encrypt(original);

        assertNotEquals(primeroCifrado, segundoCifrado,
                "Con IV aleatorio dos cifrados del mismo texto deben diferir");
    }

    @Test
    @DisplayName("Ambos ciphertexts distintos descifran al mismo texto original")
    void decrypt_ambosCiphertextsDistintos_dansMismoOriginal() {
        String original = "Datos sensibles";
        String primero = aesCryptoUtil.encrypt(original);
        String segundo = aesCryptoUtil.encrypt(original);

        assertEquals(original, aesCryptoUtil.decrypt(primero));
        assertEquals(original, aesCryptoUtil.decrypt(segundo));
    }

    @Test
    @DisplayName("Constructor falla si la clave esta vacia")
    void constructor_claveVacia_lanzaExcepcion() {
        assertThrows(IllegalStateException.class, () -> new AESCryptoUtil(""));
        assertThrows(IllegalStateException.class, () -> new AESCryptoUtil(null));
    }

    @Test
    @DisplayName("decrypt devuelve null si el payload es muy corto")
    void decrypt_payloadCorto_devuelveNull() {
        String resultado = aesCryptoUtil.decrypt("YWJjZA=="); // 4 bytes

        assertNull(resultado);
    }

    @Test
    @DisplayName("Mensajes vacios o cortos cifran y descifran correctamente")
    void encryptDecrypt_textoCorto_devuelveOriginal() {
        assertEquals("a", aesCryptoUtil.decrypt(aesCryptoUtil.encrypt("a")));
        assertEquals("12", aesCryptoUtil.decrypt(aesCryptoUtil.encrypt("12")));
    }

    @Test
    @DisplayName("Caracteres especiales y unicode se preservan")
    void encryptDecrypt_unicode_preservaCaracteres() {
        String original = "Hola ñ áéíóú 你好 🎉";

        String cifrado = aesCryptoUtil.encrypt(original);

        assertEquals(original, aesCryptoUtil.decrypt(cifrado));
    }
}
