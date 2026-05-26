package io.github.adulescentia.LUMOS_lib;

import org.junit.Test;

import static org.junit.Assert.*;

public class LumosLibraryTest {

    @Test
    public void registerDeviceAndGetDeviceList() {
        Lumos lumos = Lumos.getInstance();
        lumos.initialize();
        Device d = lumos.registerDevice(0.0, 1.0, 4.0, "TV", "DISPLAY");
        assertNotNull(d);
        assertFalse(lumos.getDeviceList().isEmpty());
    }

    @Test
    public void startIoTControlProcessRequiresInitialize() {
        Lumos lumos = Lumos.getInstance();
        lumos.initialize();
        lumos.startIoTControlProcess();
        assertNotNull(lumos.getLatestResultSnapshot());
    }

    @Test
    public void notInitializedThrowsSpecificErr() {
        Lumos lumos = Lumos.getInstance();
        lumos.shutdown();
        try {
            lumos.startIoTControlProcess();
            fail("Expected NotInitializedErr");
        } catch (NotInitializedErr expected) {
            assertTrue(expected.getMessage().contains("initialize"));
        }
    }

    @Test
    public void resultProvidesVectors() {
        Result result = new Result();
        assertNotNull(result.getDirection());
        assertNotNull(result.getCurrentPosition());
        assertNotNull(result.getCameraPos());
    }
}
