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
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class EtisProcess {

    private static final Logger logger = LoggerFactory.getLogger(EtisProcess.class);

    public static void main(String[] args) throws IOException, EdgeApplicationAuthenticatorException, EdgeApplicationConnectorException {
        String propsFile = "etis.properties";
        if (args.length > 0) {
            propsFile = args[0].trim();
            logger.info("Custom properties file given: {}", propsFile);
        }
        Properties etisProps = loadProps(propsFile);
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
        rsuService = discover(edgeApplicationConnector, etisProps.getProperty("targetServiceUrnId"));
        if (rsuService.isPresent()) {
            subscribe(rsuService.get(), edgeApplicationConnector, etisProps);
        } else {
            logger.info("no '{}' service found!", etisProps.getProperty("targetServiceUrnId"));
        }
    }

    private static Properties loadProps(final String filename) throws IOException {
        final Properties etisProps = new Properties();
        String path;
        if (filename.equals("etis.properties")) {
            path = Thread.currentThread().getContextClassLoader().getResource(filename).getPath();
        } else {
            if (Path.of(filename).isAbsolute()) {
                path = filename;
            } else {
                path = Thread.currentThread().getContextClassLoader().getResource("").getPath();
                path = String.format("%s/%s", path, filename);
            }
        }
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
        final RsuNotificationsHandle rsuHandle = new RsuNotificationsHandle(edgeApplicationConnector, etisProps);
        edgeApplicationConnector.setupNotificationChannel(etisProps.getProperty("opennessNameSpace"), etisProps.getProperty("opennessAppID"), rsuHandle);
        edgeApplicationConnector.postSubscription(rsuServiceDescriptor.getNotificationDescriptorList(),
                rsuServiceDescriptor.getServiceUrn().getNamespace(),
                rsuServiceDescriptor.getServiceUrn().getId());
        final String endpoint = rsuServiceDescriptor.getEndpointUri();
        logger.info("\tgot service endpoint: {}", endpoint);
        //getEvents(endpoint);
    }

}
