package box;

import core.SendPacket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class FileSendPacket extends SendPacket<FileInputStream> {
    private final File file;

    public FileSendPacket(File file) {
        this.file = file;
        this.length = file.length();
    }

    /**
     * 使用File构建文件读取流，用以读取本地的文件数据进行发送
     * @return 文件读取流
     */
    @Override
    protected FileInputStream createStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }
}
