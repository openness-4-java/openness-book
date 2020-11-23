package it.unimore.dipi.etis.process;

import it.unimore.dipi.iot.openness.config.AuthorizedApplicationConfiguration;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationAuthenticator;
import it.unimore.dipi.iot.openness.connector.EdgeApplicationConnector;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceDescriptor;
import it.unimore.dipi.iot.openness.dto.service.EdgeApplicationServiceList;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationAuthenticatorException;
import it.unimore.dipi.iot.openness.exception.EdgeApplicationConnectorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class EtisProcess {

    private static final Logger logger = LoggerFactory.getLogger(EtisProcess.class);

    public static void main(String[] args) throws IOException, EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException {
        Properties etisProps;
        etisProps = loadProps("etis.properties");
        logger.info("loaded props: {}", etisProps);
        AuthorizedApplicationConfiguration authorizedApplicationConfiguration;
        authorizedApplicationConfiguration = handleAuth(etisProps);
        logger.info("authenticated with AppId: {}", authorizedApplicationConfiguration.getApplicationId());
        EdgeApplicationConnector edgeApplicationConnector;
        edgeApplicationConnector = new EdgeApplicationConnector(
                etisProps.getProperty("opennessApiBase"),
                authorizedApplicationConfiguration,
                etisProps.getProperty("opennessWs"));
        logger.info("subscribing to 'rsu-digitaltwin-demo' service (if any)...");
        Optional<EdgeApplicationServiceDescriptor> rsuService;
        rsuService = discover(edgeApplicationConnector, "rsu-digitaltwin-demo");
        if (rsuService.isPresent()) {
            subscribe(rsuService.get(), edgeApplicationConnector, etisProps);
        } else {
            logger.info("no 'rsu-digitaltwin-demo' service found!");
        }
    }

    private static Properties loadProps(final String filename) throws IOException {
        final String path = Thread.currentThread().getContextClassLoader().getResource(filename).getPath();
        final Properties etisProps = new Properties();
        etisProps.load(new FileInputStream(path));
        return etisProps;
    }

    private static AuthorizedApplicationConfiguration handleAuth(final Properties etisProps) throws EdgeApplicationAuthenticatorException {
        final AuthorizedApplicationConfiguration authorizedApplicationConfiguration;
        final EdgeApplicationAuthenticator edgeApplicationAuthenticator = new EdgeApplicationAuthenticator(etisProps.getProperty("opennessAuthBase"));
        final Optional<AuthorizedApplicationConfiguration> storedConfiguration = edgeApplicationAuthenticator.loadExistingAuthorizedApplicationConfiguration(etisProps.getProperty("opennessAppID"), etisProps.getProperty("opennessOrgName"));
        if(storedConfiguration.isPresent()) {
            logger.info("AuthorizedApplicationConfiguration Loaded Correctly !");
            authorizedApplicationConfiguration = storedConfiguration.get();
        } else {
            logger.info("AuthorizedApplicationConfiguration Not Available ! Authenticating the app ...");
            authorizedApplicationConfiguration = edgeApplicationAuthenticator.authenticateApplication(etisProps.getProperty("opennessNameSpace"), etisProps.getProperty("opennessAppID"), etisProps.getProperty("opennessOrgName"));
        }
        return authorizedApplicationConfiguration;
    }

    private static Optional<EdgeApplicationServiceDescriptor> discover(final EdgeApplicationConnector edgeApplicationConnector, final String rsuService) throws EdgeApplicationConnectorException {
        logger.info("\tdiscoverying available services...");
        final EdgeApplicationServiceList availableServices = edgeApplicationConnector.getAvailableServices();
        if(availableServices != null && availableServices.getServiceList() != null){
            Optional<EdgeApplicationServiceDescriptor> result = Optional.empty();
            for(EdgeApplicationServiceDescriptor edgeApplicationServiceDescriptor : availableServices.getServiceList()){
                logger.debug("\tservice URN: {} -> {}", edgeApplicationServiceDescriptor.getServiceUrn(), edgeApplicationServiceDescriptor);
                if(edgeApplicationServiceDescriptor.getServiceUrn().getId().equals(rsuService)) {
                    result = Optional.of(edgeApplicationServiceDescriptor);
                    break;
                }
            }
            return result;
        } else {
            logger.error("\tgot no services!");
            return Optional.empty();
        }
    }

    private static void subscribe(final EdgeApplicationServiceDescriptor rsuServiceDescriptor, final EdgeApplicationConnector edgeApplicationConnector, final Properties etisProps) throws EdgeApplicationConnectorException {
        final RsuNotificationsHandle rsuHandle = new RsuNotificationsHandle();
        edgeApplicationConnector.setupNotificationChannel(etisProps.getProperty("opennessNameSpace"), etisProps.getProperty("opennessAppID"), rsuHandle);
        edgeApplicationConnector.postSubscription(rsuServiceDescriptor.getNotificationDescriptorList(),
                rsuServiceDescriptor.getServiceUrn().getNamespace(),
                rsuServiceDescriptor.getServiceUrn().getId());
        final String endpoint = rsuServiceDescriptor.getEndpointUri();
        logger.info("\tgot rsu endpoint: {}", endpoint);
        //getEvents(endpoint);
    }

}
