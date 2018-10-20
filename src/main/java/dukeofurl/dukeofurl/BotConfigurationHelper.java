package dukeofurl.dukeofurl;

import org.pircbotx.Configuration;
import org.pircbotx.UtilSSLSocketFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/**
 * Helper class to load and parse the bot configuration after starting up
 */
class BotConfigurationHelper {
    private static final String AUTO_RECONNECT_ATTEMPTS_PROPERTY_NAME = "autoReconnectAttempts";
    private static final String AUTO_RECONNECT_DELAY_PROPERTY_NAME = "autoReconnectDelay";
    private static final String AUTO_RECONNECT_PROPERTY_NAME = "autoReconnect";
    private static final String CHANNELS_PROPERTY_NAME = "channels";
    private static final String CHANNELS_PROPERTY_VALUE_SEPARATOR = ",";
    private static final String CONFIGURATION_FILE_PATH = "/dukeofurl.properties";
    private static final String COUNTRY_PROPERTY_NAME = "country";
    private static final String FINGER_PROPERTY_NAME = "finger";
    private static final String GLOBAL_PROPERTY_GROUP_NAME = "global";
    private static final String IGNORED_NICKS_PROPERTY_NAME = "ignoredNicks";
    private static final String IGNORED_NICKS_PROPERTY_VALUE_SEPARATOR = ",";
    private static final String LANGUAGE_PROPERTY_NAME = "language";
    private static final String LOGIN_PROPERTY_NAME = "login";
    private static final String NAME_PROPERTY_NAME = "name";
    private static final String PORT_PROPERTY_NAME = "port";
    private static final String PROPERTY_GROUP_SPLIT = "\\.";
    private static final String REAL_NAME_PROPERTY_NAME = "realName";
    private static final String SERVER_PROPERTY_NAME = "server";
    private static final String USE_TLS_PROPERTY_NAME = "useTls";
    private static final String VERSION_PROPERTY_NAME = "version";

    /**
     * Builds a list of pircbotx configuration objects for each set of configuration properties
     * @return
     */
    static List<Configuration> buildConfigurationList() {
        Map<String, Map<String, String>> groupedProperties = buildGroupedProperties();
        List<Configuration> configurationList = new ArrayList<>();

        for (String propertyGroup : groupedProperties.keySet()) {
            Map<String, String> properties = groupedProperties.get(propertyGroup);
            Configuration configuration = buildConfiguration(properties);
            configurationList.add(configuration);
        }

        return configurationList;
    }

    /**
     * Builds a pircbotx configuration object from a set of configuration properties
     * @param properties
     * @return
     */
    private static Configuration buildConfiguration(Map<String, String> properties) {
        BotConfiguration.Builder botConfigurationBuilder = new BotConfiguration.Builder();

        String finger = properties.get(FINGER_PROPERTY_NAME);
        String login = properties.get(LOGIN_PROPERTY_NAME);
        String name = properties.get(NAME_PROPERTY_NAME);
        String realName = properties.get(REAL_NAME_PROPERTY_NAME);
        String server = properties.get(SERVER_PROPERTY_NAME);
        String version = properties.get(VERSION_PROPERTY_NAME);
        boolean autoReconnect = Boolean.parseBoolean(properties.get(AUTO_RECONNECT_PROPERTY_NAME));
        int autoReconnectAttempts = Integer.parseInt(properties.get(AUTO_RECONNECT_ATTEMPTS_PROPERTY_NAME));
        int autoReconnectDelay = Integer.parseInt(properties.get(AUTO_RECONNECT_DELAY_PROPERTY_NAME));
        int port = Integer.parseInt(properties.get(PORT_PROPERTY_NAME));

        botConfigurationBuilder.addListener(new BotListener());
        botConfigurationBuilder.addServer(server, port);
        botConfigurationBuilder.setAutoReconnect(autoReconnect);
        botConfigurationBuilder.setAutoReconnectAttempts(autoReconnectAttempts);
        botConfigurationBuilder.setAutoReconnectDelay(autoReconnectDelay);
        botConfigurationBuilder.setFinger(finger);
        botConfigurationBuilder.setLogin(login);
        botConfigurationBuilder.setName(name);
        botConfigurationBuilder.setRealName(realName);
        botConfigurationBuilder.setVersion(version);

        String country = properties.get(COUNTRY_PROPERTY_NAME);
        String language = properties.get(LANGUAGE_PROPERTY_NAME);
        Locale locale = new Locale(language, country);
        botConfigurationBuilder.setLocale(locale);

        boolean useTls = Boolean.parseBoolean(properties.get(USE_TLS_PROPERTY_NAME));
        if (useTls) {
            botConfigurationBuilder.setSocketFactory(new UtilSSLSocketFactory().trustAllCertificates());
        }

        String channels = properties.get(CHANNELS_PROPERTY_NAME);
        if (channels != null) {
            String[] channelArray = channels.split(CHANNELS_PROPERTY_VALUE_SEPARATOR);
            List<String> channelList = Arrays.asList(channelArray);
            botConfigurationBuilder.addAutoJoinChannels(channelList);
        }

        String ignoredNicks = properties.get(IGNORED_NICKS_PROPERTY_NAME);
        if (ignoredNicks != null) {
            String[] ignoredNicksArray = ignoredNicks.split(IGNORED_NICKS_PROPERTY_VALUE_SEPARATOR);
            List<String> igonredNicksList = Arrays.asList(ignoredNicksArray);
            botConfigurationBuilder.setIgnoredNicks(igonredNicksList);
        }

        return botConfigurationBuilder.buildConfiguration();
    }

    /**
     * Fetches properties from the properties file
     * @return
     */
    private static Properties fetchProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = new FileInputStream(CONFIGURATION_FILE_PATH)) {
            properties.load(inputStream);
        } catch (IOException exception) {
            throw new BotException(exception);
        }

        return properties;
    }

    /**
     * Builds and returns sets of grouped properties
     * @return
     */
    private static Map<String, Map<String, String>> buildGroupedProperties() {
        Properties properties = fetchProperties();
        Map<String, Map<String, String>> groupedProperties = new HashMap<>();

        for (String property : properties.stringPropertyNames()) {
            String[] splitProperty = property.split(PROPERTY_GROUP_SPLIT);
            String groupName = splitProperty[0];
            String attributeName = splitProperty[1];

            if (!groupedProperties.containsKey(groupName)) {
                groupedProperties.put(groupName, new HashMap<>());
            }

            String attributeValue = properties.getProperty(property);
            groupedProperties.get(groupName).put(attributeName, attributeValue);
        }

        if (groupedProperties.containsKey(GLOBAL_PROPERTY_GROUP_NAME)) {
            processGlobalProperties(groupedProperties);
        }

        return groupedProperties;
    }

    /**
     * Applies all the global properties to the other sets for all unspecified properties and removes the global group
     * @param groupedProperties
     */
    private static void processGlobalProperties(Map<String, Map<String, String>> groupedProperties) {
        Map<String, String> globalAttributes = groupedProperties.get(GLOBAL_PROPERTY_GROUP_NAME);
        groupedProperties.remove(GLOBAL_PROPERTY_GROUP_NAME);

        for (String network : groupedProperties.keySet()) {
            Map<String, String> localAttributes = groupedProperties.get(network);
            for (String key : globalAttributes.keySet()) {
                if (!localAttributes.containsKey(key)) {
                    localAttributes.put(key, globalAttributes.get(key));
                }
            }
        }
    }
}
