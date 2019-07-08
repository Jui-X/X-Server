package core;

import java.io.Closeable;
import java.io.IOException;

public interface Sender extends Closeable {

    void setSendProcessor(IOParameter.IOParaEventProcessor processor);

    boolean postSendAsync() throws IOException;

    long getLastWriteTime();
}
