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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import static java.nio.file.StandardOpenOption.*;
import static junit.framework.Assert.*;

public class SpaceAllocatorTest {
    String filePath = "fileSystem.simple";
    BaseStructureReadWriter readWriter;
    SpaceAllocator spaceAllocator;

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
        spaceAllocator = new SpaceAllocator(readWriter, true);
    }

    @After
    public void tearDown() throws Exception {
        spaceAllocator.close();
        Files.delete(Paths.get(filePath));
    }

    @Test
    public void getRoot_Default_Got() throws Exception {
        //arrange

        //act
        SimpleINode root = spaceAllocator.getRoot();

        //assert
        assertNotNull(root);
    }

    @Test
    public void getFreeBlock_Default_Got() throws Exception {
        //arrange

        //act
        SimpleBlock block = spaceAllocator.getFreeBlock();

        //assert
        assertNotNull(block);
    }

    @Test
    public void getFreeINode_Default_Got() throws Exception {
        //arrange

        //act
        SimpleINode iNode = spaceAllocator.getFreeINode();

        //assert
        assertNotNull(iNode);
    }

    @Test
    public void markListOfBlocksAsFree_BlocksExists_Marked() throws Exception {
        //arrange
        SimpleBlock block1 = spaceAllocator.getFreeBlock();
        SimpleBlock block2 = spaceAllocator.getFreeBlock();
        SimpleBlock block3 = spaceAllocator.getFreeBlock();
        block1.setNextDataBlock(block2.getCurPos());
        block2.setNextDataBlock(block3.getCurPos());
        readWriter.writeBlock(block1);
        readWriter.writeBlock(block2);
        readWriter.writeBlock(block3);

        //act
        spaceAllocator.markListOfBlocksAsFree(block1.getCurPos(), block3.getCurPos());

        //assert
        SimpleBlock block1Read = spaceAllocator.getFreeBlock();
        SimpleBlock block2Read = spaceAllocator.getFreeBlock();
        SimpleBlock block3Read = spaceAllocator.getFreeBlock();
        assertEquals(block1.getCurPos(), block1Read.getCurPos());
        assertEquals(-1, block1Read.getNextDataBlock());
        assertEquals(block2.getCurPos(), block2Read.getCurPos());
        assertEquals(-1, block2Read.getNextDataBlock());
        assertEquals(block3.getCurPos(), block3Read.getCurPos());
        assertEquals(-1, block3Read.getNextDataBlock());
    }

    @Test
    public void markINodeAsFree_INodeExists_Marked() throws Exception {
        //arrange
        SimpleINode iNode = spaceAllocator.getFreeINode();

        //act
        spaceAllocator.markINodeAsFree(iNode.getCurPos());

        //assert
        SimpleINode iNodeRead = spaceAllocator.getFreeINode();
        assertEquals(iNode.getCurPos(), iNodeRead.getCurPos());
    }

    @Test
    public void concurrentTest() throws Exception {
        //arrange
        final Throwable[] error = {null};
        final CountDownLatch latch = new CountDownLatch(1);
        final int count = 100;
        final ConcurrentHashMap<SimpleINode, SimpleINode> iNodes = new ConcurrentHashMap<>();
        final ConcurrentHashMap<SimpleBlock, SimpleBlock> blocks = new ConcurrentHashMap<>();
        final ConcurrentHashMap<SimpleINode, SimpleINode> roots = new ConcurrentHashMap<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Thread t1 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        SimpleINode iNode = spaceAllocator.getFreeINode();
                        iNodes.put(iNode, iNode);
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
                        SimpleBlock block = spaceAllocator.getFreeBlock();
                        blocks.put(block, block);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        error[0] = e;
                    }
                }
            });
            Thread t3 = new Thread(new Runnable() {
                public void run() {
                    try {
                        latch.await();
                        SimpleINode iNode = spaceAllocator.getRoot();
                        roots.put(iNode, iNode);
                    } catch (Throwable e) {
                        e.printStackTrace();
                        error[0] = e;
                    }
                }
            });
            threads.add(t1);
            threads.add(t2);
            threads.add(t3);
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
        assertEquals(count, iNodes.size());
        assertEquals(count, blocks.size());
        assertEquals(1, roots.size());
    }
}
