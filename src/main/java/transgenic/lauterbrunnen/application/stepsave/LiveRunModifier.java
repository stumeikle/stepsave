package transgenic.lauterbrunnen.application.stepsave;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Created by stumeikle on 16/10/17.
 */
public class LiveRunModifier implements FileSystemModifier {
    private static final Log LOG = LogFactory.getLog(LiveRunModifier.class);

    public void mkdirs(Path path) throws IOException {
        FileUtils.forceMkdir(path.toFile());
    }

    public void copyLink(Path filePath, Path destPath) throws IOException {
        Files.copy(filePath, destPath, LinkOption.NOFOLLOW_LINKS);
    }

    public void copyFile(Path filePath, Path destPath)  throws IOException {
        Files.copy(filePath,destPath);
    }

    public void createHardLink(Path link, Path existing) throws IOException {
        Files.createLink(link, existing);
    }

}
