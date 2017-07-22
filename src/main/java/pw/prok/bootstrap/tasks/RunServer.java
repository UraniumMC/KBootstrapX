package pw.prok.bootstrap.tasks;

import java.io.*;

public class RunServer extends DefaultTask
{
    @Override
    public void make() throws Exception {
        final File serverDir = this.getServerDir();
        final File binDir = this.getBinDir();
        final File serverJar = new File(this.mMain.cli.getOptionValue(this.mMain.runServer.getLongOpt()));
        this.runServer(InstallServer.make(serverDir, binDir, serverJar), serverDir);
    }
}
