package fileSystem.core.structure;

import fileSystem.extended.SimpleCoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Methods for parsing byte data into directory format.
 */
public class DirectoryParser {
    private final SimpleCoder coder = SimpleCoder.get(Charset.forName("UTF-8"));

    public HashMap<String, Long> fromBytes(byte[] data) {
        HashMap<String, Long> children = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        int countOfElements = buffer.getInt();
        for (int i = 0; i < countOfElements; i++) {
            int nameLength = buffer.getInt();
            byte[] nameBytes = Arrays.copyOfRange(data, buffer.position(), buffer.position() + nameLength);
            String name = coder.toString(nameBytes);
            buffer.position(buffer.position() + nameLength);
            long value = buffer.getLong();
            children.put(name, value);
        }
        return children;
    }

    public byte[] toBytes(HashMap<String, Long> children) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ByteBuffer.allocate(4).putInt(children.size()).array());
        for (Map.Entry<String, Long> entry : children.entrySet()) {
            byte[] bytes = coder.getBytes(entry.getKey());
            outputStream.write(ByteBuffer.allocate(4).putInt(bytes.length).array());
            outputStream.write(bytes);
            outputStream.write(ByteBuffer.allocate(8).putLong(entry.getValue()).array());
        }

        return outputStream.toByteArray();
    }
}
