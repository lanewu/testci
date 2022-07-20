package py.token.controller;

import java.util.concurrent.TimeUnit;

/**
 * The class is used for simplifying the process of creating and registering an instance of {@link TokenController}.
 * 
 * @author lx
 *
 */
public class TokenControllerUtils {

    public static TokenController generateAndRegister(int bucketCapacity) {
        TokenController controller = TokenControllerFactory.getInstance().create(bucketCapacity);
        TokenControllerCenter.getInstance().register(controller);
        return controller;
    }

    public static void deregister(TokenController controller) {
        TokenControllerCenter.getInstance().deregister(controller);
    }

    public static TokenController generateBogusController() {
        return new TokenController() {

            @Override
            public long getId() {
                return 0;
            }

            @Override
            public void reset() {
            }

            @Override
            public boolean acquireToken(long timeout, TimeUnit timeUnit) {
                return true;
            }

            @Override
            public boolean acquireToken(int tokenCount, long timeout, TimeUnit timeUnit) {
                return true;
            }

            @Override
            public boolean acquireToken() {
                return true;
            }

            @Override
            public boolean acquireToken(int tokenCount) {
                return true;
            }

            @Override
            public int tryAcquireToken(int tokenCount) {
                return tokenCount;
            }

            @Override
            public void updateToken(int bucketCapacity) {

            }

        };
    }
}
