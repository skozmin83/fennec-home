package com.fennechome.common;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class PropertiesUtil {
    public static Configuration getConfig(File props) {
        Configurations configs = new Configurations();
        try {
            return configs.properties(props);
        } catch (ConfigurationException cex) {
            throw new FennecException("Unable to load props [" + props.getAbsolutePath() + "]. ", cex);
        }
    }
}
