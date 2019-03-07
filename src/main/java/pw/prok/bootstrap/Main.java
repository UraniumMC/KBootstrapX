package pw.prok.bootstrap;

import java.io.*;
import pw.prok.bootstrap.tasks.*;
import org.apache.commons.cli.*;

public class Main
{
    public final Options options;
    public final Option serverDir;
    public final Option binDir;
    public final Option jvmArgs;
    public final Option serverSymlinks;
    public final Option pidFile;
    public final Option installKCauldron;
    public final Option runKCauldron;
    public final Option installServer;
    public final Option runServer;
    public final Option libraries;
    public final Option repositories;
    public final CommandLineParser parser;
    public final HelpFormatter helpFormatter;
    public CommandLine cli;
    private boolean wasExecuted;
    public static Main instance;
    
    public Main() {
        this.wasExecuted = false;
        this.options = new Options();
        (this.serverDir = new Option("d", "serverDir", true, "Server root directory")).setArgName("dir");
        this.options.addOption(this.serverDir);
        (this.binDir = new Option("b", "binDir", true, "Server bin directory")).setArgName("dir");
        this.options.addOption(this.binDir);
        (this.jvmArgs = new Option("j", "jvmArg", true, "Server's JVM arguments")).setArgName("args");
        this.options.addOption(this.jvmArgs);
        (this.serverSymlinks = new Option("s", "serverSymlinks", true, "Server's symlinks")).setArgName("paths");
        this.serverSymlinks.setValueSeparator(File.pathSeparatorChar);
        this.options.addOption(this.serverSymlinks);
        (this.pidFile = new Option("p", "pidFile", true, "PID file for server")).setArgName("file");
        this.options.addOption(this.pidFile);
        (this.installKCauldron = new Option("k", "installKCauldron", true, "Install specified or latest KCauldron")).setArgName("version");
        this.installKCauldron.setOptionalArg(true);
        this.options.addOption(this.installKCauldron);
        (this.runKCauldron = new Option("r", "runKCauldron", true, "Install & run specified or latest KCauldron")).setArgName("version");
        this.runKCauldron.setOptionalArg(true);
        this.options.addOption(this.runKCauldron);
        (this.installServer = new Option("i", "installServer", true, "Install custom server")).setArgName("server file or url");
        this.options.addOption(this.installServer);
        (this.runServer = new Option("c", "runServer", true, "Install & run custom server")).setArgName("server file or url");
        this.options.addOption(this.runServer);
        (this.libraries = new Option("l", "libraries", true, "Install specified libraries into server dir")).setArgName("libraries");
        (this.repositories = new Option("repo","repositories",true,"Added custom repositories")).setArgName("repositories");
        this.options.addOption(this.repositories);
        this.libraries.setValueSeparator(File.pathSeparatorChar);
        this.options.addOption(this.libraries);
        this.parser = (CommandLineParser)new DefaultParser();
        this.helpFormatter = new HelpFormatter();
    }
    
    public static void main(final String[] args) {
        (Main.instance = new Main()).start(args);
    }
    
    private void start(final String[] args) {
        try {
            this.cli = this.parser.parse(this.options, args, true);
            if (this.cli.hasOption(this.libraries.getOpt())) {
                this.run(new Libraries());
            }
            if (this.cli.hasOption(this.installKCauldron.getOpt())) {
                this.run(new InstallKCauldron());
            }
            if (this.cli.hasOption(this.runKCauldron.getOpt())) {
                this.run(new RunKCauldron());
            }
            if (this.cli.hasOption(this.installServer.getOpt())) {
                this.run(new InstallServer());
            }
            if (this.cli.hasOption(this.runServer.getOpt())) {
                this.run(new RunServer());
            }
            if (!this.wasExecuted) {
                this.printHelp();
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
            this.printHelp();
        }
    }
    
    public void run(final DefaultTask task) {
        task.setMain(this);
        try {
            task.make();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        this.wasExecuted = true;
    }
    
    private void printHelp() {
        this.helpFormatter.printHelp("kbootstrap", this.options, true);
    }
}
