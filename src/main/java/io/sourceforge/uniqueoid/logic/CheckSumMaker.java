package io.sourceforge.uniqueoid.logic;

import io.sourceforge.uniqueoid.ResourcesProvider;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * I use SHA-1, SHA-256 and file size composition as "checksum".
 * <p>Created by MontolioV on 30.05.17.
 */
public class CheckSumMaker {
    private final MessageDigest MESSAGE_DIGEST_SHA256;
    private final MessageDigest MESSAGE_DIGEST_SHA1;
    private ResourcesProvider resProvider = ResourcesProvider.getInstance();
    private final int MAX_BUFFER_SIZE;

    public CheckSumMaker(FindTaskSettings findTaskSettings) {
        MAX_BUFFER_SIZE = findTaskSettings.getMaxBufferSize();
        try {
            this.MESSAGE_DIGEST_SHA256 = MessageDigest.getInstance("SHA-256");
            this.MESSAGE_DIGEST_SHA1 = MessageDigest.getInstance("SHA-1");
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

        return stringRepresentation(file.length(), makeBytesHash(file));
    }

    private byte[][] makeBytesHash(File file) throws IOException {
        try (BufferedInputStream bufferedIS = new BufferedInputStream(new FileInputStream(file))) {
            int bufferSize = file.length() < MAX_BUFFER_SIZE ? (int) file.length() : MAX_BUFFER_SIZE;
            byte[] digestBuffer = new byte[bufferSize];
            int inputStreamResponse = bufferedIS.read(digestBuffer);

            while (inputStreamResponse > -1) {
                if (inputStreamResponse == bufferSize) {
                    MESSAGE_DIGEST_SHA256.update(digestBuffer);
                    MESSAGE_DIGEST_SHA1.update(digestBuffer);
                } else {
                    MESSAGE_DIGEST_SHA256.update(digestBuffer, 0, inputStreamResponse);
                    MESSAGE_DIGEST_SHA1.update(digestBuffer, 0, inputStreamResponse);
                }
                inputStreamResponse = bufferedIS.read(digestBuffer);
            }
            byte[][] result = new byte[][]{MESSAGE_DIGEST_SHA256.digest(),
                                           MESSAGE_DIGEST_SHA1.digest()};
            return result;
        } catch (IOException e) {
            throw new IOException(resProvider.getStrFromExceptionBundle("hashingFail") +
                    "\t" + file.getAbsolutePath(), e);
        }
    }

    private String stringRepresentation(long fileLength, byte[]... bytes) {

        StringBuilder sb = new StringBuilder();
        for (byte[] bAr : bytes) {
            for (byte b : bAr) {
                sb.append(String.format("%02x", b));
            }
            sb.append("_");
        }
        sb.append(String.valueOf(fileLength));

        return sb.toString();
    }
}
