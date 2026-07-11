package io.github.adulescentia.LUMOS_lib

class GestureStateManager {
    enum class Gesture {
        FIST,
        PALM,
        ONE_FINGER,
        V_SIGN,
        UNDEF
    }

    interface ActionListener {
        fun onDeviceSelectionToggled(isSelected: Boolean)
        fun onDevicePowerToggled()
        fun onDeviceModeApplied(modeValue: Float)
    }

    private var prevGesture = Gesture.UNDEF
    @JvmField
    val isDeviceSelected: Boolean = false
    @JvmField
    var isTrackingModeActive: Boolean = false
    private var readyForAction = true

    private val baseWristY = 0.0f
    private var currentModeValue = 0.0f
    private var sensitivity = 1.0f
    private var gestureQueue = SamplingQueue(3) { Gesture.UNDEF }

    private var actionListener: ActionListener? = null

    private var lastPerformedAction : Gesture = Gesture.UNDEF

    fun setActionListener(actionListener: ActionListener?) {
        this.actionListener = actionListener
    }

    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity
    }

    fun update(currentGesture: Gesture, currentWristY: Float) {
        if (isTrackingModeActive) {
            val deltaY = baseWristY - currentWristY
            currentModeValue = deltaY * sensitivity
            LumosLog.d(
                TAG,
                "🔄 [TRACKING] 실시간 높이 변화량 추적 중... deltaY(순수): $deltaY -> 적용값(mode): $currentModeValue"
            )
        }
        processGesture()
        gestureQueue.add(currentGesture)
    }
    private fun processGesture(){
        val reliableGesture = gestureQueue.checkDataIntegrity { it != Gesture.UNDEF && it != Gesture.FIST } ?: return
        val spamProofGesture = if (reliableGesture != lastPerformedAction) reliableGesture else Gesture.UNDEF
        if(spamProofGesture == Gesture.UNDEF) return
        lastPerformedAction = spamProofGesture
        when (spamProofGesture) {
            Gesture.ONE_FINGER -> {
                actionListener!!.onDeviceSelectionToggled(true)
            }
            Gesture.PALM -> {
                actionListener!!.onDevicePowerToggled()
            }
            Gesture.V_SIGN -> {
                if(isTrackingModeActive) applyDeviceMode(currentModeValue)
                isTrackingModeActive = !isTrackingModeActive
            }
            else -> {}
        }
    }

    private fun applyDeviceMode(finalModeValue: Float) {
        if (actionListener != null) {
            actionListener!!.onDeviceModeApplied(finalModeValue+1)
        }
        println("LUMOS Core Engine -> 디바이스에 적용 완료: ${finalModeValue+1}")
    }

    companion object {
        private const val TAG = "GestureStateManager"
    }
}
