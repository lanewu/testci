package py.token.controller;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The class is used to create an instance of {@link TokenController}.
 * 
 * @author lx
 *
 */
public class TokenControllerFactory {
    private final AtomicLong id;
    private final static TokenControllerFactory factory = new TokenControllerFactory();

    public static TokenControllerFactory getInstance() {
        return factory;
    }

    private TokenControllerFactory() {
        id = new AtomicLong(0);
    }

    public TokenController create(int bucketCapacity) {
        return new TokenControllerImpl(id.getAndIncrement(), bucketCapacity);
    }
}
