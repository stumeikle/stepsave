package transgenic.lauterbrunnen.application.stepsave;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by stumeikle on 14/10/17.
 */
public interface FileSystemModifier {

    void mkdirs(Path path) throws IOException;
    void copyLink(Path filePath, Path destPath) throws IOException;
    void copyFile(Path filePath, Path destPath) throws IOException;
    void createHardLink(Path link, Path existing) throws IOException;
}
