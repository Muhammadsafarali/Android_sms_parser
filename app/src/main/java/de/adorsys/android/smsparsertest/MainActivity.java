/*
 * Copyright (C) 2017 adorsys GmbH & Co. KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.adorsys.android.smsparsertest;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import de.adorsys.android.smsparsertest.sql.SmsContentProvider;
import de.adorsys.android.smsparsertest.sql.SmsTable;

public class MainActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>  {

    private static final String pref_key_number = "ru.soyer.tom.sms_parser.number";
    private static final String pref_key_msg = "ru.soyer.tom.sms_parser.msg";

    private SimpleCursorAdapter adapter;

    @NonNull
    private TextView smsSenderTextView;
    @NonNull
    private TextView smsMessageTextView;
    @NonNull
    private LocalBroadcastManager localBroadcastManager;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SmsReceiver.INTENT_ACTION_SMS)) {
                String receivedSender = intent.getStringExtra(SmsReceiver.KEY_SMS_SENDER);
                String receivedMessage = intent.getStringExtra(SmsReceiver.KEY_SMS_MESSAGE);
                if (receivedSender != null) {
//                    PrefUtils.getEditor(context).putString(pref_key_number, receivedSender);
//                    smsSenderTextView.setText(getString(R.string.text_sms_sender_number,
//                            receivedSender != null ? receivedSender : "NO NUMBER"));
                }
                if (receivedMessage != null) {
                    saveSms(receivedMessage);
//                    PrefUtils.getEditor(context).putString(pref_key_msg, receivedMessage);
//                    smsMessageTextView.setText(getString(R.string.text_sms_message,
//                            receivedMessage != null ? receivedMessage : "NO MESSAGE"));
                }
            }
        }
    };


    private void saveSms(String sms_body) {
        Date now = new Date();
        long now_long = now.getTime();

        ContentValues values = new ContentValues();
        values.put(SmsTable.COLUMN_DATE, now_long);
        values.put(SmsTable.COLUMN_TEXT, sms_body);

        getContentResolver().insert(SmsContentProvider.CONTENT_URI, values);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == SmsTool.REQUEST_CODE_ASK_PERMISSIONS
                && (grantResults.length <= 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
            Toast.makeText(this, getString(R.string.warning_permission_not_granted),
                    Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getApplicationContext().getPackageName())));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sms_list);

//        if (savedInstanceState != null) {
//            smsSenderTextView.setText(savedInstanceState.getString(pref_key_number, ""));
//            smsMessageTextView.setText(savedInstanceState.getString(pref_key_msg, ""));
//        }

        SmsConfig.INSTANCE.initializeSmsConfig(
                "BEGIN-MESSAGE",
                "END-MESSAGE",
                "89282430378",
                "89773777541",
                "+79773777541",
                "+79282430378");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            SmsTool.requestSMSPermission(this);
        }

//        initViews();

        fillData();
    }

    private void fillData() {
        String[] from = new String[] {SmsTable.COLUMN_DATE, SmsTable.COLUMN_TEXT};
        int[] to = new int[] {R.id.smsDate, R.id.smsMessage};

        getLoaderManager().initLoader(0, null, this);
        adapter = new SimpleCursorAdapter(this, R.layout.sms_row, null, from,
                to, 0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (columnIndex == cursor.getColumnIndex(SmsTable.COLUMN_DATE)) {
                    Date d = new Date(cursor.getLong(columnIndex));
                    String formatted = new SimpleDateFormat("dd.MM.yy HH:mm").format(d);
                    ((TextView) view).setText(formatted);
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
    }

    @Override
    protected void onPause() {
        unRegisterReceiver();
        super.onPause();
    }

    @Override
    protected void onResume() {
        registerReceiver();
        super.onResume();
    }

    private void initViews() {
        smsSenderTextView = findViewById(R.id.sms_sender_text_view);
        smsMessageTextView = findViewById(R.id.sms_message_text_view);

        smsSenderTextView.setText(getString(R.string.text_sms_sender_number, ""));
        smsMessageTextView.setText(getString(R.string.text_sms_message, ""));
    }

    private void registerReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsReceiver.INTENT_ACTION_SMS);
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    private void unRegisterReceiver() {
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {SmsTable.COLUMN_ID, SmsTable.COLUMN_DATE, SmsTable.COLUMN_TEXT};
        CursorLoader cursorLoader = new CursorLoader(this, SmsContentProvider.CONTENT_URI,
                projection, null, null, "-" + SmsTable.COLUMN_DATE);
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        adapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        adapter.swapCursor(null);
    }
}