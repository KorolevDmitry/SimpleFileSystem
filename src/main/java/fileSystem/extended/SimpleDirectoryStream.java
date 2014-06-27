package fileSystem.extended;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/19/14
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleDirectoryStream implements DirectoryStream<Path> {
    public SimpleDirectoryStream(SimplePath paths, Filter<? super Path> filter) {
    }

    @Override
    public Iterator<Path> iterator() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void close() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
