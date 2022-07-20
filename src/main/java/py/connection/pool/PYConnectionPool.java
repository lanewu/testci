package py.connection.pool;


import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import py.common.struct.EndPoint;

/**
 * 
 * @author lx
 *
 */
public interface PYConnectionPool {

    /**
     * get a connection immediately on matter the connection is built or not.
     *
     * @param endPoint
     */
    PYConnection get(EndPoint endPoint);


//    /**
//     * If you want to get a connection and wait for connection to be built, you can call this method.
//     *
//     * @param endPoint
//     * @param timeout  the time you want to wait
//     * @param timeUnit the unit of time
//     * @return
//     */
//    PYConnection get(EndPoint endPoint, int timeout, TimeUnit timeUnit);

    /**
     * close all connections and recycle related resource.
     */
    void close();

}
