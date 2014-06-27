package fileSystem.core.structure.base;

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
import static org.junit.Assert.*;

public class BaseStructureReadWriterTest {
    String filePath = "fileSystem.simple";
    BaseStructureReadWriter readWriter;

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
        readWriter = new BaseStructureReadWriter(channel);
    }

    @After
    public void tearDown() throws Exception {
        readWriter.close();
        Files.delete(Paths.get(filePath));
    }

    @Test
    public void allocateNewBlock_firstBlock_allocated() throws Exception {
        //arrange

        //act
        SimpleBlock block = readWriter.allocateNewBlock();

        //assert
        assertEquals(-1, block.getNextDataBlock());
        assertNotSame(-1, block.getCurPos());
    }

    @Test
    public void readBlock_allocatedBlock_read() throws Exception {
        //arrange
        SimpleBlock block = readWriter.allocateNewBlock();

        //act
        SimpleBlock blockRead = readWriter.readBlock(block.getCurPos());

        //assert
        assertEquals(block.getNextDataBlock(), blockRead.getNextDataBlock());
        assertEquals(block.getCurPos(), blockRead.getCurPos());
    }

    @Test
    public void writeBlock_allocatedBlock_written() throws Exception {
        //arrange
        SimpleBlock block = readWriter.allocateNewBlock();
        Random random = new Random();
        random.nextBytes(block.getData());
        block.setNextDataBlock(random.nextLong());
        block.setSize(random.nextInt());

        //act
        readWriter.writeBlock(block);

        //assert
        SimpleBlock blockRead = readWriter.readBlock(block.getCurPos());
        assertEquals(block.getNextDataBlock(), blockRead.getNextDataBlock());
        assertEquals(block.getCurPos(), blockRead.getCurPos());
        assertEquals(block.getSize(), blockRead.getSize());
        assertArrayEquals(block.getData(), blockRead.getData());
    }

    @Test
    public void allocateNewINode_firstINode_allocated() throws Exception {
        //arrange

        //act
        SimpleINode iNode = readWriter.allocateNewINode();

        //assert
        assertEquals(-1, iNode.getFirstDataBlock());
        assertEquals(-1, iNode.getLastDataBlock());
        assertEquals(-1, iNode.getNextFreeINode());
        assertNotSame(-1, iNode.getCurPos());
    }

    @Test
    public void readINode_allocatedINode_read() throws Exception {
        //arrange
        SimpleINode iNode = readWriter.allocateNewINode();

        //act
        SimpleINode iNodeRead = readWriter.readINode(iNode.getCurPos());

        //assert
        assertEquals(iNode.getFirstDataBlock(), iNodeRead.getFirstDataBlock());
        assertEquals(iNode.getLastDataBlock(), iNodeRead.getLastDataBlock());
        assertEquals(iNode.getNextFreeINode(), iNodeRead.getNextFreeINode());
        assertEquals(iNode.getCurPos(), iNodeRead.getCurPos());
    }

    @Test
    public void writeINode_allocatedINode_written() throws Exception {
        //arrange
        SimpleINode iNode = readWriter.allocateNewINode();
        Random random = new Random();
        iNode.setFirstDataBlock(random.nextLong());
        iNode.setLastDataBlock(random.nextLong());
        iNode.setNextFreeINode(random.nextLong());

        //act
        readWriter.writeINode(iNode);

        //assert
        SimpleINode iNodeRead = readWriter.readINode(iNode.getCurPos());
        assertEquals(iNode.getCurPos(), iNodeRead.getCurPos());
        assertEquals(iNode.getNextFreeINode(), iNodeRead.getNextFreeINode());
        assertEquals(iNode.getLastDataBlock(), iNodeRead.getLastDataBlock());
        assertEquals(iNode.getFirstDataBlock(), iNodeRead.getFirstDataBlock());
    }

    @Test
    public void writeHeader_Default_written() throws Exception {
        //arrange
        Random random = new Random();
        SimpleHeader header = new SimpleHeader(random.nextLong(), random.nextLong(), random.nextLong());

        //act
        readWriter.writeHeader(header);

        //assert
        SimpleHeader headerRead = readWriter.readHeader();
        assertEquals(header.nextFreeBlock, headerRead.nextFreeBlock);
        assertEquals(header.nextFreeINode, headerRead.nextFreeINode);
        assertEquals(header.rootINode, headerRead.rootINode);
    }

    @Test
    public void concurrentTest() throws Exception {
        //arrange
        final boolean[] error = {false};
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 10;
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        writeBlock_allocatedBlock_written();
                    } catch (Throwable ex) {
                        error[0] = true;
                    }
                }
            });
            Thread t2 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        writeINode_allocatedINode_written();
                    } catch (Throwable e) {
                        error[0] = true;
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
        assertEquals(false, error[0]);
    }
}
