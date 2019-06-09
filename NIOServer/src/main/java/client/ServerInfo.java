package client;

import lombok.Data;

/**
 * @param: none
 * @description: 接收到的TCP服务器信息
 * @author: KingJ
 * @create: 2019-05-26 10:50
 **/
@Data
public class ServerInfo {
    private String address;
    private int port;
    private String server_index;


    public ServerInfo(String address, int port, String server_index) {
        this.address = address;
        this.port = port;
        this.server_index = server_index;
    }
}
