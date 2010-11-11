package org.limewire.security.certificate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.limewire.io.IOUtils;

public class HashCalculatorSHA1Impl implements HashCalculator {
    public byte[] calculate(byte[] in) {
        try {
            return calculate(new ByteArrayInputStream(in));
        } catch (IOException ex) {
            throw new RuntimeException("Impossible IOException caught: ", ex);
        }
    }

    public byte[] calculate(InputStream in) throws IOException {
        try {
            final MessageDigest outputSHA1 = MessageDigest.getInstance("SHA-1");
            final byte[] data = new byte[64 * 1024]; // 64k Chunks

            while (true) {
                final int bytesRead = in.read(data);
                if (bytesRead < 0)
                    break;
                outputSHA1.update(data, 0, bytesRead);
            }
            // Done, let's compute the hash
            return outputSHA1.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw IOUtils.getIOException("NoSuchAlgorithmException during computation: ", ex);
        }
    }
}
