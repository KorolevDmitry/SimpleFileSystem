package fileSystem.extended;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/16/14
 * Time: 4:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileSystemProvider extends FileSystemProvider {

    private final Map<Path, SimpleFileSystem> filesystems = new HashMap<>();

    @Override
    public String getScheme() {
        return "simple";
    }

    protected Path uriToPath(URI uri) {
        String scheme = uri.getScheme();
        if ((scheme == null) || !scheme.equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI scheme is not '" + getScheme() + "'");
        }
        return Paths.get(uri).toAbsolutePath();
    }

    private boolean ensureFile(Path path) {
        try {
            BasicFileAttributes attrs =
                    Files.readAttributes(path, BasicFileAttributes.class);
            if (!attrs.isRegularFile())
                throw new UnsupportedOperationException();
            return true;
        } catch (IOException ioe) {
            return false;
        }
    }

    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        Path path = uriToPath(uri);
        synchronized (filesystems) {
            Path realPath = null;
            if (ensureFile(path)) {
                realPath = path.toRealPath();
                if (filesystems.containsKey(realPath))
                    throw new FileSystemAlreadyExistsException();
            }
            SimpleFileSystem sfs = null;
            try {
                sfs = new SimpleFileSystem(this, path, env);
            } catch (SimpleError se) {
                String pname = path.toString();
                if (pname.endsWith(".simple"))
                    throw se;
                // assume NOT a simple file
                throw new UnsupportedOperationException();
            }
            filesystems.put(realPath, sfs);
            return sfs;
        }
    }

    @Override
    public FileSystem newFileSystem(Path path, Map<String, ?> env)
            throws IOException {
        if (path.getFileSystem() != FileSystems.getDefault()) {
            throw new UnsupportedOperationException();
        }
        ensureFile(path);
        try {
            return new SimpleFileSystem(this, path, env);
        } catch (SimpleError se) {
            String pname = path.toString();
            if (pname.endsWith(".simple"))
                throw se;
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public FileSystem getFileSystem(URI uri) {
        synchronized (filesystems) {
            SimpleFileSystem zipfs = null;
            try {
                zipfs = filesystems.get(uriToPath(uri).toRealPath());
            } catch (IOException x) {
                // ignore the ioe from toRealPath(), return FSNFE
            }
            if (zipfs == null)
                throw new FileSystemNotFoundException();
            return zipfs;
        }
    }

    @Override
    public Path getPath(URI uri) {
        String spec = uri.getSchemeSpecificPart();
        int sep = spec.indexOf("!/");
        if (sep == -1)
            throw new IllegalArgumentException("URI: "
                    + uri
                    + " does not contain path info ex. jar:file:/c:/foo.simple!/BAR");
        return getFileSystem(uri).getPath(spec.substring(sep + 1));
    }

    // Checks that the given file is a UnixPath
    static final SimplePath toSimplePath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof SimplePath))
            throw new ProviderMismatchException();
        return (SimplePath) path;
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path,
                                                              Set<? extends OpenOption> options,
                                                              ExecutorService exec,
                                                              FileAttribute<?>... attrs)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return toSimplePath(path).newByteChannel(options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return toSimplePath(dir).newDirectoryStream(filter);
    }

    @Override
    public FileChannel newFileChannel(Path path,
                                      Set<? extends OpenOption> options,
                                      FileAttribute<?>... attrs)
            throws IOException {
        return toSimplePath(path).newFileChannel(options, attrs);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        toSimplePath(dir).createDirectory(attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        toSimplePath(path).delete();
    }

    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        toSimplePath(source).copy(toSimplePath(target), options);
    }

    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        toSimplePath(source).move(toSimplePath(target), options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return toSimplePath(path).isSameFile(path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return toSimplePath(path).isHidden();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return toSimplePath(path).getFileStore();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        toSimplePath(path).checkAccess(modes);
    }

    @Override
    public InputStream newInputStream(Path path, OpenOption... options)
            throws IOException {
        return toSimplePath(path).newInputStream(options);
    }

    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options)
            throws IOException {
        return toSimplePath(path).newOutputStream(options);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return (V) SimpleFileAttributeView.get(toSimplePath(path), type);
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type == BasicFileAttributes.class || type == SimpleFileAttributes.class)
            return (A) toSimplePath(path).getAttributes();
        return null;
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return toSimplePath(path).readAttributes(attributes, options);
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        toSimplePath(path).setAttribute(attribute, value, options);
    }

    //////////////////////////////////////////////////////////////
    void removeFileSystem(Path sfpath, SimpleFileSystem sfs) throws IOException {
        synchronized (filesystems) {
            sfpath = sfpath.toRealPath();
            if (filesystems.get(sfpath) == sfs)
                filesystems.remove(sfpath);
        }
    }
}
