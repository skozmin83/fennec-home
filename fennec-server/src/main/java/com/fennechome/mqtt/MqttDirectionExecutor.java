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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

public class MqttDirectionExecutor implements IDirectionExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String controlUser;
    private final String controlBaseTopic;
    private final String uiTopic;
    private final String controlThermostatId;
    private final FennecMqttServer server;
    private final DateFormat df;

    public MqttDirectionExecutor(Configuration config, FennecMqttServer server) {
        controlUser = config.getString("fennec.mqtt.control-user");
        controlBaseTopic = config.getString("fennec.mqtt.control-base-topic");
        uiTopic = config.getString("fennec.mqtt.ui-base-topic");
        controlThermostatId = config.getString("fennec.mqtt.control-thermostat-device-id");
        this.server = server;


        TimeZone tz = TimeZone.getTimeZone("America/New_York");
//        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        // Quoted "Z" to indicate UTC, no timezone offset
        df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        df.setTimeZone(tz);
    }

    @Override
    public void send(Direction d) {
        long ts = System.currentTimeMillis();
        String controlMessage = prepareThermostatMessage(d.getThermostatState()); // todo propagate ts
        String uiMessage = prepareUiMessage(d.getThermostatState(), ts);
        logger.info("Direction sent: {}, control {}, ui {}", d, controlMessage, uiMessage);
        server.internalPublish(controlBaseTopic + controlThermostatId, controlUser, controlMessage.getBytes());
        server.internalPublish(uiTopic + controlThermostatId, controlUser, uiMessage.getBytes());
//        Map<String, HoseState> hoseStates = d.getHoseStates();
//        for (Map.Entry<String, HoseState> entry : hoseStates.entrySet()) {
//            server.internalPublish(controlBaseTopic + hoseStates);
//        }
    }

    private String prepareUiMessage(ThermostatState thermostatState, long ts) {
        return "{\"state\":\"" + thermostatState.name()
                + "\", \"etype\":\"THERMOSTAT\", \"ts\":\"" + df.format(ts) + "\"}";
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
