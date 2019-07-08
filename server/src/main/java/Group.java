import box.StringReceivePacket;
import handler.ClientHandler;
import handler.ConnectorStringPacketChain;

import java.util.ArrayList;
import java.util.List;

/**
 * @param: none
 * @description:
 * @author: KingJ
 * @create: 2019-07-08 11:30
 **/
class Group {
    private final String name;
    private final GroupMessageAdapter adapter;
    private final List<ClientHandler> members = new ArrayList<>();

    Group(String name, GroupMessageAdapter adapter) {
        this.name = name;
        this.adapter = adapter;
    }

    String getName() {
        return name;
    }

    boolean addMember(ClientHandler handler) {
        synchronized (members) {
            if (!members.contains(handler)) {
                members.add(handler);
                //
                handler.getStringPacketChain()
                        .appendLast(new ForwardConnectorStringPacketChain());

                System.out.println("Group [" + name + "] add new member: " + handler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    boolean removeMember(ClientHandler handler) {
        synchronized (members) {
            if (members.remove(handler)) {
                handler.getStringPacketChain()
                        .remove(ForwardConnectorStringPacketChain.class);
                System.out.println("Group [" + name + "] leave member: " + handler.getClientInfo());
                return true;
            }
        }
        return false;
    }

    private class ForwardConnectorStringPacketChain extends ConnectorStringPacketChain {

        @Override
        protected boolean consume(ClientHandler handler, StringReceivePacket stringReceivePacket) {
            synchronized (members) {
                for (ClientHandler member : members) {
                    if (member == handler) {
                        continue;
                    }
                    adapter.sendMessageToClient(member, stringReceivePacket.entity());
                }
            }
            return true;
        }
    }

    interface GroupMessageAdapter {
        void sendMessageToClient(ClientHandler handler, String msg);
    }
}
