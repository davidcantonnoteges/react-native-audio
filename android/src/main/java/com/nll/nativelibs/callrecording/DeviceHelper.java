package com.nll.nativelibs.callrecording;

import android.os.Build;
import android.util.Log;

import java.util.Locale;

public class DeviceHelper {
    private static String TAG = "DeviceHelper";

    /**
     * You might want to return false for Note 8 Global version s it does not have any issues
     *
     * @return
     */
    public static boolean isAndroid71FixRequired() {
        return (Build.VERSION.RELEASE.equals("7.1.1") || Build.VERSION.RELEASE.equals("7.1.2"));
    }
    public static int getNativeCPUCommand() {
        String hardwareUpperCase = Build.HARDWARE.toUpperCase(Locale.ENGLISH);
        String manufacturerUpperCase = getDeviceManufacturer();
        String deviceModelUpperCase = getDeviceModel();

        if (Build.BOARD.equalsIgnoreCase("universal9810") ||
                Build.BOARD.equalsIgnoreCase("universal8895") ||
                Build.BOARD.equalsIgnoreCase("universal8890") ||
                Build.BOARD.equalsIgnoreCase("universal7880") ||
                hardwareUpperCase.equals("KIRIN970") ||
                hardwareUpperCase.equals("HI6250")
                ) {
            return 5;

        } else if (hardwareUpperCase.startsWith("MT67")) {
            // Had a user with Lenovo K8 Note (mt6797) on Android 8.0.0 (50 - 2018-05-05). 5 caused -38 error and was not possible to init native AudioRecord. 7 worked fine
            // Also had a user with Nokia X5 mt6771 and Android 8.1 where ACR crashes with 5
            if (manufacturerUpperCase.equals("LENOVO") || manufacturerUpperCase.equals("HMD GLOBAL")) {
                return 7;
            } else {
                return 5;
            }

        } else {
            if (hardwareUpperCase.equalsIgnoreCase("qcom")) {



                if (manufacturerUpperCase.equals("SAMSUNG")) {
                    if (Build.VERSION.SDK_INT == 25) {
                        //6 might work too. But, I don't have Samsung Android 7.1.1 with qcomm to test. I had a user test with 7
                        return 7;
                    } else {
                        return 6;
                    }
                }
                if (manufacturerUpperCase.equals("SONY")) {
                    if(deviceModelUpperCase.equalsIgnoreCase("G8441") || deviceModelUpperCase.equalsIgnoreCase("G8341")) {
                        return 8; // tested on Sony Xperia XZ1 compact (G8441) and ony Xperia XZ1 (G8341)
                    }
                }
                if (manufacturerUpperCase.equals("MOTOROLA") && !isNexus()) {
                    //there is actually no need to check !isNexus() as we are already checking it at useNativeAudioRecord
                    return 7; //8 seems to work too
                }



            }
        }

        return 7; // return 7 as it might work on all
    }

    public static boolean isBlackListedFromNativeAudioRecord() {
        String manufacturerUpperCase = getDeviceManufacturer();
        String deviceModelUpperCase = getDeviceModel();
        boolean isSamsungTablet = manufacturerUpperCase.equals("SAMSUNG") && deviceModelUpperCase.startsWith("SM-T8");// Tab s3 (SM-T825). Native Audio record does not work

        return isSamsungTablet;
    }

