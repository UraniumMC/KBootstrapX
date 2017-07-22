package pw.prok.bootstrap;

import java.security.*;
import java.io.*;

public class Utils
{
    public static String binToHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (final byte b : bytes) {
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }
    
    public static String readFile(final File file) {
        try {
            final InputStream is = new BufferedInputStream(new FileInputStream(file));
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            reader.close();
            return builder.toString();
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String sha1(final File file) {
        return digest("SHA-1", file);
    }
    
    public static String md5(final File file) {
        return digest("MD5", file);
    }
    
    public static String digest(final String algorithm, final File file) {
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm.toUpperCase());
            final InputStream is = new BufferedInputStream(new FileInputStream(file));
            final byte[] buffer = new byte[4096];
            int c;
            while ((c = is.read(buffer)) > 0) {
                md.update(buffer, 0, c);
            }
            is.close();
            return binToHex(md.digest());
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static void removeDir(final File dir) {
        if (dir == null) {
            return;
        }
        final File[] files = dir.listFiles();
        if (files != null && files.length != 0) {
            for (final File f : files) {
                if (f.isDirectory()) {
                    removeDir(f);
                }
                f.delete();
            }
        }
        dir.delete();
    }
    
    public static void writeToFile(final File file, final String s) {
        try {
            file.getParentFile().mkdirs();
            final OutputStream os = new FileOutputStream(file);
            final Writer writer = new OutputStreamWriter(os, "utf-8");
            writer.write(s);
            writer.close();
            os.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static File checksumFile(final String algo, final File file) {
        return new File(file.getAbsolutePath() + "." + algo);
    }
    
    public static String readChecksum(final String algo, final File file) {
        final File checksumFile = checksumFile(algo, file);
        if (!checksumFile.exists()) {
            return null;
        }
        final String checksum = readFile(checksumFile);
        return (checksum == null) ? null : checksum.trim();
    }
    
    public static void writeChecksum(final String algo, final File file) {
        writeToFile(checksumFile(algo, file), digest(algo, file));
    }
    
    public static void copyFile(final File in, final File out) throws IOException {
        out.getParentFile().mkdirs();
        copyStream(new FileInputStream(in), new FileOutputStream(out));
    }
    
    private static void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        final byte[] bytes = new byte[4096];
        int c;
        while ((c = inputStream.read(bytes)) > 0) {
            outputStream.write(bytes, 0, c);
        }
        outputStream.close();
        inputStream.close();
    }
}
