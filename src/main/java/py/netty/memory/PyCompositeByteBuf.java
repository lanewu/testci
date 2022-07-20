package py.netty.memory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import io.netty.buffer.AbstractReferenceCountedByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.EmptyArrays;

public class PyCompositeByteBuf extends AbstractReferenceCountedByteBuf {

    private static final ByteBuffer EMPTY_NIO_BUFFER = Unpooled.EMPTY_BUFFER.nioBuffer();
    private final ByteBufAllocator alloc;
    private final boolean direct;
    private final List<Component> components = new ArrayList<Component>();
    private final int maxNumComponents;

    private boolean freed;

    public PyCompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, ByteBuf... buffers) {
        super(Integer.MAX_VALUE);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (maxNumComponents < 2) {
            throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
        }

        this.alloc = alloc;
        this.direct = direct;
        this.maxNumComponents = maxNumComponents;

        addComponents0(0, buffers);
        consolidateIfNeeded();
        setIndex(0, capacity());
        maxCapacity(capacity());
    }

    public PyCompositeByteBuf(ByteBufAllocator alloc, boolean direct, int maxNumComponents, Iterable<ByteBuf> buffers) {
        super(Integer.MAX_VALUE);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (maxNumComponents < 2) {
            throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
        }

        this.alloc = alloc;
        this.direct = direct;
        this.maxNumComponents = maxNumComponents;
        addComponents0(0, buffers);
        consolidateIfNeeded();
        setIndex(0, capacity());
        maxCapacity(capacity());
    }

    /**
     * Add the given {@link ByteBuf}.
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param buffer
     *            the {@link ByteBuf} to add
     */
    public PyCompositeByteBuf addComponent(ByteBuf buffer) {
        addComponent0(components.size(), buffer);
        consolidateIfNeeded();
        return this;
    }

    /**
     * Add the given {@link ByteBuf}s.
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param buffers
     *            the {@link ByteBuf}s to add
     */
    public PyCompositeByteBuf addComponents(ByteBuf... buffers) {
        addComponents0(components.size(), buffers);
        consolidateIfNeeded();
        return this;
    }

    /**
     * Add the given {@link ByteBuf}s.
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param buffers
     *            the {@link ByteBuf}s to add
     */
    public PyCompositeByteBuf addComponents(Iterable<ByteBuf> buffers) {
        addComponents0(components.size(), buffers);
        consolidateIfNeeded();
        return this;
    }

    /**
     * Add the given {@link ByteBuf} on the specific index.
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param cIndex
     *            the index on which the {@link ByteBuf} will be added
     * @param buffer
     *            the {@link ByteBuf} to add
     */
    public PyCompositeByteBuf addComponent(int cIndex, ByteBuf buffer) {
        addComponent0(cIndex, buffer);
        consolidateIfNeeded();
        return this;
    }

    private int addComponent0(int cIndex, ByteBuf buffer) {
        checkComponentIndex(cIndex);

        if (buffer == null) {
            throw new NullPointerException("buffer");
        }

        int readableBytes = buffer.readableBytes();

        // No need to consolidate - just add a component to the list.
        Component c = new Component(buffer.order(ByteOrder.BIG_ENDIAN).slice());
        if (cIndex == components.size()) {
            components.add(c);
            if (cIndex == 0) {
                c.endOffset = readableBytes;
            } else {
                Component prev = components.get(cIndex - 1);
                c.offset = prev.endOffset;
                c.endOffset = c.offset + readableBytes;
            }
        } else {
            components.add(cIndex, c);
            if (readableBytes != 0) {
                updateComponentOffsets(cIndex);
            }
        }
        return cIndex;
    }

    /**
     * Add the given {@link ByteBuf}s on the specific index
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param cIndex
     *            the index on which the {@link ByteBuf} will be added.
     * @param buffers
     *            the {@link ByteBuf}s to add
     */
    public PyCompositeByteBuf addComponents(int cIndex, ByteBuf... buffers) {
        addComponents0(cIndex, buffers);
        consolidateIfNeeded();
        return this;
    }

    private int addComponents0(int cIndex, ByteBuf... buffers) {
        checkComponentIndex(cIndex);

        if (buffers == null) {
            throw new NullPointerException("buffers");
        }

        // No need for consolidation
        for (ByteBuf b : buffers) {
            if (b == null) {
                break;
            }
            cIndex = addComponent0(cIndex, b) + 1;
            int size = components.size();
            if (cIndex > size) {
                cIndex = size;
            }
        }
        return cIndex;
    }

    /**
     * Add the given {@link ByteBuf}s on the specific index
     *
     * Be aware that this method does not increase the {@code writerIndex} of the {@link PyCompositeByteBuf}. If you need
     * to have it increased you need to handle it by your own.
     *
     * @param cIndex
     *            the index on which the {@link ByteBuf} will be added.
     * @param buffers
     *            the {@link ByteBuf}s to add
     */
    public PyCompositeByteBuf addComponents(int cIndex, Iterable<ByteBuf> buffers) {
        addComponents0(cIndex, buffers);
        consolidateIfNeeded();
        return this;
    }

    private int addComponents0(int cIndex, Iterable<ByteBuf> buffers) {
        if (buffers == null) {
            throw new NullPointerException("buffers");
        }

        if (buffers instanceof ByteBuf) {
            // If buffers also implements ByteBuf (e.g. CompositeByteBuf), it has to go to addComponent(ByteBuf).
            return addComponent0(cIndex, (ByteBuf) buffers);
        }

        if (!(buffers instanceof Collection)) {
            List<ByteBuf> list = new ArrayList<ByteBuf>();
            for (ByteBuf b : buffers) {
                list.add(b);
            }
            buffers = list;
        }

        Collection<ByteBuf> col = (Collection<ByteBuf>) buffers;
        return addComponents0(cIndex, col.toArray(new ByteBuf[col.size()]));
    }

    /**
     * This should only be called as last operation from a method as this may adjust the underlying array of components
     * and so affect the index etc.
     */
    private void consolidateIfNeeded() {
        // Consolidate if the number of components will exceed the allowed maximum by the current
        // operation.
        final int numComponents = components.size();
        if (numComponents > maxNumComponents) {
            final int capacity = components.get(numComponents - 1).endOffset;

            ByteBuf consolidated = allocBuffer(capacity);

            // We're not using foreach to avoid creating an iterator.
            for (int i = 0; i < numComponents; i++) {
                Component c = components.get(i);
                ByteBuf b = c.buf;
                consolidated.writeBytes(b);
                c.freeIfNecessary();
            }
            Component c = new Component(consolidated);
            c.endOffset = c.length;
            components.clear();
            components.add(c);
        }
    }

    private void checkComponentIndex(int cIndex) {
        ensureAccessible();
        if (cIndex < 0 || cIndex > components.size()) {
            throw new IndexOutOfBoundsException(
                    String.format("cIndex: %d (expected: >= 0 && <= numComponents(%d))", cIndex, components.size()));
        }
    }

    private void checkComponentIndex(int cIndex, int numComponents) {
        ensureAccessible();
        if (cIndex < 0 || cIndex + numComponents > components.size()) {
            throw new IndexOutOfBoundsException(String.format(
                    "cIndex: %d, numComponents: %d "
                            + "(expected: cIndex >= 0 && cIndex + numComponents <= totalNumComponents(%d))",
                    cIndex, numComponents, components.size()));
        }
    }

    private void updateComponentOffsets(int cIndex) {
        int size = components.size();
        if (size <= cIndex) {
            return;
        }

        Component c = components.get(cIndex);
        if (cIndex == 0) {
            c.offset = 0;
            c.endOffset = c.length;
            cIndex++;
        }

        for (int i = cIndex; i < size; i++) {
            Component prev = components.get(i - 1);
            Component cur = components.get(i);
            cur.offset = prev.endOffset;
            cur.endOffset = cur.offset + cur.length;
        }
    }

    /**
     * Remove the {@link ByteBuf} from the given index.
     *
     * @param cIndex
     *            the index on from which the {@link ByteBuf} will be remove
     */
    public PyCompositeByteBuf removeComponent(int cIndex) {
        checkComponentIndex(cIndex);
        Component comp = components.remove(cIndex);
        comp.freeIfNecessary();
        if (comp.length > 0) {
            // Only need to call updateComponentOffsets if the length was > 0
            updateComponentOffsets(cIndex);
        }
        return this;
    }

    /**
     * Remove the number of {@link ByteBuf}s starting from the given index.
     *
     * @param cIndex
     *            the index on which the {@link ByteBuf}s will be started to removed
     * @param numComponents
     *            the number of components to remove
     */
    public PyCompositeByteBuf removeComponents(int cIndex, int numComponents) {
        checkComponentIndex(cIndex, numComponents);

        if (numComponents == 0) {
            return this;
        }
        List<Component> toRemove = components.subList(cIndex, cIndex + numComponents);
        boolean needsUpdate = false;
        for (Component c : toRemove) {
            if (c.length > 0) {
                needsUpdate = true;
            }
            c.freeIfNecessary();
        }
        toRemove.clear();

        if (needsUpdate) {
            // Only need to call updateComponentOffsets if the length was > 0
            updateComponentOffsets(cIndex);
        }
        return this;
    }

    public Iterator<ByteBuf> iterator() {
        ensureAccessible();
        List<ByteBuf> list = new ArrayList<ByteBuf>(components.size());
        for (Component c : components) {
            list.add(c.buf);
        }
        return list.iterator();
    }

    /**
     * Same with {@link #slice(int, int)} except that this method returns a list.
     */
    public List<ByteBuf> decompose(int offset, int length) {
        checkIndex(offset, length);
        if (length == 0) {
            return Collections.emptyList();
        }

        int componentId = toComponentIndex(offset);
        List<ByteBuf> slice = new ArrayList<ByteBuf>(components.size());

        // The first component
        Component firstC = components.get(componentId);
        ByteBuf first = firstC.buf.duplicate();
        first.readerIndex(offset - firstC.offset);

        ByteBuf buf = first;
        int bytesToSlice = length;
        do {
            int readableBytes = buf.readableBytes();
            if (bytesToSlice <= readableBytes) {
                // Last component
                buf.writerIndex(buf.readerIndex() + bytesToSlice);
                slice.add(buf);
                break;
            } else {
                // Not the last component
                slice.add(buf);
                bytesToSlice -= readableBytes;
                componentId++;

                // Fetch the next component.
                buf = components.get(componentId).buf.duplicate();
            }
        } while (bytesToSlice > 0);

        // Slice all components because only readable bytes are interesting.
        for (int i = 0; i < slice.size(); i++) {
            slice.set(i, slice.get(i).slice());
        }

        return slice;
    }

    @Override
    public boolean isDirect() {
        int size = components.size();
        if (size == 0) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!components.get(i).buf.isDirect()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasArray() {
        switch (components.size()) {
        case 0:
            return true;
        case 1:
            return components.get(0).buf.hasArray();
        default:
            return false;
        }
    }

    @Override
    public byte[] array() {
        switch (components.size()) {
        case 0:
            return EmptyArrays.EMPTY_BYTES;
        case 1:
            return components.get(0).buf.array();
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int arrayOffset() {
        switch (components.size()) {
        case 0:
            return 0;
        case 1:
            return components.get(0).buf.arrayOffset();
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public boolean hasMemoryAddress() {
        switch (components.size()) {
        case 0:
            return Unpooled.EMPTY_BUFFER.hasMemoryAddress();
        case 1:
            return components.get(0).buf.hasMemoryAddress();
        default:
            return false;
        }
    }

    @Override
    public long memoryAddress() {
        switch (components.size()) {
        case 0:
            return Unpooled.EMPTY_BUFFER.memoryAddress();
        case 1:
            return components.get(0).buf.memoryAddress();
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public int capacity() {
        final int numComponents = components.size();
        if (numComponents == 0) {
            return 0;
        }
        return components.get(numComponents - 1).endOffset;
    }

    @Override
    public PyCompositeByteBuf capacity(int newCapacity) {
        ensureAccessible();
        if (newCapacity < 0 || newCapacity > maxCapacity()) {
            throw new IllegalArgumentException("newCapacity: " + newCapacity);
        }

        int oldCapacity = capacity();
        if (newCapacity > oldCapacity) {
            final int paddingLength = newCapacity - oldCapacity;
            ByteBuf padding;
            int nComponents = components.size();
            if (nComponents < maxNumComponents) {
                padding = allocBuffer(paddingLength);
                padding.setIndex(0, paddingLength);
                addComponent0(components.size(), padding);
            } else {
                padding = allocBuffer(paddingLength);
                padding.setIndex(0, paddingLength);
                // FIXME: No need to create a padding buffer and consolidate.
                // Just create a big single buffer and put the current content there.
                addComponent0(components.size(), padding);
                consolidateIfNeeded();
            }
        } else if (newCapacity < oldCapacity) {
            int bytesToTrim = oldCapacity - newCapacity;
            for (ListIterator<Component> i = components.listIterator(components.size()); i.hasPrevious();) {
                Component c = i.previous();
                if (bytesToTrim >= c.length) {
                    bytesToTrim -= c.length;
                    i.remove();
                    continue;
                }

                // Replace the last component with the trimmed slice.
                Component newC = new Component(c.buf.slice(0, c.length - bytesToTrim));
                newC.offset = c.offset;
                newC.endOffset = newC.offset + newC.length;
                i.set(newC);
                break;
            }

            if (readerIndex() > newCapacity) {
                setIndex(newCapacity, newCapacity);
            } else if (writerIndex() > newCapacity) {
                writerIndex(newCapacity);
            }
        }
        return this;
    }

    @Override
    public ByteBufAllocator alloc() {
        return alloc;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    /**
     * Return the current number of {@link ByteBuf}'s that are composed in this instance
     */
    public int numComponents() {
        return components.size();
    }

    /**
     * Return the max number of {@link ByteBuf}'s that are composed in this instance
     */
    public int maxNumComponents() {
        return maxNumComponents;
    }

    /**
     * Return the index for the given offset
     */
    public int toComponentIndex(int offset) {
        checkIndex(offset);

        for (int low = 0, high = components.size(); low <= high;) {
            int mid = low + high >>> 1;
            Component c = components.get(mid);
            if (offset >= c.endOffset) {
                low = mid + 1;
            } else if (offset < c.offset) {
                high = mid - 1;
            } else {
                return mid;
            }
        }

        throw new Error("should not reach here");
    }

    public int toByteIndex(int cIndex) {
        checkComponentIndex(cIndex);
        return components.get(cIndex).offset;
    }

    @Override
    public byte getByte(int index) {
        return _getByte(index);
    }

    @Override
    protected byte _getByte(int index) {
        Component c = findComponent(index);
        return c.buf.getByte(index - c.offset);
    }

    @Override
    protected short _getShort(int index) {
        Component c = findComponent(index);
        if (index + 2 <= c.endOffset) {
            return c.buf.getShort(index - c.offset);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (short) ((_getByte(index) & 0xff) << 8 | _getByte(index + 1) & 0xff);
        } else {
            return (short) (_getByte(index) & 0xff | (_getByte(index + 1) & 0xff) << 8);
        }
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        Component c = findComponent(index);
        if (index + 3 <= c.endOffset) {
            return c.buf.getUnsignedMedium(index - c.offset);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (_getShort(index) & 0xffff) << 8 | _getByte(index + 2) & 0xff;
        } else {
            return _getShort(index) & 0xFFFF | (_getByte(index + 2) & 0xFF) << 16;
        }
    }

    @Override
    protected int _getInt(int index) {
        Component c = findComponent(index);
        if (index + 4 <= c.endOffset) {
            return c.buf.getInt(index - c.offset);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (_getShort(index) & 0xffff) << 16 | _getShort(index + 2) & 0xffff;
        } else {
            return _getShort(index) & 0xFFFF | (_getShort(index + 2) & 0xFFFF) << 16;
        }
    }

    @Override
    protected long _getLong(int index) {
        Component c = findComponent(index);
        if (index + 8 <= c.endOffset) {
            return c.buf.getLong(index - c.offset);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            return (_getInt(index) & 0xffffffffL) << 32 | _getInt(index + 4) & 0xffffffffL;
        } else {
            return _getInt(index) & 0xFFFFFFFFL | (_getInt(index + 4) & 0xFFFFFFFFL) << 32;
        }
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.length);
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            i++;
        }
        return this;
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, ByteBuffer dst) {
        int limit = dst.limit();
        int length = dst.remaining();

        checkIndex(index, length);
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        try {
            while (length > 0) {
                Component c = components.get(i);
                ByteBuf s = c.buf;
                int adjustment = c.offset;
                int localLength = Math.min(length, s.capacity() - (index - adjustment));
                dst.limit(dst.position() + localLength);
                s.getBytes(index - adjustment, dst);
                index += localLength;
                length -= localLength;
                i++;
            }
        } finally {
            dst.limit(limit);
        }
        return this;
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        checkDstIndex(index, length, dstIndex, dst.capacity());
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            i++;
        }
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        int count = nioBufferCount();
        if (count == 1) {
            return out.write(internalNioBuffer(index, length));
        } else {
            long writtenBytes = out.write(nioBuffers(index, length));
            if (writtenBytes > Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            } else {
                return (int) writtenBytes;
            }
        }
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        checkIndex(index, length);
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, out, localLength);
            index += localLength;
            length -= localLength;
            i++;
        }
        return this;
    }

    @Override
    public PyCompositeByteBuf setByte(int index, int value) {
        Component c = findComponent(index);
        c.buf.setByte(index - c.offset, value);
        return this;
    }

    @Override
    protected void _setByte(int index, int value) {
        setByte(index, value);
    }

    @Override
    public PyCompositeByteBuf setShort(int index, int value) {
        return (PyCompositeByteBuf) super.setShort(index, value);
    }

    @Override
    protected void _setShort(int index, int value) {
        Component c = findComponent(index);
        if (index + 2 <= c.endOffset) {
            c.buf.setShort(index - c.offset, value);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            _setByte(index, (byte) (value >>> 8));
            _setByte(index + 1, (byte) value);
        } else {
            _setByte(index, (byte) value);
            _setByte(index + 1, (byte) (value >>> 8));
        }
    }

    @Override
    public PyCompositeByteBuf setMedium(int index, int value) {
        return (PyCompositeByteBuf) super.setMedium(index, value);
    }

    @Override
    protected void _setMedium(int index, int value) {
        Component c = findComponent(index);
        if (index + 3 <= c.endOffset) {
            c.buf.setMedium(index - c.offset, value);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            _setShort(index, (short) (value >> 8));
            _setByte(index + 2, (byte) value);
        } else {
            _setShort(index, (short) value);
            _setByte(index + 2, (byte) (value >>> 16));
        }
    }

    @Override
    public PyCompositeByteBuf setInt(int index, int value) {
        return (PyCompositeByteBuf) super.setInt(index, value);
    }

    @Override
    protected void _setInt(int index, int value) {
        Component c = findComponent(index);
        if (index + 4 <= c.endOffset) {
            c.buf.setInt(index - c.offset, value);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            _setShort(index, (short) (value >>> 16));
            _setShort(index + 2, (short) value);
        } else {
            _setShort(index, (short) value);
            _setShort(index + 2, (short) (value >>> 16));
        }
    }

    @Override
    public PyCompositeByteBuf setLong(int index, long value) {
        return (PyCompositeByteBuf) super.setLong(index, value);
    }

    @Override
    protected void _setLong(int index, long value) {
        Component c = findComponent(index);
        if (index + 8 <= c.endOffset) {
            c.buf.setLong(index - c.offset, value);
        } else if (order() == ByteOrder.BIG_ENDIAN) {
            _setInt(index, (int) (value >>> 32));
            _setInt(index + 4, (int) value);
        } else {
            _setInt(index, (int) value);
            _setInt(index + 4, (int) (value >>> 32));
        }
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.length);
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.setBytes(index - adjustment, src, srcIndex, localLength);
            index += localLength;
            srcIndex += localLength;
            length -= localLength;
            i++;
        }
        return this;
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, ByteBuffer src) {
        int limit = src.limit();
        int length = src.remaining();

        checkIndex(index, length);
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        try {
            while (length > 0) {
                Component c = components.get(i);
                ByteBuf s = c.buf;
                int adjustment = c.offset;
                int localLength = Math.min(length, s.capacity() - (index - adjustment));
                src.limit(src.position() + localLength);
                s.setBytes(index - adjustment, src);
                index += localLength;
                length -= localLength;
                i++;
            }
        } finally {
            src.limit(limit);
        }
        return this;
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        checkSrcIndex(index, length, srcIndex, src.capacity());
        if (length == 0) {
            return this;
        }

        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.setBytes(index - adjustment, src, srcIndex, localLength);
            index += localLength;
            srcIndex += localLength;
            length -= localLength;
            i++;
        }
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        checkIndex(index, length);
        if (length == 0) {
            return in.read(EmptyArrays.EMPTY_BYTES);
        }

        int i = toComponentIndex(index);
        int readBytes = 0;

        do {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            int localReadBytes = s.setBytes(index - adjustment, in, localLength);
            if (localReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                } else {
                    break;
                }
            }

            if (localReadBytes == localLength) {
                index += localLength;
                length -= localLength;
                readBytes += localLength;
                i++;
            } else {
                index += localReadBytes;
                length -= localReadBytes;
                readBytes += localReadBytes;
            }
        } while (length > 0);

        return readBytes;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        checkIndex(index, length);
        if (length == 0) {
            return in.read(EMPTY_NIO_BUFFER);
        }

        int i = toComponentIndex(index);
        int readBytes = 0;
        do {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            int localReadBytes = s.setBytes(index - adjustment, in, localLength);

            if (localReadBytes == 0) {
                break;
            }

            if (localReadBytes < 0) {
                if (readBytes == 0) {
                    return -1;
                } else {
                    break;
                }
            }

            if (localReadBytes == localLength) {
                index += localLength;
                length -= localLength;
                readBytes += localLength;
                i++;
            } else {
                index += localReadBytes;
                length -= localReadBytes;
                readBytes += localReadBytes;
            }
        } while (length > 0);

        return readBytes;
    }

    @Override
    public ByteBuf copy(int index, int length) {
        checkIndex(index, length);
        ByteBuf dst = Unpooled.buffer(length);
        if (length != 0) {
            copyTo(index, length, toComponentIndex(index), dst);
        }
        return dst;
    }

    private void copyTo(int index, int length, int componentId, ByteBuf dst) {
        int dstIndex = 0;
        int i = componentId;

        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            i++;
        }

        dst.writerIndex(dst.capacity());
    }

    /**
     * Return the {@link ByteBuf} on the specified index
     *
     * @param cIndex
     *            the index for which the {@link ByteBuf} should be returned
     * @return buf the {@link ByteBuf} on the specified index
     */
    public ByteBuf component(int cIndex) {
        return internalComponent(cIndex).duplicate();
    }

    /**
     * Return the {@link ByteBuf} on the specified index
     *
     * @param offset
     *            the offset for which the {@link ByteBuf} should be returned
     * @return the {@link ByteBuf} on the specified index
     */
    public ByteBuf componentAtOffset(int offset) {
        return internalComponentAtOffset(offset).duplicate();
    }

    /**
     * Return the internal {@link ByteBuf} on the specified index. Note that updating the indexes of the returned buffer
     * will lead to an undefined behavior of this buffer.
     *
     * @param cIndex
     *            the index for which the {@link ByteBuf} should be returned
     */
    public ByteBuf internalComponent(int cIndex) {
        checkComponentIndex(cIndex);
        return components.get(cIndex).buf;
    }

    /**
     * Return the internal {@link ByteBuf} on the specified offset. Note that updating the indexes of the returned
     * buffer will lead to an undefined behavior of this buffer.
     *
     * @param offset
     *            the offset for which the {@link ByteBuf} should be returned
     */
    public ByteBuf internalComponentAtOffset(int offset) {
        return findComponent(offset).buf;
    }

    private Component findComponent(int offset) {
        checkIndex(offset);

        for (int low = 0, high = components.size(); low <= high;) {
            int mid = low + high >>> 1;
            Component c = components.get(mid);
            if (offset >= c.endOffset) {
                low = mid + 1;
            } else if (offset < c.offset) {
                high = mid - 1;
            } else {
                assert c.length != 0;
                return c;
            }
        }

        throw new Error("should not reach here");
    }

    @Override
    public int nioBufferCount() {
        switch (components.size()) {
        case 0:
            return 1;
        case 1:
            return components.get(0).buf.nioBufferCount();
        default:
            int count = 0;
            int componentsCount = components.size();
            for (int i = 0; i < componentsCount; i++) {
                Component c = components.get(i);
                count += c.buf.nioBufferCount();
            }
            return count;
        }
    }

    @Override
    public ByteBuffer internalNioBuffer(int index, int length) {
        switch (components.size()) {
        case 0:
            return EMPTY_NIO_BUFFER;
        case 1:
            return components.get(0).buf.internalNioBuffer(index, length);
        default:
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        checkIndex(index, length);

        switch (components.size()) {
        case 0:
            return EMPTY_NIO_BUFFER;
        case 1:
            ByteBuf buf = components.get(0).buf;
            if (buf.nioBufferCount() == 1) {
                return components.get(0).buf.nioBuffer(index, length);
            }
        }

        ByteBuffer merged = ByteBuffer.allocate(length).order(order());
        ByteBuffer[] buffers = nioBuffers(index, length);

        for (ByteBuffer buf : buffers) {
            merged.put(buf);
        }

        merged.flip();
        return merged;
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        checkIndex(index, length);
        if (length == 0) {
            return new ByteBuffer[] { EMPTY_NIO_BUFFER };
        }

        List<ByteBuffer> buffers = new ArrayList<ByteBuffer>(components.size());
        int i = toComponentIndex(index);
        while (length > 0) {
            Component c = components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            switch (s.nioBufferCount()) {
            case 0:
                throw new UnsupportedOperationException();
            case 1:
                buffers.add(s.nioBuffer(index - adjustment, localLength));
                break;
            default:
                Collections.addAll(buffers, s.nioBuffers(index - adjustment, localLength));
            }

            index += localLength;
            length -= localLength;
            i++;
        }

        return buffers.toArray(new ByteBuffer[buffers.size()]);
    }

    /**
     * Consolidate the composed {@link ByteBuf}s
     */
    public PyCompositeByteBuf consolidate() {
        ensureAccessible();
        final int numComponents = numComponents();
        if (numComponents <= 1) {
            return this;
        }

        final Component last = components.get(numComponents - 1);
        final int capacity = last.endOffset;
        final ByteBuf consolidated = allocBuffer(capacity);

        for (int i = 0; i < numComponents; i++) {
            Component c = components.get(i);
            ByteBuf b = c.buf;
            consolidated.writeBytes(b);
            c.freeIfNecessary();
        }

        components.clear();
        components.add(new Component(consolidated));
        updateComponentOffsets(0);
        return this;
    }

    /**
     * Consolidate the composed {@link ByteBuf}s
     *
     * @param cIndex
     *            the index on which to start to compose
     * @param numComponents
     *            the number of components to compose
     */
    public PyCompositeByteBuf consolidate(int cIndex, int numComponents) {
        checkComponentIndex(cIndex, numComponents);
        if (numComponents <= 1) {
            return this;
        }

        final int endCIndex = cIndex + numComponents;
        final Component last = components.get(endCIndex - 1);
        final int capacity = last.endOffset - components.get(cIndex).offset;
        final ByteBuf consolidated = allocBuffer(capacity);

        for (int i = cIndex; i < endCIndex; i++) {
            Component c = components.get(i);
            ByteBuf b = c.buf;
            consolidated.writeBytes(b);
            c.freeIfNecessary();
        }

        components.subList(cIndex + 1, endCIndex).clear();
        components.set(cIndex, new Component(consolidated));
        updateComponentOffsets(cIndex);
        return this;
    }

    /**
     * Discard all {@link ByteBuf}s which are read.
     */
    public PyCompositeByteBuf discardReadComponents() {
        ensureAccessible();
        final int readerIndex = readerIndex();
        if (readerIndex == 0) {
            return this;
        }

        // Discard everything if (readerIndex = writerIndex = capacity).
        int writerIndex = writerIndex();
        if (readerIndex == writerIndex && writerIndex == capacity()) {
            for (Component c : components) {
                c.freeIfNecessary();
            }
            components.clear();
            setIndex(0, 0);
            adjustMarkers(readerIndex);
            return this;
        }

        // Remove read components.
        int firstComponentId = toComponentIndex(readerIndex);
        for (int i = 0; i < firstComponentId; i++) {
            components.get(i).freeIfNecessary();
        }
        components.subList(0, firstComponentId).clear();

        // Update indexes and markers.
        Component first = components.get(0);
        int offset = first.offset;
        updateComponentOffsets(0);
        setIndex(readerIndex - offset, writerIndex - offset);
        adjustMarkers(offset);
        return this;
    }

    @Override
    public PyCompositeByteBuf discardReadBytes() {
        ensureAccessible();
        final int readerIndex = readerIndex();
        if (readerIndex == 0) {
            return this;
        }

        // Discard everything if (readerIndex = writerIndex = capacity).
        int writerIndex = writerIndex();
        if (readerIndex == writerIndex && writerIndex == capacity()) {
            for (Component c : components) {
                c.freeIfNecessary();
            }
            components.clear();
            setIndex(0, 0);
            adjustMarkers(readerIndex);
            return this;
        }

        // Remove read components.
        int firstComponentId = toComponentIndex(readerIndex);
        for (int i = 0; i < firstComponentId; i++) {
            components.get(i).freeIfNecessary();
        }
        components.subList(0, firstComponentId).clear();

        // Remove or replace the first readable component with a new slice.
        Component c = components.get(0);
        int adjustment = readerIndex - c.offset;
        if (adjustment == c.length) {
            // new slice would be empty, so remove instead
            components.remove(0);
        } else {
            Component newC = new Component(c.buf.slice(adjustment, c.length - adjustment));
            components.set(0, newC);
        }

        // Update indexes and markers.
        updateComponentOffsets(0);
        setIndex(0, writerIndex - readerIndex);
        adjustMarkers(readerIndex);
        return this;
    }

    private ByteBuf allocBuffer(int capacity) {
        if (direct) {
            return alloc().directBuffer(capacity);
        }
        return alloc().heapBuffer(capacity);
    }

    @Override
    public String toString() {
        String result = super.toString();
        result = result.substring(0, result.length() - 1);
        return result + ", components=" + components.size() + ')';
    }

    private static final class Component {
        final ByteBuf buf;
        final int length;
        int offset;
        int endOffset;

        Component(ByteBuf buf) {
            this.buf = buf;
            length = buf.readableBytes();
        }

        void freeIfNecessary() {
            // Unwrap so that we can free slices, too.
            buf.release(); // We should not get a NPE here. If so, it must be a bug.
        }
    }

    @Override
    public PyCompositeByteBuf readerIndex(int readerIndex) {
        return (PyCompositeByteBuf) super.readerIndex(readerIndex);
    }

    @Override
    public PyCompositeByteBuf writerIndex(int writerIndex) {
        return (PyCompositeByteBuf) super.writerIndex(writerIndex);
    }

    @Override
    public PyCompositeByteBuf setIndex(int readerIndex, int writerIndex) {
        return (PyCompositeByteBuf) super.setIndex(readerIndex, writerIndex);
    }

    @Override
    public PyCompositeByteBuf clear() {
        return (PyCompositeByteBuf) super.clear();
    }

    @Override
    public PyCompositeByteBuf markReaderIndex() {
        return (PyCompositeByteBuf) super.markReaderIndex();
    }

    @Override
    public PyCompositeByteBuf resetReaderIndex() {
        return (PyCompositeByteBuf) super.resetReaderIndex();
    }

    @Override
    public PyCompositeByteBuf markWriterIndex() {
        return (PyCompositeByteBuf) super.markWriterIndex();
    }

    @Override
    public PyCompositeByteBuf resetWriterIndex() {
        return (PyCompositeByteBuf) super.resetWriterIndex();
    }

    @Override
    public PyCompositeByteBuf ensureWritable(int minWritableBytes) {
        return (PyCompositeByteBuf) super.ensureWritable(minWritableBytes);
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, ByteBuf dst) {
        return (PyCompositeByteBuf) super.getBytes(index, dst);
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, ByteBuf dst, int length) {
        return (PyCompositeByteBuf) super.getBytes(index, dst, length);
    }

    @Override
    public PyCompositeByteBuf getBytes(int index, byte[] dst) {
        return (PyCompositeByteBuf) super.getBytes(index, dst);
    }

    @Override
    public PyCompositeByteBuf setBoolean(int index, boolean value) {
        return (PyCompositeByteBuf) super.setBoolean(index, value);
    }

    @Override
    public PyCompositeByteBuf setChar(int index, int value) {
        return (PyCompositeByteBuf) super.setChar(index, value);
    }

    @Override
    public PyCompositeByteBuf setFloat(int index, float value) {
        return (PyCompositeByteBuf) super.setFloat(index, value);
    }

    @Override
    public PyCompositeByteBuf setDouble(int index, double value) {
        return (PyCompositeByteBuf) super.setDouble(index, value);
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, ByteBuf src) {
        return (PyCompositeByteBuf) super.setBytes(index, src);
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, ByteBuf src, int length) {
        return (PyCompositeByteBuf) super.setBytes(index, src, length);
    }

    @Override
    public PyCompositeByteBuf setBytes(int index, byte[] src) {
        return (PyCompositeByteBuf) super.setBytes(index, src);
    }

    @Override
    public PyCompositeByteBuf setZero(int index, int length) {
        return (PyCompositeByteBuf) super.setZero(index, length);
    }

    @Override
    public PyCompositeByteBuf readBytes(ByteBuf dst) {
        return (PyCompositeByteBuf) super.readBytes(dst);
    }

    @Override
    public PyCompositeByteBuf readBytes(ByteBuf dst, int length) {
        return (PyCompositeByteBuf) super.readBytes(dst, length);
    }

    @Override
    public PyCompositeByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return (PyCompositeByteBuf) super.readBytes(dst, dstIndex, length);
    }

    @Override
    public PyCompositeByteBuf readBytes(byte[] dst) {
        return (PyCompositeByteBuf) super.readBytes(dst);
    }

    @Override
    public PyCompositeByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return (PyCompositeByteBuf) super.readBytes(dst, dstIndex, length);
    }

    @Override
    public PyCompositeByteBuf readBytes(ByteBuffer dst) {
        return (PyCompositeByteBuf) super.readBytes(dst);
    }

    @Override
    public PyCompositeByteBuf readBytes(OutputStream out, int length) throws IOException {
        return (PyCompositeByteBuf) super.readBytes(out, length);
    }

    @Override
    public PyCompositeByteBuf skipBytes(int length) {
        return (PyCompositeByteBuf) super.skipBytes(length);
    }

    @Override
    public PyCompositeByteBuf writeBoolean(boolean value) {
        return (PyCompositeByteBuf) super.writeBoolean(value);
    }

    @Override
    public PyCompositeByteBuf writeByte(int value) {
        return (PyCompositeByteBuf) super.writeByte(value);
    }

    @Override
    public PyCompositeByteBuf writeShort(int value) {
        return (PyCompositeByteBuf) super.writeShort(value);
    }

    @Override
    public PyCompositeByteBuf writeMedium(int value) {
        return (PyCompositeByteBuf) super.writeMedium(value);
    }

    @Override
    public PyCompositeByteBuf writeInt(int value) {
        return (PyCompositeByteBuf) super.writeInt(value);
    }

    @Override
    public PyCompositeByteBuf writeLong(long value) {
        return (PyCompositeByteBuf) super.writeLong(value);
    }

    @Override
    public PyCompositeByteBuf writeChar(int value) {
        return (PyCompositeByteBuf) super.writeChar(value);
    }

    @Override
    public PyCompositeByteBuf writeFloat(float value) {
        return (PyCompositeByteBuf) super.writeFloat(value);
    }

    @Override
    public PyCompositeByteBuf writeDouble(double value) {
        return (PyCompositeByteBuf) super.writeDouble(value);
    }

    @Override
    public PyCompositeByteBuf writeBytes(ByteBuf src) {
        return (PyCompositeByteBuf) super.writeBytes(src);
    }

    @Override
    public PyCompositeByteBuf writeBytes(ByteBuf src, int length) {
        return (PyCompositeByteBuf) super.writeBytes(src, length);
    }

    @Override
    public PyCompositeByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return (PyCompositeByteBuf) super.writeBytes(src, srcIndex, length);
    }

    @Override
    public PyCompositeByteBuf writeBytes(byte[] src) {
        return (PyCompositeByteBuf) super.writeBytes(src);
    }

    @Override
    public PyCompositeByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return (PyCompositeByteBuf) super.writeBytes(src, srcIndex, length);
    }

    @Override
    public PyCompositeByteBuf writeBytes(ByteBuffer src) {
        return (PyCompositeByteBuf) super.writeBytes(src);
    }

    @Override
    public PyCompositeByteBuf writeZero(int length) {
        return (PyCompositeByteBuf) super.writeZero(length);
    }

    @Override
    public PyCompositeByteBuf retain(int increment) {
        return (PyCompositeByteBuf) super.retain(increment);
    }

    @Override
    public PyCompositeByteBuf retain() {
        return (PyCompositeByteBuf) super.retain();
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return nioBuffers(readerIndex(), readableBytes());
    }

    @Override
    public PyCompositeByteBuf discardSomeReadBytes() {
        return discardReadComponents();
    }

    @Override
    protected void deallocate() {
        if (freed) {
            return;
        }

        freed = true;
        int size = components.size();
        // We're not using foreach to avoid creating an iterator.
        // see https://github.com/netty/netty/issues/2642
        for (int i = 0; i < size; i++) {
            components.get(i).freeIfNecessary();
        }
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

	@Override
	protected short _getShortLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int _getUnsignedMediumLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected int _getIntLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected long _getLongLE(int index) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void _setShortLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setMediumLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setIntLE(int index, int value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void _setLongLE(int index, long value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getBytes(int index, FileChannel out, long position, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int setBytes(int index, FileChannel in, long position, int length)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
}
