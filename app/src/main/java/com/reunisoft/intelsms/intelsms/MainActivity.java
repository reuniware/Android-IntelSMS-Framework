package com.reunisoft.intelsms.intelsms;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "intelsms";
    Button buttonStart, buttonStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        buttonStart = (Button) findViewById(R.id.btnStartServer);
        buttonStop = (Button) findViewById(R.id.btnStopServer);

        buttonStart.setOnClickListener(this);
        buttonStop.setOnClickListener(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
        linearLayout.setBackgroundColor(Color.BLACK);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        wm = null;
        log("IP for server : " + ip);
        //SmsManager smsManager = SmsManager.getDefault();
        //smsManager.sendTextMessage("0763230000", "+33660003000", "sms message", null, null);
    }

    public void onClick(View src) {
        switch (src.getId()) {
            case R.id.btnStartServer:
                Log.d(TAG, "onClick: starting service");
                startService(new Intent(this, MyService.class));
                break;
            case R.id.btnStopServer:
                Log.d(TAG, "onClick: stopping service");
                stopService(new Intent(this, MyService.class));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private int currentTextColor = Color.GREEN;
    public void log(String str) {
        final LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        // Si plus de 1000 lignes dans le log à l'écran alors effacer la zone de log
        if (linearLayout.getChildCount() > 1024) {
            linearLayout.removeAllViews();
        }

        final TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(currentTextColor);
        if (currentTextColor == Color.GREEN) currentTextColor = Color.LTGRAY; else currentTextColor = Color.GREEN;

        textView.setTextSize(12);
        textView.setText(str);
        linearLayout.addView(textView);

        ((ScrollView) findViewById(R.id.scrollView)).post(new Runnable() {
            public void run() {
                ((ScrollView) findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
            }
        });

    }

}
