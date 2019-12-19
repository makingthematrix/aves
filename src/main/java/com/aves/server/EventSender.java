package com.aves.server;

import com.aves.server.DAO.ClientsDAO;
import com.aves.server.DAO.NotificationsDAO;
import com.aves.server.model.*;
import com.aves.server.tools.Logger;
import com.aves.server.websocket.ServerEndpoint;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jdbi.v3.core.Jdbi;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.aves.server.tools.Util.time;

public class EventSender {
    private static ObjectMapper mapper = new ObjectMapper();

    public static void sendEvent(Event event, UUID to, Jdbi jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);
        ClientsDAO clientsDAO = jdbi.onDemand(ClientsDAO.class);

        List<String> clients = clientsDAO.getClients(to);
        if (clients.isEmpty()) {
            String strEvent = mapper.writeValueAsString(event);
            notificationsDAO.insert(event.id, null, to, strEvent);
            return;
        }

        for (String clientId : clients) {
            event.id = UUID.randomUUID();
            String strEvent = mapper.writeValueAsString(event);
            notificationsDAO.insert(event.id, clientId, to, strEvent);

            //Send event via Socket
            boolean send = ServerEndpoint.send(clientId, event);
            Logger.debug("sendEvent (%s): to: %s:%s %s", send, to, clientId, strEvent);
        }
    }

    public static String sendEvent(Event event, UUID to, String clientId, Jdbi jdbi) throws JsonProcessingException {
        NotificationsDAO notificationsDAO = jdbi.onDemand(NotificationsDAO.class);

        // Persist event into Notification stream
        String strEvent = mapper.writeValueAsString(event);
        notificationsDAO.insert(event.id, clientId, to, strEvent);

        // Send event via Socket
        boolean send = ServerEndpoint.send(clientId, event);
        Logger.debug("sendEvent (%s): to: %s:%s %s", send, to, clientId, strEvent);

        return strEvent;
    }

    public static Event userClientAddEvent(Device device) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "user.client-add";
        payload.time = time();
        payload.device = device;
        event.payload.add(payload);
        return event;
    }

    public static Event userUpdateEvent(User user) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "user.update";
        payload.time = time();
        payload.user = user;
        event.payload.add(payload);
        return event;
    }

    public static Event connectionEvent(Connection connection) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "user.connection";
        payload.time = time();
        payload.connection = connection;
        payload.convId = connection.conversation;
        event.payload.add(payload);
        return event;
    }

    public static Event memberJoinEvent(UUID from, UUID convId, List<UUID> users) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "conversation.member-join";
        payload.time = time();
        payload.convId = convId;
        payload.from = from;
        payload.data = new Payload.Data();
        payload.data.userIds = users;
        event.payload.add(payload);
        return event;
    }

    public static Event memberLeaveEvent(UUID from, UUID convId, UUID member) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "conversation.member-leave";
        payload.time = time();
        payload.convId = convId;
        payload.from = from;
        payload.data = new Payload.Data();
        payload.data.userIds = Collections.singletonList(member);
        event.payload.add(payload);
        return event;
    }

    public static Event conversationCreateEvent(UUID from, Conversation conv) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "conversation.create";
        payload.convId = conv.id;
        payload.from = from;
        payload.time = time();
        payload.data = new Payload.Data();
        payload.data.id = conv.id;
        payload.data.creator = conv.creator;
        payload.data.name = conv.name;
        payload.data.type = conv.type;
        payload.data.members = conv.members;
        event.payload.add(payload);
        return event;
    }

    public static Event conversationOtrMessageAddEvent(UUID convId, UUID from, Payload.Data data) {
        Event event = new Event();
        event.id = UUID.randomUUID();

        Payload payload = new Payload();
        payload.type = "conversation.otr-message-add";
        payload.convId = convId;
        payload.from = from;
        payload.time = time();
        payload.data = data;
        event.payload.add(payload);
        return event;
    }
}
