package fileSystem.core.structure;

import fileSystem.core.structure.base.BaseStructureReadWriter;
import fileSystem.core.structure.base.SimpleINode;

import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Logic for sync operations on files.
 */
public class INodeLocker implements AutoCloseable {
    private final ReadWriteLock rwlock;
    private final BaseStructureReadWriter readWriter;

    public INodeLocker(BaseStructureReadWriter readWriter) {
        this.readWriter = readWriter;
        this.rwlock = new ReentrantReadWriteLock();
    }

    public void beginWrite(SimpleINode file) throws IOException {
        rwlock.writeLock().lock();
        if (!check(file)) {
            rwlock.writeLock().unlock();
            throw new IllegalStateException("File was changed.");
        }
    }

    public void endWrite(SimpleINode file) {
        rwlock.writeLock().unlock();
    }

    public void beginRead(SimpleINode file) {
        rwlock.readLock().lock();
    }

    public void endRead(SimpleINode file) {
        rwlock.readLock().unlock();
    }

    private boolean check(SimpleINode iNode) throws IOException {
        if (iNode.isDirectory) return true;
        SimpleINode readINode = readWriter.readINode(iNode.getCurPos());
        return readINode.getTimeStamp() == iNode.getTimeStamp();
    }

    @Override
    public void close() throws IOException {
    }
}
