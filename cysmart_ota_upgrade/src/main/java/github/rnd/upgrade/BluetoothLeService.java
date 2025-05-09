/*
 * Copyright Cypress Semiconductor Corporation, 2014-2014-2015 All rights reserved.
 *
 * This software, associated documentation and materials ("Software") is
 * owned by Cypress Semiconductor Corporation ("Cypress") and is
 * protected by and subject to worldwide patent protection (UnitedStates and foreign), United States copyright laws and international
 * treaty provisions. Therefore, unless otherwise specified in a separate license agreement between you and Cypress, this Software
 * must be treated like any other copyrighted material. Reproduction,
 * modification, translation, compilation, or representation of this
 * Software in any other form (e.g., paper, magnetic, optical, silicon)
 * is prohibited without Cypress's express written permission.
 *
 * Disclaimer: THIS SOFTWARE IS PROVIDED AS-IS, WITH NO WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO,
 * NONINFRINGEMENT, IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE. Cypress reserves the right to make changes
 * to the Software without notice. Cypress does not assume any liability
 * arising out of the application or use of Software or any product or
 * circuit described in the Software. Cypress does not authorize its
 * products for use as critical components in any products where a
 * malfunction or failure may reasonably be expected to result in
 * significant injury or death ("High Risk Product"). By including
 * Cypress's product in a High Risk Product, the manufacturer of such
 * system or application assumes all risk of such use and in doing so
 * indemnifies Cypress against all liability.
 *
 * Use of this Software may be limited by and subject to the applicable
 * Cypress software license agreement.
 *
 *
 */

package github.rnd.upgrade;


import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;




/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given BlueTooth LE device.
 */
public class BluetoothLeService extends Service {

    /**
     * GATT Status constants
     */
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_DISCONNECTED_CAROUSEL =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_CAROUSEL";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.cysmart.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED_OTA";
    public final static String ACTION_GATT_CONNECT_OTA =
            "com.example.bluetooth.le.ACTION_GATT_CONNECT_OTA";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_OTA =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_OTA";
    public final static String ACTION_GATT_CHARACTERISTIC_ERROR =
            "com.example.bluetooth.le.ACTION_GATT_CHARACTERISTIC_ERROR";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.example.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";
    public final static String ACTION_PAIR_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";
    public final static String ACTION_WRITE_COMPLETED =
            "android.bluetooth.device.action.ACTION_WRITE_COMPLETED";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    /**
     * Connection status Constants
     */

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 4;
    private final static String ACTION_GATT_DISCONNECTING =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTING";
    private final static String ACTION_PAIRING_REQUEST =
            "com.example.bluetooth.le.PAIRING_REQUEST";
    private static final int STATE_BONDED = 5;

    /**
     * BluetoothAdapter for handling connections
     */
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    /**
     * Disable?enable notification
     */
    public static ArrayList<BluetoothGattCharacteristic> mEnabledCharacteristics =
            new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mRDKCharacteristics =
            new ArrayList<>();
    public static ArrayList<BluetoothGattCharacteristic> mGlucoseCharacteristics =
            new ArrayList<>();
    public static boolean mDisableNotificationFlag = false;
    public static boolean mEnableRDKNotificationFlag = false;

    public static boolean mEnableGlucoseFlag = false;


    private static int mConnectionState = STATE_DISCONNECTED;
    private static boolean mOtaExitBootloaderCmdInProgress = false;
    /**
     * Device address
     */
    private static String mBluetoothDeviceAddress;
    private static String mBluetoothDeviceName;
    private static Context mContext;

    /**
     * Implements callback methods for GATT events that the app cares about. For
     * example,connection change and services discovered.
     */
    private final static BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Logger.i("onConnectionStateChange");
            String intentAction;
            // GATT Server connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_established);
                Logger.datalog(dataLog);

