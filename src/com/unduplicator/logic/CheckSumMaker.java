package com.unduplicator.logic;

import com.unduplicator.ResourcesProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * <p>Created by MontolioV on 30.05.17.
 */
public class CheckSumMaker {
    private final MessageDigest MESSAGE_DIGEST;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();

    public CheckSumMaker(String mdAlgorithm) {
        try {
            this.MESSAGE_DIGEST = MessageDigest.getInstance(mdAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    resProvider.getStrFromExceptionBundle("noSuchHashAlgorithm"), e);
        }
    }

    public String makeCheckSum(File file) throws IOException {
        if (file.length() == 0) {
            throw new IOException(
                    resProvider.getStrFromExceptionBundle("fileIsEmpty") +
                    "\t" + file.toString());
        }

        byte[] bytesHash = makeBytesHash(file);
        return stringRepresentation(bytesHash);
    }

    private byte[] makeBytesHash(File file) throws IOException {
        try (BufferedInputStream bufferedIS = new BufferedInputStream(new FileInputStream(file))) {
            byte[] digestBuffer = new byte[1024];
            int inputStreamResponse = bufferedIS.read(digestBuffer);

            while (inputStreamResponse > -1) {
                MESSAGE_DIGEST.update(digestBuffer);
                inputStreamResponse = bufferedIS.read(digestBuffer);
            }
            return MESSAGE_DIGEST.digest();
        } catch (IOException e) {
            throw new IOException(resProvider.getStrFromExceptionBundle("hashingFail") +
                    "\t" + file.getAbsolutePath(), e);
        }
    }

    private String stringRepresentation(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
