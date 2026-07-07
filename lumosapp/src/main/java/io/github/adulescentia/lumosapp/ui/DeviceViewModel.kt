package io.github.adulescentia.lumosapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adulescentia.LUMOS_lib.Device
import io.github.adulescentia.LUMOS_lib.LumosImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeviceViewModel : ViewModel() {

    // 1. 백엔드에서 받아온 기기 목록 상태를 담는 통 (초기값은 빈 리스트)
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    // 2. 로딩 상태 관리
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 화면이 처음 켜질 때 백엔드에서 데이터를 가져옵니다.
        fetchDevicesFromBackend()
    }

    fun fetchDevicesFromBackend() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 여기서 백엔드 API를 호출합니다 (예시: Retrofit 통신)
                // val response = backendApi.getDevices()
                // _devices.value = response

                // 가상의 백엔드 데이터 삽입 테스트
                _devices.value = LumosImpl.getInstance().deviceList.toList()
            } catch (e: Exception) {
                // 에러 처리
            } finally {
                _isLoading.value = false
            }
        }
    }
}