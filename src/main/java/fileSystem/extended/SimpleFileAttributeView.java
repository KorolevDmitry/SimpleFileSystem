package fileSystem.extended;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/17/14
 * Time: 4:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileAttributeView implements BasicFileAttributeView {

    private SimpleFileAttributeView(SimplePath path, boolean isSimpleView) {
    }

    @Override
    public String name() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BasicFileAttributes readAttributes() throws IOException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    static <V extends FileAttributeView> V get(SimplePath path, Class<V> type) {
        if (type == null)
            throw new NullPointerException();
        if (type == BasicFileAttributeView.class)
            return (V) new SimpleFileAttributeView(path, false);
        if (type == SimpleFileAttributeView.class)
            return (V) new SimpleFileAttributeView(path, true);
        return null;
    }

    static SimpleFileAttributeView get(SimplePath path, String type) {
        if (type == null)
            throw new NullPointerException();
        if (type.equals("basic"))
            return new SimpleFileAttributeView(path, false);
        if (type.equals("simple"))
            return new SimpleFileAttributeView(path, true);
        return null;
    }

    public Map<String, Object> readAttributes(String attrs) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }

    public void setAttribute(String attr, Object value) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
