# Crash report upload

CrashReporter can ship the crash reports it captures — both JVM stack traces
(`*_crash.txt` / `*_exception.txt`) and native Breakpad minidumps
(`core_dump/*.dmp`) — to a backend of your choice, and tracks the upload state of
each report locally so nothing is sent twice and failures are retried.

Uploading is **opt-in and disabled by default**: behaviour is unchanged until you
register an uploader and enable it.

## How it works

- **You** provide the transport by implementing `CrashReportUploader`. The library
  stays backend-agnostic and declares no `INTERNET` permission — your app declares
  whatever its uploader needs.
- **The library** owns discovery, state, and retry. On each upload pass it scans the
  crash-report directory, reconciles it with a small local Room database
  (`crashreporter_uploads.db`, in internal storage), and drains everything still
  `PENDING`/`FAILED` through your uploader on a WorkManager background thread.
- State per report: `PENDING → UPLOADING → UPLOADED`, or `FAILED` (retried with
  exponential backoff until `maxUploadAttempts`, default 5).

Reports are **not** uploaded on the crashing thread. A fatal crash is handled on a
dying process, and native minidumps are written entirely in C++ — so uploads happen
on the next launch's background pass (or immediately via `uploadPendingReports()`
when the app is already running). Register your uploader early and unconditionally.

## Usage

```java
public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Register your transport. Called off the main thread; blocking IO is fine.
        // Return true only when the backend has durably accepted the report.
        CrashReporter.setUploader((report, type) -> {
            return MyBackend.upload(report, type.name()); // your HTTP/S3/etc. call
        });

        CrashReporter.setUploadEnabled(true);
    }
}
```

Kotlin:

```kotlin
CrashReporter.setUploader { report, type -> myBackend.upload(report, type) }
CrashReporter.setUploadEnabled(true)
```

Because uploads need the network, make sure your app declares
`<uses-permission android:name="android.permission.INTERNET" />`.

## API (`CrashReporter`)

| Method | Purpose |
|---|---|
| `setUploader(CrashReportUploader)` | Register (or clear with `null`) the transport strategy. |
| `setUploadEnabled(boolean)` | Master switch. Default `false`. Enabling with an uploader schedules a pass. |
| `setDeleteAfterUpload(boolean)` | Delete a report file after a successful upload. Default `true`. |
| `setMaxUploadAttempts(int)` | Attempts before a report is left permanently `FAILED`. Default `5`. |
| `uploadPendingReports()` | Force an upload pass now (subject to network availability). |

`CrashReportUploader.upload(File report, ReportType type)` returns `true` on durable
success; returning `false` or throwing schedules a retry.
