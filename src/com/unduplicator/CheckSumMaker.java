package com.unduplicator;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>Created by MontolioV on 30.05.17.
 */
public class CheckSumMaker {
    private final MessageDigest messageDigest;

    public CheckSumMaker(String mdAlgorithm) {
        try {
            this.messageDigest = MessageDigest.getInstance(mdAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Choose another hash algorithm.", e);
        }
    }

    public String makeCheckSum(File file) throws IOException {
        try (BufferedInputStream buffIS = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[1024];
            while (buffIS.read(bytes) > 0) {
                messageDigest.update(bytes);
            }
            byte[] bCheckSum = messageDigest.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : bCheckSum) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (IOException e) {
            throw new IOException("Couldn't make checksum cause of IO trouble\t" +
                    file.getAbsolutePath(), e);
        }
    }
}
