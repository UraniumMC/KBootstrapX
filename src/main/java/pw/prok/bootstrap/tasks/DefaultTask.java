package pw.prok.bootstrap.tasks;

import pw.prok.bootstrap.*;
import pw.prok.damask.*;
import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

public abstract class DefaultTask
{
    protected Main mMain;
    
    public void setMain(final Main main) {
        this.mMain = main;
        Damask.get().addRepository("uraniummc","https://repo.uraniummc.cc/repository/maven-public/");
        //Damask.get().addRepository("yumc", "http://repo.yumc.pw/content/groups/public");
        Damask.get().addRepository("mavencentral", "http://repo1.maven.org/maven2");
    }
    
    public File getServerDir() {
        try {
            final File dir = new File(this.mMain.cli.getOptionValue(this.mMain.serverDir.getOpt(), ".")).getCanonicalFile();
            dir.mkdirs();
            return dir;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public File getBinDir() {
        final String bin = this.mMain.cli.getOptionValue(this.mMain.binDir.getOpt());
        File dir;
        if (bin != null) {
            dir = new File(bin);
            if (!dir.isAbsolute()) {
                dir = new File(this.getServerDir(), bin);
            }
        }
        else {
            dir = new File(this.getServerDir(), "bin");
        }
        dir.mkdirs();
        return dir;
    }
    
    public File getPidFile() {
        final String pid = this.mMain.cli.getOptionValue(this.mMain.pidFile.getOpt());
        File file;
        if (pid != null) {
            file = new File(pid);
            if (!file.isAbsolute()) {
                file = new File(this.getServerDir(), pid);
            }
        }
        else {
            file = new File(this.getServerDir(), "server.pid");
        }
        file.getParentFile().mkdirs();
        return file;
    }
    
    public void putJvmArgs(final List<String> args) {
        final String[] z = this.mMain.cli.getOptionValues(this.mMain.jvmArgs.getOpt());
        if (z == null) {
            return;
        }
        for (final String argRaw : z) {
            for (String arg : argRaw.split(" ")) {
                arg = arg.trim();
                if (arg.length() > 0) {
                    args.add(arg);
                }
            }
        }
    }
    
    public void runServer(final File serverJar, final File serverDir) throws Exception {
        final String javaHome = System.getProperty("java.home");
        final String javaPath = String.format("%s/bin/java", javaHome);
        final List<String> args = new ArrayList<String>();
        args.add(javaPath);
        this.putJvmArgs(args);
        args.add("-jar");
        args.add(serverJar.getCanonicalPath());
        args.add("nogui");
        args.addAll(this.mMain.cli.getArgList());
        final ProcessBuilder builder = new ProcessBuilder(new String[0]);
        builder.directory(serverDir);
        builder.command(args);
        builder.environment().put("JAVA_HOME", javaHome);
        builder.environment().put("KCAULDRON_HOME", serverDir.getCanonicalPath());
        builder.environment().put("KBOOTSTRAP_ACTIVE", "true");
        builder.inheritIO();
        final Process process = builder.start();
        final int pid = getPid(process);
        if (pid > 0) {
            this.writePid(pid);
        }
        process.waitFor();
    }
    
    private void writePid(final int pid) {
        try {
            final File pidFile = this.getPidFile();
            pidFile.deleteOnExit();
            final Writer writer = new FileWriter(pidFile);
            writer.write(String.format("%d\n", pid));
            writer.close();
        }
        catch (Exception e) {
            new IllegalStateException("Failed to write pid file, ignoring...", e).printStackTrace();
        }
    }
    
    public static int getPid(final Process process) {
        try {
            final Class<?> processClass = process.getClass();
            final Field field = processClass.getDeclaredField("pid");
            field.setAccessible(true);
            return field.getInt(process);
        }
        catch (NoSuchFieldException ex) {}
        catch (IllegalAccessException ex2) {}
        catch (IllegalArgumentException ex3) {}
        return -1;
    }
    
    public abstract void make() throws Exception;
    
    public static void postInstall(final File serverDir, final File serverJar) throws Exception {
        final String[] symlinks = Main.instance.cli.getOptionValues(Main.instance.serverSymlinks.getOpt());
        if (symlinks != null) {
            for (final String symlink : symlinks) {
                final File symlinkPath = new File(serverDir, symlink);
                Files.deleteIfExists(symlinkPath.toPath());
                Files.createSymbolicLink(symlinkPath.toPath(), serverJar.toPath(), (FileAttribute<?>[])new FileAttribute[0]);
            }
        }
    }
}
