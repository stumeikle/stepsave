package transgenic.lauterbrunnen.application.stepsave;

import org.junit.Test;

/**
 * Created by stumeikle on 14/10/17.
 */
public class TestLinkCopy {


    @Test
    public void testLinkCopy() {
        StepSave        ss = new StepSave();

        try {
            ss.go("src/test/resources/testlinktree", "/tmp/backup");

            //check the result

            //clear /tmp
        }
        catch(Exception e) {
            assert(true);
        }
        //and check the result
    }
}
