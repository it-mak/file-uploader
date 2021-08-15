package com.file.uploader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static Map<String, String> items = null;

    private static void readConfig() throws IOException {
        final String configPath = Config.class.getClassLoader().getResource("config.properties").getPath();
        properties.load(new FileInputStream(configPath));
    }

    private static Properties getConfig() {
        if (properties.size() == 0) {
            try {
                readConfig();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    public static String get(String key) {
        if (items == null) {
            items = (Map) getConfig();
        }
        return items.get(key);
    }
}