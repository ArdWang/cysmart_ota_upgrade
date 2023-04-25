package github.rnd.upgrade;

public class BroadCast {

    //自定义的
    public final static String ACTION_OTA_DATA_AVAILABLE =
            "com.dt.bluetooth.le.ACTION_OTA_DATA_AVAILABLE";

    public final static String ACTION_DATA_AVAILABLE
            = "com.dt.action.ACTION_DATA_AVAILABLE";

    //数据接受成功
    public final static String ACTION_DATA_RECIVER_AVAILABLE = "com.dt.action.ACTION_DATA_RECIVER_AVAILABLE";

    public static final String EXTRA_CHARACTERISTIC_ERROR_MESSAGE = "com.dt.EXTRA_CHARACTERISTIC_ERROR_MESSAGE";

    public static final String ACTION_GATT_CHARACTERISTIC_ERROR = "com.dt.action.ACTION_GATT_CHARACTERISTIC_ERROR";

    public final static String ACTION_GATT_CONNECTED
            = "com.dt.action.ACTION_GATT_CONNECTED";   //连接

    public final static String ACTION_GATT_DISCONNECTED
            = "com.dt.action.ACTION_GATT_DISCONNECTED";  //断开链接

    public final static String ACTION_GATT_SERVICES_DISCOVERED
            = "com.dt.action.ACTION_GATT_SERVICES_DISCOVERED"; //发现设备




    //OTA的
    public final static String ACTION_GATT_CONNECTING =
            "com.dt.bluetooth.le.ACTION_GATT_CONNECTING";

    public final static String ACTION_GATT_DISCONNECTED_CAROUSEL =
            "com.dt.bluetooth.le.ACTION_GATT_DISCONNECTED_CAROUSEL";
    public final static String ACTION_GATT_DISCONNECTED_OTA =
            "com.dt.bluetooth.le.ACTION_GATT_DISCONNECTED_OTA";
    public final static String ACTION_GATT_CONNECT_OTA =
            "com.dt.bluetooth.le.ACTION_GATT_CONNECT_OTA";
    public final static String ACTION_GATT_SERVICES_DISCOVERED_OTA =
            "com.dt.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED_OTA";
    public final static String ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL =
            "com.dt.bluetooth.le.ACTION_GATT_SERVICE_DISCOVERY_UNSUCCESSFUL";

    //系统的
    //系统的
    public final static String ACTION_WRITE_SUCCESS =
            "android.bluetooth.device.action.ACTION_WRITE_SUCCESS";
    public final static String ACTION_WRITE_FAILED =
            "android.bluetooth.device.action.ACTION_WRITE_FAILED";
    public final static String ACTION_PAIR_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";
    public final static String ACTION_WRITE_COMPLETED =
            "android.bluetooth.device.action.ACTION_WRITE_COMPLETED";
}
