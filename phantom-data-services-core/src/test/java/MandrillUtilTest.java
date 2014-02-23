import com.phantom.ds.framework.email.MandrillConfiguration;
import com.phantom.ds.framework.email.MandrillUtil;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Created by aparrish on 2/22/14.
 */
public class MandrillUtilTest extends TestCase {


    @Test
    public void testMandrillSend() throws Exception {

        MandrillUtil.sendMailViaMandrill(
                new MandrillConfiguration(
                        "7jwLjLnzZPTq5eG3HILiAg",
                        "smtp.mandrillapp.com",
                        "587",
                        "admin@sneekyapp.com"
                ), "aparrish@neosavvy.com", "newPassword");


    }
}
