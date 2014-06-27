package fileSystem.extended;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/17/14
 * Time: 6:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleCoder {
    public String toString(byte[] ba, int length) {
        CharsetDecoder cd = decoder().reset();
        int len = (int) (length * cd.maxCharsPerByte());
        char[] ca = new char[len];
        if (len == 0)
            return new String(ca);
        ByteBuffer bb = ByteBuffer.wrap(ba, 0, length);
        CharBuffer cb = CharBuffer.wrap(ca);
        CoderResult cr = cd.decode(bb, cb, true);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        cr = cd.flush(cb);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        return new String(ca, 0, cb.position());
    }

    public String toString(byte[] ba) {
        return toString(ba, ba.length);
    }

    public byte[] getBytes(String s) {
        CharsetEncoder ce = encoder().reset();
        char[] ca = s.toCharArray();
        int len = (int) (ca.length * ce.maxBytesPerChar());
        byte[] ba = new byte[len];
        if (len == 0)
            return ba;
        ByteBuffer bb = ByteBuffer.wrap(ba);
        CharBuffer cb = CharBuffer.wrap(ca);
        CoderResult cr = ce.encode(cb, bb, true);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        cr = ce.flush(bb);
        if (!cr.isUnderflow())
            throw new IllegalArgumentException(cr.toString());
        if (bb.position() == ba.length)  // defensive copy?
            return ba;
        else
            return Arrays.copyOf(ba, bb.position());
    }

    // assume invoked only if "this" is not utf8
    byte[] getBytesUTF8(String s) {
        if (isutf8)
            return getBytes(s);
        if (utf8 == null)
            utf8 = new SimpleCoder(Charset.forName("UTF-8"));
        return utf8.getBytes(s);
    }

    String toStringUTF8(byte[] ba, int len) {
        if (isutf8)
            return toString(ba, len);
        if (utf8 == null)
            utf8 = new SimpleCoder(Charset.forName("UTF-8"));
        return utf8.toString(ba, len);
    }

    boolean isUTF8() {
        return isutf8;
    }

    private Charset cs;
    private boolean isutf8;
    private SimpleCoder utf8;

    private SimpleCoder(Charset cs) {
        this.cs = cs;
        this.isutf8 = cs.name().equals("UTF-8");
    }

    public static SimpleCoder get(Charset charset) {
        return new SimpleCoder(charset);
    }

    static SimpleCoder get(String csn) {
        try {
            return new SimpleCoder(Charset.forName(csn));
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return new SimpleCoder(Charset.defaultCharset());
    }

    private final ThreadLocal<CharsetDecoder> decTL = new ThreadLocal<>();
    private final ThreadLocal<CharsetEncoder> encTL = new ThreadLocal<>();

    private CharsetDecoder decoder() {
        CharsetDecoder dec = decTL.get();
        if (dec == null) {
            dec = cs.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            decTL.set(dec);
        }
        return dec;
    }

    private CharsetEncoder encoder() {
        CharsetEncoder enc = encTL.get();
        if (enc == null) {
            enc = cs.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            encTL.set(enc);
        }
        return enc;
    }
}
