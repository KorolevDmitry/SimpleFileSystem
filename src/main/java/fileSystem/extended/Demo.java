package fileSystem.extended;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/27/14
 * Time: 1:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class Demo {
    static final String fileSystemPath = "fileSystem.simple";

    static final String filePath = "text.txt";
    static final String filePath2 = "test2.txt";

    public static void main(String[] args) throws Throwable {
        FileSystemProvider provider = new SimpleFileSystemProvider();
        Map<String, ?> env = Collections.emptyMap();
        Path path = Paths.get(fileSystemPath);
        FileSystem fileSystem = provider.newFileSystem(path, env);

        //create new inner dir
        Path innerFolder = fileSystem.getPath("/test");
        Files.createDirectory(innerFolder);

        //create new inner file
        Path innerFile = fileSystem.getPath("/test/text.txt");
        Files.createFile(innerFile);

        //copy inner file
        Path innerFile2 = fileSystem.getPath("/text2.txt");
        Files.copy(innerFile, innerFile2);

        //copy outer file in
        Path outerFile = Paths.get(filePath2);
        Files.createFile(outerFile);
        Path innerFile3 = fileSystem.getPath("/text3.txt");
        Files.copy(outerFile, innerFile3);

        //copy inner file out
        Path outerFile2 = Paths.get(filePath);
        Files.copy(innerFile, outerFile2);

        //delete all
        Files.delete(outerFile2);
        Files.delete(outerFile);
        Files.delete(innerFile3);
        Files.delete(innerFile2);
        Files.delete(innerFile);
        Files.delete(innerFolder);
        Files.delete(path);
    }
}
