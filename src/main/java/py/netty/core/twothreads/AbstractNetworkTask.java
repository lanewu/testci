package py.netty.core.twothreads;

import org.apache.commons.lang3.NotImplementedException;
import py.engine.Task;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

public abstract class AbstractNetworkTask implements Task {
    @Override
    public void destroy() {
        throw new NotImplementedException("");
    }

    @Override
    public void cancel() {
        throw new NotImplementedException("");
    }

    @Override
    public boolean isCancel() {
        throw new NotImplementedException("");
    }

    @Override
    public void setToken(int token) {
        throw new NotImplementedException("");
    }

    @Override
    public int getToken() {
        throw new NotImplementedException("");
    }

    @Override
    public long getDelay(TimeUnit unit) {
        throw new NotImplementedException("");
    }

    @Override
    public int compareTo(Delayed o) {
        throw new NotImplementedException("");
    }
}
