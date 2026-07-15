package com.balsikandar.crashreporter;

import android.content.Context;
import android.content.Intent;

import com.balsikandar.crashreporter.ui.CrashReporterActivity;
import com.balsikandar.crashreporter.upload.CrashReportUploader;
import com.balsikandar.crashreporter.upload.CrashUploadManager;
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
        applicationContext = context;
        setUpExceptionHandler();
        initNative(context);
    }

    public static void initialize(Context context, String crashReportSavePath) {
        applicationContext = context;
        crashReportPath = crashReportSavePath;
        setUpExceptionHandler();
        initNative(context);
    }

    private static void initNative(Context context) {
        String fileDir = resolveCrashReportRoot(context);
        File coreFolder = new File(new File(fileDir), "core_dump");
        if (!coreFolder.exists())
            coreFolder.mkdir();

        CrashReporter.initBreakpad(coreFolder.getAbsolutePath());

        // Set up (deferred, opt-in) upload state tracking for both text reports and
        // native minidumps living under this root. Uploading stays disabled until the
        // host app registers an uploader and enables it.
        CrashUploadManager.setup(context, fileDir);
    }

    private static String resolveCrashReportRoot(Context context) {
        return crashReportPath == null ? CrashUtil.getDefaultPath(context) : crashReportPath;
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

    //Upload APIs

    /**
     * Register the strategy that ships crash reports to your backend. The library handles
     * discovery, state tracking and retry; your implementation only performs the transfer.
     * Register early (e.g. in {@code Application.onCreate}) since the process may be
     * recreated between a crash and its upload. Pass {@code null} to clear.
     */
    public static void setUploader(CrashReportUploader uploader) {
        CrashUploadManager.setUploader(uploader);
    }

    /**
     * Enable/disable uploading. Disabled by default; enabling with an uploader registered
     * schedules a background upload pass (subject to network availability).
     */
    public static void setUploadEnabled(boolean enabled) {
        CrashUploadManager.setUploadEnabled(enabled);
    }

    /** Whether to delete a report file after it uploads successfully. Defaults to {@code true}. */
    public static void setDeleteAfterUpload(boolean delete) {
        CrashUploadManager.setDeleteAfterUpload(delete);
    }

    /** Max upload attempts before a report is left permanently failed. Defaults to 5. */
    public static void setMaxUploadAttempts(int maxAttempts) {
        CrashUploadManager.setMaxAttempts(maxAttempts);
    }

    /** Trigger an upload pass immediately (subject to the network constraint). */
    public static void uploadPendingReports() {
        CrashUploadManager.uploadNow();
    }

}
