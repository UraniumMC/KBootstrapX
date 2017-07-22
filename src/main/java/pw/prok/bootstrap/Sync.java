package pw.prok.bootstrap;

import java.util.zip.*;
import java.io.*;
import java.util.jar.*;
import java.util.*;
import pw.prok.damask.*;

public class Sync
{
    private static final String[] ALGORITHMS;
    
    public static KCauldronInfo getInfo(final File jar) {
        boolean kcauldron = false;
        String group = null;
        String channel = null;
        String version = null;
        String[] classpath = null;
        try {
            final ZipFile serverZip = new ZipFile(jar);
            final ZipEntry entry = serverZip.getEntry("META-INF/MANIFEST.MF");
            final InputStream is = serverZip.getInputStream(entry);
            final Manifest manifest = new Manifest(is);
            is.close();
            serverZip.close();
            final Attributes attributes = manifest.getMainAttributes();
            if (attributes.getValue("KCauldron-Version") != null) {
                kcauldron = true;
                version = attributes.getValue("KCauldron-Version");
                channel = attributes.getValue("KCauldron-Channel");
                group = attributes.getValue("KCauldron-Group");
                if (group == null) {
                    group = "pw.prok";
                }
            }
            else {
                version = attributes.getValue("Implementation-Version");
                group = "unknown";
                channel = "unknown";
            }
            final String cp = attributes.getValue("Class-Path");
            classpath = ((cp == null) ? new String[0] : cp.split(" "));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return new KCauldronInfo(kcauldron, group, channel, version, classpath);
    }
    
    public static void parseLibraries(final File jar, final List<LibraryArtifact> artifacts) {
        final String[] cp = getInfo(jar).classpath;
        if (cp == null) {
            return;
        }
        for (String s : cp) {
            if ("minecraft_server.1.7.10.jar".equals(s)) {
                artifacts.add(new LibraryArtifact("net.minecraft", "server", "1.7.10", ".", "minecraft_server.1.7.10.jar"));
            }
            else {
                final boolean legacy = s.startsWith("libraries/");
                if (legacy) {
                    s = s.substring("libraries/".length());
                }
                int c = s.lastIndexOf(47);
                final String filename = s.substring(c + 1);
                s = s.substring(0, c);
                final String version = s.substring((c = s.lastIndexOf(47)) + 1).trim();
                s = s.substring(0, c);
                final String artifact = s.substring((c = s.lastIndexOf(47)) + 1).trim();
                s = s.substring(0, c);
                final String group = s.replace("../", "").replace('/', '.');
                artifacts.add(new LibraryArtifact(group, artifact, version, legacy ? "libraries/<group>/<artifact>/<version>" : null, legacy ? filename : null));
            }
        }
    }
    
    public static boolean sync(final File serverFile, final File rootDir, final boolean recursive) {
        final List<LibraryArtifact> artifacts = new ArrayList<LibraryArtifact>();
        parseLibraries(serverFile, artifacts);
        return sync(artifacts, rootDir, recursive);
    }
    
    public static boolean sync(final List<LibraryArtifact> artifacts, final File rootDir, final boolean recursive) {
        try {
            for (final LibraryArtifact artifact : artifacts) {
                if (syncArtifact(artifact, rootDir, recursive) == null) {
                    return false;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    private static boolean checksum(final File artifactFile) {
        for (final String algorithm : Sync.ALGORITHMS) {
            try {
                final String digest = Utils.digest(algorithm, artifactFile);
                final String checksum = Utils.readChecksum(algorithm, artifactFile);
                if (digest == null || checksum == null || !digest.equalsIgnoreCase(checksum)) {
                    return false;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }
    
    public static File syncArtifact(final LibraryArtifact artifact, final File rootDir, final boolean recursive) {
        final File artifactFile = artifact.getTarget(rootDir);
        if (!artifactFile.exists() || !checksum(artifactFile)) {
            System.out.print("Downloading " + artifact + "... ");
            try {
                artifactFile.getParentFile().mkdirs();
                Damask.get().artifactResolve(artifact.getArtifact(), artifactFile, false);
                for (final String algorithm : Sync.ALGORITHMS) {
                    Utils.writeChecksum(algorithm, artifactFile);
                }
                System.out.println("DONE!");
            }
            catch (Exception e) {
                System.out.println("ERROR!");
                e.printStackTrace();
                return null;
            }
        }
        if (recursive) {
            final List<LibraryArtifact> artifacts = new ArrayList<LibraryArtifact>();
            parseLibraries(artifactFile, artifacts);
            if (!sync(artifacts, rootDir, true)) {
                return null;
            }
        }
        return artifactFile;
    }
    
    public static void resolveLatestVersion(final File basedir, final LibraryArtifact lib) {
    }
    
    static {
        ALGORITHMS = new String[] { "sha-1", "md5" };
    }
    
    public static class KCauldronInfo
    {
        public final boolean kcauldron;
        public final String group;
        public final String channel;
        public final String version;
        public final String[] classpath;
        
        public KCauldronInfo(final boolean kcauldron, final String group, final String channel, final String version, final String[] classpath) {
            this.kcauldron = kcauldron;
            this.group = group;
            this.channel = channel;
            this.version = version;
            this.classpath = classpath;
        }
    }
}
