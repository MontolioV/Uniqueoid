package com.unduplicator;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ResourceBundle;

/**
 * <p>Created by MontolioV on 30.05.17.
 */
public class CheckSumMaker {
    private ResourceBundle exceptionBundle;
    private final MessageDigest MESSAGE_DIGEST;

    public CheckSumMaker(ResourceBundle exceptionBundle, String mdAlgorithm) {
        this.exceptionBundle = exceptionBundle;
        try {
            this.MESSAGE_DIGEST = MessageDigest.getInstance(mdAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(exceptionBundle.getString("noSuchHashAlgorithm"), e);
        }
    }

    public String makeCheckSum(File file) throws IOException {
        if (file.length() == 0) {
            throw new IOException(exceptionBundle.getString("fileIsEmpty") + "\t" + file.toString());
        }
        try (BufferedInputStream buffIS = new BufferedInputStream(new FileInputStream(file))) {
            byte[] bytes = new byte[1024];
            while (buffIS.read(bytes) > 0) {
                MESSAGE_DIGEST.update(bytes);
            }
            byte[] bCheckSum = MESSAGE_DIGEST.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : bCheckSum) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (IOException e) {
            throw new IOException(exceptionBundle.getString("hashingFail") + "\t" +
                    file.getAbsolutePath(), e);
        }
    }
}
