package fileSystem.extended;

import fileSystem.core.structure.SimpleDirectory;
import fileSystem.core.structure.SimpleFile;
import fileSystem.core.structure.SimpleFileReadWriter;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;

import static java.nio.file.StandardOpenOption.*;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/16/14
 * Time: 2:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleFileSystem extends java.nio.file.FileSystem {
    private final SimpleFileSystemProvider provider;
    private final SimplePath defaultdir;
    private boolean readOnly = false;
    private final Path sfpath;
    private final SimpleCoder sc;

    private static final boolean isWindows =
            System.getProperty("os.name").startsWith("Windows");

    private volatile boolean isOpen = true;
    //private final SeekableByteChannel ch; // channel to the simpleFile
    private final SimpleFileReadWriter readWriter;

    public SimpleFileSystem(SimpleFileSystemProvider simpleFileSystemProvider, Path path, Map<String, ?> env)
            throws IOException {
        boolean createNew = true;
        String nameEncoding = "UTF-8";
        String defaultDir = "/";

        this.provider = simpleFileSystemProvider;
        this.sfpath = path;
        if (Files.notExists(sfpath)) {
            if (createNew) {
                try (OutputStream os = Files.newOutputStream(sfpath, CREATE_NEW, WRITE)) {
                }
            } else {
                throw new FileSystemNotFoundException(sfpath.toString());
            }
        }
        // sm and existence check
        sfpath.getFileSystem().provider().checkAccess(sfpath, AccessMode.READ, AccessMode.WRITE);
        if (!Files.isWritable(sfpath))
            this.readOnly = true;
        this.sc = SimpleCoder.get(nameEncoding);
        this.defaultdir = new SimplePath(this, getBytes(defaultDir));
        //this.ch = Files.newByteChannel(sfpath, READ, WRITE);
        SeekableByteChannel channel = Files.newByteChannel(sfpath, WRITE, READ);
        readWriter = new SimpleFileReadWriter(channel);
        //this.cen = initCEN();
    }

    @Override
    public FileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        readWriter.close();
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public String getSeparator() {
        return "/";
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        ArrayList<Path> roots = new ArrayList<>();
        roots.add(defaultdir);
        return roots;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        ArrayList<FileStore> list = new ArrayList<>(1);
        list.add(new SimpleFileStore(new SimplePath(this, new byte[]{'/'})));
        return list;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return Collections.unmodifiableSet(
                new HashSet(Arrays.asList("basic", "simple")));
    }

    @Override
    public Path getPath(String first, String... more) {
        String path;
        if (more.length == 0) {
            path = first;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(first);
            for (String segment : more) {
                if (segment.length() > 0) {
                    if (sb.length() > 0)
                        sb.append('/');
                    sb.append(segment);
                }
            }
            path = sb.toString();
        }
        return new SimplePath(this, getBytes(path));
    }

    private static final String GLOB_SYNTAX = "glob";
    private static final String REGEX_SYNTAX = "regex";

    @Override
    public PathMatcher getPathMatcher(String syntaxAndInput) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }


    final byte[] getBytes(String name) {
        return sc.getBytes(name);
    }

    final String getString(byte[] name) {
        return sc.toString(name);
    }

    SimplePath getDefaultDir() {  // package private
        return defaultdir;
    }

    Path getSimpleFile() {
        return sfpath;
    }

    private void checkWritable() throws IOException {
        if (readOnly)
            throw new ReadOnlyFileSystemException();
    }

    public SimpleFileAttributes getFileAttributes(byte[] resolvedPath) throws IOException {
        SimpleFile file = readWriter.getFile(getPath(resolvedPath));
        if (file == null)
            return null;
        return new SimpleFileAttributes(file);
    }

    private String[] getPath(byte[] resolvedPath) {
        return getString(resolvedPath).split(getSeparator());
    }

    private String[] getParentPath(byte[] resolvedPath) {
        String[] path = getPath(resolvedPath);
        if (path.length == 0)
            return null;
        String[] parentPath = new String[path.length - 1];
        System.arraycopy(path, 0, parentPath, 0, path.length - 1);
        return parentPath;
    }

    public void copyFile(boolean b, byte[] src, byte[] dst, CopyOption[] options) throws IOException {
        checkWritable();
        if (Arrays.equals(src, dst))
            return;    // do nothing, src and dst are the same
        SimpleFile srcFile = readWriter.getFile(getPath(src));
        SimpleFile dstFile = readWriter.getFile(getPath(dst));
        if (dstFile == null) {
            dstFile = readWriter.createFile(getPath(dst), false);
        }
        dstFile.writeData(srcFile.readData());
    }

    public void deleteFile(byte[] resolvedPath, boolean failIfNotExists) throws IOException {
        SimpleFile file = readWriter.getFile(getPath(resolvedPath));
        if (file == null && failIfNotExists) {
            throw new NoSuchFileException(getString(resolvedPath));
        }
        if (file != null) {
            file.delete();
        }
    }

    public boolean exists(byte[] resolvedPath) throws IOException {
        SimpleFile file = readWriter.getFile(getPath(resolvedPath));
        return file != null;
    }

    public InputStream newInputStream(byte[] resolvedPath) throws IOException {
        return new ByteArrayInputStream(readWriter.getFile(getPath(resolvedPath)).readData());
    }

    public void createDirectory(byte[] resolvedPath, FileAttribute<?>[] attrs) throws IOException {
        SimpleDirectory dir = (SimpleDirectory) readWriter.getFile(getParentPath(resolvedPath));
        readWriter.createFile(getPath(resolvedPath), true);
    }

    public FileStore getFileStore(SimplePath path) {
        return new SimpleFileStore(path);
    }

    private void checkOptions(Set<? extends OpenOption> options) {
        // check for options of null type and option is an intance of StandardOpenOption
        for (OpenOption option : options) {
            if (option == null)
                throw new NullPointerException();
            if (!(option instanceof StandardOpenOption))
                throw new IllegalArgumentException();
        }
    }

    public SeekableByteChannel newByteChannel(byte[] resolvedPath, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) throws IOException {
        checkOptions(options);
        if (options.contains(StandardOpenOption.WRITE) ||
                options.contains(StandardOpenOption.APPEND)) {
            checkWritable();
            final WritableByteChannel wbc = Channels.newChannel(
                    newOutputStream(resolvedPath, options.toArray(new OpenOption[0])));
            long leftover = 0;
            if (options.contains(StandardOpenOption.APPEND)) {
                leftover = readWriter.getFile(getPath(resolvedPath)).getTotalSize();
            }
            final long offset = leftover;
            return new SeekableByteChannel() {
                long written = offset;

                public boolean isOpen() {
                    return wbc.isOpen();
                }

                public long position() throws IOException {
                    return written;
                }

                public SeekableByteChannel position(long pos)
                        throws IOException {
                    throw new UnsupportedOperationException();
                }

                public int read(ByteBuffer dst) throws IOException {
                    throw new UnsupportedOperationException();
                }

                public SeekableByteChannel truncate(long size)
                        throws IOException {
                    throw new UnsupportedOperationException();
                }

                public int write(ByteBuffer src) throws IOException {
                    int n = wbc.write(src);
                    written += n;
                    return n;
                }

                public long size() throws IOException {
                    return written;
                }

                public void close() throws IOException {
                    wbc.close();
                }
            };
        } else {
            SimpleFile file = readWriter.getFile(getPath(resolvedPath));
            if (file == null || file instanceof SimpleDirectory)
                throw new NoSuchFileException(getString(resolvedPath));
            final ReadableByteChannel rbc =
                    Channels.newChannel(newInputStream(resolvedPath));
            final long size = file.getTotalSize();
            return new SeekableByteChannel() {
                long read = 0;

                public boolean isOpen() {
                    return rbc.isOpen();
                }

                public long position() throws IOException {
                    return read;
                }

                public SeekableByteChannel position(long pos)
                        throws IOException {
                    throw new UnsupportedOperationException();
                }

                public int read(ByteBuffer dst) throws IOException {
                    int n = rbc.read(dst);
                    if (n > 0) {
                        read += n;
                    }
                    return n;
                }

                public SeekableByteChannel truncate(long size)
                        throws IOException {
                    throw new NonWritableChannelException();
                }

                public int write(ByteBuffer src) throws IOException {
                    throw new NonWritableChannelException();
                }

                public long size() throws IOException {
                    return size;
                }

                public void close() throws IOException {
                    rbc.close();
                }
            };
        }
    }

    public FileChannel newFileChannel(byte[] resolvedPath, Set<? extends OpenOption> options, FileAttribute<?>[] attrs) {
        throw new UnsupportedOperationException();
    }

    public OutputStream newOutputStream(byte[] resolvedPath, StandardOpenOption createNew, StandardOpenOption write) throws IOException {
        final SimpleFile file = readWriter.getFile(getPath(resolvedPath));
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                byte[] data = toByteArray();
                file.writeData(data);
                super.close();
            }

            @Override
            public void flush() throws IOException {
                byte[] data = toByteArray();
                file.writeData(data);
            }
        };
    }

    public OutputStream newOutputStream(byte[] resolvedPath, OpenOption[] options) throws IOException {
        String[] path = getPath(resolvedPath);
        SimpleFile fileExistence = readWriter.getFile(path);
        if (fileExistence == null) {
            fileExistence = readWriter.createFile(path, false);
        }
        final SimpleFile file = fileExistence;
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                byte[] data = toByteArray();
                file.writeData(data);
                super.close();
            }

            @Override
            public void flush() throws IOException {
                byte[] data = toByteArray();
                file.writeData(data);
            }
        };
    }
}
