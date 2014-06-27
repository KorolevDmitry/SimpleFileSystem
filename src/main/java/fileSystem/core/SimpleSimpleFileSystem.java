package fileSystem.core;

import fileSystem.core.structure.SimpleDirectory;
import fileSystem.core.structure.SimpleFile;
import fileSystem.core.structure.SimpleFileReadWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

/**
 * Simple interface for file system
 */
public class SimpleSimpleFileSystem implements AutoCloseable {
    private final SimpleFileReadWriter readWriter;

    public SimpleSimpleFileSystem(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        boolean createNew = true;

        if (Files.notExists(path)) {
            if (createNew) {
                try (OutputStream os = Files.newOutputStream(path, CREATE_NEW, WRITE)) {
                }
            } else {
                throw new FileSystemNotFoundException(path.toString());
            }
        }
        SeekableByteChannel channel = Files.newByteChannel(path, WRITE, READ);
        readWriter = new SimpleFileReadWriter(channel);
    }

    public SimpleDirectory getDirectory(String path) throws IOException {
        return (SimpleDirectory) readWriter.getFile(getPath(path));
    }

    public SimpleDirectory createDirectory(String path) throws IOException {
        return (SimpleDirectory) readWriter.createFile(getPath(path), true);
    }

    public void deleteDirectory(String path) throws IOException {
        readWriter.deleteFile(readWriter.getFile(getPath(path)));
    }

    public SimpleFile createFile(String path) throws IOException {
        return readWriter.createFile(getPath(path), false);
    }

    public SimpleFile getFile(String path) throws IOException {
        return readWriter.getFile(getPath(path));
    }

    public void deleteFile(String path) throws IOException {
        readWriter.deleteFile(readWriter.getFile(getPath(path)));
    }

    private String[] getPath(String path) {
        if (path == null)
            throw new IllegalArgumentException("Path can not be null");
        if (!path.startsWith("/"))
            throw new IllegalStateException("Path should be absolute.");
        if ("/".equals(path)) {
            return new String[0];
        }
        return path.substring(1).split("/");
    }

    @Override
    public void close() throws Exception {
        readWriter.close();
    }
}
