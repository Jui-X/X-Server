package box;

import core.ReceivePacket;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class FileReceivePacket extends ReceivePacket<FileOutputStream, File> {
    private File file;

    public FileReceivePacket(long len, File file) {
        super(len);
        this.file = file;
    }

    /**
     * 从流转变为对应实体时直接返回创建时传入的File文件
     * 传入的File文件由业务层进行控制
     * @param stream 文件传输流
     * @return File
     */
    @Override
    protected File buildEntity(FileOutputStream stream) {
        return file;
    }

    @Override
    protected FileOutputStream createStream() {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public byte type() {
        return TYPE_STREAM_FILE;
    }
}
