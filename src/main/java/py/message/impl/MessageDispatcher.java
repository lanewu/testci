package py.message.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import py.message.IMessage;
import py.message.IMessageHandler;
import py.message.IReceiver;
import py.message.exceptions.MessageHandlerMissingException;
import py.message.exceptions.MessageMapMissingException;

/**
 * This class is a singleton class
 * 
 * @author sxl
 * 
 */
public class MessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);

    private static final int DEFAULT_DISPATCHER_SIZE = 3;
    private int engineSize = 0;

    private Map<IMessage<?>, Set<IReceiver>> messageToObjectMap = null;
    private Set<IReceiver> listeners;
    private ExecutorService dispatcherExecutorPool;
    private Thread dispatcherThread;
    private boolean shutdownDispatcher = false;
    private boolean dispatcherStarted = false;

    private MessageDispatcher() {
        messageToObjectMap = new ConcurrentHashMap<IMessage<?>, Set<IReceiver>>();
        listeners = new HashSet<IReceiver>();
    }

    private static class LazyHolder {
        private static final MessageDispatcher singletonInstance = new MessageDispatcher();
    }

    public static MessageDispatcher getInstance() {
        return LazyHolder.singletonInstance;
    }

    protected synchronized void addLisener(IReceiver receiver) {
        listeners.add(receiver);
    }

    protected synchronized void removeLisener(IReceiver receiver) {
        listeners.remove(receiver);
    }

    protected void add(IMessage<?> message, IReceiver receiver) {
        logger.debug("regist message : {} with {}", message, receiver);
        Set<IReceiver> receivers = messageToObjectMap.get(message);
        if (receivers != null) {
            receivers.add(receiver);
        } else {
            receivers = new HashSet<IReceiver>();
            receivers.add(receiver);
            messageToObjectMap.put(message, receivers);
        }
    }

    protected void add(IMessage<?> message) {
        logger.debug("register message : {} with all receivers", message);
        messageToObjectMap.put(message, listeners);
    }

    public void start() {
        if (dispatcherStarted) {
            logger.debug("Dispatcher has already been started");
            return;
        }
        logger.debug("Going to start message dispatcher");

        // start thread pool
        int trueEngineSize = engineSize == 0 ? DEFAULT_DISPATCHER_SIZE : this.engineSize;
        dispatcherExecutorPool = Executors.newFixedThreadPool(trueEngineSize);

        // create threads
        logger.debug("Start message dispatcher");
        dispatcherThread = new Thread() {
            public void run() {
                while (!shutdownDispatcher) {
                    for (Entry<IMessage<?>, Set<IReceiver>> entry : messageToObjectMap.entrySet()) {
                        logger.debug("dispatcher thread is running, there are {} messages to be done",
                                messageToObjectMap.size());
                        IMessage<?> message = entry.getKey();
                        Set<IReceiver> receivers = entry.getValue();

                        try {
                            // direct message
                            logger.debug("Directionally send message {} to {}", message, receivers);
                            dispatcherExecutorPool.execute(new HandleMessageTask(message, receivers));
                            messageToObjectMap.remove(message);
                        } catch (RejectedExecutionException e) {
                            // if reject, put it back to the "message to object map" again
                            // messageToObjectMap.put(message, lisener);
                            logger.warn("Caught an exception", e);
                        }
                    }

                    try {
                        sleep(5);
                    } catch (InterruptedException e) {
                        logger.warn("Caught an exception", e);
                    }
                }
            }
        };
        dispatcherThread.start();

        dispatcherStarted = true;
    }

    public void stop() {
        logger.debug("Going to stop message pump");
        // stop dispatcher first
        shutdownDispatcher = true;
        dispatcherThread.interrupt();

        // stop message handler thread pool after
        if (dispatcherExecutorPool != null) {
            dispatcherExecutorPool.shutdown();
        }

        logger.debug("Message pump has been stopped");
    }

    private class HandleMessageTask implements Runnable {
        private IMessage<?> message;
        private Set<IReceiver> receivers;

        HandleMessageTask(IMessage<?> message, Set<IReceiver> receivers) {
            this.message = message;
            this.receivers = receivers;
        }

        /** remove use message */

        @Override
        public synchronized void run() {
            try {
                for (IReceiver receiver : receivers) {
                    Map<Class<?>, IMessageHandler> messageMap = receiver.getMessageMap();
                    boolean messageMatched = false;
                    for (Entry<Class<?>, IMessageHandler> entry : messageMap.entrySet()) {
                        Class<?> message = entry.getKey();
                        IMessageHandler handler = entry.getValue();

                        if (handler == null) {
                            logger.warn("invalid handler");
                            throw new MessageHandlerMissingException();
                        } else {
                            if (this.message.getClass().getName().equals(message.getName())) {
                                logger.debug("Message handler has been found");
                                messageMatched = true;
                                handler.execute(this.message);

                                // there must be only one message handler in one message receiver. so, break here
                                break;
                            } else {
                                logger.debug("No message handler has been found for {} on message {}", receivers,
                                        message);
                            }
                        }
                    }

                    // if (!messageMatched) {
                    // throw new MessageHandlerMissingException();
                    // }
                }

                if (dispatcherExecutorPool.isShutdown()) {
                    // thread pool has shutdown, do not do anything and return
                    return;
                }
            } catch (MessageMapMissingException e) {
                logger.warn("Caught an exception", e);
            } catch (MessageHandlerMissingException e) {
                logger.warn("Caught an exception", e);
            } catch (Exception e) {
                logger.error("Caught an exception", e);
            }
        }
    }

}
