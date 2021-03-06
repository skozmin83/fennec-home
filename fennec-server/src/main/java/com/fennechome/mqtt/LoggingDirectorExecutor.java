package com.fennechome.mqtt;

import com.fennechome.controller.Direction;
import com.fennechome.controller.IDirectionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingDirectorExecutor implements IDirectionExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Override
    public void send(Direction d) {
        logger.info("Direction sent: {}", d);
    }
}
