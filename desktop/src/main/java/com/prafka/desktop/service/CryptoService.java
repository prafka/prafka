package com.prafka.desktop.service;

import jakarta.inject.Singleton;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;

/**
 * Provides cryptographic operations for password hashing and data encryption.
 *
 * <p>Supports BCrypt password hashing, SHA-512 hashing, Base64 encoding/decoding,
 * and AES encryption/decryption for secure data storage.
 */
@Singleton
public class CryptoService {

    private final MessageDigest sha512;

    public CryptoService() throws NoSuchAlgorithmException {
        sha512 = MessageDigest.getInstance("SHA-512");
    }

    public String hashPasswordForStorage(String password) {
        return OpenBSDBCrypt.generate(sha512(password), new SecureRandom().generateSeed(16), 12);
    }

    public String hashPasswordForSession(String password) {
        return Hex.toHexString(sha512(password));
    }

    public boolean checkPasswordForStorage(String password, String hash) {
        return OpenBSDBCrypt.checkPassword(hash, sha512(password));
    }

    public byte[] sha512(String data) {
        return sha512.digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public String encodeBase64(String data) {
        return Base64.toBase64String(data.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] encodeBase64(byte[] data) {
        return Base64.encode(data);
    }

    public String decodeBase64(String data) {
        return Strings.fromByteArray(Base64.decode(data));
    }

    public byte[] decodeBase64(byte[] data) {
        return Base64.decode(data);
    }

    public byte[] encryptAES(byte[] data, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] ivBytes = new byte[16];
        new SecureRandom().nextBytes(ivBytes);
        var ivParameterSpec = new IvParameterSpec(ivBytes);

        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);

        byte[] encryptedBytes = cipher.doFinal(data);

        var byteBuffer = ByteBuffer.allocate(1 + ivBytes.length + encryptedBytes.length);
        byteBuffer.put((byte) ivBytes.length);
        byteBuffer.put(ivBytes);
        byteBuffer.put(encryptedBytes);

        return byteBuffer.array();
    }

    public byte[] decryptAES(byte[] data, SecretKey secretKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var byteBuffer = ByteBuffer.wrap(data);

        byte ivLength = byteBuffer.get();
        if (ivLength != 16) throw new IllegalArgumentException();
        byte[] ivBytes = new byte[ivLength];
        byteBuffer.get(ivBytes);
        var ivParameterSpec = new IvParameterSpec(ivBytes);

        byte[] encryptedBytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedBytes);

        var cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);

        return cipher.doFinal(encryptedBytes);
    }
}
