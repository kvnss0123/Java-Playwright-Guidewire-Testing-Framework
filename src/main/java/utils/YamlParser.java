package utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Parser for YAML locator files.
 */
public class YamlParser {
    private static final Logger logger = Logger.getLogger(YamlParser.class.getName());

    /**
     * Loads a YAML file and returns a map of locators for the elements.
     *
     * @param yamlFile Name of the YAML file to parse (without the `.yaml` extension).
     * @return A map of element names to their locators.
     * @throws IllegalArgumentException If the YAML file is not found or invalid.
     */
    public static Map<String, String> parseYamlFile(String yamlFile) {
        if (yamlFile == null || yamlFile.isEmpty()) {
            throw new IllegalArgumentException("YAML file name cannot be null or empty.");
        }

        String resourcePath = "locatorpages/" + yamlFile + ".yaml";
        try (InputStream inputStream = YamlParser.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("YAML file not found: " + resourcePath);
            }

            Yaml yaml = new Yaml();
            Map<String, Map<String, Object>> data = yaml.load(inputStream);

            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("YAML file is empty or invalid: " + resourcePath);
            }

            Map<String, String> elements = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                String elementName = entry.getKey();
                Map<String, Object> elementData = entry.getValue();

                if (elementData == null || !elementData.containsKey("locator")) {
                    throw new IllegalArgumentException("Missing 'locator' field for element: " + elementName);
                }

                String locator = (String) elementData.get("locator");
                if (locator == null || locator.isEmpty()) {
                    throw new IllegalArgumentException("'locator' field is null or empty for element: " + elementName);
                }

                elements.put(elementName, locator);
            }

            return elements;
        } catch (Exception e) {
            logger.severe("Failed to parse YAML file: " + resourcePath + " - " + e.getMessage());
            throw new RuntimeException("Failed to parse YAML file: " + resourcePath, e);
        }
    }
}