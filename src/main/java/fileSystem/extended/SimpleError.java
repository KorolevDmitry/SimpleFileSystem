package fileSystem.extended;

/**
 * Created with IntelliJ IDEA.
 * User: deemo_000
 * Date: 6/16/14
 * Time: 4:53 PM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleError extends InternalError {
    private static final long serialVersionUID = 8539734222861979L;

    /**
     * Constructs a SimpleError with the given detail message.
     *
     * @param s the {@code String} containing a detail message
     */
    public SimpleError(String s) {
        super(s);
    }
}
