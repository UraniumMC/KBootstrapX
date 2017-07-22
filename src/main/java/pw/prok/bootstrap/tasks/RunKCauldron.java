package pw.prok.bootstrap.tasks;

import java.io.*;

public class RunKCauldron extends DefaultTask
{
    @Override
    public void make() throws Exception {
        final File serverDir = this.getServerDir();
        final File binDir = this.getBinDir();
        final String artifactNotation = this.mMain.cli.getOptionValue(this.mMain.runKCauldron.getLongOpt());
        this.runServer(InstallKCauldron.make(serverDir, binDir, artifactNotation), serverDir);
    }
}
