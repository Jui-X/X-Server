package core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    boolean sendAsync(IOParameter parament, IOParameter.IOParaEventListener listener) throws IOException;
}
