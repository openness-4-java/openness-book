package it.unimore.dipi.book;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimore.dipi.iot.openness.dto.service.NotificationToConsumer;
import it.unimore.dipi.iot.openness.notification.AbstractWebSocketHandler;

import java.io.IOException;

/**
 * The notification handle for a simple emulated vehicle receiving notifications from the e-TIS
 */
// extend provided abstract class for convenience
public class VehicleHandle extends AbstractWebSocketHandler {

    // required to decode incoming notifications
    private final ObjectMapper json;

    public VehicleHandle() {
        this.json = new ObjectMapper();
    }

    @Override
    public void onWebSocketText(String message) { // called for every incoming notification
        try {
            // decode notification
            final NotificationToConsumer inbound = json.readValue(message, NotificationToConsumer.class);
            // just log it in our case
            System.out.printf("Got notification < %s > from < %s > with content: %s\n", inbound.getName(), inbound.getProducer().getId(), inbound.getPayload());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
