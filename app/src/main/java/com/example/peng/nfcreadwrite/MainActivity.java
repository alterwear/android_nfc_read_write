package com.example.peng.nfcreadwrite;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class MainActivity extends Activity {

    private String TAG = MainActivity.class.getSimpleName();

    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Activity context;

    TextView tvNFCContent;
    private TextView justWritten = null;
    private TextView timeToWrite = null;
    TextView message;
    Button btnWaitToWrite;
    Button btnWrite800;
    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        tvNFCContent = (TextView) findViewById(R.id.nfc_contents);
        timeToWrite = (TextView) findViewById(R.id.time_to_write);
        justWritten = (TextView) findViewById(R.id.just_written);
        message = (TextView) findViewById(R.id.edit_message);
        btnWaitToWrite = (Button) findViewById(R.id.btn_wait_to_write);
        btnWrite800 = (Button) findViewById(R.id.btn_write_800);


        btnWaitToWrite.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Log.d(TAG, "Send msg: " + message.getText());
                  //message = createNdefTextMessage("3");
                  if (message != null) {
                      dialog = new ProgressDialog(MainActivity.this);
                      dialog.setMessage("Tag NFC Tag please");
                      dialog.show();
                  }
              }
          }
        );

        btnWrite800.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "Send msg: " + R.string.Eight_hundo);
                message.setText(R.string.Eight_hundo);
                if (message != null) {
                    dialog = new ProgressDialog(MainActivity.this);
                    dialog.setMessage("Tag NFC Tag please");
                    dialog.show();
                }
            }
        });

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        //readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }

    @Override
    public void onNewIntent (final Intent intent) {
        Log.d( TAG, "onNewIntent");
        readFromIntent(intent);

        //setIntent(intent);// was in original codexpedia code...need to figure out what it does. //todo
        //This might be useful: http://www.codexpedia.com/android/android-nfc-read-and-write-example/
        // this too: https://www.survivingwithandroid.com/2016/01/nfc-tag-writer-android.html


        if (intent == null) return;
        myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.d(TAG, "myTag: " + myTag.toString());

        // next chunk mostly from here: https://www.survivingwithandroid.com/2015/03/android-nfc-app-android-nfc-tutorial.html
        String type = intent.getType();
        String action = intent.getAction();
        Log.d(TAG, "type: " + type + ", action: " + action);

        NdefRecord[] records;
        try {
            records = new NdefRecord[]{ createRecord(message.getText().toString()) };

            NdefMessage message = new NdefMessage(records);
            // Get an instance of Ndef for the tag.
            Ndef ndef = Ndef.get(myTag);

            // Time statistics to return
            long timeNdefWrite = 0;
            long RegTimeOutStart = System.currentTimeMillis();
            if (ndef != null) {
                // Enable I/O
                ndef.connect();
                // Write the message
                Log.d(TAG, "writing message...");

                // Time statistics to return
                timeNdefWrite = 0;
                RegTimeOutStart = System.currentTimeMillis();
                ndef.writeNdefMessage(message);
                Log.d(TAG, "closing connection to tag...");
                ndef.close();
                timeNdefWrite = System.currentTimeMillis() - RegTimeOutStart;
            } else {
                byte[] answer;
                byte[] command;
                MifareUltralight mfu = MifareUltralight.get(myTag);
                if (mfu != null) {
                    mfu.connect();
                    command = new byte[1];
                    command[0] = (byte) 0x60; // GET_VERSION
                    answer = mfu.transceive(command);
                    Log.d(TAG, "answer: " + answer);
                    mfu.close();
                } else {
                    String msg = "The Tag could not be identified or this NFC device does not "
                            + "support the NFC Forum commands needed to access this tag";
                    String title = "Communication failed";
                    showAlert(msg, title);
                }
            }



            dialog.dismiss();
            Toast.makeText(context, "time to write: " + timeNdefWrite, Toast.LENGTH_SHORT).show();
            timeToWrite.setText("time to write: " + timeNdefWrite + "ms");
            NdefMessage[] msgs = new NdefMessage[1];
            msgs[0] = message;
            buildTagViews(msgs, "Just Written: ", justWritten);; // is this going to byte me in teh butt later?
           // justWritten.setText("Successfully wrote: " + new String(message.getRecords()[0].getPayload(), "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onNewIntent( intent );
    }



    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        Log.d(TAG, "readFromIntent called...");
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs, "Previous NFC Content: ", tvNFCContent);
        }
    }
    private void buildTagViews(NdefMessage[] msgs, String text, TextView tv) {
        if (msgs == null || msgs.length == 0) return;

        String msg = "";
//        String tagId = new String(msgs[0].getRecords()[0].getType());
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");

        try {
            // Get the Text
            msg = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        tv.setText(text + msg);
    }


    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(tag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }


    /*
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }
    */

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    private void showAlert(final String message, final String title) {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setMessage(message)
                        .setTitle(title)
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {

                                    }
                                }).show();
            }
        });
    }


    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }
}