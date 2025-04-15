package github.rnd.cysmart_ota_upgrade

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import github.rnd.upgrade.GattOtaBroadcastReceiver

/**
 * created by ArdWang 2025/4/15:15:31
 */
class OtaActivity: AppCompatActivity(), GattOtaBroadcastReceiver.GattOtaListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /// 更新兼容旧版本
    override fun processBondStateChange(intent: Intent?) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent!!.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }


    }

    override fun handleGetFlashSize(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleEnterBootloader(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleSendData(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleProgramRow(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleVerifyRow(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleVerifyChecksum(extras: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun handleExitBootloader(extras: Bundle?) {
        TODO("Not yet implemented")
    }


}