package io.github.adulescentia.LUMOS_lib;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GestureStateManagerTest {

    private static class TestListener implements GestureStateManager.ActionListener {
        int selectionToggleCount = 0;
        boolean latestSelectionState = false;
        int powerToggleCount = 0;
        int modeAppliedCount = 0;
        float latestModeValue = 0f;

        @Override
        public void onDeviceSelectionToggled(boolean isSelected) {
            selectionToggleCount++;
            latestSelectionState = isSelected;
        }

        @Override
        public void onDevicePowerToggled() {
            powerToggleCount++;
        }

        @Override
        public void onDeviceModeApplied(float modeValue) {
            modeAppliedCount++;
            latestModeValue = modeValue;
        }
    }

    @Test
    public void deviceSelection_onlyWorksAfterFistOrUndef_andToggles() {
        GestureStateManager manager = new GestureStateManager();
        TestListener listener = new TestListener();
        manager.setActionListener(listener);

        manager.update(GestureStateManager.Gesture.PALM, 0f);
        manager.update(GestureStateManager.Gesture.ONE_FINGER, 0f);
        assertEquals(0, listener.selectionToggleCount);

        manager.update(GestureStateManager.Gesture.FIST, 0f);
        manager.update(GestureStateManager.Gesture.ONE_FINGER, 0f);
        assertEquals(1, listener.selectionToggleCount);
        assertTrue(listener.latestSelectionState);
        assertTrue(manager.isDeviceSelected);

        manager.update(GestureStateManager.Gesture.FIST, 0f);
        manager.update(GestureStateManager.Gesture.ONE_FINGER, 0f);
        assertEquals(2, listener.selectionToggleCount);
        assertFalse(listener.latestSelectionState);
        assertFalse(manager.isDeviceSelected);
    }

    @Test
    public void powerToggle_onlyWorksWithFistToPalmTransition() {
        GestureStateManager manager = new GestureStateManager();
        TestListener listener = new TestListener();
        manager.setActionListener(listener);

        manager.update(GestureStateManager.Gesture.UNDEF, 0f);
        manager.update(GestureStateManager.Gesture.PALM, 0f);
        assertEquals(0, listener.powerToggleCount);

        manager.update(GestureStateManager.Gesture.FIST, 0f);
        manager.update(GestureStateManager.Gesture.PALM, 0f);
        assertEquals(1, listener.powerToggleCount);
    }

    @Test
    public void modeControl_vSignSequence_startsTracksAndAppliesOnSecondVSign() {
        GestureStateManager manager = new GestureStateManager();
        TestListener listener = new TestListener();
        manager.setActionListener(listener);
        manager.setSensitivity(2.0f);

        manager.update(GestureStateManager.Gesture.FIST, 0.5f);
        manager.update(GestureStateManager.Gesture.ONE_FINGER, 0.5f);
        assertTrue(manager.isDeviceSelected);

        manager.update(GestureStateManager.Gesture.FIST, 0.5f);
        manager.update(GestureStateManager.Gesture.V_SIGN, 0.5f);
        assertTrue(manager.isTrackingModeActive);

        manager.update(GestureStateManager.Gesture.UNDEF, 0.3f);

        manager.update(GestureStateManager.Gesture.FIST, 0.3f);
        manager.update(GestureStateManager.Gesture.V_SIGN, 0.3f);
        assertFalse(manager.isTrackingModeActive);
        assertEquals(1, listener.modeAppliedCount);
        assertEquals(0.4f, listener.latestModeValue, 0.0001f);
    }
}
