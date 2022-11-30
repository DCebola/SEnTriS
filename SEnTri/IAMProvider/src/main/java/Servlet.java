import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.apache.commons.codec.binary.Base64;
import pt.fct.nova.id.srv.application.clients.LocksClient;
import pt.fct.nova.id.srv.application.IAMStorage;
import pt.fct.nova.id.srv.application.crypto.PasswordUtils;


@WebListener
public class Servlet implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        if (!IAMStorage.isInit()) {
            try {
                String defaultAdminUsername = System.getenv("DEFAULT_ADMIN_USERNAME");
                String defaultAdminPassword = Base64.encodeBase64URLSafeString(PasswordUtils.hash(System.getenv("DEFAULT_ADMIN_PASSWORD")));
                String lockID = LocksClient.acquireUserLock(defaultAdminUsername);
                IAMStorage.init(defaultAdminUsername, defaultAdminPassword);
                LocksClient.releaseUserLock(defaultAdminUsername, lockID);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
