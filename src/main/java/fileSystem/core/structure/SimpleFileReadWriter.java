package fileSystem.core.structure;

import fileSystem.core.structure.base.*;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Base operations on files.
 */
public class SimpleFileReadWriter implements AutoCloseable {
    private final BaseStructureReadWriter readWriter;
    private final SpaceAllocator spaceAllocator;
    private final INodeLocker iNodeLocker;
    private final DirectoryParser directoryParser;

    public SimpleFileReadWriter(SeekableByteChannel channel) throws IOException {
        boolean channelIsEmpty = channel.size() == 0;
        readWriter = new BaseStructureReadWriter(channel);
        spaceAllocator = new SpaceAllocator(readWriter, channelIsEmpty);
        iNodeLocker = new INodeLocker(readWriter);
        directoryParser = new DirectoryParser();

        if (channelIsEmpty) {
            createRootDirectory();
        }
    }


    public SimpleFile getFile(String[] path) throws IOException {
        SimpleINode iNode = spaceAllocator.getRoot();
        for (int i = 0; i < path.length; i++) {
            if (!iNode.isDirectory)
                throw new FileNotFoundException(path[i]);
            Long pos = directoryParser.fromBytes(readData(iNode)).get(path[i]);
            if (pos == null)
                return null;
            iNode = readWriter.readINode(pos);
        }
        return SimpleFile.get(path, iNode, this);
    }

    public byte[] readData(SimpleFile file) throws IOException {
        return readData(file.iNode);
    }

    public ArrayList<SimpleFile> readDirectoryData(SimpleDirectory dir) throws IOException {
        ArrayList<SimpleFile> files = new ArrayList();
        byte[] parentData = readData(dir);
        HashMap<String, Long> children = directoryParser.fromBytes(parentData);
        for (Map.Entry<String, Long> entry : children.entrySet()) {
            SimpleINode child = readWriter.readINode(entry.getValue());
            String[] childPath = new String[dir.path.length + 1];
            System.arraycopy(dir.path, 0, childPath, 0, dir.path.length);
            childPath[dir.path.length] = entry.getKey();
            if (child.isDirectory) {
                files.add(new SimpleDirectory(childPath, child, this));
            } else {
                files.add(new SimpleFile(childPath, child, this));
            }
        }
        return files;
    }

    public void writeData(SimpleFile file, byte[] data) throws IOException {
        if (file instanceof SimpleDirectory)
            throw new IllegalStateException("Can not do it with directories.");
        writeData(file.iNode, data);
    }

    public SimpleFile createFile(String[] path, boolean isDirectory) throws IOException {
        if (path.length == 0)
            throw new IllegalStateException("Root directory is already created");
        String[] parentPath = getParentPath(path);
        SimpleDirectory dir = (SimpleDirectory) getFile(parentPath);
        String name = path[path.length - 1];

        return createFile(dir, name, isDirectory);
    }

    public void deleteFile(SimpleFile file) throws IOException {
        iNodeLocker.beginWrite(file.iNode);
        try {
            SimpleDirectory parent = getParent(file);
            if (parent == null)
                throw new IllegalStateException("Can not remove root directory");
            if (file.iNode.isDirectory) {
                if (readDirectoryData((SimpleDirectory) file).size() != 0)
                    throw new IllegalStateException("Can not remove not empty directory");
            }

            removeFromDirectory(parent, file.getName());
            spaceAllocator.markListOfBlocksAsFree(file.iNode.getFirstDataBlock(), file.iNode.getLastDataBlock());
            spaceAllocator.markINodeAsFree(file.iNode.getCurPos());
        } finally {
            iNodeLocker.endWrite(file.iNode);
        }
    }

    public SimpleDirectory getParent(SimpleFile file) throws IOException {
        String[] parentPath = getParentPath(file.path);
        if (parentPath == null)
            return null;
        return (SimpleDirectory) getFile(parentPath);
    }


