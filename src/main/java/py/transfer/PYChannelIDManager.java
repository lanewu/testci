package py.transfer;

import io.netty.channel.Channel;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class PYChannelIDManager {

    private BiMap<Integer,Channel> biMapIDChannel ;
    private AtomicInteger atomic ;
    
    public PYChannelIDManager(int size) {
        int initialCapacity = 128;
        if(size>0 && size<2048)
            initialCapacity = size;
        
        biMapIDChannel = HashBiMap.create(initialCapacity);
        atomic = new AtomicInteger(0);
    }
    
    public BiMap<Integer,Channel> getMapIDChannel()
    {
        return biMapIDChannel; 
    }
    
    public Channel getChannel(int channelid) {
        return biMapIDChannel.get(channelid);
    }

    public Integer getId(Channel channel) {
        return biMapIDChannel.inverse().get(channel);
    }

    synchronized public void addChannel(Channel channel) {
        if (biMapIDChannel.inverse().get(channel) != null)
            return;
        
        biMapIDChannel.put(atomic.getAndIncrement(), channel);
    }

    synchronized public Integer delChannel(Channel channel) {
        return biMapIDChannel.inverse().remove(channel);
    }

    synchronized Channel delChannel(Integer id) {
        return biMapIDChannel.remove(id);
    }
}
