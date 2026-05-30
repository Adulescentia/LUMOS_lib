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
    public void serializeAndDeserializeDevicesRoundTrip() {
        Lumos lumos = Lumos.getInstance();
        lumos.initialize();
        lumos.deserializeDevices(new String[0]);

        Device original = lumos.registerDevice(1.25, 2.5, -3.75, "Kitchen|TV\nA", "DISPLAY|PANEL");
        String[] serialized = lumos.serializeDevices();
        Device[] restored = lumos.deserializeDevices(serialized);

        assertEquals(1, serialized.length);
        assertEquals(1, restored.length);
        assertEquals(original.getId(), restored[0].getId());
        assertEquals("Kitchen|TV\nA", restored[0].getName());
        assertEquals("DISPLAY|PANEL", restored[0].getType());
        assertEquals(1.25f, restored[0].getPosition().x, 0.0001f);
        assertEquals(2.5f, restored[0].getPosition().y, 0.0001f);
        assertEquals(-3.75f, restored[0].getPosition().z, 0.0001f);
    }

    @Test
    public void deserializeDevicesRejectsMalformedInputWithoutClearingExistingDevices() {
        Lumos lumos = Lumos.getInstance();
        lumos.initialize();
        lumos.deserializeDevices(new String[0]);
        lumos.registerDevice(0.0, 1.0, 2.0, "Safe Device", "LIGHT");
        int before = lumos.getDeviceList().size();

        try {
            lumos.deserializeDevices(new String[]{"not-a-valid-device-payload"});
            fail("Expected InvalidInputErr");
        } catch (InvalidInputErr expected) {
            assertTrue(expected.getMessage().contains("field count"));
        }

        assertEquals(before, lumos.getDeviceList().size());
    }

    @Test
    public void resultProvidesVectors() {
        Result result = new Result();
        assertNotNull(result.getDirection());
        assertNotNull(result.getCurrentPosition());
        assertNotNull(result.getCameraPos());
    }
}
