package pw.prok.bootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pw.prok.damask.Damask;
import pw.prok.damask.dsl.IArtifact;

public class Sync{

    private static File mMavenRepoDir=null;
    private static boolean mMavenRepoDirResloved=false;
    private static File mGradleRepoDir=null;
    private static boolean mGradleRepoDirResloved=false;

    public static File getUserDir(){
        for(String sEnv : new String[]{"HOME","USERPROFILE","USER_HOME"}){
            String tUserDirStr=System.getenv(sEnv);
            if(tUserDirStr!=null){
                File tDir=new File(tUserDirStr);
                if(tDir.isDirectory()) return tDir;
            }
        }

        String tPath=System.getProperty("user.home");
        return tPath==null?null:new File(tPath);
    }

    public static File getMavenRepoDir(){
        synchronized(Sync.class){
            if(!Sync.mMavenRepoDirResloved){
                Sync.mMavenRepoDirResloved=true;
                String tDir=System.getenv("M2_REPO");
                if(tDir==null){
                    File tUserDir=Sync.getUserDir();
                    if(tUserDir==null) return null;
                    Sync.mMavenRepoDir=tUserDir;
                    File tMVNSetting=new File(tUserDir,File.separator+".m2"+File.separator+"settings.xml");
                    try{
                        Document tDoc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(tMVNSetting);
                        NodeList tList=tDoc.getElementsByTagName("localRepository");
                        for(int i=0;i<tList.getLength();i++){
                            Node tNode=tList.item(i);
                            if(tNode.getNodeType()!=Node.ELEMENT_NODE)
                                continue;

                            File tFile=new File(tNode.getTextContent()+"".replace("${user.home}",System.getProperty("user.home")));
                            if(tFile.isDirectory()){
                                Sync.mMavenRepoDir=tFile;
                                break;
                            }
                        }
                    }catch(SAXException|IOException|ParserConfigurationException e){
                        System.out.println("Cannot reload maven setting file: "+e.getMessage());
                    }
                }else{
                    Sync.mMavenRepoDir=new File(tDir);
                }
            }
        }
        return Sync.mMavenRepoDir;
    }

    public static File getGradleRepoDir(){
        synchronized(Sync.class){
            if(!Sync.mGradleRepoDirResloved){
                Sync.mGradleRepoDirResloved=true;
                String tDir=System.getenv("GRADLE_USER_HOME");
                if(tDir==null){
                    File tUserDir=Sync.getUserDir();
                    if(tUserDir==null) return null;
                    Sync.mGradleRepoDir=new File(tUserDir,File.separator+".gradle");
                }else{
                    Sync.mGradleRepoDir=new File(tDir);
                }

                if(Sync.mGradleRepoDir!=null&&Sync.mGradleRepoDir.isDirectory()){
                    Sync.mGradleRepoDir=new File(Sync.mGradleRepoDir,"caches"+File.separator+"modules-2"+File.separator+"files-2.1");
                }
            }
        }
        return Sync.mGradleRepoDir;
    }

    private static final String[] ALGORITHMS;

    public static KCauldronInfo getInfo(final File jar){
        boolean kcauldron=false;
        String group=null;
        String channel=null;
        String version=null;
        String[] classpath=null;
        try{
            final ZipFile serverZip=new ZipFile(jar);
            final ZipEntry entry=serverZip.getEntry("META-INF/MANIFEST.MF");
            final InputStream is=serverZip.getInputStream(entry);
            final Manifest manifest=new Manifest(is);
            is.close();
            serverZip.close();
            final Attributes attributes=manifest.getMainAttributes();
            if(attributes.getValue("KCauldronX-Version")!=null){
                kcauldron=true;
                version=attributes.getValue("KCauldronX-Version");
                channel=attributes.getValue("KCauldronX-Channel");
                group=attributes.getValue("KCauldronX-Group");
                if(group==null){
                    group="pw.prok";
                }
            }else{
                version=attributes.getValue("Implementation-Version");
                group="unknown";
                channel="unknown";
            }
            final String cp=attributes.getValue("Class-Path");
            classpath=((cp==null)?new String[0]:cp.split(" "));
        }catch(Exception e){
            e.printStackTrace();
        }
        return new KCauldronInfo(kcauldron,group,channel,version,classpath);
    }

    public static void parseLibraries(final File jar,final List<LibraryArtifact> artifacts){
        final String[] cp=getInfo(jar).classpath;
        if(cp==null){
            return;
        }
        for(String s : cp){
            if("minecraft_server.1.7.10.jar".equals(s)){
                artifacts.add(new LibraryArtifact("net.minecraft","server","1.7.10",".","minecraft_server.1.7.10.jar"));
            }else{
                final boolean legacy=s.startsWith("libraries/");
                if(legacy){
                    s=s.substring("libraries/".length());
                }
                int c=s.lastIndexOf(47);
                final String filename=s.substring(c+1);
                s=s.substring(0,c);
                final String version=s.substring((c=s.lastIndexOf(47))+1).trim();
                s=s.substring(0,c);
                final String artifact=s.substring((c=s.lastIndexOf(47))+1).trim();
                s=s.substring(0,c);
                final String group=s.replace("../","").replace('/','.');
                artifacts.add(new LibraryArtifact(group,artifact,version,legacy?"libraries/<group>/<artifact>/<version>":null,legacy?filename:null));
            }
        }
    }

