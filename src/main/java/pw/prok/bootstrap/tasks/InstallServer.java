package pw.prok.bootstrap.tasks;

import java.io.*;
import pw.prok.bootstrap.*;

public class InstallServer extends DefaultTask
{
    @Override
    public void make() throws Exception {
        final File serverDir = this.getServerDir();
        final File binDir = this.getBinDir();
        final File serverJar = new File(this.mMain.cli.getOptionValue(this.mMain.installServer.getOpt())).getCanonicalFile();
        if (!serverJar.exists()) {
            System.err.println("Server file not exists: " + serverJar);
            return;
        }
        make(serverDir, binDir, serverJar);
    }
    
    public static File make(final File serverDir, final File binDir, final File serverJar) throws Exception {
        System.out.println("Server directory: " + serverDir.getAbsolutePath());
        //final Sync.KCauldronInfo info = Sync.getInfo(serverJar);
        //final File targetServerJar = new LibraryArtifact(info.group, info.channel, info.version).getTarget(binDir);
        //if (!targetServerJar.getCanonicalPath().equals(serverJar.getCanonicalPath())) {
        //    Utils.copyFile(serverJar, targetServerJar);
        //}
        if (!Sync.sync(serverJar, binDir, true)) {
            throw new IllegalStateException("Could not install libraries");
        }
        DefaultTask.postInstall(serverDir, serverJar);
        return serverJar;
    }
}
