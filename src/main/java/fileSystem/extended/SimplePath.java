package fileSystem.extended;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileAttribute;
import java.util.*;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.*;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/17/14
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimplePath implements Path {

    private final SimpleFileSystem sfs;
    private final byte[] path;
    private volatile int[] offsets;
    private int hashcode = 0;  // cached hashcode (created lazily)

    SimplePath(SimpleFileSystem sfs, byte[] path) {
        this(sfs, path, false);
    }

    SimplePath(SimpleFileSystem sfs, byte[] path, boolean normalized) {
        this.sfs = sfs;
        if (normalized)
            this.path = path;
        else
            this.path = normalize(path);
    }

    // removes redundant slashs, replace "\" to zip separator "/"
    // and check for invalid characters
    private byte[] normalize(byte[] path) {
        if (path.length == 0)
            return path;
        byte prevC = 0;
        for (int i = 0; i < path.length; i++) {
            byte c = path[i];
            if (c == '\\')
                return normalize(path, i);
            if (c == (byte) '/' && prevC == '/')
                return normalize(path, i - 1);
            if (c == '\u0000')
                throw new InvalidPathException(sfs.getString(path),
                        "Path: nul character not allowed");
            prevC = c;
        }
        return path;
    }

    private byte[] normalize(byte[] path, int off) {
        byte[] to = new byte[path.length];
        int n = 0;
        while (n < off) {
            to[n] = path[n];
            n++;
        }
        int m = n;
        byte prevC = 0;
        while (n < path.length) {
            byte c = path[n++];
            if (c == (byte) '\\')
                c = (byte) '/';
            if (c == (byte) '/' && prevC == (byte) '/')
                continue;
            if (c == '\u0000')
                throw new InvalidPathException(sfs.getString(path),
                        "Path: nul character not allowed");
            to[m++] = c;
            prevC = c;
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length) ? to : Arrays.copyOf(to, m);
    }

    // create offset list if not already created
    private void initOffsets() {
        if (offsets == null) {
            int count, index;
            // count names
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index++];
                if (c != '/') {
                    count++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            // populate offsets
            int[] result = new int[count];
            count = 0;
            index = 0;
            while (index < path.length) {
                byte c = path[index];
                if (c == '/') {
                    index++;
                } else {
                    result[count++] = index++;
                    while (index < path.length && path[index] != '/')
                        index++;
                }
            }
            synchronized (this) {
                if (offsets == null)
                    offsets = result;
            }
        }
    }

    // resolved path for locating zip entry inside the zip file,
    // the result path does not contain ./ and .. components
    private volatile byte[] resolved = null;

    byte[] getResolvedPath() {
        byte[] r = resolved;
        if (r == null) {
            if (isAbsolute())
                r = getResolved();
            else
                r = toAbsolutePath().getResolvedPath();
            if (r[0] == '/')
                r = Arrays.copyOfRange(r, 1, r.length);
            resolved = r;
        }
        return resolved;
    }

    // Remove DotSlash(./) and resolve DotDot (..) components
    private byte[] getResolved() {
        if (path.length == 0)
            return path;
        for (int i = 0; i < path.length; i++) {
            byte c = path[i];
            if (c == (byte) '.')
                return resolve0();
        }
        return path;
    }

    // TBD: performance, avoid initOffsets
    private byte[] resolve0() {
        byte[] to = new byte[path.length];
        int nc = getNameCount();
        int[] lastM = new int[nc];
        int lastMOff = -1;
        int m = 0;
        for (int i = 0; i < nc; i++) {
            int n = offsets[i];
            int len = (i == offsets.length - 1) ?
                    (path.length - n) : (offsets[i + 1] - n - 1);
            if (len == 1 && path[n] == (byte) '.') {
                if (m == 0 && path[0] == '/')   // absolute path
                    to[m++] = '/';
                continue;
            }
            if (len == 2 && path[n] == '.' && path[n + 1] == '.') {
                if (lastMOff >= 0) {
                    m = lastM[lastMOff--];  // retreat
                    continue;
                }
                if (path[0] == '/') {  // "/../xyz" skip
                    if (m == 0)
                        to[m++] = '/';
                } else {               // "../xyz" -> "../xyz"
                    if (m != 0 && to[m - 1] != '/')
                        to[m++] = '/';
                    while (len-- > 0)
                        to[m++] = path[n++];
                }
                continue;
            }
            if (m == 0 && path[0] == '/' ||   // absolute path
                    m != 0 && to[m - 1] != '/') {   // not the first name
                to[m++] = '/';
            }
            lastM[++lastMOff] = m;
            while (len-- > 0)
                to[m++] = path[n++];
        }
        if (m > 1 && to[m - 1] == '/')
            m--;
        return (m == to.length) ? to : Arrays.copyOf(to, m);
    }

    @Override
    public SimpleFileSystem getFileSystem() {
        return sfs;
    }

    private SimplePath checkPath(Path path) {
        if (path == null)
            throw new NullPointerException();
        if (!(path instanceof SimplePath))
            throw new ProviderMismatchException();
        return (SimplePath) path;
    }

    @Override
    public boolean isAbsolute() {
        return (this.path.length > 0 && path[0] == '/');
    }

    @Override
    public Path getRoot() {
        if (this.isAbsolute())
            return new SimplePath(sfs, new byte[]{path[0]});
        else
            return null;
    }

    @Override
    public Path getFileName() {
        initOffsets();
        int count = offsets.length;
        if (count == 0)
            return null;  // no elements so no name
        if (count == 1 && path[0] != '/')
            return this;
        int lastOffset = offsets[count - 1];
        int len = path.length - lastOffset;
        byte[] result = new byte[len];
        System.arraycopy(path, lastOffset, result, 0, len);
        return new SimplePath(sfs, result);
    }

    @Override
    public Path getParent() {
        initOffsets();
        int count = offsets.length;
        if (count == 0)    // no elements so no parent
            return null;
        int len = offsets[count - 1] - 1;
        if (len <= 0)      // parent is root only (may be null)
            return getRoot();
        byte[] result = new byte[len];
        System.arraycopy(path, 0, result, 0, len);
        return new SimplePath(sfs, result);
    }

    @Override
    public int getNameCount() {
        initOffsets();
        return offsets.length;
    }

    @Override
    public Path getName(int index) {
        initOffsets();
        if (index < 0 || index >= offsets.length)
            throw new IllegalArgumentException();
        int begin = offsets[index];
        int len;
        if (index == (offsets.length - 1))
            len = path.length - begin;
        else
            len = offsets[index + 1] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new SimplePath(sfs, result);
    }

    @Override
    public Path subpath(int beginIndex, int endIndex) {
        initOffsets();
        if (beginIndex < 0 ||
                beginIndex >= offsets.length ||
                endIndex > offsets.length ||
                beginIndex >= endIndex)
            throw new IllegalArgumentException();

        // starting offset and length
        int begin = offsets[beginIndex];
        int len;
        if (endIndex == offsets.length)
            len = path.length - begin;
        else
            len = offsets[endIndex] - begin - 1;
        // construct result
        byte[] result = new byte[len];
        System.arraycopy(path, begin, result, 0, len);
        return new SimplePath(sfs, result);
    }

    @Override
    public boolean startsWith(Path other) {
        final SimplePath o = checkPath(other);
        if (o.isAbsolute() != this.isAbsolute() ||
                o.path.length > this.path.length)
            return false;
        int olast = o.path.length;
        for (int i = 0; i < olast; i++) {
            if (o.path[i] != this.path[i])
                return false;
        }
        olast--;
        return o.path.length == this.path.length ||
                o.path[olast] == '/' ||
                this.path[olast + 1] == '/';
    }

    @Override
    public boolean startsWith(String other) {
        return startsWith(getFileSystem().getPath(other));
    }

    @Override
    public boolean endsWith(Path other) {
        final SimplePath o = checkPath(other);
        int olast = o.path.length - 1;
        if (olast > 0 && o.path[olast] == '/')
            olast--;
        int last = this.path.length - 1;
        if (last > 0 && this.path[last] == '/')
            last--;
        if (olast == -1)    // o.path.length == 0
            return last == -1;
        if ((o.isAbsolute() && (!this.isAbsolute() || olast != last)) ||
                (last < olast))
            return false;
        for (; olast >= 0; olast--, last--) {
            if (o.path[olast] != this.path[last])
                return false;
        }
        return o.path[olast + 1] == '/' ||
                last == -1 || this.path[last] == '/';
    }

    @Override
    public boolean endsWith(String other) {
        return endsWith(getFileSystem().getPath(other));
    }

    @Override
    public Path normalize() {
        byte[] resolved = getResolved();
        if (resolved == path)    // no change
            return this;
        return new SimplePath(sfs, resolved, true);
    }

    @Override
    public Path resolve(Path other) {
        final SimplePath o = checkPath(other);
        if (o.isAbsolute())
            return o;
        byte[] resolved = null;
        if (this.path[path.length - 1] == '/') {
            resolved = new byte[path.length + o.path.length];
            System.arraycopy(path, 0, resolved, 0, path.length);
            System.arraycopy(o.path, 0, resolved, path.length, o.path.length);
        } else {
            resolved = new byte[path.length + 1 + o.path.length];
            System.arraycopy(path, 0, resolved, 0, path.length);
            resolved[path.length] = '/';
            System.arraycopy(o.path, 0, resolved, path.length + 1, o.path.length);
        }
        return new SimplePath(sfs, resolved);
    }

    @Override
    public Path resolve(String other) {
        return resolve(getFileSystem().getPath(other));
    }

    @Override
    public Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }

    @Override
    public Path resolveSibling(String other) {
        return resolveSibling(getFileSystem().getPath(other));
    }

    @Override
    public Path relativize(Path other) {
        final SimplePath o = checkPath(other);
        if (o.equals(this))
            return new SimplePath(getFileSystem(), new byte[0], true);
        if (/* this.getFileSystem() != o.getFileSystem() || */
                this.isAbsolute() != o.isAbsolute()) {
            throw new IllegalArgumentException();
        }
        int mc = this.getNameCount();
        int oc = o.getNameCount();
        int n = Math.min(mc, oc);
        int i = 0;
        while (i < n) {
            if (!equalsNameAt(o, i))
                break;
            i++;
        }
        int dotdots = mc - i;
        int len = dotdots * 3 - 1;
        if (i < oc)
            len += (o.path.length - o.offsets[i] + 1);
        byte[] result = new byte[len];

        int pos = 0;
        while (dotdots > 0) {
            result[pos++] = (byte) '.';
            result[pos++] = (byte) '.';
            if (pos < len)       // no tailing slash at the end
                result[pos++] = (byte) '/';
            dotdots--;
        }
        if (i < oc)
            System.arraycopy(o.path, o.offsets[i],
                    result, pos,
                    o.path.length - o.offsets[i]);
        return new SimplePath(getFileSystem(), result);
    }

    @Override
    public URI toUri() {
        try {
            return new URI("simple",
                    sfs.getSimpleFile().toUri() +
                            "!" +
                            sfs.getString(toAbsolutePath().path),
                    null);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    public SimplePath toAbsolutePath() {
        if (isAbsolute()) {
            return this;
        } else {
            //add / bofore the existing path
            byte[] defaultdir = sfs.getDefaultDir().path;
            int defaultlen = defaultdir.length;
            boolean endsWith = (defaultdir[defaultlen - 1] == '/');
            byte[] t = null;
            if (endsWith)
                t = new byte[defaultlen + path.length];
            else
                t = new byte[defaultlen + 1 + path.length];
            System.arraycopy(defaultdir, 0, t, 0, defaultlen);
            if (!endsWith)
                t[defaultlen++] = '/';
            System.arraycopy(path, 0, t, defaultlen, path.length);
            return new SimplePath(sfs, t, true);  // normalized
        }
    }

    private boolean equalsNameAt(SimplePath other, int index) {
        int mbegin = offsets[index];
        int mlen = 0;
        if (index == (offsets.length - 1))
            mlen = path.length - mbegin;
        else
            mlen = offsets[index + 1] - mbegin - 1;
        int obegin = other.offsets[index];
        int olen = 0;
        if (index == (other.offsets.length - 1))
            olen = other.path.length - obegin;
        else
            olen = other.offsets[index + 1] - obegin - 1;
        if (mlen != olen)
            return false;
        int n = 0;
        while (n < mlen) {
            if (path[mbegin + n] != other.path[obegin + n])
                return false;
            n++;
        }
        return true;
    }

    @Override
    public Path toRealPath(LinkOption... options) throws IOException {
        SimplePath realPath = new SimplePath(sfs, getResolvedPath()).toAbsolutePath();
        realPath.checkAccess();
        return realPath;
    }

    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
        return register(watcher, events, new WatchEvent.Modifier[0]);
    }

    @Override
    public Iterator<Path> iterator() {
        return new Iterator<Path>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }

            @Override
            public void remove() {
                throw new ReadOnlyFileSystemException();
            }
        };
    }

    @Override
    public int compareTo(Path other) {
        final SimplePath o = checkPath(other);
        int len1 = this.path.length;
        int len2 = o.path.length;

        int n = Math.min(len1, len2);
        byte v1[] = this.path;
        byte v2[] = o.path;

        int k = 0;
        while (k < n) {
            int c1 = v1[k] & 0xff;
            int c2 = v2[k] & 0xff;
            if (c1 != c2)
                return c1 - c2;
            k++;
        }
        return len1 - len2;
    }

    public void checkAccess(AccessMode... modes) throws IOException {
        boolean w = false;
        boolean x = false;
        for (AccessMode mode : modes) {
            switch (mode) {
                case READ:
                    break;
                case WRITE:
                    w = true;
                    break;
                case EXECUTE:
                    x = true;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }
        SimpleFileAttributes attrs = sfs.getFileAttributes(getResolvedPath());
        if (attrs == null && (path.length != 1 || path[0] != '/'))
            throw new NoSuchFileException(toString());
        if (w) {
            if (sfs.isReadOnly())
                throw new AccessDeniedException(toString());
        }
        if (x)
            throw new AccessDeniedException(toString());
    }

    public void copy(SimplePath target, CopyOption... options) throws IOException {
        if (Files.isSameFile(this.sfs.getSimpleFile(), target.sfs.getSimpleFile()))
            sfs.copyFile(false,
                    getResolvedPath(), target.getResolvedPath(),
                    options);
        else
            copyToTarget(target, options);
    }

    private void copyToTarget(SimplePath target, CopyOption... options)
            throws IOException {
        boolean replaceExisting = false;
        boolean copyAttrs = false;
        for (CopyOption opt : options) {
            if (opt == REPLACE_EXISTING)
                replaceExisting = true;
            else if (opt == COPY_ATTRIBUTES)
                copyAttrs = true;
        }
        // attributes of source file
        SimpleFileAttributes sfas = getAttributes();
        // check if target exists
        boolean exists;
        if (replaceExisting) {
            try {
                target.deleteIfExists();
                exists = false;
            } catch (DirectoryNotEmptyException x) {
                exists = true;
            }
        } else {
            exists = target.exists();
        }
        if (exists)
            throw new FileAlreadyExistsException(target.toString());

        if (sfas.isDirectory()) {
            // create directory or file
            target.createDirectory();
        } else {
            InputStream is = sfs.newInputStream(getResolvedPath());
            try {
                OutputStream os = target.newOutputStream();
                try {
                    byte[] buf = new byte[8192];
                    int n = 0;
                    while ((n = is.read(buf)) != -1) {
                        os.write(buf, 0, n);
                    }
                } finally {
                    os.close();
                }
            } finally {
                is.close();
            }
        }
        if (copyAttrs) {
            BasicFileAttributeView view =
                    SimpleFileAttributeView.get(target, BasicFileAttributeView.class);
            try {
                view.setTimes(sfas.lastModifiedTime(),
                        sfas.lastAccessTime(),
                        sfas.creationTime());
            } catch (IOException x) {
                // rollback?
                try {
                    target.delete();
                } catch (IOException ignore) {
                }
                throw x;
            }
        }
    }

    void deleteIfExists() throws IOException {
        sfs.deleteFile(getResolvedPath(), false);
    }

    boolean exists() {
        if (path.length == 1 && path[0] == '/')
            return true;
        try {
            return sfs.exists(getResolvedPath());
        } catch (IOException x) {
        }
        return false;
    }

    void createDirectory(FileAttribute<?>... attrs)
            throws IOException {
        sfs.createDirectory(getResolvedPath(), attrs);
    }


    void delete() throws IOException {
        sfs.deleteFile(getResolvedPath(), true);
    }

    FileStore getFileStore() throws IOException {
        // each ZipFileSystem only has one root (as requested for now)
        if (exists())
            return sfs.getFileStore(this);
        throw new NoSuchFileException(sfs.getString(path));
    }

    public boolean isHidden() {
        return false;
    }

    boolean isSameFile(Path other) throws IOException {
        if (this.equals(other))
            return true;
        if (other == null ||
                this.getFileSystem() != other.getFileSystem())
            return false;
        this.checkAccess();
        ((SimplePath) other).checkAccess();
        return Arrays.equals(this.getResolvedPath(),
                ((SimplePath) other).getResolvedPath());
    }


    void move(SimplePath target, CopyOption... options)
            throws IOException {
        if (Files.isSameFile(this.sfs.getSimpleFile(), target.sfs.getSimpleFile())) {
            sfs.copyFile(true,
                    getResolvedPath(), target.getResolvedPath(),
                    options);
        } else {
            copyToTarget(target, options);
            delete();
        }
    }

    SeekableByteChannel newByteChannel(Set<? extends OpenOption> options,
                                       FileAttribute<?>... attrs)
            throws IOException {
        return sfs.newByteChannel(getResolvedPath(), options, attrs);
    }

    DirectoryStream<Path> newDirectoryStream(DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        return new SimpleDirectoryStream(this, filter);
    }

    FileChannel newFileChannel(Set<? extends OpenOption> options,
                               FileAttribute<?>... attrs)
            throws IOException {
        return sfs.newFileChannel(getResolvedPath(), options, attrs);
    }

    InputStream newInputStream(OpenOption... options) throws IOException {
        if (options.length > 0) {
            for (OpenOption opt : options) {
                if (opt != READ)
                    throw new UnsupportedOperationException("'" + opt + "' not allowed");
            }
        }
        return sfs.newInputStream(getResolvedPath());
    }

    OutputStream newOutputStream(OpenOption... options) throws IOException {
        if (options.length == 0)
            return sfs.newOutputStream(getResolvedPath(),
                    CREATE_NEW, WRITE);
        return sfs.newOutputStream(getResolvedPath(), options);
    }

    public SimpleFileAttributes getAttributes() throws IOException {
        SimpleFileAttributes zfas = sfs.getFileAttributes(getResolvedPath());
        if (zfas == null)
            throw new NoSuchFileException(toString());
        return zfas;
    }

    Map<String, Object> readAttributes(String attributes, LinkOption... options)
            throws IOException {
        String view = null;
        String attrs = null;
        int colonPos = attributes.indexOf(':');
        if (colonPos == -1) {
            view = "basic";
            attrs = attributes;
        } else {
            view = attributes.substring(0, colonPos++);
            attrs = attributes.substring(colonPos);
        }
        SimpleFileAttributeView sfv = SimpleFileAttributeView.get(this, view);
        if (sfv == null) {
            throw new UnsupportedOperationException("view not supported");
        }
        return sfv.readAttributes(attrs);
    }

    void setAttribute(String attribute, Object value, LinkOption... options)
            throws IOException {
        String type = null;
        String attr = null;
        int colonPos = attribute.indexOf(':');
        if (colonPos == -1) {
            type = "basic";
            attr = attribute;
        } else {
            type = attribute.substring(0, colonPos++);
            attr = attribute.substring(colonPos);
        }
        SimpleFileAttributeView view = SimpleFileAttributeView.get(this, type);
        if (view == null)
            throw new UnsupportedOperationException("view <" + view + "> is not supported");
        view.setAttribute(attr, value);
    }
}
