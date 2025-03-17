package configurations;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import org.yaml.snakeyaml.Yaml;

/**
 * A utility class to load and retrieve environment-specific configurations
 * from a YAML file and a properties file.
 */
public class EnvironmentConfig {

    // Static variables to store environment and configuration data
    private static String environment;
    private static Map<String, Map<String, Map<String, String>>> environments;

    // Static block to initialize configurations when the class is loaded
    static {
        loadConfigurations();
    }

    /**
     * Loads configurations from the properties and YAML files.
     * This method is called automatically when the class is loaded.
     */
    private static void loadConfigurations() {
        try {
            // Load the config.properties file
            Properties properties = new Properties();
            InputStream propertiesInputStream = EnvironmentConfig.class
                    .getClassLoader()
                    .getResourceAsStream("properties/config.properties");  // Path relative to classpath

            if (propertiesInputStream == null) {
                throw new IOException("config.properties file not found in the classpath.");
            }

            properties.load(propertiesInputStream);

            // Get the environment and configuration file name from properties
            environment = properties.getProperty("environment", "CLOCK2");  // Default to "CLOCK2"
            String configFile = properties.getProperty("configFile", "properties/environments.yaml");

            // Load the environment configuration file (YAML) based on configFile
            Yaml yaml = new Yaml();
            InputStream yamlInputStream = EnvironmentConfig.class
                    .getClassLoader()
                    .getResourceAsStream(configFile);

            if (yamlInputStream == null) {
                throw new IOException(configFile + " file not found in the classpath.");
            }

            environments = yaml.load(yamlInputStream);

        } catch (IOException e) {
            // Log the error and rethrow as a runtime exception to fail fast
            throw new RuntimeException("Failed to load configuration files: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves the URL for the current environment.
     *
     * @return The URL as a String, or null if not found.
     */
    public static String getUrl() {
        return getEnvironmentProperty("url");
    }

    /**
     * Retrieves the username for the current environment.
     *
     * @return The username as a String, or null if not found.
     */
    public static String getUsername() {
        return getEnvironmentProperty("username");
    }

    /**
     * Retrieves the password for the current environment.
     *
     * @return The password as a String, or null if not found.
     */
    public static String getPassword() {
        return getEnvironmentProperty("password");
    }

    /**
     * Helper method to retrieve a specific property for the current environment.
     *
     * @param propertyName The name of the property to retrieve (e.g., "url", "username", "password").
     * @return The property value as a String, or null if not found.
     */
    private static String getEnvironmentProperty(String propertyName) {
        if (environments == null || environments.get("environments") == null) {
            throw new IllegalStateException("Environments configuration is not loaded correctly.");
        }

        Map<String, String> environmentDetails = environments.get("environments").get(environment);
        if (environmentDetails == null) {
            throw new IllegalArgumentException("Environment '" + environment + "' not found in the configuration.");
        }

        return environmentDetails.get(propertyName);
    }
}