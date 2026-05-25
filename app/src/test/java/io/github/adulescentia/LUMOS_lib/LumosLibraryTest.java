package io.github.adulescentia.LUMOS_lib;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class LumosLibraryTest {

    @Test
    public void registerDeviceAndGetDeviceList() {
        Lumos lumos = new Lumos();
        Device d = lumos.registerDevice();
        assertNotNull(d);
        assertFalse(lumos.getDeviceList().isEmpty());
    }

    @Test
    public void startIoTControlProcessInvokesResultConsumer() {
        Lumos lumos = new Lumos();
        AtomicBoolean called = new AtomicBoolean(false);
        lumos.registerExternalResultChannel(result -> called.set(true));
        lumos.startIoTControlProcess();
        assertTrue(called.get());
    }

    @Test
    public void resultProvidesVectors() {
        Result result = new Result();
        assertNotNull(result.getDirection());
        assertNotNull(result.getCurrentPosition());
        assertNotNull(result.getCameraPos());
    }
}
