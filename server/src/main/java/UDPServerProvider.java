import Utils.ByteUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.UUID;

import static Constant.Constant.HEADER;
import static Enum.Enum.*;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-05-25 22:27
 **/
public class UDPServerProvider {

    /**
     *
     * UDPServerProvider 的单例
     *
     **/
    private static Provider PROVIDER_INSTANCE;

    static void start(int port) {
        stop();
        String index = UUID.randomUUID().toString();
        Provider provider = new Provider(port, index.getBytes());
        provider.start();
        PROVIDER_INSTANCE = provider;
    }

    static void stop() {
        if (PROVIDER_INSTANCE != null) {
            PROVIDER_INSTANCE.exit();
            PROVIDER_INSTANCE = null;
        }
    }

    private static class Provider extends Thread {
        private final int port;
        private final byte[] server_idx;
        private boolean done = true;
        private DatagramSocket datagramSocket = null;
        /**
         *
         * 存储消息的buffer
         *
         **/
        final byte[] buffer = new byte[256];

        public Provider(int port, byte[] index) {
            super();
            this.port = port;
            this.server_idx = index;
        }

        @Override
        public void run() {
            super.run();

            System.out.println("UDPServerProvider => UDP Provider Start.");

            try {
                datagramSocket = new DatagramSocket(UDP_PORT.getValue());
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);

                while (done) {
                    // 接收到数据报
                    datagramSocket.receive(receivePacket);

                    // 接收到的信息，包括了发送者信息
                    String clientIP = receivePacket.getAddress().getHostAddress();
                    int clientPort = receivePacket.getPort();
                    int dataSize = receivePacket.getLength();
                    byte[] clientData = receivePacket.getData();

                    // 校验接收到的数据是否有误
                    boolean isValid = (dataSize > HEADER.length + SHORT_LENGTH.getValue() + INT_LENGTH.getValue())
                            && ByteUtils.startsWith(clientData, HEADER);

                    // 打印发送者信息
                    System.out.println("UDPServerProvider => UDP Provider received from ip " + clientIP +
                            "\tport = " + clientPort);

                    if (!isValid) {
                        continue;
                    }

                    int index = HEADER.length;
                    short cmd = (short) ((clientData[index++] << 8) | (clientData[index++] & 0xff));
                    int resPort = (int) ((clientData[index++] << 24) |
                            ((clientData[index++] & 0xff) << 16) |
                            ((clientData[index++] & 0xff) << 8) |
                            (clientData[index++] & 0xff));

                    if (cmd == CMD_RECEIVE.getValue() && resPort > 0) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        byteBuffer.put(HEADER);
                        byteBuffer.putShort((short) CMD_SEND.getValue());
                        byteBuffer.putInt(port);
                        byteBuffer.put(server_idx);
                        int len = byteBuffer.position();
                        DatagramPacket responsePacket = new DatagramPacket(buffer,
                                len,
                                receivePacket.getAddress(),
                                resPort);
                        datagramSocket.send(responsePacket);
                        System.out.println("UDPServerProvider => UDP Server Provider response to:" + clientIP + "\tport:" +
                                resPort + "\tdataSize:" + len);
                    } else {
                        System.out.println("UDPServerProvider => UDP Server Provider response to:" + clientIP + "\tport:" + resPort);
                    }

                }
            } catch (IOException e) {

            } finally {
                close();
                System.out.println("UDPServerProvider => UDP Provider finished.");
            }
        }

        private void close() {
            if (datagramSocket != null) {
                datagramSocket.close();
                datagramSocket = null;
            }
        }

        void exit() {
            done = false;
            close();
        }
    }
}
