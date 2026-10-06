package com.mine.autooffbluetooth;

import android.util.Log;
import java.io.DataOutputStream;
import java.io.IOException;

public class RootUtils {

    private static final String TAG = "RootUtils";
  
    public static boolean requestRootAccess() {
        return executeRootCommand("id");
    }

    public static boolean executeRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Root command failed: " + e.getMessage());
            return false;
        } finally {
            if (os != null) {
                try { os.close(); } catch (IOException ignored) {}
            }
            if (process != null) process.destroy();
        }
    }
}
