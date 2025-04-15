/*
 * Copyright Cypress Semiconductor Corporation, 2014-2015 All rights reserved.
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;


/**
 * Update Android 14+
 */

public class Logger {

    private static String mLogTag = "CySmart Android";
    private static boolean mLogflag = true;
    private static File mDataLoggerDirectory;
    private static File mDataLoggerFile;
    private static File mDataLoggerOldFile;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    // Set custom log tag
    public static void setLogTag(String tag) {
        mLogTag = tag;
    }

    public static void d(String message) {
        show(Log.DEBUG, mLogTag, message);
    }

    public static void d(String tag, String message) {
        show(Log.DEBUG, tag, message);
    }

    public static void w(String message) {
        show(Log.WARN, mLogTag, message);
    }

    public static void i(String message) {
        show(Log.INFO, mLogTag, message);
    }

    public static void e(String message) {
        show(Log.ERROR, mLogTag, message);
    }

    public static void v(String message) {
        show(Log.VERBOSE, mLogTag, message);  // Fixed: was using ERROR instead of VERBOSE
    }

    public static void datalog(String message) {
        saveLogData(message);
    }

    private static void show(int type, String tag, String msg) {
        if (!mLogflag) return;

        // Split long messages into chunks
        while (msg.length() > 4000) {
            String chunk = msg.substring(0, 4000);
            msg = msg.substring(4000);
            logChunk(type, tag, chunk);
        }
        logChunk(type, tag, msg);
    }

    private static void logChunk(int type, String tag, String msg) {
        switch (type) {
            case Log.INFO:
                Log.i(tag, msg);
                break;
            case Log.ERROR:
                Log.e(tag, msg);
                break;
            case Log.DEBUG:
                Log.d(tag, msg);
                break;
            case Log.WARN:
                Log.w(tag, msg);
                break;
            case Log.VERBOSE:
                Log.v(tag, msg);
                break;
            case Log.ASSERT:
                Log.wtf(tag, msg);
                break;
        }
    }

    public static void show(Exception exception) {
        if (mLogflag && exception != null) {
            exception.printStackTrace();
        }
    }

    public static boolean enableLog() {
        mLogflag = true;
        return true;
    }

    public static boolean disableLog() {
        mLogflag = false;
        return false;
    }

    public static void createDataLoggerFile(Context context) {
        if (context == null) {
            Log.e(mLogTag, "Context cannot be null");
            return;
        }

        mContext = context.getApplicationContext();

        try {
            // Use app-specific storage for better compatibility with Android 10+
            File externalFilesDir = mContext.getExternalFilesDir(null);
            if (externalFilesDir == null) {
                Log.e(mLogTag, "External storage not available");
                return;
            }

            // Create directory
            mDataLoggerDirectory = new File(externalFilesDir,
                    context.getResources().getString(R.string.dl_directory));

            if (!mDataLoggerDirectory.exists() && !mDataLoggerDirectory.mkdirs()) {
                Log.e(mLogTag, "Failed to create directory: " + mDataLoggerDirectory.getAbsolutePath());
                return;
            }

            // Create file
            String fileName = Utils.GetDate() +
                    context.getResources().getString(R.string.dl_file_extension);
            mDataLoggerFile = new File(mDataLoggerDirectory, fileName);

            if (!mDataLoggerFile.exists()) {
                try {
                    if (!mDataLoggerFile.createNewFile()) {
                        Log.e(mLogTag, "Failed to create file: " + mDataLoggerFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    Log.e(mLogTag, "Error creating file: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            deleteOLDFiles();
        } catch (Exception e) {
            Log.e(mLogTag, "Error in createDataLoggerFile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteOLDFiles() {
        if (mDataLoggerDirectory == null || !mDataLoggerDirectory.exists()) {
            return;
        }

        File[] allFilesList = mDataLoggerDirectory.listFiles();
        if (allFilesList == null) return;

        long cutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);

        for (File currentFile : allFilesList) {
            if (currentFile.lastModified() < cutoff) {
                if (!currentFile.delete()) {
                    Log.w(mLogTag, "Failed to delete old file: " + currentFile.getAbsolutePath());
                }
            }
        }

        if (mContext != null) {
            String oldFileName = Utils.GetDateSevenDaysBack() +
                    mContext.getResources().getString(R.string.dl_file_extension);
            mDataLoggerOldFile = new File(mDataLoggerDirectory, oldFileName);

            if (mDataLoggerOldFile.exists() && !mDataLoggerOldFile.delete()) {
                Log.w(mLogTag, "Failed to delete old file: " + mDataLoggerOldFile.getAbsolutePath());
            }
        }
    }

    private static void saveLogData(String message) {
        if (mDataLoggerDirectory == null || !mDataLoggerDirectory.exists()) {
            Log.e(mLogTag, "Log directory not initialized");
            return;
        }

        // Recreate file if needed
        if (mContext != null) {
            String fileName = Utils.GetDate() +
                    mContext.getResources().getString(R.string.dl_file_extension);
            mDataLoggerFile = new File(mDataLoggerDirectory, fileName);
        }

        if (!mDataLoggerFile.exists()) {
            try {
                if (!mDataLoggerFile.createNewFile()) {
                    Log.e(mLogTag, "Failed to create log file");
                    return;
                }
            } catch (IOException e) {
                Log.e(mLogTag, "Error creating log file: " + e.getMessage());
                return;
            }
        }

        message = Utils.GetTimeandDate() + message;

        try (BufferedWriter fbw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(mDataLoggerFile, true),
                        StandardCharsets.UTF_8))) {
            fbw.write(message);
            fbw.newLine();
        } catch (IOException e) {
            Log.e(mLogTag, "Error writing to log file: " + e.getMessage());
        }
    }
}
