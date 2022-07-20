package py.connection.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import py.netty.core.ProtocolFactory;
import py.netty.core.TransferenceConfiguration;

public interface BootstrapBuilder {

    void setCfg(TransferenceConfiguration cfg);

    void setHashedWheelTimer(HashedWheelTimer hashedWheelTimer);

    void setProtocolFactory(ProtocolFactory protocolFactory);

    Bootstrap build();

    void setAllocator(ByteBufAllocator allocator);

    void setIoEventGroup(EventLoopGroup ioEventGroup);
}
