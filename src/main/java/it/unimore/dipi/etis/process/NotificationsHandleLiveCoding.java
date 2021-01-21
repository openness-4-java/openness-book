package it.unimore.dipi.etis.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.*;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NotificationsHandleLiveCoding extends AbstractWebSocketHandler {

    private final EdgeApplicationConnector eac;
    private final ObjectMapper json;
    private static boolean IS_PUBLISHED = false;

    public NotificationsHandleLiveCoding(EdgeApplicationConnector eac) {
        this.eac = eac;
        this.json = new ObjectMapper();
    }

    @Override
    public void onWebSocketText(String message) {
        try {
            NotificationToConsumer inbound = json.readValue(message, NotificationToConsumer.class);
            if (!IS_PUBLISHED) {
                List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();
                EdgeApplicationServiceNotificationDescriptor n1 = new EdgeApplicationServiceNotificationDescriptor(
                        inbound.getName(),
                        inbound.getVersion(),
                        "description"
                );
                notifications.add(n1);
                EdgeApplicationServiceDescriptor etis = new EdgeApplicationServiceDescriptor(
                        new EdgeApplicationServiceUrn(EtisLiveCoding.OPENNESS_APP_ID, EtisLiveCoding.OPENNESS_NAME_SPACE),
                        "description",
                        EtisLiveCoding.OPENNESS_ETIS_URL,
                        "status",
                        notifications,
                        "info"
                );
                this.eac.postService(etis);
                IS_PUBLISHED = true;
            }
            NotificationFromProducer outbound = new NotificationFromProducer(
                    inbound.getName(),
                    inbound.getVersion(),
                    inbound.getPayload()
            );
            this.eac.postNotification(outbound);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EdgeApplicationConnectorException e) {
            e.printStackTrace();
        }
    }
}
