package py.app.healthcheck;

/** 
 * check instance health periodically. 
 */
public interface HealthChecker {
    public void startHealthCheck() throws Exception;
    
    /**
     * Stop health checking immediately
     */
    public void stopHealthCheck();
}
