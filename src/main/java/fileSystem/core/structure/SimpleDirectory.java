package fileSystem.core.structure;

import fileSystem.core.structure.base.SimpleINode;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Directory representation.
 */
public class SimpleDirectory extends SimpleFile {

    protected SimpleDirectory(String[] path, SimpleINode iNode, SimpleFileReadWriter fileReadWriter) {
        super(path, iNode, fileReadWriter);
    }

    public ArrayList<SimpleFile> getChildren() throws IOException {
        return fileReadWriter.readDirectoryData(this);
    }

    public SimpleDirectory createSubDirectory(String name) throws IOException {
        return (SimpleDirectory) fileReadWriter.createFile(this, name, true);
    }

    public SimpleFile createFile(String name) throws IOException {
        return fileReadWriter.createFile(this, name, false);
    }
}