    SimpleFile createFile(SimpleDirectory dir, String name, boolean isDirectory) throws IOException {

        byte[] parentData = readData(dir);
        HashMap<String, Long> children = directoryParser.fromBytes(parentData);
        if (children.containsKey(name))
            throw new FileAlreadyExistsException(name);

        SimpleINode iNode = spaceAllocator.getFreeINode();
        iNode.isDirectory = isDirectory;
        if (isDirectory) {
            writeData(iNode, directoryParser.toBytes(new HashMap<String, Long>()));
        } else {
            writeData(iNode, new byte[0]);
        }
        addToDirectory(dir, name, iNode.getCurPos());

        String[] childPath = new String[dir.path.length + 1];
        System.arraycopy(dir.path, 0, childPath, 0, dir.path.length);
        childPath[dir.path.length] = name;

        if (isDirectory)
            return new SimpleDirectory(childPath, iNode, this);
        else
            return new SimpleFile(childPath, iNode, this);
    }

    private String[] getParentPath(String[] path) {
        if (path.length == 0)
            return null;
        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, path.length - 1);
        return parentPath;
    }

    private void removeFromDirectory(SimpleDirectory dir, String name) throws IOException {
        iNodeLocker.beginWrite(dir.iNode);
        try {
            byte[] parentData = readData(dir.iNode);
            HashMap<String, Long> children = directoryParser.fromBytes(parentData);
            children.remove(name);
            parentData = directoryParser.toBytes(children);
            writeData(dir.iNode, parentData);
        } finally {
            iNodeLocker.endWrite(dir.iNode);
        }
    }

    private void addToDirectory(SimpleDirectory dir, String name, Long pos) throws IOException {
        iNodeLocker.beginWrite(dir.iNode);
        try {
            byte[] parentData = readData(dir.iNode);
            HashMap<String, Long> children = directoryParser.fromBytes(parentData);
            children.put(name, pos);
            parentData = directoryParser.toBytes(children);
            writeData(dir.iNode, parentData);
        } finally {
            iNodeLocker.endWrite(dir.iNode);
        }
    }

    private void writeData(SimpleINode iNode, byte[] data) throws IOException {
        iNodeLocker.beginWrite(iNode);
        try {
            if (iNode.getFirstDataBlock() != -1) {
                spaceAllocator.markListOfBlocksAsFree(iNode.getFirstDataBlock(), iNode.getLastDataBlock());
            }
            int i = 0;
            SimpleBlock next = spaceAllocator.getFreeBlock();
            iNode.setFirstDataBlock(next.getCurPos());
            for (; i < data.length - Constants.BLOCK_DATA_SIZE; i += Constants.BLOCK_DATA_SIZE) {
                SimpleBlock current = next;
                System.arraycopy(data, i, current.getData(), 0, Constants.BLOCK_DATA_SIZE);
                current.setSize(Constants.BLOCK_DATA_SIZE);
                next = spaceAllocator.getFreeBlock();
                current.setNextDataBlock(next.getCurPos());
                readWriter.writeBlock(current);
            }
            if (i != data.length) {
                int length = data.length - i;
                System.arraycopy(data, i, next.getData(), 0, length);
                next.setSize(length);
            }
            readWriter.writeBlock(next);
            iNode.setLastDataBlock(next.getCurPos());
            readWriter.writeINode(iNode);
        } finally {
            iNodeLocker.endWrite(iNode);
        }
    }

    private byte[] readData(SimpleINode iNode) throws IOException {
        iNodeLocker.beginRead(iNode);
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            long pos = iNode.getFirstDataBlock();
            while (pos != -1) {
                SimpleBlock block = readWriter.readBlock(pos);
                stream.write(block.getData(), 0, block.getSize());
                pos = block.getNextDataBlock();
            }

            return stream.toByteArray();
        } finally {
            iNodeLocker.endRead(iNode);
        }
    }

    private void createRootDirectory() throws IOException {
        SimpleINode iNode = spaceAllocator.getRoot();
        iNodeLocker.beginWrite(iNode);
        try {
            iNode.isDirectory = true;
            writeData(iNode, directoryParser.toBytes(new HashMap<String, Long>()));
        } finally {
            iNodeLocker.endWrite(iNode);
        }
    }

    @Override
    public void close() throws IOException {
        readWriter.close();
        spaceAllocator.close();
        iNodeLocker.close();
    }
}
