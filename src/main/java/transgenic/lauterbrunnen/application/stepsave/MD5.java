package transgenic.lauterbrunnen.application.stepsave;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by stumeikle on 14/10/17.
 */
public class MD5 {

    public static final Log LOG = LogFactory.getLog(MD5.class);

    public static String computeMD5(String filename) {
        String result = null;
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filename))){
            result = DigestUtils.md5Hex(bufferedInputStream);
        }catch(IOException e) {
            LOG.warn("Unable to compute md5 for file:" + filename);
            LOG.debug(e);
        }

        return result;
    }
}
