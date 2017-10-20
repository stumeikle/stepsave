package transgenic.lauterbrunnen.application.stepsave;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

/**
 * Created by stumeikle on 19/10/17.
 */
public class TestCmdLine {

    @Test
    public void TestCmdLine() {

        try {
            StepSave    ss = new StepSave();
            BasicConfigurator.configure();
            ss.goFromCmdLine(new String[]{"-dryrun","src/test/resources/testlinktree","/tmp/backup","/tmp/index"});
        }
        catch(Exception e){
            assert(true);
        }

    }
}
