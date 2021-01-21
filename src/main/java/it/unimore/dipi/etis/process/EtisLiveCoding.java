package it.unimore.dipi.etis.process;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;

public class EtisLiveCoding {

    public static final String OPENNESS_APP_ID = "etis-openness-livecoding";
    public static final String OPENNESS_NAME_SPACE = "etis-openness-livecoding";
    private static final String OPENNESS_ORG = "it.unimore.dipi";
    private static final String OPENNESS_AUTH_URL = "http://eaa.openness:7080/";
    private static final String OPENNESS_API_URL = "https://eaa.openness:7443/";
    private static final String OPENNESS_WS = "wss://eaa.openness:7443/";
    public static final String OPENNESS_ETIS_URL = "http://eaa.openness:7070/api/etis/";
    private static final String TARGET_SERVICE_URN_ID = "rsu-digitaltwin-demo";

    public static void main(String[] args) throws EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException {
        EdgeApplicationAuthenticator eaa = new EdgeApplicationAuthenticator(OPENNESS_AUTH_URL);
        AuthorizedApplicationConfiguration aac = eaa.authenticateApplication(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, OPENNESS_ORG);
        EdgeApplicationConnector eac = new EdgeApplicationConnector(OPENNESS_API_URL, aac, OPENNESS_WS);
        EdgeApplicationServiceList services = eac.getAvailableServices();
        if (services != null && services.getServiceList() != null) {
            for (EdgeApplicationServiceDescriptor service: services.getServiceList()) {
                if (service.getServiceUrn().getId().equals(TARGET_SERVICE_URN_ID)) {
                    NotificationsHandleLiveCoding handle = new NotificationsHandleLiveCoding(eac);
                    eac.setupNotificationChannel(OPENNESS_NAME_SPACE, OPENNESS_APP_ID, handle);
                    eac.postSubscription(service.getNotificationDescriptorList(), service.getServiceUrn().getNamespace(), service.getServiceUrn().getId());
                }
            }
        }
    }

}
