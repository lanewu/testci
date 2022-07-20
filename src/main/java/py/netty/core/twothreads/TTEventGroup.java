package py.netty.core.twothreads;
/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.channel.*;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorChooserFactory;
import io.netty.util.concurrent.RejectedExecutionHandler;
import io.netty.util.concurrent.RejectedExecutionHandlers;
import py.netty.core.nio.MyEventLoop;

import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

/**
 * {@link MultithreadEventLoopGroup} implementations which is used for NIO {@link Selector} based {@link Channel}s.
 */
public class TTEventGroup extends MultithreadEventLoopGroup {

    private String groupPrefix;

    /**
     * Create a new instance using the default number of threads, the default {@link ThreadFactory} and the {@link
     * SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public TTEventGroup() {
        this(0);
    }

    /**
     * Create a new instance using the specified number of threads, {@link ThreadFactory} and the {@link
     * SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public TTEventGroup(int nThreads) {
        this(nThreads, (Executor) null);
    }

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the {@link
     * SelectorProvider} which is returned by {@link SelectorProvider#provider()}.
     */
    public TTEventGroup(int nThreads, ThreadFactory threadFactory) {
        this(nThreads, threadFactory, SelectorProvider.provider());
    }

    public TTEventGroup(int nThreads, Executor executor) {
        this(nThreads, executor, SelectorProvider.provider());
    }


    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory} and the given {@link
     * SelectorProvider}.
     */
    public TTEventGroup(int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider) {
        this(nThreads, threadFactory, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    public TTEventGroup(int nThreads, ThreadFactory threadFactory, final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, threadFactory, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    public TTEventGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider) {
        this(nThreads, executor, selectorProvider, DefaultSelectStrategyFactory.INSTANCE);
    }

    public TTEventGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
            final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }

    public TTEventGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory,
                RejectedExecutionHandlers.reject());
    }

    public TTEventGroup(int nThreads, Executor executor, EventExecutorChooserFactory chooserFactory,
            final SelectorProvider selectorProvider, final SelectStrategyFactory selectStrategyFactory,
            final RejectedExecutionHandler rejectedExecutionHandler) {
        super(nThreads, executor, chooserFactory, selectorProvider, selectStrategyFactory, rejectedExecutionHandler);
    }

    public void setGroupPrefix(String prefix) {
        this.groupPrefix = prefix;
    }

    @Override
    protected EventLoop newChild(Executor executor, Object... args) throws Exception {
        return new TTEventLoop(this, (SelectorProvider) args[0], ((SelectStrategyFactory) args[1])
                .newSelectStrategy());
    }
}
