package fileSystem.core.structure.base;

import java.nio.ByteBuffer;

/**
 * Contains main info about whole file system
 */
public class SimpleHeader {
    volatile long nextFreeINode;
    volatile long nextFreeBlock;
    volatile long rootINode;

    public SimpleHeader(long nextFreeINode, long nextFreeBlock, long rootINode) {
        this.nextFreeINode = nextFreeINode;
        this.nextFreeBlock = nextFreeBlock;
        this.rootINode = rootINode;
    }

    public SimpleHeader(ByteBuffer buffer) {
        nextFreeINode = buffer.getLong();
        nextFreeBlock = buffer.getLong();
        rootINode = buffer.getLong();
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.HEADER_SIZE]);
        buffer.putLong(nextFreeINode);
        buffer.putLong(nextFreeBlock);
        buffer.putLong(rootINode);
        buffer.rewind();
        return buffer;
    }
}
