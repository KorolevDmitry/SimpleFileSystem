package fileSystem.core.structure.base;

import java.io.IOException;

/**
 * Logic for allocating free blocks and nodes.
 */
public class SpaceAllocator implements AutoCloseable {
    private final BaseStructureReadWriter readWriter;
    private final SimpleHeader header;

    public SpaceAllocator(BaseStructureReadWriter readWriter, boolean createHeader) throws IOException {
        this.readWriter = readWriter;
        if (createHeader)
            this.header = createHeader(readWriter);
        else
            this.header = readWriter.readHeader();
    }

    private SimpleHeader createHeader(BaseStructureReadWriter readWriter) throws IOException {
        SimpleHeader header = new SimpleHeader(-1, -1, -1);
        readWriter.writeHeader(header);

        header.nextFreeINode = readWriter.allocateNewINode().getCurPos();
        header.nextFreeBlock = readWriter.allocateNewBlock().getCurPos();
        header.rootINode = readWriter.allocateNewINode().getCurPos();

        readWriter.writeHeader(header);
        return header;
    }

    public SimpleINode getRoot() throws IOException {
        return readWriter.readINode(header.rootINode);
    }

    public SimpleBlock getFreeBlock() throws IOException {
        synchronized (header) {
            SimpleBlock result = readWriter.readBlock(header.nextFreeBlock);
            if (result.getNextDataBlock() == -1) {
                header.nextFreeBlock = readWriter.allocateNewBlock().getCurPos();
            } else {
                header.nextFreeBlock = result.getNextDataBlock();
            }
            readWriter.writeHeader(header);

            result.setNextDataBlock(-1);
            return result;
        }
    }

    public SimpleINode getFreeINode() throws IOException {
        synchronized (header) {
            SimpleINode result = readWriter.readINode(header.nextFreeINode);
            if (result.getNextFreeINode() == -1) {
                header.nextFreeINode = readWriter.allocateNewINode().getCurPos();
            } else {
                header.nextFreeINode = result.getNextFreeINode();
            }
            readWriter.writeHeader(header);

            result.setNextFreeINode(-1);
            return result;
        }
    }

    public void markListOfBlocksAsFree(long firstBlock, long lastBlock) throws IOException {
        synchronized (header) {
            SimpleBlock block = readWriter.readBlock(lastBlock);
            block.setNextDataBlock(header.nextFreeBlock);
            readWriter.writeBlock(block);
            header.nextFreeBlock = firstBlock;
            readWriter.writeHeader(header);
        }
    }

    public void markINodeAsFree(long pos) throws IOException {
        synchronized (header) {
            SimpleINode iNode = readWriter.readINode(pos);
            iNode.setNextFreeINode(header.nextFreeINode);
            readWriter.writeINode(iNode);
            header.nextFreeINode = iNode.getCurPos();
            readWriter.writeHeader(header);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
