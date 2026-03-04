package com.gi.systemmonitor.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import com.gi.systemmonitor.domain.model.Device
import com.gi.systemmonitor.domain.model.DeviceStatus
import com.gi.systemmonitor.domain.repository.DeviceRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private lateinit var repository: DeviceRepository
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: DetailViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val testDevice = Device(
        id = "test-device-id", 
        name = "Test Device", 
        ipAddress = "192.168.1.10", 
        port = 5000,
        status = DeviceStatus.ONLINE, 
        cpuUsage = 10, 
        ramUsage = 20
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        savedStateHandle = mockk(relaxed = true)
        
        // Mock SavedStateHandle
        every { savedStateHandle.get<String>("deviceId") } returns "test-device-id"
        every { savedStateHandle.get<Boolean>("disablePolling") } returns true

        // Mock Repository flow
        every { repository.getDeviceById("test-device-id") } returns flowOf(testDevice)

        viewModel = DetailViewModel(repository, savedStateHandle)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `killProcess calls repository with correct PID`() = runTest {
        val pid = 1234
        coEvery { repository.killProcess(any(), pid) } returns Result.success(Unit)
        coEvery { repository.refreshDevice(any()) } returns Unit

        viewModel.killProcess(pid)

        // Allow coroutine to run
        testScheduler.advanceUntilIdle()

        coVerify { repository.killProcess(match { it.id == "test-device-id" }, pid) }
        coVerify { repository.refreshDevice(match { it.id == "test-device-id" }) }
    }

    @Test
    fun `deleteDevice calls repository delete and triggers callback`() = runTest {
        val onDeleted = mockk<() -> Unit>(relaxed = true)
        coEvery { repository.deleteDevice(any()) } returns Unit

        viewModel.deleteDevice(onDeleted)
        
        testScheduler.advanceUntilIdle()

        coVerify { repository.deleteDevice(match { it.id == "test-device-id" }) }
        verify { onDeleted() }
    }
}
