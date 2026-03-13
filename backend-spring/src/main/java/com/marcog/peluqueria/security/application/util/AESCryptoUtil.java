package com.marcog.peluqueria.security.application.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utilidad de Criptografía de Grado Empresarial (AES-256)
 * Basado en Advanced Encryption Standard (AES) en modo CBC con PKCS5Padding
 */
@Component
@Slf4j
public class AESCryptoUtil {

    private final SecretKeySpec secretKey;
    private final byte[] key;

    // Una clave privada configurada desde el archivo .env o application.properties
    public AESCryptoUtil(@Value("${chat.aes.secret-key:mySuperSecretKey123!}") String myKey) {
        MessageDigest sha = null;
        try {
            key = myKey.getBytes(StandardCharsets.UTF_8);
            sha = MessageDigest.getInstance("SHA-256");
            byte[] hashedKey = sha.digest(key);
            // Tomamos los primeros 16 bytes (128 bits) o 32 (256 bits) para la clave
            // secreta
            this.secretKey = new SecretKeySpec(Arrays.copyOf(hashedKey, 32), "AES");
            log.info("Key AES inicializada correctamente para el chat interno.");
        } catch (Exception e) {
            log.error("Error inicializando AES Key: {}", e.getMessage());
            throw new RuntimeException("Fallo criptográfico crítico", e);
        }
    }

    /**
     * Encripta un mensaje de texto plano y devuelve Base64
     */
    public String encrypt(String strToEncrypt) {
        try {
            // El Vector de Inicialización (IV) hace que el mismo mensaje cifrado dos veces
            // dé distintos hashes (Evita ataques de diccionario)
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);

            byte[] cipherText = cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            log.error("Error al encriptar: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Desencripta un string en Base64 volviéndolo texto legible
     */
    public String decrypt(String strToDecrypt) {
        try {
            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);

            byte[] cipherText = cipher.doFinal(Base64.getDecoder().decode(strToDecrypt));
            return new String(cipherText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al desencriptar: {}", e.getMessage());
        }
        return null;
    }
}
