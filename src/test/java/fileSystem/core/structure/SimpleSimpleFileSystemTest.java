package fileSystem.core.structure;

import fileSystem.core.SimpleSimpleFileSystem;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.*;

public class SimpleSimpleFileSystemTest {
    SimpleSimpleFileSystem fileSystem;
    String filePath = "fileSystem.simple";

    @Before
    public void setUp() throws Exception {
        filePath += new Random().nextInt();
        fileSystem = new SimpleSimpleFileSystem(filePath);
    }

    @After
    public void tearDown() throws Exception {
        fileSystem.close();
        Files.delete(Paths.get(filePath));
    }

    @Test
    public void GetDirectory_RootNoChildren_Got() throws Exception {
        //arrange

        //act
        SimpleDirectory directory = fileSystem.getDirectory("/");

        //assert
        assertNotNull(directory);
        assertEquals(0, directory.getChildren().size());
    }

    @Test
    public void GetDirectory_ChildrenExists_Got() throws Exception {
        //arrange
        fileSystem.createDirectory("/test1");
        fileSystem.createDirectory("/test2");

        //act
        SimpleDirectory directory = fileSystem.getDirectory("/");

        //assert
        assertNotNull(directory);
        assertEquals(2, directory.getChildren().size());
    }

    @Test
    public void CreateDirectory_NotExists_Created() throws Exception {
        //arrange

        //act
        fileSystem.createDirectory("/test1");

        //assert
        SimpleDirectory parentDirectory = fileSystem.getDirectory("/");
        SimpleDirectory directory = fileSystem.getDirectory("/test1/");
        assertNotNull(parentDirectory);
        assertNotNull(directory);
        assertEquals(1, parentDirectory.getChildren().size());
        assertNotNull(parentDirectory.getChildren().get(0).getName(), "test1");
    }

    @Test
    public void DeleteDirectory_DirectoryExistsWithoutSubElements_Deleted() throws Exception {
        //arrange
        fileSystem.createDirectory("/test1");

        //act
        fileSystem.deleteDirectory("/test1");

        //assert
        SimpleDirectory parentDirectory = fileSystem.getDirectory("/");
        SimpleDirectory directory = fileSystem.getDirectory("/test1/");
        assertNotNull(parentDirectory);
        assertNull(directory);
        assertEquals(0, parentDirectory.getChildren().size());
    }

    @Test
    public void CreateFile_NotExists_Created() throws Exception {
        //arrange
        byte[] data = new byte[8888];

        //act
        SimpleFile file = fileSystem.createFile("/test1");
        file.writeData(data);

        //assert
        SimpleDirectory parentDirectory = fileSystem.getDirectory("/");
        file = fileSystem.getFile("/test1");
        assertNotNull(parentDirectory);
        assertNotNull(file);
        assertEquals(1, parentDirectory.getChildren().size());
        assertNotNull(parentDirectory.getChildren().get(0).getName(), "test1");
        assertArrayEquals(data, file.readData());
    }

    @Test
    public void DeleteFile_FileExists_Deleted() throws Exception {
        //arrange
        fileSystem.createFile("/test1");

        //act
        fileSystem.deleteFile("/test1");

        //assert
        SimpleDirectory parentDirectory = fileSystem.getDirectory("/");
        SimpleFile file = fileSystem.getFile("/test1");
        assertNotNull(parentDirectory);
        assertNull(file);
        assertEquals(0, parentDirectory.getChildren().size());
    }

    @Test
    public void ChangeFile_FileAlreadyWasChanged_Exception() throws Exception {
        //arrange
        byte[] data = new byte[8888];
        SimpleFile file = fileSystem.createFile("/test1");
        SimpleFile file1 = fileSystem.getFile("/test1");
        file1.writeData(data);
        boolean exc = false;

        //act
        try {
            file.writeData(data);
        } catch (IllegalStateException e) {
            exc = true;
        }

        //assert
        assertTrue(exc);
    }
}
