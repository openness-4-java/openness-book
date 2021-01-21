package it.unimore.dipi.book;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;

/**
 * A simple e-TIS acting as a proxy between RSUs and vehicles:
 *  - scans OpenNESS services looking for the RSU
 *  - subscribes to its notifications
 *  - (continues in EtisHandle)
 */
public class Etis {

    /*
     * Configuration data: change URLs as needed
     */
    public static final String OPENNESS_APP_ID = "openness4j-etis";
    public static final String OPENNESS_NAME_SPACE = "openness4j";
    public static final String OPENNESS_ORG = "it.unimore.dipi";
    public static final String OPENNESS_AUTH_URL = "http://eaa.openness:7080/";
    public static final String OPENNESS_API_URL = "https://eaa.openness:7443/";
    public static final String OPENNESS_WS = "wss://eaa.openness:7443/";
    private static final String TARGET_SERVICE_URN_ID = "openness4j-rsu";

    public static void main(String[] args) throws EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException {
        // create authenticator providing URL of OpenNESS EAA
        final EdgeApplicationAuthenticator eaa = new EdgeApplicationAuthenticator(OPENNESS_AUTH_URL);
        // request authentication providing data of app
        final AuthorizedApplicationConfiguration aac = eaa.authenticateApplication(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, OPENNESS_ORG);
        // create connector providing authentication configuration just obtained, URL of OpenNESS EA, and URL of OpenNESS websocket channel for notifications
        final EdgeApplicationConnector eac = new EdgeApplicationConnector(OPENNESS_API_URL, aac, OPENNESS_WS);
        // request list of registered services
        final EdgeApplicationServiceList services = eac.getAvailableServices();
        if (services != null && services.getServiceList() != null) {
            for (EdgeApplicationServiceDescriptor service: services.getServiceList()) {
                // find relevant service to subscribe to (RSU in our case)
                if (service.getServiceUrn().getId().equals(TARGET_SERVICE_URN_ID)) {
                    // create handle for incoming notifications (in our case will use same connector to forward notifications)
                    final EtisHandle handle = new EtisHandle(eac);
                    // setup the websocket notification channel (necessary before subscription) providing data of app and just created handle
                    eac.setupNotificationChannel(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, handle);
                    // subscribe to the target service notifications (RSU in our case) providing the notifications descriptor, and producer data (namespace and app id)
                    eac.postSubscription(service.getNotificationDescriptorList(), service.getServiceUrn().getNamespace(), service.getServiceUrn().getId());
                    break;
                }
            }
        }
    }

}