                // 立即去发现服务
                gatt.discoverServices();

            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTED;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_disconnected);
                Logger.datalog(dataLog);
            }
            // GATT Server Connecting
            if (newState == BluetoothProfile.STATE_CONNECTING) {
                intentAction = ACTION_GATT_CONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_CONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_connection_establishing);
                Logger.datalog(dataLog);
            }
            // GATT Server disconnected
            else if (newState == BluetoothProfile.STATE_DISCONNECTING) {
                intentAction = ACTION_GATT_DISCONNECTING;
                synchronized (mGattCallback) {
                    mConnectionState = STATE_DISCONNECTING;
                }
                broadcastConnectionUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.i("BluetoothLeService", "onServicesDiscovered");
            // GATT Services discovered
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothLeService", "disconver Services success");
                String dataLog2 = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_success);
                Logger.datalog(dataLog2);
                broadcastConnectionUpdate(ACTION_GATT_SERVICES_DISCOVERED);

            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION ||
                    status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {

                Log.i("BluetoothLeService", "disconver Services GATT_INSUFFICIENT_ENCRYPTION");
                bondDevice();
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            } else {
                Log.i("BluetoothLeService", "disconver Services Failed");

                String dataLog2 = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_service_discovery_status) +
                        mContext.getResources().getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog2);
                broadcastConnectionUpdate(ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().
                    getService().getUuid(), serviceUUID);


            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().
                    getUuid(), characteristicUUID);

            String descriptorUUID = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUID);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[00]";
                Intent intent = new Intent(ACTION_WRITE_SUCCESS);
                mContext.sendBroadcast(intent);
                Logger.datalog(dataLog);
                if (descriptor.getValue() != null)
                    addRemoveData(descriptor);
                if (mDisableNotificationFlag) {
                    disableAllEnabledCharacteristics();
                } else if (mEnableRDKNotificationFlag) {
                    if (mRDKCharacteristics.size() > 0) {
                        mRDKCharacteristics.remove(0);
                        enableAllRDKCharacteristics();
                    }

                } else if (mEnableGlucoseFlag) {
                    if (mGlucoseCharacteristics.size() > 0) {
                        mGlucoseCharacteristics.remove(0);
                        enableAllGlucoseCharacteristics();
                    }

                }
            } else if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                    || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                bondDevice();
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                mContext.sendBroadcast(intent);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_failure) +
                        +status;
                Logger.datalog(dataLog);
                mDisableNotificationFlag = false;
                mEnableRDKNotificationFlag = false;
                mEnableGlucoseFlag = false;
                Intent intent = new Intent(ACTION_WRITE_FAILED);
                mContext.sendBroadcast(intent);
            }
        }


        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

            String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

            String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);

            String descriptorUUIDText = descriptor.getUuid().toString();
            String descriptorName = GattAttributes.lookupUUID(descriptor.getUuid(), descriptorUUIDText);

            String descriptorValue = " " + Utils.ByteArraytoHex(descriptor.getValue()) + " ";

            if (status == BluetoothGatt.GATT_SUCCESS) {
                UUID descriptorUUID = descriptor.getUuid();
                final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
                Bundle mBundle = new Bundle();
                // Putting the byte value read for GATT Db
                mBundle.putByteArray(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE,
                        descriptor.getValue());
                mBundle.putInt(Constants.EXTRA_BYTE_DESCRIPTOR_INSTANCE_VALUE,
                        descriptor.getCharacteristic().getInstanceId());
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + descriptorValue + "]";
                Logger.datalog(dataLog);
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_UUID,
                        descriptor.getUuid().toString());
                mBundle.putString(Constants.EXTRA_DESCRIPTOR_BYTE_VALUE_CHARACTERISTIC_UUID,
                        descriptor.getCharacteristic().getUuid().toString());
                if (descriptorUUID.equals(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG)) {
                    String valueReceived = DescriptorParser
                            .getClientCharacteristicConfiguration(descriptor, mContext);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, valueReceived);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_EXTENDED_PROPERTIES)) {
                    HashMap<String, String> receivedValuesMap = DescriptorParser
                            .getCharacteristicExtendedProperties(descriptor, mContext);
                    String reliableWriteStatus = receivedValuesMap.get(Constants.FIRST_BIT_KEY_VALUE);
                    String writeAuxillaryStatus = receivedValuesMap.get(Constants.SECOND_BIT_KEY_VALUE);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reliableWriteStatus + "\n"
                            + writeAuxillaryStatus);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_USER_DESCRIPTION)) {
                    String description = DescriptorParser
                            .getCharacteristicUserDescription(descriptor);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, description);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_SERVER_CHARACTERISTIC_CONFIGURATION)) {
                    String broadcastStatus = DescriptorParser.
                            getServerCharacteristicConfiguration(descriptor, mContext);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, broadcastStatus);
                } else if (descriptorUUID.equals(UUIDDatabase.UUID_REPORT_REFERENCE)) {
                    ArrayList<String> reportReferencealues = DescriptorParser.getReportReference(descriptor);
                    String reportReference;
                    String reportReferenceType;
                    if (reportReferencealues.size() == 2) {
                        reportReference = reportReferencealues.get(0);
                        reportReferenceType = reportReferencealues.get(1);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_ID, reportReference);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_REPORT_REFERENCE_TYPE, reportReferenceType);
                        mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, reportReference + "\n" +
                                reportReferenceType);
                    }

                } else if (descriptorUUID.equals(UUIDDatabase.UUID_CHARACTERISTIC_PRESENTATION_FORMAT)) {
                    String value = DescriptorParser.getCharacteristicPresentationFormat(descriptor, mContext);
                    mBundle.putString(Constants.EXTRA_DESCRIPTOR_VALUE, value);
                }
                intent.putExtras(mBundle);
                /**
                 * Sending the broad cast so that it can be received on
                 * registered receivers
                 */

                mContext.sendBroadcast(intent);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog);
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String dataLog = "";
            if (status == BluetoothGatt.GATT_SUCCESS) {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status)
                        + mContext.getResources().getString(R.string.dl_status_success);

                //timeStamp("OTA WRITE RESPONSE TIMESTAMP ");

                Logger.datalog(dataLog);
            } else {
                dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Intent intent = new Intent(ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                mContext.sendBroadcast(intent);
                Logger.datalog(dataLog);
            }

            Logger.d("CYSMART", dataLog);
            boolean isExitBootloaderCmd = false;
            synchronized (mGattCallback) {
                isExitBootloaderCmd = mOtaExitBootloaderCmdInProgress;
                if (mOtaExitBootloaderCmdInProgress)
                    mOtaExitBootloaderCmdInProgress = false;
            }

            if (isExitBootloaderCmd)
                onOtaExitBootloaderComplete(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = " " + Utils.ByteArraytoHex(characteristic.getValue()) + " ";
            // GATT Characteristic read
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_response) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + characteristicValue + "]";
                Logger.datalog(dataLog);
                broadcastNotifyUpdate(characteristic);
            } else {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                        + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_read_request_status) +
                        mContext.getResources().
                                getString(R.string.dl_status_failure) + status;
                Logger.datalog(dataLog);
                if (status == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION
                        || status == BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) {
                    bondDevice();
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String serviceUUID = characteristic.getService().getUuid().toString();
            String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

            String characteristicUUID = characteristic.getUuid().toString();
            String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

            String characteristicValue = Utils.ByteArraytoHex(characteristic.getValue());
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().
                            getString(R.string.dl_characteristic_notification_response) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);
            broadcastNotifyUpdate(characteristic);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Resources res = mContext.getResources();
            String dataLog = String.format(
                    res.getString(R.string.exchange_mtu_rsp),
                    mBluetoothDeviceName,
                    mBluetoothDeviceAddress,
                    res.getString(R.string.exchange_mtu),
                    mtu,
                    status);

            Logger.datalog(dataLog);
        }
    };

    @SuppressLint({"MissingPermission", "NewApi"})
    public static void exchangeGattMtu(int mtu) {

        int retry = 5;
        boolean status = false;
        while (!status && retry > 0) {
            status = mBluetoothGatt.requestMtu(mtu);
            retry--;
        }

        Resources res = mContext.getResources();
        String dataLog = String.format(
                res.getString(R.string.exchange_mtu_request),
                mBluetoothDeviceName,
                mBluetoothDeviceAddress,
                res.getString(R.string.exchange_mtu),
                mtu,
                status ? 0x00 : 0x01);

        Logger.datalog(dataLog);
    }


