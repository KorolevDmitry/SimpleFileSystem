package fileSystem.core.structure.base;

import java.nio.ByteBuffer;

/**
 * Contains main info about file or directory.
 */
public class SimpleINode {

    private final long curPos;
    private long nextFreeINode;
    private long totalSize;
    private long firstDataBlock;
    private long lastDataBlock;
    private long timeStamp;
    public boolean isDirectory;

    SimpleINode(long curPos, long timeStamp) {
        this.curPos = curPos;
        this.timeStamp = timeStamp;
        this.nextFreeINode = -1;
        this.totalSize = -1;
        this.firstDataBlock = -1;
        this.lastDataBlock = -1;
        this.isDirectory = false;
    }

    SimpleINode(ByteBuffer buffer, long curPos) {
        this.curPos = curPos;
        this.nextFreeINode = buffer.getLong();
        this.totalSize = buffer.getLong();
        this.firstDataBlock = buffer.getLong();
        this.lastDataBlock = buffer.getLong();
        this.timeStamp = buffer.getLong();
        this.isDirectory = buffer.get() == 1;
    }

    public long getCurPos() {
        return curPos;
    }

    public void setNextFreeINode(long nextFreeINode) {
        this.nextFreeINode = nextFreeINode;
    }

    public long getNextFreeINode() {
        return nextFreeINode;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setFirstDataBlock(long firstDataBlock) {
        this.firstDataBlock = firstDataBlock;
    }

    public long getFirstDataBlock() {
        return firstDataBlock;
    }

    public void setLastDataBlock(long lastDataBlock) {
        this.lastDataBlock = lastDataBlock;
    }

    public long getLastDataBlock() {
        return lastDataBlock;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }


    public ByteBuffer toBuffer() {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.INODE_SIZE]);
        buffer.putLong(nextFreeINode);
        buffer.putLong(totalSize);
        buffer.putLong(firstDataBlock);
        buffer.putLong(lastDataBlock);
        buffer.putLong(timeStamp);
        if (isDirectory)
            buffer.put((byte) 1);
        else
            buffer.put((byte) 0);
        buffer.rewind();
        return buffer;
    }

    @Override
    public int hashCode() {
        return (int) curPos;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleINode))
            return false;
        return curPos == ((SimpleINode) obj).curPos;
    }
}
