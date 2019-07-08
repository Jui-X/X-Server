package x;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 11:08
 **/
public class Xyz {
    public static final String COMMAND_EXIT = "byebye";
    public static final String COMMAND_GROUP_JOIN = "-g join";
    public static final String COMMAND_GROUP_LEAVE = "-g leave";
    public static final String GROUP_NAME = "X-";

    private static final String CACHE_DIR = "cache";

    public static File getCacheDir(String dir) {
        String path = System.getProperty("user.dir") + (File.separator + CACHE_DIR + File.separator + dir);
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                throw new RuntimeException("Create File Error...");
            }
        }
        return file;
    }

    public static File createTempFile(File parent) {
        String string = UUID.randomUUID().toString() + ".tmp";
        File file = new File(parent, string);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
}
