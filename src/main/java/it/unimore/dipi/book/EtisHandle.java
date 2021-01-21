package it.unimore.dipi.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.etis.process.EtisLiveCoding;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.*;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The notification handle for a simple e-TIS acting as a proxy between RSUs and vehicles:
 *  - (continues from Etis)
 *  - decodes notifications incoming from RSU
 *  - registers itself as a OpenNESS service delivering the decoded notifications
 *  - forwards the received notification
 */
// extend provided abstract class for convenience
public class EtisHandle extends AbstractWebSocketHandler {

    // required to register as a OpenNESS service and to forward notifications
    private final EdgeApplicationConnector eac;
    // required to decode incoming notifications
    private final ObjectMapper json;
    // required to avoid re-registering
    private static boolean IS_PUBLISHED = false;

    public EtisHandle(EdgeApplicationConnector eac) {
        this.eac = eac;
        this.json = new ObjectMapper();
    }

    /*
     * we care only about incoming notifications
     * other available methods are
     *  - onWebSocketConnect
     *  - onWebSocketError
     *  - onWebSocketClose
     *  - awaitClose (for graceful shutdown)
     */
    @Override
    public void onWebSocketText(String message) { // called for every incoming notification
        try {
            // decode notification
            final NotificationToConsumer inbound = json.readValue(message, NotificationToConsumer.class);
            if (!IS_PUBLISHED) {
                // register as OpenNESS service once
                selfPublish(inbound);
            }
            // create "copy" of received notification...
            final NotificationFromProducer outbound = new NotificationFromProducer(
                    inbound.getName(),
                    inbound.getVersion(),
                    inbound.getPayload()
            );
            // ...to be forwarded
            this.eac.postNotification(outbound);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EdgeApplicationConnectorException e) {
            e.printStackTrace();
        }
    }

    private void selfPublish(NotificationToConsumer inbound) throws EdgeApplicationConnectorException {
        final List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();
        // create new notification descriptor from incoming notification
        final EdgeApplicationServiceNotificationDescriptor n1 = new EdgeApplicationServiceNotificationDescriptor(
                inbound.getName(),
                inbound.getVersion(),
                "description"
        );
        notifications.add(n1);
        // create own service descriptor (unnecessary data added just for completeness)
        final EdgeApplicationServiceDescriptor etis = new EdgeApplicationServiceDescriptor(
                new EdgeApplicationServiceUrn(EtisLiveCoding.OPENNESS_APP_ID, EtisLiveCoding.OPENNESS_NAME_SPACE),
                "description",
                EtisLiveCoding.OPENNESS_ETIS_URL,
                "status",
                notifications,
                "info"
        );
        // register itself as OpenNESS service
        this.eac.postService(etis);
        IS_PUBLISHED = true;
    }

}
