package com.marcog.peluqueria.security.application.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utilidad de cifrado AES-256 en modo CBC con PKCS5Padding.
 *
 * El IV (vector de inicialización) se genera aleatorio en cada cifrado y se
 * antepone al ciphertext. Asi mensajes idénticos producen ciphertexts distintos
 * y no se pueden detectar patrones (CWE-329).
 *
 * Formato del payload base64: [16 bytes IV][N bytes ciphertext]
 */
@Component
@Slf4j
public class AESCryptoUtil {

    private static final int LONGITUD_IV_BYTES = 16;
    private static final String ALGORITMO = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec claveSecreta;
    private final SecureRandom generadorIv = new SecureRandom();

    public AESCryptoUtil(@Value("${chat.aes.secret-key:}") String claveCruda) {
        if (claveCruda == null || claveCruda.isBlank()) {
            throw new IllegalStateException(
                    "chat.aes.secret-key no configurada. Es obligatoria para el chat interno.");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(claveCruda.getBytes(StandardCharsets.UTF_8));
            this.claveSecreta = new SecretKeySpec(Arrays.copyOf(hash, 32), "AES");
            log.info("Key AES inicializada correctamente para el chat interno.");
        } catch (Exception ex) {
            log.error("Error inicializando AES Key: {}", ex.getMessage());
            throw new RuntimeException("Fallo criptográfico crítico", ex);
        }
    }

    public String encrypt(String textoPlano) {
        try {
            byte[] iv = generarIvAleatorio();
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, claveSecreta, new IvParameterSpec(iv));
            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            // Anteponer el IV al ciphertext para poder descifrar despues
            byte[] payload = new byte[LONGITUD_IV_BYTES + textoCifrado.length];
            System.arraycopy(iv, 0, payload, 0, LONGITUD_IV_BYTES);
            System.arraycopy(textoCifrado, 0, payload, LONGITUD_IV_BYTES, textoCifrado.length);

            return Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            log.error("Error al encriptar: {}", ex.getMessage());
            return null;
        }
    }

    public String decrypt(String payloadBase64) {
        try {
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            if (payload.length <= LONGITUD_IV_BYTES) {
                log.warn("Payload AES demasiado corto");
                return null;
            }

            byte[] iv = Arrays.copyOfRange(payload, 0, LONGITUD_IV_BYTES);
            byte[] textoCifrado = Arrays.copyOfRange(payload, LONGITUD_IV_BYTES, payload.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, claveSecreta, new IvParameterSpec(iv));
            byte[] textoPlano = cipher.doFinal(textoCifrado);
            return new String(textoPlano, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.warn("Payload AES invalido: {}", ex.getMessage());
            return null;
        }
    }

    private byte[] generarIvAleatorio() {
        byte[] iv = new byte[LONGITUD_IV_BYTES];
        generadorIv.nextBytes(iv);
        return iv;
    }
}
