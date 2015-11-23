package com.reunisoft.intelsms.intelsms;

/**
 * Created by Idjed on 22/11/2015.
 */
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.format.Formatter;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

//import android.media.MediaPlayer;

public class MyService extends Service {
    private static final String TAG = "intelsms-service";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "IntelSMS Service Started", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "IntelSMS Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy");
        enabled = false;
        try{
            if (socket !=null) socket.close();
            Log.d(TAG,"socket closed");
        }catch (IOException e){}
        try{
            if (serverSocket !=null) serverSocket.close();
            Log.d(TAG, "server socket closed");
        }catch (IOException e){}
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        Log.d(TAG, "onStart");
        enabled = true;
        startServer();
        return START_NOT_STICKY;
    }

    boolean enabled = true;
    ServerSocket serverSocket = null;
    Socket socket = null;
    public void startServer() {
        Log.d(TAG,"startServer");
        log("Starting server");

        UUID uuid = UUID.randomUUID();
        final String sessionId = uuid.toString().replace("-", "");
        Log.d(TAG, "Session ID = " + sessionId);

        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        wm = null;
        Log.d(TAG, "IP=" + ip);
        if (ip.equals("0.0.0.0"))
        {
            Log.d(TAG, "Cannot start server because no WIFI IP address.");
            return;
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while(enabled==true)
                {
                    try {
                        if (serverSocket == null) {
                            serverSocket = new ServerSocket(7777);
                        }

                        serverSocket.setReuseAddress(true);

                        if (socket == null)
                            socket = serverSocket.accept();

                        if (socket != null) {
                            socket.close();
                            socket = serverSocket.accept();
                        }

                        Log.d(TAG, "client connected");

                        InputStream inputStream = socket.getInputStream();
                        String strFromClient = "";
                        int available = inputStream.available();
                        Log.d(TAG, "available from client:" + available);
                        for (int i=0;i<available;i++){
                            int c = inputStream.read();
                            strFromClient+=(char)c;
                        }

                        Log.d(TAG, "received from client:" + strFromClient);

                        OutputStream outputStream = socket.getOutputStream();
                        if (strFromClient.toLowerCase().trim().equals("hello from client")){
                            String strToClient = "hello from server";
                            outputStream.write(strToClient.getBytes());
                            outputStream.flush();
                            Log.d(TAG, "sent to client:" + strToClient);
                        }
                        else if (strFromClient.toLowerCase().trim().equals("get sms")){
                            String sms = getSmsMessages();
                            outputStream.write(sms.getBytes());
                            outputStream.flush();
                            Log.d(TAG, "bytes sent to client:" + sms.length());
                        }
                        else if (strFromClient.toLowerCase().trim().equals("get contacts")){
                            String contacts = getContacts();
                            outputStream.write(contacts.getBytes());
                            outputStream.flush();
                            Log.d(TAG, "bytes sent to client:" + contacts.length());
                        }

                        outputStream.close();
                        inputStream.close();

                        Log.d(TAG, "end of server processing");
                    } catch (Exception e) {
                        Log.d(TAG, "001:" + e.toString());
                    }
                }
            }
        });
        t.start();
    }

    public String getSmsMessages(){
        try {
            // public static final String INBOX = "content://sms/inbox";
            // public static final String SENT = "content://sms/sent";
            // public static final String DRAFT = "content://sms/draft";
            Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, null);
            String msgData = "", colName="", value="";
            assert cursor != null;
            String encoded = "", date="";
            if (cursor.moveToFirst()) { // must check the result to prevent exception
                do {
                    for (int idx = 0; idx < cursor.getColumnCount(); idx++) {
                        colName = cursor.getColumnName(idx);
                        value = cursor.getString(idx);

                        msgData += " " + colName + ":";

                        try {
                            if (!colName.contains("date")) {
                                encoded = Base64.encodeToString(value.getBytes(), Base64.DEFAULT);
                                msgData += encoded;
                            }
                            else if (colName.contains("date")){
                                if (!value.contains("null")) {
                                    date = convertMillisecondsToDate(value);
                                    encoded = Base64.encodeToString(date.getBytes(), Base64.DEFAULT);
                                    msgData += encoded;
                                }
                            }
                        } catch (Exception ex){
                            msgData += value;
                        }
                    }
                    // use msgData
                } while (cursor.moveToNext());
            } else {
                // empty box, no SMS
            }
            cursor.close();
            return msgData;
        }catch (Exception ex){
            Log.d(TAG, ex.toString());
        }
        return "";
    }

    private String convertMillisecondsToDate(String value) {
        String finalDateString = "";
        try {
            long milliSeconds = Long.parseLong(value);
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(milliSeconds);
            finalDateString = formatter.format(calendar.getTime());
            Log.d(TAG, "date parsed =" + finalDateString);
        }
        catch(Exception pe) {
            Log.d(TAG, "date parsing exception:" + pe.toString());
        }
        return finalDateString;
    }

    public String getContacts(){
        String finalResult = "";
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
        String contactData="", colName="", value="";
        while (cursor.moveToNext())
        {
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            finalResult += name + ";" + phoneNumber + "\r\n";
        }
        cursor.close();
        return finalResult;
    }

    private int currentTextColor = Color.GREEN;
    public void log(final String str) {
        final LinearLayout linearLayout = (LinearLayout) MainActivity.mThis.findViewById(R.id.linearLayout);

        // Si plus de 1000 lignes dans le log à l'écran alors effacer la zone de log
        if (linearLayout.getChildCount() > 1024) {
            linearLayout.post(new Runnable() {
                @Override
                public void run() {
                    ((LinearLayout) MainActivity.mThis.findViewById(R.id.linearLayout)).removeAllViews();
                }
            });
        }

        final TextView textView = new TextView(MainActivity.mThis);

        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.setTextColor(currentTextColor);
                textView.setTextSize(12);
                textView.setText(str);
            }
        });

        if (currentTextColor == Color.GREEN) currentTextColor = Color.LTGRAY; else currentTextColor = Color.GREEN;

        linearLayout.post(new Runnable() {
            @Override
            public void run() {
                linearLayout.addView(textView);
            }
        });

        (MainActivity.mThis.findViewById(R.id.scrollView)).post(new Runnable() {
            public void run() {
                ((ScrollView) MainActivity.mThis.findViewById(R.id.scrollView)).fullScroll(View.FOCUS_DOWN);
            }
        });

    }


}

