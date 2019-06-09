package xlink.core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    boolean receiveAsync(IOParameter.IOParaEventListener listener) throws IOException;
}
