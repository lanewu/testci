package py.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.token.controller.TokenControllerUtils;

/**
 * 
 * @author lx
 *
 */
public class SingleTaskEngine extends AbstractTaskEngine {

    public SingleTaskEngine() {
        queue = new LinkedBlockingQueue<>();
        setPrefix("single-task");
    }

    public SingleTaskEngine(int maxTaskCount) {
        if (maxTaskCount > 0 && maxTaskCount < Integer.MAX_VALUE) {
            queue = new ArrayBlockingQueue<>(maxTaskCount);
        } else {
            queue = new LinkedBlockingQueue<>();
        }
    }

}
