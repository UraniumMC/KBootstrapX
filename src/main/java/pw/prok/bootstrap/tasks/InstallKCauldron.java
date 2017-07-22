package pw.prok.bootstrap.tasks;

import java.io.*;
import pw.prok.damask.*;
import pw.prok.bootstrap.*;
import pw.prok.damask.dsl.*;

public class InstallKCauldron extends DefaultTask
{
    private static final FileFilter VERSION_FILTER;
    
    @Override
    public void make() throws Exception {
        final File serverDir = this.getServerDir();
        final File binDir = this.getBinDir();
        final String artifactNotation = this.mMain.cli.getOptionValue(this.mMain.installKCauldron.getLongOpt());
        make(serverDir, binDir, artifactNotation);
    }
    
    public static File make(final File serverDir, final File binDir, String artifactNotation) throws Exception {
        artifactNotation = shorthand(artifactNotation);
        IArtifact artifact = Builder.create().parse(artifactNotation).asArtifact();
        LibraryArtifact jar = null;
        System.out.print("Resolve KCauldron version... ");
        try {
            artifact = Builder.create().fromArtifact(artifact).fromModuleVersion(Damask.get().versionList((IModule)artifact).getLatestVersion()).asArtifact();
            jar = findJar(binDir, artifact);
            System.out.println("SUCCESS: " + artifact.getVersion());
        }
        catch (Exception ignored) {
            System.out.print("FAILED\nTrying to find latest local version... ");
            jar = findJar(binDir, artifact);
            if (jar != null) {
                System.out.println("FOUND: " + jar.getArtifact().getVersion().toRawString() + "\nSo we're found something, attempting to launch...");
                artifact = jar.getArtifact();
            }
            else {
                System.out.println("FAILED\nNothing to launch ;( Goodbye!");
            }
        }
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        if (jar == null) {
            jar = new LibraryArtifact(artifact, new File(binDir, Builder.asPath((IModule)artifact, true, true)));
        }
        final File file = Sync.syncArtifact(jar, binDir, true);
        if (file == null) {
            throw new IllegalStateException("Could not install libraries");
        }
        DefaultTask.postInstall(serverDir, file);
        return file;
    }
    
    private static LibraryArtifact findJar(final File binDir, IArtifact artifact) {
        if (artifact.getVersion().isSpecified()) {
            final File f = new File(binDir, Builder.asPath((IModule)artifact, true, true));
            return f.exists() ? new LibraryArtifact(artifact, f) : null;
        }
        final File dir = new File(binDir, Builder.asPath((IModule)artifact, false, false));
        if (!dir.exists()) {
            return null;
        }
        final File[] versionDirs = dir.listFiles(InstallKCauldron.VERSION_FILTER);
        if (versionDirs == null || versionDirs.length == 0) {
            return null;
        }
        Version maxVersion = null;
        for (final File file : versionDirs) {
            final Version version = new Version(file.getName());
            if (maxVersion == null || version.compareTo(maxVersion) > 0) {
                maxVersion = version;
            }
        }
        if (maxVersion != null) {
            artifact = Builder.create().fromArtifact(artifact).version(maxVersion).asArtifact();
            final File f2 = new File(binDir, Builder.asPath((IModule)artifact, true, true));
            return f2.exists() ? new LibraryArtifact(artifact, f2) : null;
        }
        return null;
    }
    
    private static String shorthand(final String s) {
        if (s == null || "latest".equals(s)) {
            return "pw.prok:KCauldron:0+";
        }
        if (s.startsWith("backport-")) {
            return String.format("pw.prok:KCauldron-Backport-%s:0+", s.substring("backport-".length()));
        }
        return s;
    }
    
    static {
        VERSION_FILTER = new FileFilter() {
            public boolean accept(final File file) {
                return file.isDirectory();
            }
        };
    }
}
