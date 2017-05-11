package com.fennechome.mqtt;

import com.fennechome.common.FennecException;
import com.fennechome.controller.Direction;
import com.fennechome.controller.HoseState;
import com.fennechome.controller.IDirectionExecutor;
import com.fennechome.controller.ThermostatState;
import com.fennechome.server.FennecMqttServer;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MqttDirectionExecutor implements IDirectionExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String controlUser;
    private final String controlBaseTopic;
    private final String controlThermostatId;
    private final FennecMqttServer server;

    public MqttDirectionExecutor(Configuration config, FennecMqttServer server) {
        controlUser = config.getString("fennec.mqtt.control-user");
        controlBaseTopic = config.getString("fennec.mqtt.control-base-topic");
        controlThermostatId = config.getString("fennec.mqtt.control-thermostat-device-id");
        this.server = server;
    }

    @Override
    public void send(Direction d) {
        logger.info("Direction sent: {}", d);
        String message = prepareThermostatMessage(d.getThermostatState());
        server.internalPublish(controlBaseTopic + controlThermostatId, controlUser, message.getBytes());
//        Map<String, HoseState> hoseStates = d.getHoseStates();
//        for (Map.Entry<String, HoseState> entry : hoseStates.entrySet()) {
//            server.internalPublish(controlBaseTopic + hoseStates);
//        }
    }

    private String prepareThermostatMessage(ThermostatState thermostatState) {
        String message;
        switch (thermostatState) {
            case COOL:
            case COOL_LEVEL_2:
                message = "01";
                break;
            case HEAT:
            case HEAT_LEVEL_2:
            case EMERGENCY_HEAT:
                message = "01";
                break;
            case FAN:
                message = "01";
                break;
            case OFF:
                message = "00";
                break;
            default:
                throw new FennecException("Unable to handle [" + thermostatState + "]. ");
        }
        return message;
    }
}
