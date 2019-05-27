package TCPDemo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-20 22:46
 **/
public class TCPClient {
    private static final int PORT = 50000;
    private static final int LOCAL_PORT = 50001;

    public static void main(String[] args) throws IOException {
        Socket socket = createSocket();
        initSocket(socket);

        socket.connect(new InetSocketAddress(Inet4Address.getLocalHost(), PORT), 3000);

        System.out.println("客户端信息：" + socket.getLocalAddress() + " Port:" + socket.getLocalPort());
        System.out.println("服务器端信息：" + socket.getInetAddress() + " Port:" + socket.getPort());

        try {
            toDo(socket);
        } catch (Exception e) {
            System.out.println("Socket异常关闭!");
        }

        // 资源释放
        socket.close();
        System.out.println("客户端已退出~");
    }

    private static Socket createSocket() throws IOException {

        /*// 无代理模式，等效于空构造函数
        Socket socket = new Socket(Proxy.NO_PROXY);

        // 新建一份具有HTTP代理的套机字，传输数据通过www.googole.com:8080端口传输转发
        Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(Inet4Address.getByName("www.google.com"), 8080));
        socket = new Socket(proxy);

        // 新建一个套接字，并直接连接到本地50000端口的服务器上
        socket = new Socket("localhost", PORT);
        socket = new Socket(Inet4Address.getLocalHost(), PORT);

        // 新建一个套接字，直接连接到本地50000端口的服务器上，并绑定到本地50001的端口上
        socket = new Socket("localhost", PORT, Inet4Address.getLocalHost(), LOCAL_PORT);
        socket = new Socket(Inet4Address.getLocalHost(), PORT, Inet4Address.getLocalHost(), LOCAL_PORT);*/

        // 创建新的套接字，并绑定到本地50001端口
        Socket socket = new Socket();
        socket.bind(new InetSocketAddress(Inet4Address.getLocalHost(), LOCAL_PORT));

        return socket;
    }

    private static void initSocket(Socket socket) throws SocketException {
        // 设置read阻塞时间为2s
        socket.setSoTimeout(2000);

        // 是否复用未完全关闭的Socket，对于指定bind操作后的套接字有效
        socket.setReuseAddress(true);

        // 是否开启Nagle算法，即数据包在传送过程中是否需要延迟
        socket.setTcpNoDelay(true);

        // 是否需要在长时间无数据响应的情况下发送确认数据，时间大约为2h
        socket.setKeepAlive(true);

        // 对于close关闭操作行为进行怎么样的处理，默认为false, 0
        // false, 0: 默认情况，关闭时立即返回，底层系统接管输出流，将缓冲区的数据发送完成
        // true, 0: 关闭时立即返回，缓冲区数据抛弃，直接发送RST结束命令到对方，无需经过2MSL等待
        // true，20: 关闭时最长阻塞200ms，随后按第二种情况继续处理
        socket.setSoLinger(true, 20);

        // 是否让紧急数据内敛，默认为false
        // 紧急数据通过 socket.sendUrgentData(1) 设置
        socket.setOOBInline(true);

        // 设置接收和发送缓冲区大小
        socket.setReceiveBufferSize(64 * 1024 * 1024);
        socket.setSendBufferSize(64 * 1024 * 1024);

        // 设置性能参数：短链接，延迟，带宽利用率的相对重要性
        socket.setPerformancePreferences(1, 1, 1);
    }

    private static void toDo(Socket client) throws IOException {
        // 得到Socket输出流
        OutputStream outputStream = client.getOutputStream();
        // 得到Socket输入流
        InputStream inputStream = client.getInputStream();

        byte[] buffer = new byte[256];
        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

        byteBuffer.put((byte) 126);

        char c = 'c';
        byteBuffer.putChar(c);

        int a = 1024;
        byteBuffer.putInt(a);

        boolean b = true;
        byteBuffer.put(b ? (byte) 1 : (byte) 0);

        long l = 123456789;
        byteBuffer.putLong(l);

        float f = 123.45f;
        byteBuffer.putFloat(f);

        double d = 123456.7890;
        byteBuffer.putDouble(d);

        String str = "hello 你好 ~";
        byteBuffer.put(str.getBytes());

        // 发送到服务器
        outputStream.write(buffer, 0, byteBuffer.position() + 1);

        // 接收服务器返回数据
        int read = inputStream.read(buffer);
        System.out.printf("收到数据长度为:" + read + "\n");

        // 关闭
        outputStream.close();
        inputStream.close();
    }
}
