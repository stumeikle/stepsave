package transgenic.lauterbrunnen.application.stepsave;

import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Created by stumeikle on 14/10/17.
 *
 *
 * Really this class needs to remember created directories and return true for exists() tests on them TODO
 */
public class DryRunModifier implements FileSystemModifier {

    private static final Log LOG = LogFactory.getLog(DryRunModifier.class);

    public void mkdirs(Path path) throws IOException {
        LOG.info("Making directories: " + path);
    }

    public void copyLink(Path filePath, Path destPath) throws IOException {
        LOG.info("Coping link from " + filePath + " to " + destPath);
    }

    public void copyFile(Path filePath, Path destPath)  throws IOException {
        LOG.info("Copying file from " + filePath + " to " + destPath);
  }

    public void createHardLink(Path link, Path existing) throws IOException {
        LOG.info("Creating hard link from " + existing + " to " + link);
    }

}
