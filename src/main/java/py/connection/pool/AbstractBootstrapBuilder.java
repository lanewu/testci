package py.connection.pool;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.HashedWheelTimer;
import py.netty.core.ProtocolFactory;
import py.netty.core.TransferenceConfiguration;

public abstract class AbstractBootstrapBuilder implements BootstrapBuilder {
    protected TransferenceConfiguration cfg;
    protected ByteBufAllocator allocator;

    protected HashedWheelTimer hashedWheelTimer;
    protected ProtocolFactory protocolFactory;
    public AbstractBootstrapBuilder() {

    }

    protected EventLoopGroup ioEventGroup;

    @Override
    public void setAllocator(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public void setIoEventGroup(EventLoopGroup ioEventGroup) {
        this.ioEventGroup = ioEventGroup;
    }

    @Override
    public void setCfg(TransferenceConfiguration cfg) {
        this.cfg = cfg;
    }

    @Override
    public void setHashedWheelTimer(HashedWheelTimer hashedWheelTimer) {
        this.hashedWheelTimer = hashedWheelTimer;
    }

    @Override
    public void setProtocolFactory(ProtocolFactory protocolFactory) {
        this.protocolFactory = protocolFactory;
    }
}
