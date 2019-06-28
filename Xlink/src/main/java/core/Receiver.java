package core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void setReceiveListener(IOParameter.IOParaEventListener listener);

    boolean receiveAsync(IOParameter parameter) throws IOException;
}