    public static boolean useNativeAudioRecord() {
        //first process black listed devices
        if (isBlackListedFromNativeAudioRecord())
            return false;


        boolean isOreoOROreoMR1 = Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27;
        boolean isAndroid711 = Build.VERSION.SDK_INT == 25;
        String motherBoardNoCase = Build.BOARD;
        String manufacturerUpperCase = getDeviceManufacturer();
        String hardwareUpperCase = Build.HARDWARE.toUpperCase(Locale.ENGLISH);
        String deviceModelUpperCase = getDeviceModel();

        //Samsung
        boolean isSamsung = manufacturerUpperCase.equals("SAMSUNG");
        //Exynos
        boolean isS9 = motherBoardNoCase.equalsIgnoreCase("universal9810");
        boolean isNote8OrS8 = motherBoardNoCase.equalsIgnoreCase("universal8895");
        boolean isS7 = motherBoardNoCase.equalsIgnoreCase("universal8890");
        boolean isASeries = motherBoardNoCase.equalsIgnoreCase("universal7880");
        //qcomm
        boolean isQComm = hardwareUpperCase.equalsIgnoreCase("qcom");
        //Samsung 7.1.1 and qcomm
        boolean useNativeForSamsung711WithQComm = isQComm && isSamsung && isAndroid711;
        //Samsung other
        boolean useNativeForSamsung = isOreoOROreoMR1 && isSamsung && (isS9 || isNote8OrS8 || isS7 || isASeries || isQComm);


        //Motorola
        boolean useNativeForMotorolaQComAndroid8With = manufacturerUpperCase.equals("MOTOROLA") && isQComm && isOreoOROreoMR1 && !isNexus();


        //Huawei. Tested on kirin970 Mate 10 pro and P20 pro
        boolean isHuawei = manufacturerUpperCase.equals("HUAWEI") && !isNexus();
        boolean isKirin970 = hardwareUpperCase.equals("KIRIN970");
        boolean isKirinHi6250 = hardwareUpperCase.equals("HI6250");
        boolean useNativeForHuawei = isOreoOROreoMR1 && isHuawei && (isKirin970 || isKirinHi6250);


        //All MediaTek devices seems to be fine even on android 7.1 works fine: GeneralMobile GM6 mt6737, Moto E (4) Plus mt6735, ITEL I375 mt6735, BLACKVIEW BV9000Pro-F mt6757, BLU Vivo X mt6757 etc
        boolean isMediaTek67Series = hardwareUpperCase.startsWith("MT67");
        boolean useNativeMediaTekAndroid7and8 = isMediaTek67Series && (isOreoOROreoMR1 || isAndroid711);


        //Sony Android 8. Tested on Sony Xperia XZ1 compact (G8441) and ony Xperia XZ1 (G8341)
        boolean useNativeForSonyQComAndroid8 = manufacturerUpperCase.equals("SONY") && isQComm && isOreoOROreoMR1 && (deviceModelUpperCase.equalsIgnoreCase("G8441") || deviceModelUpperCase.equalsIgnoreCase("G8341"));

        //Hold for now.There aren't many issues with LG android 8
        // ---- CPU CODE 7 WORKS ON LG V30 (tested y user) ----
        //LG, JOAN is LG V30. Hardware name is different on every model on LGE.
        //boolean isLGQComm = manufacturerUpperCase.equals("LGE") && (hardwareUpperCase.equals("JOAN") || hardwareUpperCase.equals("LUCYE"));
        //boolean useNativeForLG = isLGQComm && !isNexus() && isOreoOROreoMR1;


        return useNativeForSamsung || useNativeForSamsung711WithQComm || useNativeForHuawei || useNativeMediaTekAndroid7and8 || useNativeForMotorolaQComAndroid8With || useNativeForSonyQComAndroid8;

    }
    public static boolean mustUseApi3() {
        return isHuaweiWithApi3() || isMediaTekCPUAndAndroid8OrAbove() || isMotorolaWithApi3();
    }

    private static boolean isMediaTekCPUAndAndroid8OrAbove() {
        return Build.VERSION.SDK_INT >= 26 && Build.HARDWARE.toUpperCase(Locale.ENGLISH).startsWith("MT");
    }

    private static boolean isHuaweiAndroid8AndAbove() {
        return getDeviceManufacturer().equals("HUAWEI") && Build.VERSION.SDK_INT >= 26;
    }

    private static boolean isHuaweiWithApi3() {
        //always 10 plus on Android 8 and above
        if (isHuaweiAndroid8AndAbove()) {
            return true;
        } else {
            //check for others
            if (getDeviceManufacturer().equals("HUAWEI")) {
                //Check ro.build.hw_emui_api_level property and call it only if it's 10 or higher (emui 4.1+)
                int emuiVersion = PropManager.getInt("ro.build.hw_emui_api_level");
                Log.d(TAG, "emuiVersion: " + emuiVersion);
                return emuiVersion >= 10;
            } else {
                return false;
            }
        }
    }

    public static void sleepForAndroid71() {
        Log.d(TAG, "Android 7.1.1 requires more delay and extra sleep. Sometimes delay does not help either. It seems to be issue with OnePlus and Sony 7.1.1");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private static boolean isMotorolaWithApi3() {
        //motorola move to api 3 on android 8
        return getDeviceManufacturer().equals("MOTOROLA") && Build.VERSION.SDK_INT >= 26 && !isNexus();

    }

    private static boolean isNexus() {
        return getDeviceModel().contains("NEXUS");
    }

    private static String getDeviceModel() {
        try {
            String model = Build.MODEL;
            return model.toUpperCase(Locale.ENGLISH);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getDeviceManufacturer() {
        try {
            String manufacturer = Build.MANUFACTURER;
            return manufacturer.toUpperCase(Locale.ENGLISH);

        } catch (Exception e) {
            return "";
        }
    }
}
