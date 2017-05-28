package org.morozov.market.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by Morozov on 5/28/2017.
 */
public class AppContext {

    private static final Logger logger = LogManager.getLogger(AppContext.class);

    private static Properties properties = new Properties();

    public static void init(@NotNull String configPath) {
        try {
            InputStream inputStream = new FileInputStream(configPath);
            properties.load(inputStream);
        } catch (FileNotFoundException e) {
            logger.error("Config file is not found!", e);
            throw new IllegalStateException();
        } catch (IOException e) {
            logger.error("Error while reading of config file!", e);
            throw new IllegalStateException();
        }
    }

    public static String getProperty(@NotNull final String propertyName) {
        return properties.getProperty(propertyName);
    }
}
