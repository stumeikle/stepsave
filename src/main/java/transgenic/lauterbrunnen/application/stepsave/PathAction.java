package transgenic.lauterbrunnen.application.stepsave;

import java.io.File;
import java.io.IOException;

/**
 * Created by stumeikle on 14/10/17.
 */
public interface PathAction {

    void run(String srcRoot, File element, String destRoot) throws IOException;
}
