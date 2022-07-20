package py.db;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.mchange.v2.c3p0.PooledDataSource;

/**
 * This bean simply blocks until there are active connections in the given pool
 */
public class ConnectionPoolWaiter {
    private static final Logger logger = Logger.getLogger(ConnectionPoolWaiter.class);

    private static final int DEFAULT_TIMEOUT_WAIT_FOR_ACTIVE_CONNECTION_MS = 10 * 1000;

    public ConnectionPoolWaiter(PooledDataSource pool) throws SQLException, InterruptedException {
        logger.info("Waiting until the connection pool has active connections...");

        int amountOfConnections = 0;
        long startTime = System.currentTimeMillis();
        while ((amountOfConnections = pool.getNumConnectionsAllUsers()) == 0) {
            logger.info("No connection got, let's wait 1 seconds more ...");
            if (System.currentTimeMillis() - startTime < DEFAULT_TIMEOUT_WAIT_FOR_ACTIVE_CONNECTION_MS) {
                Thread.sleep(500);
            } else {
                throw new SQLException("can not build connection with db: {}" + pool.getDataSourceName());
            }
        }

        logger.info(amountOfConnections + " connection(s) established...");
    }
}