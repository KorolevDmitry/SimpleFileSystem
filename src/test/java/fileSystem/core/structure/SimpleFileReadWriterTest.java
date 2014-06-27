package fileSystem.core.structure;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static java.nio.file.StandardOpenOption.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;

public class SimpleFileReadWriterTest {

    String filePath = "fileSystem.simple";
    SimpleFileReadWriter simpleFileReadWriter;

    @Before
    public void setUp() throws Exception {
        filePath += new Random().nextInt();
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
        simpleFileReadWriter = new SimpleFileReadWriter(channel);
    }

    @After
    public void tearDown() throws Exception {
        simpleFileReadWriter.close();
        Files.delete(Paths.get(filePath));
    }

    @Test
    public void getFile_RootDirectory_Exists() throws Exception {
        //arrange

        //act
        SimpleFile file = simpleFileReadWriter.getFile(new String[0]);

        //assert
        assertNotNull(file);
        assertTrue(file instanceof SimpleDirectory);
    }

    @Test
    public void readData_RootDirectory_Exists() throws Exception {
        //arrange
        SimpleFile file = simpleFileReadWriter.getFile(new String[0]);

        //act
        byte[] bytes = simpleFileReadWriter.readData(file);

        //assert
        assertNotNull(bytes);
    }

    @Test
    public void readDirectoryData_RootDirectory_Empty() throws Exception {
        //arrange
        SimpleFile file = simpleFileReadWriter.getFile(new String[0]);

        //act
        ArrayList<SimpleFile> files = simpleFileReadWriter.readDirectoryData((SimpleDirectory) file);

        //assert
        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void createFile_Directory_Created() throws Exception {
        //arrange
        SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        String[] path = new String[]{"test"};

        //act
        SimpleFile createdFile = simpleFileReadWriter.createFile(path, true);

        //assert
        assertNotNull(createdFile);
        assertTrue(createdFile instanceof SimpleDirectory);
        assertEquals(path[0], createdFile.getName());
        assertEquals(1, simpleFileReadWriter.readDirectoryData(root).size());
        assertEquals(createdFile.getName(), simpleFileReadWriter.readDirectoryData(root).get(0).getName());
    }

    @Test
    public void createFile_File_Created() throws Exception {
        //arrange
        SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        String[] path = new String[]{"test"};

        //act
        SimpleFile createdFile = simpleFileReadWriter.createFile(path, false);

        //assert
        assertNotNull(createdFile);
        assertFalse(createdFile instanceof SimpleDirectory);
        assertEquals(path[0], createdFile.getName());
        assertEquals(1, simpleFileReadWriter.readDirectoryData(root).size());
        assertEquals(createdFile.getName(), simpleFileReadWriter.readDirectoryData(root).get(0).getName());
    }

    @Test
    public void writeData_File_Written() throws Exception {
        //arrange
        SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        String[] path = new String[]{"test"};
        SimpleFile createdFile = simpleFileReadWriter.createFile(path, false);
        Random random = new Random();
        byte[] data = new byte[8046];
        random.nextBytes(data);

        //act
        simpleFileReadWriter.writeData(createdFile, data);

        //assert
        assertArrayEquals(data, simpleFileReadWriter.readData(createdFile));
    }

    @Test
    public void getFile_FileExists_Gotten() throws Exception {
        //arrange
        SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        String[] path1 = new String[]{"test1"};
        String[] path2 = new String[]{"test1", "test2"};
        String[] path3 = new String[]{"test1", "test2", "test3"};
        SimpleDirectory file1 = (SimpleDirectory) simpleFileReadWriter.createFile(path1, true);
        SimpleDirectory file2 = (SimpleDirectory) simpleFileReadWriter.createFile(path2, true);
        SimpleDirectory file3 = (SimpleDirectory) simpleFileReadWriter.createFile(path3, true);

        //act
        SimpleFile file = simpleFileReadWriter.getFile(path3);

        //assert
        assertNotNull(file);
        assertTrue(file instanceof SimpleDirectory);
    }

    @Test
    public void deleteFile_SingleFile_Deleted() throws Exception {
        //arrange
        SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        String[] path1 = new String[]{"test1"};
        String[] path2 = new String[]{"test1", "test2"};
        String[] path3 = new String[]{"test1", "test2", "test3"};
        SimpleDirectory file1 = (SimpleDirectory) simpleFileReadWriter.createFile(path1, true);
        SimpleDirectory file2 = (SimpleDirectory) simpleFileReadWriter.createFile(path2, true);
        SimpleFile file3 = simpleFileReadWriter.createFile(path3, false);

        //act
        simpleFileReadWriter.deleteFile(file3);

        //assert
        SimpleFile file = simpleFileReadWriter.getFile(path3);
        assertNull(file);
        assertEquals(0, simpleFileReadWriter.readDirectoryData(file2).size());
    }

    @Test
    public void concurrentTest() throws Exception {
        //arrange
        final SimpleDirectory root = (SimpleDirectory) simpleFileReadWriter.getFile(new String[0]);
        final Throwable[] error = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final String[] fileName = new String[]{"file" + i};
            final String[] dirName = new String[]{"dir" + i};
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        simpleFileReadWriter.createFile(fileName, false);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        error[0] = e;
                    }
                }
            });
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        simpleFileReadWriter.createFile(dirName, true);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        error[0] = e;
                    }
                }
            });
            threads.add(t1);
            threads.add(t2);
        }

        //act
        for (Thread t : threads) {
            t.start();
        }
        latch.countDown();
        for (Thread t : threads) {
            t.join();
        }

        //assert
        assertNull(error[0]);
        assertEquals(threads.size(), simpleFileReadWriter.readDirectoryData(root).size());
    }

}