    public static boolean sync(final File serverFile,final File rootDir,final boolean recursive){
        final List<LibraryArtifact> artifacts=new ArrayList<LibraryArtifact>();
        parseLibraries(serverFile,artifacts);
        return sync(artifacts,rootDir,recursive);
    }

    public static boolean sync(final List<LibraryArtifact> artifacts,final File rootDir,final boolean recursive){
        try{
            for(final LibraryArtifact artifact : artifacts){
                if(syncArtifact(artifact,rootDir,recursive)==null){
                    return false;
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static boolean checksum(final File artifactFile){
        for(final String algorithm : Sync.ALGORITHMS){
            try{
                final String digest=Utils.digest(algorithm,artifactFile);
                final String checksum=Utils.readChecksum(algorithm,artifactFile);
                if(digest==null||checksum==null||!digest.equalsIgnoreCase(checksum)){
                    return false;
                }
            }catch(Exception e){
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static File syncArtifact(final LibraryArtifact artifact,final File rootDir,final boolean recursive){
        final File artifactFile=artifact.getTarget(rootDir);
        if(!artifactFile.exists()||!checksum(artifactFile)){
            artifactFile.getParentFile().mkdirs();
            File tRepoDir=Sync.getMavenRepoDir(),tMavenLocalFile=null;
            boolean tResolved=false;
            if(tRepoDir!=null){// resolve by maven
                IArtifact tArtifact=artifact.getArtifact();
                File tLocalFile=new File(tRepoDir,
                        (tArtifact.getGroup().replace('.',File.separatorChar))+File.separator+tArtifact.getName()+File.separator+tArtifact.getVersion());
                if(tLocalFile.isDirectory()){
                    String tTargetFile=tArtifact.getName()+"-"+tArtifact.getVersion();
                    if(tArtifact.getClassifier()!=null&&!tArtifact.getClassifier().isEmpty())
                        tTargetFile+='-'+tArtifact.getClassifier();
                    tTargetFile+='.'+tArtifact.getExtension();
                    tLocalFile=new File(tLocalFile,tTargetFile);
                    if(tLocalFile.isFile()){
                        System.out.println("Maven local found "+artifact);
                        try{
                            Utils.copyFile(tLocalFile,artifactFile);
                            tResolved=true;
                        }catch(IOException e){
                            System.out.println("Error on copy maven repo file! "+e.getMessage());
                        }
                    }else{
                        tMavenLocalFile=tLocalFile;
                    }
                }
            }
            if(!tResolved&&(tRepoDir=Sync.getGradleRepoDir())!=null){// resolve by gradle
                IArtifact tArtifact=artifact.getArtifact();
                File tLocalFile=new File(tRepoDir,tArtifact.getGroup()+File.separator+tArtifact.getName()+File.separator+tArtifact.getVersion());
                if(tLocalFile.isDirectory()){
                    String tTargetFile=tArtifact.getName()+"-"+tArtifact.getVersion();
                    if(tArtifact.getClassifier()!=null&&!tArtifact.getClassifier().isEmpty())
                        tTargetFile+='-'+tArtifact.getClassifier();
                    tTargetFile+='.'+tArtifact.getExtension();
                    for(File sDir : tLocalFile.listFiles()){
                        File tFile=new File(sDir,tTargetFile);
                        if(tFile.isFile()){
                            System.out.println("Gradle local found "+artifact);
                            try{
                                Utils.copyFile(tFile,artifactFile);
                                tResolved=true;
                            }catch(IOException e){
                                System.out.println("Error on copy gradle repo file! "+e.getMessage());
                            }
                        }
                    }
                }
            }
            if(!tResolved){
                System.out.print("Downloading "+artifact+"... ");
                try{
                    Damask.get().artifactResolve(artifact.getArtifact(),artifactFile,false);
                    for(final String algorithm : Sync.ALGORITHMS){
                        Utils.writeChecksum(algorithm,artifactFile);
                    }
                    System.out.println("DONE!");
                }catch(Exception e){
                    System.out.println("ERROR!");
                    e.printStackTrace();
                    return null;
                }

                if(tMavenLocalFile!=null){
                    try{
                        System.out.println("Push download lib to local maven repo");
                        Utils.copyFile(artifactFile,tMavenLocalFile);
                        for(final String algorithm : Sync.ALGORITHMS){
                            Utils.writeChecksum(algorithm,tMavenLocalFile);
                        }
                    }catch(IOException e){
                        System.out.println("Error on push lib to local maven repo : "+e.getMessage());
                    }
                }

            }
        }
        if(recursive){
            final List<LibraryArtifact> artifacts=new ArrayList<LibraryArtifact>();
            parseLibraries(artifactFile,artifacts);
            if(!sync(artifacts,rootDir,true)){
                return null;
            }
        }
        return artifactFile;
    }

    public static void resolveLatestVersion(final File basedir,final LibraryArtifact lib){}

    static{
        ALGORITHMS=new String[]{"sha-1","md5"};
    }

    public static class KCauldronInfo{

        public final boolean kcauldron;
        public final String group;
        public final String channel;
        public final String version;
        public final String[] classpath;

        public KCauldronInfo(final boolean kcauldron,final String group,final String channel,final String version,final String[] classpath){
            this.kcauldron=kcauldron;
            this.group=group;
            this.channel=channel;
            this.version=version;
            this.classpath=classpath;
        }
    }
}
