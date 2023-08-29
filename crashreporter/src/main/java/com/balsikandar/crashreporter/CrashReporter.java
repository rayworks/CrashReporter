package com.balsikandar.crashreporter;

import android.content.Context;
import android.content.Intent;

import com.balsikandar.crashreporter.ui.CrashReporterActivity;
import com.balsikandar.crashreporter.utils.CrashReporterNotInitializedException;
import com.balsikandar.crashreporter.utils.CrashReporterExceptionHandler;
import com.balsikandar.crashreporter.utils.CrashUtil;

import java.io.File;

public class CrashReporter {

    static {
        System.loadLibrary("breakpad-core");
    }

    public static void initBreakpad(String path){
        initBreakpadNative(path);
    }

    private static native void initBreakpadNative(String path);

    private static Context applicationContext;

    private static String crashReportPath;

    private static boolean isNotificationEnabled = true;

    private CrashReporter() {
        // This class in not publicly instantiable
    }

    public static void initialize(Context context) {
        System.out.println(">>><<< init CrashReporter");

        applicationContext = context;
        setUpExceptionHandler();
        initNative(context);
    }

    private static void initNative(Context context) {
        String fileDir = crashReportPath == null ? CrashUtil.getDefaultPath(context) : crashReportPath;
        File coreFolder = new File(new File(fileDir), "core_dump");
        if (!coreFolder.exists())
            coreFolder.mkdir();

        System.out.println(">>> core dump path : " + coreFolder.getAbsolutePath());
        CrashReporter.initBreakpad(coreFolder.getAbsolutePath());
    }

    public static void initialize(Context context, String crashReportSavePath) {
        applicationContext = context;
        crashReportPath = crashReportSavePath;
        setUpExceptionHandler();
        initNative(context);
    }

    private static void setUpExceptionHandler() {
        if (!(Thread.getDefaultUncaughtExceptionHandler() instanceof CrashReporterExceptionHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(new CrashReporterExceptionHandler());
        }
    }

    public static Context getContext() {
        if (applicationContext == null) {
            try {
                throw new CrashReporterNotInitializedException("Initialize CrashReporter : call CrashReporter.initialize(context, crashReportPath)");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return applicationContext;
    }

    public static String getCrashReportPath() {
        return crashReportPath;
    }

    public static boolean isNotificationEnabled() {
        return isNotificationEnabled;
    }

    //LOG Exception APIs
    public static void logException(Exception exception) {
        CrashUtil.logException(exception);
    }

    public static Intent getLaunchIntent() {
        return new Intent(applicationContext, CrashReporterActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static void disableNotification() {
        isNotificationEnabled = false;
    }

}
