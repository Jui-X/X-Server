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
    // 绑定语音Stream到一个命令链接上（带参数：参数为语音链接的唯一标识）
    public static final String COMMAND_CONNECTOR_BIND = "-a bind ";
    // 创建对话房间
    public static final String COMMAND_AUDIO_CREATE_ROOM = "-a create";
    // 加入对话房间（带参数：参数为房间号）
    public static final String COMMAND_AUDIO_JOIN_ROOM = "-a join ";
    // 主动离开对话房间
    public static final String COMMAND_AUDIO_LEAVE_ROOM = "-a leave";

    // 回送链接在服务器上的唯一标识（带参数）
    public static final String COMMAND_INFO_NAME = "-i server ";
    // 回送语音群名（带参数：参数为）
    public static final String COMMAND_INFO_AUDIO_ROOM = "-i room ";
    // 回送语音开始（带参数）
    public static final String COMMAND_INFO_AUDIO_START = "-i start ";
    // 回送语音结束
    public static final String COMMAND_INFO_AUDIO_STOP = "-i stop";
    // 回送语音操作出错
    public static final String COMMAND_INFO_AUDIO_ERROR = "-i error";

    // 退出
    public static final String COMMAND_EXIT = "byebye";
    // 加入/离开群聊
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
