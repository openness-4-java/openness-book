package it.unimore.dipi.book;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;

import static it.unimore.dipi.book.Etis.OPENNESS_NAME_SPACE;

/**
 * A simple emulated vehicle subscribing to parking occupancy notifications
 */
public class Vehicle {

    public static final String OPENNESS_APP_ID = "openness4j-vehicle";
    private static final String TARGET_SERVICE_URN_ID = "openness4j-etis";

    public static void main(String[] args) throws EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException {
        // create authenticator providing URL of OpenNESS EAA
        final EdgeApplicationAuthenticator eaa = new EdgeApplicationAuthenticator(Etis.OPENNESS_AUTH_URL);
        // request authentication providing data of app
        final AuthorizedApplicationConfiguration aac = eaa.authenticateApplication(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, Etis.OPENNESS_ORG);
        // create connector providing authentication configuration just obtained, URL of OpenNESS EA, and URL of OpenNESS websocket channel for notifications
        final EdgeApplicationConnector eac = new EdgeApplicationConnector(Etis.OPENNESS_API_URL, aac, Etis.OPENNESS_WS);
        // request list of registered services
        final EdgeApplicationServiceList services = eac.getAvailableServices();
        if (services != null && services.getServiceList() != null) {
            for (EdgeApplicationServiceDescriptor service: services.getServiceList()) {
                // find relevant service to subscribe to (RSU in our case)
                if (service.getServiceUrn().getId().equals(TARGET_SERVICE_URN_ID)) {
                    // create handle for incoming notifications
                    final VehicleHandle handle = new VehicleHandle();
                    // setup the websocket notification channel (necessary before subscription) providing data of app and just created handle
                    eac.setupNotificationChannel(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, handle);
                    // subscribe to the target service notifications (e-TIS in our case) providing the notifications descriptor, and producer data (namespace and app id)
                    eac.postSubscription(service.getNotificationDescriptorList(), service.getServiceUrn().getNamespace(), service.getServiceUrn().getId());
                    break;
                }
            }
        }
    }
}
