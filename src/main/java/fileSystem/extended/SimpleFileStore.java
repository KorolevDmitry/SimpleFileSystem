package fileSystem.extended;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/26/14
 * Time: 2:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileStore extends FileStore {
    public SimpleFileStore(SimplePath path) {
    }

    @Override
    public String name() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String type() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isReadOnly() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTotalSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getUsableSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
