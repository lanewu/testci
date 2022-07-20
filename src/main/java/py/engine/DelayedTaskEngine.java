package py.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import py.token.controller.TokenControllerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by kobofare on 2017/4/12.
 */
public class DelayedTaskEngine extends AbstractTaskEngine {
    public DelayedTaskEngine() {
        queue = new DelayQueue<>();
        this.prefix = "delay";
    }
}
