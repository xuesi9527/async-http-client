package org.asynchttpclient.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.asynchttpclient.chmv8.ConcurrentHashMapV8;

public class AsyncPropertiesHelper {

    private static volatile Config config;

    public static Config getAsyncHttpClientConfig() {
        if (config == null) {
            config = new Config();
        }

        return config;
    }

    /**
     * This method invalidates the property caches. So if a system property has
     * been changed and the effect of this change is to be seen then call
     * reloadProperties() and then getAsyncHttpClientConfig() to get the new
     * property values.
     */
    public static void reloadProperties() {
        if (config != null)
            config.reload();
    }

    public static class Config {

        public static final String DEFAULT_AHC_PROPERTIES = "ahc-default.properties";
        public static final String CUSTOM_AHC_PROPERTIES = "ahc.properties";

        private final ConcurrentHashMapV8<String, String> propsCache = new ConcurrentHashMapV8<String, String>();
        private final Map<String, String> defaultProperties = parsePropertiesFile(DEFAULT_AHC_PROPERTIES);
        private Map<String, String> customProperties = parsePropertiesFile(CUSTOM_AHC_PROPERTIES);

        public void reload() {
            customProperties = parsePropertiesFile(CUSTOM_AHC_PROPERTIES);
            propsCache.clear();
        }

        private Map<String, String> parsePropertiesFile(String file) {

            try {
                InputStream is = getClass().getClassLoader().getResourceAsStream(file);
                if (is == null) {
                    return Collections.emptyMap();
                } else {
                    Map<String, String> map = new HashMap<>();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            String[] part = line.split("=");
                            map.put(part[0], part[1]);
                        }
                        return map;
                    }
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Can't parse file", e);
            }
        }

        private final ConcurrentHashMapV8.Fun<String, String> computer = new ConcurrentHashMapV8.Fun<String, String>() {

            @Override
            public String apply(String key) {
                String value = System.getProperty(key);
                if (value == null) {
                    value = customProperties.get(key);
                }
                if (value == null) {
                    value = defaultProperties.get(key);
                }

                return value;
            }
        };

        public String getString(String key) {
            return propsCache.computeIfAbsent(key, computer);
        }
        
        public int getInt(String key) {
            return Integer.parseInt(getString(key));
        }
        
        public boolean getBoolean(String key) {
            return Boolean.parseBoolean(getString(key));
        }
    }
}