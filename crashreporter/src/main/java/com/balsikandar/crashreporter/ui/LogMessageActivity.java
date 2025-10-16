package com.balsikandar.crashreporter.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowInsetsCompat;

import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.balsikandar.crashreporter.R;
import com.balsikandar.crashreporter.utils.AppUtils;
import com.balsikandar.crashreporter.utils.ExtensionsKt;
import com.balsikandar.crashreporter.utils.FileUtils;

import java.io.File;

public class LogMessageActivity extends AppCompatActivity {

    private TextView appInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ExtensionsKt.enableEdgeToEdgeModeCompat(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_message);
        ViewGroup root = findViewById(R.id.topLayout);

        ExtensionsKt.applyWindowInsetsCompat(root, WindowInsetsCompat.Type.statusBars()
                | WindowInsetsCompat.Type.navigationBars()
                | WindowInsetsCompat.Type.displayCutout()
        );

        appInfo = (TextView) findViewById(R.id.appInfo);

        Intent intent = getIntent();
        if (intent != null) {
            String dirPath = intent.getStringExtra("LogMessage");
            File file = new File(dirPath);
            String crashLog = FileUtils.readFromFile(file);
            TextView textView = (TextView) findViewById(R.id.logMessage);
            textView.setText(crashLog);
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        myToolbar.setTitle(getString(R.string.crash_reporter));
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getAppInfo();
    }

    private void getAppInfo() {
        appInfo.setText(AppUtils.getDeviceDetails(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.crash_detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = getIntent();
        String filePath = null;
        if (intent != null) {
            filePath = intent.getStringExtra("LogMessage");
        }

        if (item.getItemId() == R.id.delete_log) {
            if (FileUtils.delete(filePath)) {
                finish();
            }
            return true;
        } else if (item.getItemId() == R.id.share_crash_log) {
            shareCrashReport(filePath);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void shareCrashReport(String filePath) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_TEXT, appInfo.getText().toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            File file = new File(filePath);
            Context context = getApplicationContext();
            Uri imageUri = FileProvider.getUriForFile(context, context.getPackageName()+".fileprovider", file);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
        }
        startActivity(Intent.createChooser(intent, "Share via"));
    }
}
