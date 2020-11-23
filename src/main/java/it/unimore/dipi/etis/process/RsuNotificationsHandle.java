package it.unimore.dipi.etis.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.NotificationFromProducer;
import it.unimore.dipi.iot.openness.dto.service.NotificationToConsumer;
import it.unimore.dipi.iot.openness.dto.service.TerminateNotification;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class RsuNotificationsHandle extends AbstractWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(RsuNotificationsHandle.class);
    private ObjectMapper objectMapper;
    private final EdgeApplicationConnector eac;

    public RsuNotificationsHandle(EdgeApplicationConnector eac) {
        this.objectMapper = new ObjectMapper();
        this.eac = eac;
    }

    @Override
    public void onWebSocketText(String message) {
        logger.info("Message got: {}", message);
        handleTermination(message);
        try {
            forwardEvent(message, this.eac);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardEvent(final String message, final EdgeApplicationConnector eac) throws IOException {
        final NotificationToConsumer incoming = objectMapper.readValue(message, NotificationToConsumer.class);
        logger.info("\tincoming {} from {} ({}) -> {}", incoming.getName(), incoming.getProducer(), incoming.getVersion(), incoming.getPayload());
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
