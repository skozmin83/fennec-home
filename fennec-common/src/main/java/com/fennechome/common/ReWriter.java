package com.fennechome.common;

import java.io.IOException;
import java.io.Writer;

public class ReWriter extends Writer {
    private final StringBuilder sb = new StringBuilder();

    public void clean() {
        sb.setLength(0);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        sb.append(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