//    private static void timeStamp(String log) {
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//        Long tsLong = System.currentTimeMillis() / 1000;
//        String ts = tsLong.toString();
//        String format = sdf.format(new Date());
//        Logger.datalog(log + ts + " TIME " + format);
//    }

    private final IBinder mBinder = new LocalBinder();
    /**
     * Flag to check the mBound status
     */
    public boolean mBound;
    /**
     * BlueTooth manager for handling connections
     */
    private BluetoothManager mBluetoothManager;

    public static String getmBluetoothDeviceAddress() {
        return mBluetoothDeviceAddress;
    }

    public static String getmBluetoothDeviceName() {
        return mBluetoothDeviceName;
    }

    private static void broadcastConnectionUpdate(final String action) {
        Logger.i("action :" + action);
        final Intent intent = new Intent(action);
        intent.setPackage(mContext.getPackageName());
        mContext.sendBroadcast(intent);
    }

    private static void broadcastWritwStatusUpdate(final String action) {
        final Intent intent = new Intent((action));
        mContext.sendBroadcast(intent);
    }

    private static void broadcastNotifyUpdate(final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);
        Bundle mBundle = new Bundle();
        // Putting the byte value read for GATT Db
        mBundle.putByteArray(Constants.EXTRA_BYTE_VALUE,
                characteristic.getValue());
        mBundle.putString(Constants.EXTRA_BYTE_UUID_VALUE,
                characteristic.getUuid().toString());
        mBundle.putInt(Constants.EXTRA_BYTE_INSTANCE_VALUE,
                characteristic.getInstanceId());
        mBundle.putString(Constants.EXTRA_BYTE_SERVICE_UUID_VALUE,
                characteristic.getService().getUuid().toString());
        mBundle.putInt(Constants.EXTRA_BYTE_SERVICE_INSTANCE_VALUE,
                characteristic.getService().getInstanceId());


        //case for OTA characteristic received
        if (characteristic.getUuid().equals(UUIDDatabase.UUID_OTA_UPDATE_CHARACTERISTIC)) {
            Intent mIntentOTA = new Intent(BroadCast.ACTION_OTA_DATA_AVAILABLE);
            mIntentOTA.putExtras(mBundle);
            mContext.sendBroadcast(mIntentOTA);
        }

        // Battery level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_BATTERY_LEVEL)) {
            mBundle.putString(Constants.EXTRA_BTL_VALUE,
                    Utils.getBatteryLevel(characteristic));
        }

        // Alert level read value
        else if (characteristic.getUuid().equals(UUIDDatabase.UUID_ALERT_LEVEL)) {
            mBundle.putString(Constants.EXTRA_ALERT_VALUE,
                    Utils.getAlertLevel(characteristic));
        }
        // Transmission power level read value
        else if (characteristic.getUuid()
                .equals(UUIDDatabase.UUID_TRANSMISSION_POWER_LEVEL)) {
            mBundle.putInt(Constants.EXTRA_POWER_VALUE,
                    Utils.getTransmissionPower(characteristic));
        }


        intent.putExtras(mBundle);
        /**
         * Sending the broad cast so that it can be received on registered
         * receivers
         */

        mContext.sendBroadcast(intent);
    }

    private static void onOtaExitBootloaderComplete(int status) {
        Bundle bundle = new Bundle();
        bundle.putByteArray(Constants.EXTRA_BYTE_VALUE, new byte[]{(byte) status});
        Intent intentOTA = new Intent(BroadCast.ACTION_OTA_DATA_AVAILABLE);
        intentOTA.putExtras(bundle);
        mContext.sendBroadcast(intentOTA);
    }

    /**
     * Connects to the GATT server hosted on the BlueTooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public static void connect(final String address, final String devicename, Context context) {
        mContext = context;
        if (mBluetoothAdapter == null || address == null) {
            return;
        }

        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            return;
        }

        // Previously connected device.  Try to reconnect.
        if(mBluetoothDeviceAddress!=null) {
            // Previously connected device. Try to reconnect. if (address.equals(bluetoothDeviceAddress)
            if (address.equals(mBluetoothDeviceAddress)&& mBluetoothGatt != null) {
                Log.d("TAG", "Trying to use an existing mBluetoothGatt for connection.");
                mBluetoothGatt.connect();
            }
        }


        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(context,
                    false, mGattCallback, TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(context,
                    false, mGattCallback);
        }


       // mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        //Clearing Bluetooth cache before disconnecting to the device
        /*
        if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_PAIR_CACHE_STATUS)) {
            //Logger.e(getActivity().getClass().getName() + "Cache cleared on disconnect!");
            BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
        }
         */
        mBluetoothDeviceAddress = address;
        mBluetoothDeviceName = devicename;
        /**
         * Adding data to the data logger
         */
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + devicename + "|" + address + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.datalog(dataLog);
    }

    /**
     * Reconnect method to connect to already connected device
     */
    @SuppressLint("MissingPermission")
    public static void reconnect() {
        Logger.e("<--Reconnecting device-->");
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            return;
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        Log.i("ProfileScan", "reconnect");
        mBluetoothGatt = null;//Creating a new instance of GATT before connect
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        /**
         * Adding data to the data logger
         */
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.datalog(dataLog);
    }

    /**
     * Reconnect method to connect to already connected device
     */
    @SuppressLint("MissingPermission")
    public static void reDiscoverServices() {
        Logger.e("<--Rediscovering services-->");
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        if (device == null) {
            return;
        }
        /**
         * Disconnecting the device
         */
        if (mBluetoothGatt != null)
            mBluetoothGatt.disconnect();

        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback);
        /**
         * Adding data to the data logger
         */
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                mContext.getResources().getString(R.string.dl_connection_request);
        Logger.datalog(dataLog);
    }

    /**
     * Method to clear the device cache
     *
     * @param gatt
     * @return boolean
     */
    public static boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            Method localMethod = gatt.getClass().getMethod("refresh");
            return (Boolean) localMethod.invoke(gatt);
        } catch (Exception localException) {
            Logger.i("An exception occured while refreshing device");
        }
        return false;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The
     * disconnection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @SuppressLint("MissingPermission")
    public static void disconnect() {
        Logger.i("disconnect called");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            //Clearing Bluetooth cache before disconnecting to the device
            if (Utils.getBooleanSharedPreference(mContext, Constants.PREF_PAIR_CACHE_STATUS)) {
                //Logger.e(getActivity().getClass().getName() + "Cache cleared on disconnect!");
                BluetoothLeService.refreshDeviceCache(BluetoothLeService.mBluetoothGatt);
            }
            mBluetoothGatt.disconnect();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_disconnection_request);
            Logger.datalog(dataLog);
            close();
        }

    }

    public static boolean discoverServices() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return false;
        } else {
            @SuppressLint("MissingPermission") boolean result = mBluetoothGatt.discoverServices();
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator)
                    + "[" + mBluetoothDeviceName + "|" + mBluetoothDeviceAddress + "] " +
                    mContext.getResources().getString(R.string.dl_service_discovery_request);
            return result;
        }
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    @SuppressLint("MissingPermission")
    public static void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Logger.datalog(dataLog);
    }

    /**
     * Request a read on a given {@code BluetoothGattDescriptor }.
     *
     * @param descriptor The descriptor to read from.
     */
    @SuppressLint("MissingPermission")
    public static void readDescriptor(BluetoothGattDescriptor descriptor) {
        String serviceUUID = descriptor.getCharacteristic().getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getService().getUuid(), serviceUUID);

        String characteristicUUID = descriptor.getCharacteristic().getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(descriptor.getCharacteristic().getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        //Logger.datalog(mContext.getResources().getString(R.string.dl_descriptor_read_request));
        mBluetoothGatt.readDescriptor(descriptor);
        String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                "[" + serviceName + "|" + characteristicName + "] " +
                mContext.getResources().getString(R.string.dl_characteristic_read_request);
        Logger.datalog(dataLog);
    }

    /**
     * Request a write with no response on a given
     * {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray      to write
     */
    @SuppressLint("MissingPermission")
    public static void writeCharacteristicNoresponse(BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);

        }
    }

    public static void writeOTABootLoaderCommand(
            BluetoothGattCharacteristic characteristic,
            byte[] value,
            boolean isExitBootloaderCmd) {

        synchronized (mGattCallback) {
            writeOTABootLoaderCommand(characteristic, value);
            if (isExitBootloaderCmd)
                mOtaExitBootloaderCmdInProgress = true;
        }
    }

    @SuppressLint("MissingPermission")
    public static void writeOTABootLoaderCommand(BluetoothGattCharacteristic characteristic, byte[] value) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(value);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = value;
            characteristic.setValue(valueByte);
            int counter = 20;
            boolean status;
            do {
                int i = 0;
                characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                status = mBluetoothGatt.writeCharacteristic(characteristic);
                if (!status) {
                    Log.v("CYSMART", "writeCharacteristic() status: False");
                    try {
                        Log.v("CYSMART OTA SLEEP>>>>", "" + i);
                        i++;
                        Thread.sleep(100, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!status && (counter-- > 0));


            if (status) {
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                        mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[ " + characteristicValue + " ]";
                Logger.datalog(dataLog);
                Log.v("CYSMART", dataLog);

                //timeStamp("OTA WRITE TIMESTAMP ");

            } else {
                Log.v("CYSMART", "writeOTABootLoaderCommand failed!");
            }
        }

    }

    private static String getHexValue(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (byte byteChar : array) {
            sb.append(String.format("%02x", byteChar));
        }
        return "" + sb;
    }

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param characteristic
     * @param byteArray
     */

    @SuppressLint("MissingPermission")
    public static void writeCharacteristicGattDb(BluetoothGattCharacteristic characteristic, byte[] byteArray) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String characteristicValue = Utils.ByteArraytoHex(byteArray);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = byteArray;
            characteristic.setValue(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);
        }

    }

    /**
     * Writes the characteristic value to the given characteristic.
     *
     * @param characteristic the characteristic to write to
     * @return true if request has been sent
     */
    @SuppressLint("MissingPermission")
    public static final boolean writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        final BluetoothGatt gatt = mBluetoothGatt;
        if (gatt == null || characteristic == null)
            return false;

        // Check characteristic property
        final int properties = characteristic.getProperties();
        if ((properties & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0)
            return false;

        //Logger.v("Writing characteristic " + characteristic.getUuid());
        Logger.d("gatt.writeCharacteristic(" + characteristic.getUuid() + ")");
        return gatt.writeCharacteristic(characteristic);
    }


    /**
     * Request a write on a given {@code BluetoothGattCharacteristic} for RGB.
     *
     * @param characteristic
     * @param red
     * @param green
     * @param blue
     * @param intensity
     */
    @SuppressLint("MissingPermission")
    public static void writeCharacteristicRGB(BluetoothGattCharacteristic characteristic, int red, int green,
                                              int blue, int intensity) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        } else {
            byte[] valueByte = new byte[4];
            valueByte[0] = (byte) red;
            valueByte[1] = (byte) green;
            valueByte[2] = (byte) blue;
            valueByte[3] = (byte) intensity;
            characteristic.setValue(valueByte);
            String characteristicValue = Utils.ByteArraytoHex(valueByte);
            mBluetoothGatt.writeCharacteristic(characteristic);
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_write_request) +
                    mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[ " + characteristicValue + " ]";
            Logger.datalog(dataLog);

        }

    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    @SuppressLint("MissingPermission")
    public static void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(), serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(), characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }
        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled == true) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);

            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_notification);
            Logger.datalog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_notification);
            Logger.datalog(dataLog);
        }

    }

    /**
     * Enables or disables indications on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable indications. False otherwise.
     */
    @SuppressLint("MissingPermission")
    public static void setCharacteristicIndication(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String serviceName = GattAttributes.lookupUUID(characteristic.getService().getUuid(),
                serviceUUID);

        String characteristicUUID = characteristic.getUuid().toString();
        String characteristicName = GattAttributes.lookupUUID(characteristic.getUuid(),
                characteristicUUID);

        String descriptorUUID = GattAttributes.CLIENT_CHARACTERISTIC_CONFIG;
        String descriptorName = GattAttributes.lookupUUID(UUIDDatabase.
                UUID_CLIENT_CHARACTERISTIC_CONFIG, descriptorUUID);
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            return;
        }

        if (characteristic.getDescriptor(UUID
                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" +
                        descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.
                        ENABLE_INDICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(descriptor);
                String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + serviceName + "|" + characteristicName + "|" + descriptorName + "] " +
                        mContext.getResources().getString(R.string.dl_characteristic_write_request)
                        + mContext.getResources().getString(R.string.dl_commaseparator) +
                        "[" + Utils.ByteArraytoHex(BluetoothGattDescriptor.
                        DISABLE_NOTIFICATION_VALUE) + "]";
                Logger.datalog(dataLog);
            }
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        if (enabled) {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_start_indication);
            Logger.datalog(dataLog);
        } else {
            String dataLog = mContext.getResources().getString(R.string.dl_commaseparator) +
                    "[" + serviceName + "|" + characteristicName + "] " +
                    mContext.getResources().getString(R.string.dl_characteristic_stop_indication);
            Logger.datalog(dataLog);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public static List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;

        return mBluetoothGatt.getServices();
    }

    public static int getConnectionState() {
        synchronized (mGattCallback) {
            return mConnectionState;
        }
    }

    @SuppressLint("MissingPermission")
    public static boolean getBondedState() {
        Boolean bonded;
        BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(mBluetoothDeviceAddress);
        bonded = device.getBondState() == BluetoothDevice.BOND_BONDED;
        return bonded;
    }

    public static void bondDevice() {
        try {
            Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
            Method createBondMethod = class1.getMethod("createBond");
            Boolean returnValue = (Boolean) createBondMethod.invoke(mBluetoothGatt.getDevice());
            Logger.e("Pair initates status-->" + returnValue);
        } catch (Exception e) {
            Logger.e("Exception Pair" + e.getMessage());
        }

    }

    public static void addRemoveData(BluetoothGattDescriptor descriptor) {
        switch (descriptor.getValue()[0]) {
            case 0:
                //Disabled notification and indication
                removeEnabledCharacteristic(descriptor.getCharacteristic());
                Logger.e("Removed characteristic");
                break;
            case 1:
                //Enabled notification
                Logger.e("added notify characteristic");
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
            case 2:
                //Enabled indication
                Logger.e("added indicate characteristic");
                addEnabledCharacteristic(descriptor.getCharacteristic());
                break;
        }
    }

    public static void addEnabledCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (!mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.add(bluetoothGattCharacteristic);
    }

    public static void removeEnabledCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (mEnabledCharacteristics.contains(bluetoothGattCharacteristic))
            mEnabledCharacteristics.remove(bluetoothGattCharacteristic);
    }

    public static void disableAllEnabledCharacteristics() {
        if (!mEnabledCharacteristics.isEmpty()) {
            mDisableNotificationFlag = true;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = mEnabledCharacteristics.
                    get(0);
            Logger.e("Disabling characteristic--" + bluetoothGattCharacteristic.getUuid());
            setCharacteristicNotification(bluetoothGattCharacteristic, false);
        } else {
            mDisableNotificationFlag = false;
        }

    }

    public static void enableAllRDKCharacteristics() {
        if (!mRDKCharacteristics.isEmpty()) {
            mEnableRDKNotificationFlag = true;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = mRDKCharacteristics.
                    get(0);
            Logger.e("enabling characteristic--" + bluetoothGattCharacteristic.getInstanceId());
            setCharacteristicNotification(bluetoothGattCharacteristic, true);
        } else {
            Logger.e("All RDK Chara enabled");
            mEnableRDKNotificationFlag = false;
            broadcastWritwStatusUpdate(ACTION_WRITE_COMPLETED);
        }

    }

    public static void enableAllGlucoseCharacteristics() {
        if (!mGlucoseCharacteristics.isEmpty()) {
            mEnableGlucoseFlag = true;
            BluetoothGattCharacteristic bluetoothGattCharacteristic = mGlucoseCharacteristics.
                    get(0);
            Logger.e("enabling characteristic--" + bluetoothGattCharacteristic);
            if (bluetoothGattCharacteristic.getUuid().equals(UUIDDatabase.UUID_RECORD_ACCESS_CONTROL_POINT)) {
                setCharacteristicIndication(bluetoothGattCharacteristic, true);
                Logger.e("RACP Indicate");
            } else {
                setCharacteristicNotification(bluetoothGattCharacteristic, true);
            }
        } else {
            Logger.e("All Gluocse Char enabled");
            mEnableGlucoseFlag = false;
            broadcastWritwStatusUpdate(ACTION_WRITE_COMPLETED);
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure
     * resources are released properly.
     */
    @SuppressLint("MissingPermission")
    public static void close() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        mBluetoothGatt = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        mBound = true;
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mBound = false;
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local BlueTooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        // through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        return mBluetoothAdapter != null;

    }

    @Override
    public void onCreate() {
        // Initializing the service
        if (!initialize()) {
            Logger.d("Service not initialized");
        }
    }

    /**
     * Local binder class
     */
    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public static boolean unpairDevice(BluetoothDevice device) {
        try {
            //Logger.i("Un-Pair status: " + rv);
            return (Boolean) invokeBluetoothDeviceMethod(device, "removeBond");
        } catch (Exception e) {
            //Logger.e("Un-Pair: exception: " + e.getMessage());
            return false;
        }
    }

    private static Object invokeBluetoothDeviceMethod(BluetoothDevice dev, String methodName, Object... args)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        //Android 4.4 API 19 以上才开放Bond接口
        Class<? extends BluetoothDevice> c = dev.getClass();
        Method m = c.getMethod(methodName);
        m.setAccessible(true);
        return m.invoke(dev, args);
    }

    public boolean pairDevice(BluetoothDevice device) {
        try {
            // TODO: use BluetoothDevice.createBond() public method
            return (Boolean) invokeBluetoothDeviceMethod(device, "createBond");
            //Logger.i("Pair status: " + rv);
        } catch (Exception e) {
            //Logger.e("Pair: exception: " + e.getMessage());
            return false;
        }
    }







}