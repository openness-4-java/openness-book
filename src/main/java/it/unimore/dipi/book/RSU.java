package it.unimore.dipi.book;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceNotificationDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceUrn;
import it.unimore.dipi.iot.openness.dto.service.NotificationFromProducer;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple emulated RSU providing parking occupancy notifications every second for 10 times
 */
public class RSU {

    public static final String OPENNESS_APP_ID = "openness4j-rsu";
    public static final String NAME = "parking";
    public static final String VERSION = "1.0";

    public static void main(String[] args) throws EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException, InterruptedException {
        // create authenticator providing URL of OpenNESS EAA
        final EdgeApplicationAuthenticator eaa = new EdgeApplicationAuthenticator(Etis.OPENNESS_AUTH_URL);
        // request authentication providing data of app
        final AuthorizedApplicationConfiguration aac = eaa.authenticateApplication(Etis.OPENNESS_NAME_SPACE, OPENNESS_APP_ID, Etis.OPENNESS_ORG);
        // create connector providing authentication configuration just obtained, URL of OpenNESS EA, and URL of OpenNESS websocket channel for notifications
        final EdgeApplicationConnector eac = new EdgeApplicationConnector(Etis.OPENNESS_API_URL, aac, Etis.OPENNESS_WS);
        // create service descriptor
        final EdgeApplicationServiceDescriptor rsuService = createServiceDescriptor();
        // registers
        eac.postService(rsuService);
        int occupancy = 0;
        // for 10 times...
        for (int i = 0; i < 10; i++) {
            // ...sends updated occupancy...
            final NotificationFromProducer notification = new NotificationFromProducer(
                    NAME,
                    VERSION,
                    occupancy++
            );
            eac.postNotification(notification);
            // ...each second
            Thread.sleep(1000);
        }
    }

    @NotNull
    private static EdgeApplicationServiceDescriptor createServiceDescriptor() {
        // create notification descriptor
        final List<EdgeApplicationServiceNotificationDescriptor> notifications = new ArrayList<>();
        final EdgeApplicationServiceNotificationDescriptor parkingNot = new EdgeApplicationServiceNotificationDescriptor(
                NAME,
                VERSION,
                "parking occupancy"
        );
        notifications.add(parkingNot);
        // create service descriptor
        return new EdgeApplicationServiceDescriptor(
                new EdgeApplicationServiceUrn(OPENNESS_APP_ID, Etis.OPENNESS_NAME_SPACE),
                "parking occupancy",
                "http://eaa.openness:7070/rsu/fake/uri",
                "ready",
                notifications,
                ""
        );
    }

}
