package pw.prok.bootstrap;

import java.io.*;
import pw.prok.damask.dsl.*;

public final class LibraryArtifact
{
    private IArtifact mArtifact;
    private final String mLocation;
    private final String mFilename;
    private File mTarget;
    
    public LibraryArtifact(final String group, final String name, final String version, final String location, final String filename) {
        this(Builder.create().group(group).name(name).version(version).asArtifact(), location, filename);
    }
    
    public LibraryArtifact(final String group, final String name, final String version) {
        this(Builder.create().group(group).name(name).version(version).asArtifact(), null, null);
    }
    
    public LibraryArtifact(final IArtifact artifact) {
        this(artifact, null, null);
    }
    
    public LibraryArtifact(final IArtifact artifact, final String location, final String filename) {
        this.mArtifact = artifact;
        this.mLocation = location;
        this.mFilename = filename;
    }
    
    public LibraryArtifact(final IArtifact artifact, final File target) {
        this(artifact, null, null);
        this.mTarget = target;
    }
    
    public IArtifact getArtifact() {
        return this.mArtifact;
    }
    
    public boolean hasLocation() {
        return this.mLocation != null;
    }
    
    public String getLocation() {
        return this.mLocation;
    }
    
    public String getRealLocation() {
        if (this.mLocation != null) {
            return this.compute(this.mLocation);
        }
        final String groupId = this.mArtifact.getGroup().replace('.', '/');
        final String artifactId = this.mArtifact.getName();
        final String version = this.mArtifact.getVersion().toRawString();
        return String.format("%s/%s/%s", groupId, artifactId, version);
    }
    
    public boolean hasFilename() {
        return this.mFilename != null;
    }
    
    public String getFilename() {
        return this.mFilename;
    }
    
    public String getRealFilename() {
        if (this.mFilename != null) {
            return this.compute(this.mFilename);
        }
        final String artifactId = this.mArtifact.getName();
        final String version = this.mArtifact.getVersion().toRawString();
        String classifier = this.mArtifact.getClassifier();
        final String extension = this.mArtifact.getExtension();
        classifier = ((classifier != null && classifier.length() > 0) ? ('-' + classifier) : "");
        return String.format("%s-%s%s.%s", artifactId, version, classifier, extension);
    }
    
    public String compute(String s) {
        s = s.replace("<group>", this.mArtifact.getGroup().replace('.', '/'));
        s = s.replace("<artifact>", this.mArtifact.getName());
        s = s.replace("<version>", this.mArtifact.getVersion().toRawString());
        s = s.replace("<classifier>", this.mArtifact.getClassifier());
        s = s.replace("<extension>", this.mArtifact.getExtension());
        return s;
    }
    
    @Override
    public String toString() {
        return String.valueOf(this.mArtifact);
    }
    
    public void setArtifact(final IArtifact artifact) {
        this.mArtifact = artifact;
    }
    
    public File getTarget(final File rootDir) {
        if (this.mTarget != null) {
            return this.mTarget;
        }
        return new File(new File(rootDir, this.getRealLocation()), this.getRealFilename());
    }
    
    public void setTarget(final File target) {
        this.mTarget = target;
    }
}
