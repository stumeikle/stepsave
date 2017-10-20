package transgenic.lauterbrunnen.application.stepsave;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by stumeikle on 14/10/17.
 *
 * Usage tips:
 * LogManager.getRootLogger().setLevel(Level.ALL);
 * ss.setIndexPath("/tmp/index");
 * ss.go("src/test/resources/testlinktree", "/tmp/backup");
 * ss.go("/home/stumeikle/usrlocal/hazelcast-3.8.2", "/tmp/backup");
 *
 */
public class StepSave {

    private static final Log LOG = LogFactory.getLog(StepSave.class);
    private static final Pattern datePattern=Pattern.compile("\\d\\d\\d\\d-\\d\\d-\\d\\d$");
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    private String  indexPath="../.index";
    private Set<Pattern> dirSkipPatterns = new HashSet<>();
    private Set<Pattern> fileSkipPatterns = new HashSet<>();
    private FileSystemModifier fileSystemModifier = new LiveRunModifier();
    private boolean hasPreviousBackup = false;
    private long previousBackupTime = 0;
    private String previousBackupRoot = null;

    public StepSave() {
        init();
    }

    public void goFromCmdLine(String[] args) throws IOException {
        StringBuilder   sb = new StringBuilder();
        sb.append("\n");
        sb.append("      Step Save v0.1\n");
        sb.append("----------------------------\n");
        sb.append("   Simple backup mechanism\n");
        sb.append("\n");
        sb.append("            by\n");
        sb.append("    Stu Meikle 20150415\n");
        sb.append("   java rewrite 20171016\n");
        sb.append("\n");

        System.out.println(sb.toString());

        if (args==null || args.length<2) {
            sb = new StringBuilder();
            sb.append("Syntax:\n");
            sb.append("    stepSave <backupfrom> <backupto>[/date-dir] [index-path]\n");
            sb.append("eg  stepSave /home ../2008-mylaptop \n");
            sb.append("eg  stepSave /home ../2008-mylaptop/2015-01-02 ../index\n");
            sb.append("\n");
            sb.append("Add -dryrun flag to run without making changes");
            sb.append("\n");
            System.out.println(sb.toString());

            System.exit(0);
        }

        boolean dryrun=false;
        List<String>        copyArgs = new ArrayList<>(args.length);
        for(int i=0;i<args.length;i++) {
            if ("-dryrun".equals(args[i])) {
                dryrun=true;
            } else {
                copyArgs.add(args[i]);
            }
        }
        if (dryrun) {
            args = copyArgs.toArray(new String[0]);
            fileSystemModifier = new DryRunModifier();
        }

        if (args.length==3) {
            setIndexPath(args[2]);
        }
        go(args[0], args[1]);
    }

    public void setIndexPath(String indexPath) {
        this.indexPath = indexPath;
    }

    public void go(String srcPath, String destPath) throws IOException {

        //if either path is not absolute, we need to convert it to such
        File srcFile = new File(srcPath);
        File destFile = new File(destPath);
        srcPath = srcFile.getCanonicalPath();
        destPath = destFile.getCanonicalPath();

        //(0) either create a subdirectory with the current date or use the value passed in
        destPath = createDateSubDirIfNeeded(destPath);

        //(1) find the previous backup if there is one
        findPreviousBackup(destPath);

        try {
            // (1) copy the directory structure
            LOG.info("Copying directory structure...");
            scanDirectories(srcPath, destPath, this::copyDirectory);

            //(2) copy the files as needed
            LOG.info("Backing up files...");
            scanFiles(srcPath, destPath, this::copyFile );

        } catch (IOException e) {
            LOG.error("Exception thrown when creating directory. ", e);
        }
    }

    private void init() {
        Properties  properties = new Properties();

        try {
            FileInputStream propertiesFile = new FileInputStream("etc/stepsave.properties");
            properties.load(propertiesFile);
        } catch (IOException e) {
            System.out.println("Unable to open properties file."); //Logging not initialised here
        }

        //log4j
        PropertyConfigurator.configure(properties);

        for(String key: properties.stringPropertyNames()) {
            if (key.startsWith("stepsave.dir.skip.")) {
                String value = (String)properties.get(key);

                Pattern p= Pattern.compile(value);
                dirSkipPatterns.add(p);

                /* embedded unit test
                System.out.println("Got value " + value);
                Matcher m = p.matcher("/home/stumeikle/.netbeansABC/cache/tony");
                if (m.find()) {
                    System.out.printf("Matches string");
                }*/
            }
            else if (key.startsWith("stepsave.file.skip.")){
                String value = (String)properties.get(key);
                Pattern p= Pattern.compile(value);
                fileSkipPatterns.add(p);
            }
        }
    }


