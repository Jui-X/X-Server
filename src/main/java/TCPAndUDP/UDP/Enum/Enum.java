package TCPAndUDP.UDP.Enum;


public enum Enum {
    /*
     *
     * 服务器固化UDP接收端口
     * 服务器监听到UDP请求后将此TCP端口发送出去
     *
     **/
    UDP_PORT(50000, "UDP"),
    TCP_PORT(50001, "TCP"),
    SHORT_LENGTH(2, "short"),
    INT_LENGTH(2, "int"),
    CMD_RECEIVE(1, "receive"),
    CMD_SEND(2, "send")
    ;

    private int value;
    private String desc;

    Enum(int port, String desc) {
        this.value = port;
        this.desc = desc;
    }

    public int getValue() {
        return this.value;
    }

    public String getDesc() {
        return this.desc;
    }
}
