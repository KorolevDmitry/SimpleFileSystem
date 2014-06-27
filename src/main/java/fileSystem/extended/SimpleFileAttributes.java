package fileSystem.extended;

import fileSystem.core.structure.SimpleDirectory;
import fileSystem.core.structure.SimpleFile;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/17/14
 * Time: 5:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileAttributes implements BasicFileAttributes {
    SimpleFile file;

    public SimpleFileAttributes(SimpleFile file) {
        this.file = file;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(file.getTimeStamp());
    }

    @Override
    public FileTime lastAccessTime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public FileTime creationTime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isRegularFile() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isDirectory() {
        return file instanceof SimpleDirectory;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isOther() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long size() {
        return file.getTotalSize();
    }

    @Override
    public Object fileKey() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
