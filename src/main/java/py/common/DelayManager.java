package py.common;

import java.util.concurrent.Delayed;

public interface DelayManager {
    public void put(Delayed delayed);

    public void pause();

    public void start();

    public void stop();
}