    private void scanDirectories(String path, String destPath, PathAction pathAction) throws IOException {

        //Apache, could it not be more complex?
        List<File>      allMatchingDirs = (List<File>)FileUtils.listFilesAndDirs(new File(path),new NotFileFilter(new WildcardFileFilter("*")), new WildcardFileFilter("*"));
        for(File file : allMatchingDirs) {
            if (file.isDirectory()) {

                boolean skip = false;
                for(Pattern pattern: dirSkipPatterns) {
                    Matcher m = pattern.matcher(file.getAbsolutePath());
                    if (m.find()) {
                        skip =true;
                        break;
                    }
                }

                if (!skip) {
                    pathAction.run(path, file, destPath);
                }
            }
        }
    }

    private void    scanFiles(String srcRoot, String destRoot, PathAction pathAction) throws IOException {
        List<File>      allMatchingFiles = (List<File>)FileUtils.listFiles(new File(srcRoot),null,true);

        int total = allMatchingFiles.size();
        long lastWrite=0;
        int count=0;
        for(File file: allMatchingFiles) {
            count++;
            if (System.currentTimeMillis() - lastWrite>2000) {
                double p = (double)count/(double)total;
                p=Math.floor(p*1000.0)/10.0;
                LOG.info("Completed " + count + "/" + total + " files (" + p + "%)");
                lastWrite=System.currentTimeMillis();
            }
            boolean skip=false;
            for(Pattern pattern: fileSkipPatterns) {
                Matcher m = pattern.matcher(file.getAbsolutePath());
                if (m.find()) {
                    skip=true;
                    break;
                }
            }
            if (!skip) {
                pathAction.run(srcRoot, file, destRoot);
            }
        }
    }

    private void   copyDirectory(String srcRoot, File directory, String destRoot) throws IOException {
        Path    srcRootPath = Paths.get(srcRoot);
        Path    filePath = Paths.get(directory.getAbsolutePath());
        Path    relativePath = srcRootPath.relativize(filePath);

        //combine relative and dest root
        Path    destPath = Paths.get(destRoot, relativePath.toString());
        fileSystemModifier.mkdirs( destPath );
    }

