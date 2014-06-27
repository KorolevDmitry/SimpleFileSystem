package fileSystem.core.structure.base;

import java.nio.ByteBuffer;

/**
 * Contains data of file or directory.
 */
public class SimpleBlock {
    private final byte[] data;
    private final long curPos;
    private long nextDataBlock;
    private int size;

    SimpleBlock(ByteBuffer buffer, long curPos) {
        this.curPos = curPos;
        this.nextDataBlock = buffer.getLong();
        this.size = buffer.getInt();
        this.data = new byte[Constants.BLOCK_DATA_SIZE];
        buffer.get(this.data);
    }

    SimpleBlock(long curPos) {
        this.curPos = curPos;
        this.nextDataBlock = -1;
        this.size = 0;
        this.data = new byte[Constants.BLOCK_DATA_SIZE];
    }

    public byte[] getData() {
        return data;
    }

    public long getNextDataBlock() {
        return nextDataBlock;
    }

    public void setNextDataBlock(long nextDataBlock) {
        this.nextDataBlock = nextDataBlock;
    }

    public long getCurPos() {
        return curPos;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.BLOCK_SIZE]);
        buffer.putLong(nextDataBlock);
        buffer.putInt(size);
        //buffer.position(Constants.BLOCK_SIZE-Constants.BLOCK_DATA_SIZE);
        buffer.put(data);
        buffer.rewind();
        return buffer;
    }

    @Override
    public int hashCode() {
        return (int) curPos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleBlock))
            return false;
        return curPos == ((SimpleBlock) obj).curPos;
    }
}
