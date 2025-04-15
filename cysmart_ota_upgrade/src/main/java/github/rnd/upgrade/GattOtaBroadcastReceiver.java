package github.rnd.upgrade;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * created by ArdWang 2025/4/15:14:56
 * Receive broadcast data
 */
public class GattOtaBroadcastReceiver extends BroadcastReceiver {

    public interface GattOtaListener {
        void processBondStateChange(Intent intent);
        void handleGetFlashSize(Bundle extras);
        void handleEnterBootloader(Bundle extras);
        void handleSendData(Bundle extras);
        void handleProgramRow(Bundle extras);
        void handleVerifyRow(Bundle extras);
        void handleVerifyChecksum(Bundle extras);
        void handleExitBootloader(Bundle extras);

    }

    private final GattOtaListener mListener;

    private final Context mContext;

    public GattOtaBroadcastReceiver(Context context, GattOtaListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action == null) return;

        if (BootLoaderUtils.ACTION_OTA_STATUS.equals(action)) {
            processOTAStatus(intent);
        } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
            mListener.processBondStateChange(intent);
        }
    }

    private void processOTAStatus(Intent intent){
        final String sharedPrefStatus = Utils.getStringSharedPreference(mContext,
                Constants.PREF_BOOTLOADER_STATE);
        Bundle extras = intent.getExtras();
        switch (sharedPrefStatus) {
            case "" + BootLoaderCommands.ENTER_BOOTLOADER:
                mListener.handleEnterBootloader(extras);
                break;
            case "" + BootLoaderCommands.GET_FLASH_SIZE:
                mListener.handleGetFlashSize(extras);
                break;
            case "" + BootLoaderCommands.SEND_DATA:
                mListener.handleSendData(extras);
                break;
            case "" + BootLoaderCommands.PROGRAM_ROW:
                mListener.handleProgramRow(extras);
                break;
            case "" + BootLoaderCommands.VERIFY_ROW:
                mListener.handleVerifyRow(extras);
                break;
            case "" + BootLoaderCommands.VERIFY_CHECK_SUM:
                mListener.handleVerifyChecksum(extras);
                break;
            case "" + BootLoaderCommands.EXIT_BOOTLOADER:
                mListener.handleExitBootloader(extras);
                break;
        }
    }

}