    private void     copyFile(String srcRoot, File file, String destRoot) throws IOException {
        //(1) if the destination parent directory does not exist then skip
        Path    srcRootPath = Paths.get(srcRoot);
        Path    filePath = Paths.get(file.getAbsolutePath());
        Path    relativePath = srcRootPath.relativize(filePath);

        //combine relative and dest root
        Path    destPath = Paths.get(destRoot, relativePath.toString());
        Path    destParentDir = Paths.get(destPath.toString(),"..");

        //if the dest path file already exists then we've created it as part of a previous
        //backup, so skip
        if (destPath.toFile().exists()) {
            LOG.trace("File exists in backup already, skipping." + filePath);
            return;
        } else {
            //grr http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4956115
            if (Files.isSymbolicLink(destPath)) {
                //doesn't exist but is symbollic link, IE its a broken link
                LOG.trace("File exists in backup already, skipping." + filePath);
                return;
            }
        }

        //seems this doesn't work so well. tsk
        try {
            String canonDestParentDirPath= new File(destParentDir.toString()).getCanonicalPath();
            File destParentDirFile = new File(canonDestParentDirPath);
            if (!destParentDirFile.exists()) {
                LOG.trace("Destination parent path does not exist, skipping file. Parent dir:" + destParentDirFile.getAbsolutePath());
                return;
            }
        } catch (IOException e) {
            LOG.warn("Unable to form path:" + destParentDir.toString() + " + ..");
            return;
        }

        //(2) if the file is a symlink just do a simple copy
        if (Files.isSymbolicLink(filePath)) {
            LOG.trace("Copying link from " + filePath + " to " + destPath);
            fileSystemModifier.copyLink( filePath, destPath );
        } else {
            // #if this file exists, is older than the previous backup
            // #and has the same path and size as the previous backup
            // #then just link to it. else copy and md5
            boolean justLink = false;
            if (hasPreviousBackup) {
                long lastModified = file.lastModified();
                if (lastModified< previousBackupTime) {
                    //compare the sizes
                    Path    prevPath = Paths.get(previousBackupRoot, relativePath.toString());
                    if (prevPath.toFile().exists() && file.length()==prevPath.toFile().length()) {
                        justLink=true;
                        LOG.trace("Quick linking " + destPath);
                        try {
                            fileSystemModifier.createHardLink(destPath, prevPath);
                        }catch(Exception e) {
                            LOG.warn("Unable to create hard link from " + destPath + " to " + prevPath);
                            LOG.debug(e);
                        }
                    }
                }
            }

            if (!justLink) {
                LOG.trace("Computing MD5 for " + filePath.toString());
                String md5 = MD5.computeMD5(filePath.toString());
                String prefix = md5.substring(0,2);
                String body = md5.substring(2);
                String name = file.getName();

                //create the index dir if it doesn't exist
                Path indexDir = Paths.get(indexPath, prefix, body);
                if (!indexDir.toFile().exists()) {
                    fileSystemModifier.mkdirs(indexDir);
                    LOG.trace("Making index dirs:" + indexDir.toString());
                }

                //copy the file into the index if its not already there
                Path indexFilePath = Paths.get(indexPath, prefix, body, name);
                if (!indexFilePath.toFile().exists()) {
                    LOG.trace("Copying file into index. From:" + filePath.toString() + " to " + indexFilePath);
                    fileSystemModifier.copyFile(filePath, indexFilePath);
                }

                //link the backup dir file to the index file
                LOG.trace("Hard linking " + destPath + " to " + indexFilePath);
                fileSystemModifier.createHardLink(destPath,indexFilePath);
            }
        }
    }

    private String  createDateSubDirIfNeeded(String destPath) {
        Matcher matcher = datePattern.matcher(destPath);

        if (matcher.find()) {
            //has a date at the end anyway
            return destPath;
        }

        //else
        String today = dateFormat.format(new Date());
        Path combined = Paths.get(destPath, today);
        return combined.toString();
    }

    private void    findPreviousBackup(String destPath) {
        //find all the subdirs in the directory destpath is in
        Path    parent = Paths.get(destPath,"..");

        //seems this doesn't work so well. tsk
        try {
            String canonDestParentDirPath= parent.toFile().getCanonicalPath();
            File destParentDirFile = new File(canonDestParentDirPath);
            if (!destParentDirFile.exists()) {
                return;
            }

            //Now for each entry
            long latest = 0;
            String latestPath = null;
            for(File file: destParentDirFile.listFiles()) {
                if (!file.isDirectory()) continue;

                if (file.getAbsolutePath().equals(destPath)) continue;

                Matcher dateMatcher = datePattern.matcher(file.getAbsolutePath());
                if (dateMatcher.find()) {
                    String fileAbsolutePath = file.getAbsolutePath();
                    String datePart = fileAbsolutePath.substring(fileAbsolutePath.lastIndexOf(File.separator)+1);

                    //found directory of format yyyy-MM-dd.
                    try {
                        Date d = dateFormat.parse(datePart);
                        if (d.getTime()>latest) {
                            latest = d.getTime();
                            latestPath = file.getAbsolutePath();
                        }
                    } catch(Exception e) {
                        LOG.debug("Unable to parse date " + datePart);
                    }
                }
            }

            if (latest!=0) {
                hasPreviousBackup = true;
                previousBackupRoot = latestPath;
                previousBackupTime = latest; // i suppose this'll be at 00:00:01 am or so. is that ok? TODO consider
                LOG.trace("Found previous backup dir: " + previousBackupRoot.toString());
            }

        } catch (IOException e) {
            LOG.warn("Unable to form path:" + destPath + " + ..");
            return;
        }

    }

    public static void main(String[] args) {

        try {
            StepSave    ss = new StepSave();
            ss.goFromCmdLine(args);
        }
        catch(Exception e){
            LOG.error("Exception.", e);
        }
    }
}
