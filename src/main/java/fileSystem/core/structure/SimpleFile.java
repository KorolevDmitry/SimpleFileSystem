package fileSystem.core.structure;

import fileSystem.core.structure.base.SimpleINode;

import java.io.IOException;

/**
 * File or directory representation.
 */
public class SimpleFile {
    protected final String[] path;
    protected final SimpleINode iNode;
    protected final SimpleFileReadWriter fileReadWriter;

    public static SimpleFile get(String[] path, SimpleINode iNode, SimpleFileReadWriter fileReadWriter) {
        if (iNode.isDirectory) {
            return new SimpleDirectory(path, iNode, fileReadWriter);
        } else {
            return new SimpleFile(path, iNode, fileReadWriter);
        }
    }

    protected SimpleFile(String[] path, SimpleINode iNode, SimpleFileReadWriter fileReadWriter) {
        this.path = path;
        this.iNode = iNode;
        this.fileReadWriter = fileReadWriter;
    }

    public String getName() {
        if (path.length > 0)
            return path[path.length - 1];
        return "";
    }

    public String[] getPath() {
        return path.clone();
    }

    public SimpleDirectory getParent() throws IOException {
        return fileReadWriter.getParent(this);
    }

    public void delete() throws IOException {
        fileReadWriter.deleteFile(this);
    }

    public byte[] readData() throws IOException {
        return fileReadWriter.readData(this);
    }

    public void writeData(byte[] data) throws IOException {
        fileReadWriter.writeData(this, data);
    }

    public long getTimeStamp() {
        return iNode.getTimeStamp();
    }

    public long getTotalSize() {
        return iNode.getTotalSize();
    }
}
