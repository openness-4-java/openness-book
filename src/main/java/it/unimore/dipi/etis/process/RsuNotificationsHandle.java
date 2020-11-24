package it.unimore.dipi.etis.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.*;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class RsuNotificationsHandle extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(RsuNotificationsHandle.class);
    private ObjectMapper objectMapper;
    private final EdgeApplicationConnector eac;
    private final Properties etisProps;

    public RsuNotificationsHandle(EdgeApplicationConnector eac, Properties etisProps) {
        this.objectMapper = new ObjectMapper();
        this.eac = eac;
        this.etisProps = etisProps;
    }

    @Override
    public void onWebSocketText(String message) {
        logger.info("Message got: {}", message);
        handleTermination(message);
        try {
            selfPublish(message);
            forwardEvent(message);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EdgeApplicationConnectorException e) {
            e.printStackTrace();
        }
    }

    private void forwardEvent(final String message) throws IOException, EdgeApplicationConnectorException {
        final NotificationToConsumer incoming = objectMapper.readValue(message, NotificationToConsumer.class);
        logger.info("\tincoming '{}' from '{}' ({}) -> {}", incoming.getName(), incoming.getProducer(), incoming.getVersion(), incoming.getPayload());
        logger.info("\tforwarding '{}' to subscribers...", incoming.getName());
        final NotificationFromProducer outgoing = new NotificationFromProducer(
                incoming.getName(),
                incoming.getVersion(),
                incoming.getPayload()
        );
        eac.postNotification(outgoing);
        logger.info("\t...'{}' forwarded -> {}", outgoing.getName(), outgoing.getPayload());
    }

    private void selfPublish(String message) throws IOException, EdgeApplicationConnectorException {
        final NotificationToConsumer incoming = objectMapper.readValue(message, NotificationToConsumer.class);
        final List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();  // TODO handle many notifications: can the service re-publish itself with new notification descriptors so as to update itself?
        final EdgeApplicationServiceNotificationDescriptor outgoingDescriptor = new EdgeApplicationServiceNotificationDescriptor(
                incoming.getName(),
                incoming.getVersion(),
                String.format("%s:%s", incoming.getProducer().getId(), incoming.getProducer().getNamespace())  // custom
        );
        notifications.add(outgoingDescriptor);
        final EdgeApplicationServiceDescriptor etisService = new EdgeApplicationServiceDescriptor(
                new EdgeApplicationServiceUrn(etisProps.getProperty("opennessAppID"), etisProps.getProperty("opennessNameSpace")),  // MUST BE AS DURING AUTHENTICATION
                "ETIS service",
                String.format("%s", etisProps.getProperty("opennessAppUrl")),
                "ready",
                notifications,
                ""
        );
        logger.info("\tpublishing ETIS service descriptor: {}...", etisService);
        this.eac.postService(etisService);
    }

    private void handleTermination(final String message) {
        try {
            final NotificationToConsumer shutdown = this.objectMapper.readValue(message, NotificationToConsumer.class);
            final TerminateNotification tn = new TerminateNotification();
            if (shutdown.getName().equals(tn.getName()) && shutdown.getVersion().equals(tn.getVersion())) {
                logger.info("\treceived notifications termination request, closing web socket...");
                session.close();
                session.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
