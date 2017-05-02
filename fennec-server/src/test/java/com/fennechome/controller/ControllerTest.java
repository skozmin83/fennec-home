package com.fennechome.controller;

import org.junit.Assert;

/**
 * Created by sergey on 5/2/2017.
 */
public class ControllerTest {
    protected IDirectionExecutor executor;
    private long id;

    protected void checkDirections(String expected) {
        Assert.assertEquals(expected, executor.toString());
    }

    long nextId() { return id++; }
}
