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
import java.nio.charset.Charset;

public class MainActivity extends Activity {

    // for reading: https://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278
    // above does it on another thread so seems good.

    private String TAG = MainActivity.class.getSimpleName();

    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";

    private static final int MIFARE_ULTRALIGHT_SIZE_LIMIT = 48; // bytes, 12 pages a 4 bytes
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    Activity context;

    TextView prevContent;
    TextView justRead;
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

        prevContent = (TextView) findViewById(R.id.previous_content);
        justRead = (TextView) findViewById(R.id.just_read);
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

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED); // todo: NDEF discovered and tech discovered.
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };
    }

    @Override
    public void onNewIntent (final Intent intent) {
        Log.d(TAG, "onNewIntent");
        String type = intent.getType();
        String action = intent.getAction();
        Log.d(TAG, "type: " + type + ", action: " + action);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            if (message.getText().toString().equals("")) {
                // no message to write, so read only
                readFromIntent(intent);
            }

            if (intent == null) return;

            // write
            myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Log.d(TAG, "myTag: " + myTag.toString());

            // build message to write
            NdefRecord[] records;
            try {
                records = new NdefRecord[]{ createRecord(message.getText().toString()) };
                NdefMessage ndefMsg = new NdefMessage(records);

                // Try to get an instance of Ndef for the tag.
                Ndef ndef = Ndef.get(myTag);
                long timeNdefWrite = 100000000;
                if (ndef != null) {
                    timeNdefWrite = handleNDEF(ndef, ndefMsg);
                } else {
                    MifareUltralight mfu = MifareUltralight.get(myTag);
                    if (mfu != null) {
                        // write to mifare
                        handleMifare();
                    } else {
                        String msg = "The Tag could not be identified or this NFC device does not "
                                + "support the NFC Forum commands needed to access this tag";
                        String title = "Communication failed";
                        showAlert(msg, title);
                    }
                }
                if (dialog != null) {
                    dialog.dismiss();
                    Toast.makeText(context, "time to write: " + timeNdefWrite, Toast.LENGTH_SHORT).show();
                    timeToWrite.setText("time to write: " + timeNdefWrite + "ms");
                    NdefMessage[] msgs = new NdefMessage[1];
                    msgs[0] = ndefMsg;
                    buildTagViews(msgs, "Just Written: ", justWritten);; // is this going to byte me in teh butt later?
                    // justWritten.setText("Successfully wrote: " + new String(message.getRecords()[0].getPayload(), "UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        super.onNewIntent( intent );
    }

    private long handleNDEF(Ndef ndef, NdefMessage ndefMsg) {
        long timeNdefWrite = 0;
        try {

            // Time statistics to return

            long RegTimeOutStart = System.currentTimeMillis();
            if (ndef != null) {
                // Enable I/O
                ndef.connect();

                // read first?
                NdefMessage readMessage = ndef.getCachedNdefMessage();
                NdefMessage[] msgs = {readMessage};
                buildTagViews(msgs, "Just read NFC Content: ", justRead);
                //Log.d(TAG, "reading cached msg from ndef tag: " + readMessage.getByteArrayLength());
                // Write the message
                Log.d(TAG, "writing message..." + message.toString());

                // Time statistics to return
                timeNdefWrite = 0;
                RegTimeOutStart = System.currentTimeMillis();
                ndef.writeNdefMessage(ndefMsg);
                Log.d(TAG, "closing connection to tag...");
                ndef.close();
                timeNdefWrite = System.currentTimeMillis() - RegTimeOutStart;
                return timeNdefWrite;
            }

        } catch (FormatException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timeNdefWrite;
    }

    // looks like read only????????
    private void handleMifare() {
        byte[] answer;
        byte[] command;
        MifareUltralight mfu = MifareUltralight.get(myTag);
        if (mfu != null) {
            try {
                mfu.connect();
                // various attempts at reading the tag, all of which fail w/ the default tag and the resined one.
                // the one w/ wires coming off works fine.
                // This is the example from the android docs and it just prints nonsense:
                // �Ten1���l yo
                //readTag(); // android version
                // read first?
                //byte[] pages = readMifareUltralight(mfu); // another website version
                // the above one prints out nonsense and more: mfu, pageString: �Ten1���l your cleverness and buy bewilderme
                //String pageString = new String(pages);
                //Log.d(TAG, "mfu, pageString: " + pageString);

                //NdefMessage readMessage =
                command = new byte[1];
                command[0] = (byte) 0x60; // GET_VERSION
                answer = mfu.transceive(command);
                Log.d(TAG, "answer: " + answer);
                mfu.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


        //setIntent(intent);// was in original codexpedia code...need to figure out what it does. //todo
        //This might be useful: http://www.codexpedia.com/android/android-nfc-read-and-write-example/
        // this too: https://www.survivingwithandroid.com/2016/01/nfc-tag-writer-android.html





        // next chunk mostly from here: https://www.survivingwithandroid.com/2015/03/android-nfc-app-android-nfc-tutorial.html




    /******************************************************************************
     **********************************Read From NFC Tag***************************
     ******************************************************************************/
    private void readFromIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        NdefMessage[] msgs = null;
        Log.d(TAG, "rawMsgs: " + rawMsgs);
        if (rawMsgs != null) {
            msgs = new NdefMessage[rawMsgs.length];
            for (int i = 0; i < rawMsgs.length; i++) {
                msgs[i] = (NdefMessage) rawMsgs[i];
            }
        }
        buildTagViews(msgs, "Just read NFC Content: ", justRead);
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
            Log.d(TAG, "buildTagViews, payload: " + text + msg);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        tv.setText(text + msg);
    }

    // from android docs: https://stuff.mit.edu/afs/sipb/project/android/docs/guide/topics/connectivity/nfc/advanced-nfc.html
    public String readTag() {
        MifareUltralight mifare = MifareUltralight.get(myTag);
        try {
            mifare.connect();
            byte[] payload = mifare.readPages(4);
            String payloadString = new String(payload, Charset.forName("US-ASCII"));
            Log.d(TAG, payloadString);
            return payloadString;
        } catch (IOException e) {
            Log.e(TAG, "IOException while writing MifareUltralight message...", e);
        } finally {
            if (mifare != null) {
                try {
                    mifare.close();
                }
                catch (IOException e) {
                    Log.e(TAG, "Error closing tag...", e);
                }
            }
        }
        return null;
    }

    private byte[] readMifareUltralight(MifareUltralight tag) throws IOException {
        byte[] payload = new byte[MIFARE_ULTRALIGHT_SIZE_LIMIT];
        try {
            tag.connect();
            for (int i = 4; i < 16; i++) {
                System.arraycopy(
                        tag.readPages(i),
                        0,
                        payload,
                        (i - 4) * 4,
                        4
                );
            }
        } finally {
            //tag.close();
        }

        return payload;
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