package com.fennechome.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fennechome.common.FennecException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sergey on 5/1/2017.
 */
class TestDirectionCapturer implements IDirectionExecutor {
    ObjectMapper mapper = new ObjectMapper();
    ObjectWriter writer = mapper.writer();
    StringBuilder sb = new StringBuilder();
    List<Direction> list = new ArrayList<>();

    @Override
    public void send(Direction d) {
        list.add(d);
    }

    @Override
    public String toString() {
        try {
            sb.setLength(0);
            for (int i = 0; i < list.size(); i++) {
                sb.append(writer.writeValueAsString(list.get(i))).append("\n");
            }
            return sb.toString().replaceAll("\"", "");
        } catch (JsonProcessingException e) {
            throw new FennecException(e);
        }
    }

    public void reset() {
        list.clear();
    }
}
