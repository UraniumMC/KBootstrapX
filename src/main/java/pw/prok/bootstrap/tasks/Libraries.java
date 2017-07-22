package pw.prok.bootstrap.tasks;

import java.io.*;
import pw.prok.damask.dsl.*;
import pw.prok.bootstrap.*;

public class Libraries extends DefaultTask
{
    @Override
    public void make() {
        final File serverDir = this.getBinDir();
        final File libraries = new File(serverDir, "libraries");
        for (final String library : this.mMain.cli.getOptionValues(this.mMain.libraries.getOpt())) {
            Sync.syncArtifact(new LibraryArtifact(Builder.create().parse(library).asArtifact()), libraries, true);
        }
    }
}
