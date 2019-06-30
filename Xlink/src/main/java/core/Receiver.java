package core;

import java.io.Closeable;
import java.io.IOException;

public interface Receiver extends Closeable {

    void setReceiveListener(IOParameter.IOParaEventProcessor processor);

    boolean postReceiveAsync() throws IOException;
}
