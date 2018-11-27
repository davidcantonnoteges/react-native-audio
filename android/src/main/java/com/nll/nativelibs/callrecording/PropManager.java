package com.nll.nativelibs.callrecording;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;


public class PropManager {


    public static ArrayList<String> get() {
        return getRuntimeExecResult("/system/bin/getprop");
    }

    public static String get(String key) {
        ArrayList<String> result = getRuntimeExecResult(String.format("/system/bin/getprop %s", key));
        return result.size() > 0 ? result.get(0) : null;
    }

    public static int getInt(String key) {
        return toInt(get(key));
    }

    private static int toInt(String prop) {
        if (prop != null) {
            if (TextUtils.isDigitsOnly(prop))
                return Integer.parseInt(prop);
        }
        return 0;
    }

    private static ArrayList<String> getRuntimeExecResult(String prog) {
        InputStream input = null;
        InputStream error = null;
        Process process;

        ArrayList<String> ret = new ArrayList<String>();
        try {
            process = Runtime.getRuntime().exec(prog);

            input = process.getInputStream();
            error = process.getErrorStream();

            Scanner input_scanner = new Scanner(input).useDelimiter("\\n");
            while (input_scanner.hasNext()) {
                ret.add(input_scanner.next());
            }

            Scanner error_scaner = new Scanner(error).useDelimiter("\\n");
            while (error_scaner.hasNext()) {
                ret.add(error_scaner.next());
            }
        } catch (IOException e1) {
            e1.printStackTrace();

        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (error != null) {
                try {
                    error.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return ret;
    }
}
