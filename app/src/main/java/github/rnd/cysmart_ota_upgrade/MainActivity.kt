package github.rnd.cysmart_ota_upgrade

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import github.rnd.upgrade.BluetoothLeService

/**
 * created by ArdWang 2023/4/25:9:50 AM
 */
class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        print(BluetoothLeService.ACTION_GATT_CONNECTED)

    }


    /// usage  读取蓝牙列表




}