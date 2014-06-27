package fileSystem.core.structure.base;

/**
 * Some constants - should be configurable
 */
public class Constants {
    public static final int HEADER_POS = 0;
    public static final int HEADER_SIZE = 4 * 8; //in bytes

    public static final int BLOCK_SIZE = 1024; //in bytes
    public static final int BLOCK_DATA_SIZE = 512; //in bytes

    public static final int INODE_SIZE = 1024; //in bytes
}
