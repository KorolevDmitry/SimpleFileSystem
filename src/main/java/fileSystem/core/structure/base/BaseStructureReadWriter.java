package fileSystem.core.structure.base;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;

/**
 * Sync operations for base structure.
 */
public class BaseStructureReadWriter implements AutoCloseable {
    private final SeekableByteChannel channel;

    public BaseStructureReadWriter(SeekableByteChannel channel) {
        this.channel = channel;
    }

    public SimpleHeader readHeader() throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.HEADER_SIZE]);
        readFromChannel(Constants.HEADER_POS, buffer);
        assert buffer.position() == Constants.HEADER_SIZE;
        buffer.rewind();
        return new SimpleHeader(buffer);
    }

    public void writeHeader(SimpleHeader header) throws IOException {
        writeToChannel(Constants.HEADER_POS, header.toBuffer());
    }


    public SimpleBlock readBlock(long pos) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.BLOCK_SIZE]);
        readFromChannel(pos, buffer);
        assert buffer.position() == Constants.BLOCK_SIZE;
        buffer.rewind();
        return new SimpleBlock(buffer, pos);
    }

    public SimpleBlock allocateNewBlock() throws IOException {
        SimpleBlock block = new SimpleBlock(-1);
        long pos = writeToChannel(block.toBuffer());
        return new SimpleBlock(pos);
    }

    public void writeBlock(SimpleBlock block) throws IOException {
        assert block.getCurPos() != -1;
        writeToChannel(block.getCurPos(), block.toBuffer());
    }


    public SimpleINode readINode(long pos) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[Constants.INODE_SIZE]);
        readFromChannel(pos, buffer);
        assert buffer.position() == Constants.BLOCK_SIZE;
        buffer.rewind();
        return new SimpleINode(buffer, pos);
    }

    public SimpleINode allocateNewINode() throws IOException {
        SimpleINode iNode = new SimpleINode(-1, System.currentTimeMillis());
        long pos = writeToChannel(iNode.toBuffer());
        return new SimpleINode(pos, iNode.getTimeStamp());
    }

    public void writeINode(SimpleINode iNode) throws IOException {
        assert iNode.getCurPos() != -1;
        iNode.setTimeStamp(System.currentTimeMillis());
        writeToChannel(iNode.getCurPos(), iNode.toBuffer());
    }


    private void readFromChannel(long pos, ByteBuffer target) throws IOException {
        synchronized (channel) {
            channel.position(pos);
            channel.read(target);
        }
    }

    private void writeToChannel(long pos, ByteBuffer source) throws IOException {
        synchronized (channel) {
            channel.position(pos);
            channel.write(source);
        }
    }

    private long writeToChannel(ByteBuffer source) throws IOException {
        synchronized (channel) {
            long pos = channel.size();
            channel.position(pos);
            channel.write(source);
            return pos;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
