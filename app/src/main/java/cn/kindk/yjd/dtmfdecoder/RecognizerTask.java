package cn.kindk.yjd.dtmfdecoder;

import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.EditText;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;

/**
 * Created by yjd on 1/3/17.
 */

public class RecognizerTask extends AsyncTask<Void, Object, Void> {
    private Controller controller;
    private BlockingQueue<DataBlock> blockingQueue;
    private Recognizer recognizer;
    private final String TAG = "RecognizerTask";
    private TimerTask timerTask;
    private Timer timer;

    private final int dtmfCodeMaxLength = 500; //TODO
    private final int sendCodeMaxLength = 200;
    private final int ssidCodeMaxLength = 100;
    private final int pwdCodeMaxLength = 100;
    boolean isStartReceive = false;
    boolean isReceiving = false;

    private final int zeroNumMin = 5;


    final private int prefixLength = 5;
    final private byte divideCode = 15;


    char[] ssidCode;
    char[] pwdCode;

    byte[] dtmfCode;
    byte[] sendCode;
    int dtmfIndex;
    int sendIndex = 0;
    int ssidIndex;
    int pwdIndex;

    int prefixIndex = 0;
    int divideIndex = 0;
    int suffixIndex = 0;


    int ssidStartIndex = 0;
    int ssidEndIndex = 0;
    int pwdStartIndex = 0;
    int pwdEndIndex = 0;

 //   private char[] res;
    private byte[] res;
    private int resIdx;

    int ssidStart;
    int ssidEnd;

    int pwdStart;
    int pwdEnd;

    byte[] ssid;
    byte[] pwd;

    boolean findSSID = false;
    boolean findPwd = false;

    public RecognizerTask(Controller controller, BlockingQueue<DataBlock> blockingQueue){
        this.controller = controller;
        this.blockingQueue = blockingQueue;
        this.recognizer = new Recognizer();

        dtmfCode = new byte[dtmfCodeMaxLength];
        sendCode = new byte[sendCodeMaxLength];
        ssidCode = new char[ssidCodeMaxLength];
        pwdCode = new char[pwdCodeMaxLength];

        //res = new char[10000];
        res = new byte[10000];
//        ssid  = new byte[20];
//        pwd = new byte[20];

        resIdx = 0;
        dtmfIndex = 0;
        sendIndex = 0;

        Log.w("RecognizerTask", "new recognizer");
    }


    private void dtmfCode2Digital(byte[] code, int length) {

        for (int i = 0; i < length; i++) {
            if (code[i] >= '0' && code[i] <= '9') {
                code[i] -= '0';
            }

            if (code[i] >= 'A' && code[i] <= 'D') {
                code[i] = (byte) (code[i] - 'A' + 12);
            }

            if (code[i] == '*') {
                code[i] = 10;
            }

            if (code[i] == '#') {
                code[i] = 11;
            }
        }

        Log.i(TAG, "dtmfCode2Digital: " + length);
        Log.i(TAG, Arrays.toString(code));
    }

    private void getSsidAndPwd(byte[] code, int length, byte[] ssid, byte[] pwd) {
        int zeroNum = 0;
        int ssidStart = 0, ssidEnd = 0;
        int pwdStart = 0, pwdEnd = 0;

        for (int i = 0; i < length; i++) {
            if (code[i] == 0) {
                zeroNum++;
            } else {
                if (zeroNum >= zeroNumMin) {
                    if (ssidStart == 0) {
                        ssidStart = i;
                    } else if (pwdStart == 0) {
                        ssidEnd = i - zeroNum - 1;
                        pwdStart = i;
                    } else if (pwdEnd == 0) {
                        pwdEnd = i - zeroNum - 1;
                    }
                }
                zeroNum = 0;
            }
        }

        int j = 0;
        for (int i = ssidStart; i < ssidEnd; i++) {
            ssid[j] = code[i];
            j++;
        }

        for (int i = pwdStart; i < pwdEnd; i++) {
            pwd[j] = code[i];
            j++;
        }
    }

    private void getSSIDCode(byte[] code, int length, byte[] ssid) {

        for (int i = 0; i < length; i++) {

        }
    }

    private void findContinuousCode(byte code, int a) {};


    private void removeDuplicateCode(byte[] code, int length) {
        byte last;
        for (int i = 0; i < length; i++) {

        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.w("RecognizerTask", "start recognizer " + controller.isStarted());
        char lastKey = 0;

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (isStartReceive && !isReceiving) {
                    isStartReceive = false;

                    dtmfCode2Digital(dtmfCode, dtmfIndex);
                    //Now, dtmfCode include many duplicate code than original code
                    // as recording rate is not same as tone playing rate





                    sendIndex = 1;
                    int lastNum = 1;
                    byte lastCode = dtmfCode[0];
                    sendCode[0] = dtmfCode[0];
                    byte code;

                    prefixIndex = 0;
                    divideIndex = 0;
                    suffixIndex = 0;

                    //哨兵
                    dtmfCode[dtmfIndex] = 1; /////Only to find the pwdEndIndex, not equal to 0
                    for (int i = 1; i < (dtmfIndex+1); i++) {
                        code = dtmfCode[i];

                        if (code == lastCode) {
                            lastNum ++;
                        } else {
                            if (lastCode == 0 && lastNum >= prefixLength) {
                                for (int j = 0; j < 4; j++) {
                                    sendCode[sendIndex++] = 0;
                                }

                                if (ssidStartIndex == 0) {
                                    ssidStartIndex = sendIndex;
                                } else if (pwdStartIndex == 0) {
                                    ssidEndIndex = sendIndex - 6;
                                    pwdStartIndex = sendIndex;
                                } else if (pwdEndIndex == 0) {
//                                    Log.i(TAG, )
                                    pwdEndIndex = sendIndex - 6;
                                } else {
                                    Log.i(TAG, "Wrong Index");
                                }
                            }

                            lastCode = code;
                            lastNum = 1;

                            sendCode[sendIndex++] = code;
                        }
                    }

                    Log.i(TAG, "sendCode Index: " + ssidStartIndex + " " + ssidEndIndex + " " + pwdStartIndex + " " + pwdEndIndex);
                    Log.i(TAG, Arrays.toString(sendCode));


                    // sendCode -------> ssidCode

                    ssidIndex = 0;
                    byte h = 0,l = 0;
                    int k = 0;
                    for (int i = ssidStartIndex; i <= ssidEndIndex; i++) {
                        if (sendCode[i] == 15) {
                            continue;
                        }

                        if (k % 2 == 0) {
                            h = sendCode[i];
                        } else {
                            l = sendCode[i];
                            ssidCode[ssidIndex++] = (char)(h * 15 + l);
                        }
                        k++;
                    }
                    
                    Log.i(TAG, "ssidCode length: " + ssidIndex);
                    Log.i(TAG, new String(ssidCode));
                    //Log.i(TAG, Arrays.toString(ssidCode));



                    pwdIndex = 0;
                    h = 0;
                    l = 0;
                    k = 0;
                    for (int i = pwdStartIndex; i <= pwdEndIndex; i++) {
                        if (sendCode[i] == 15) {
                            continue;
                        }

                        if (k % 2 == 0) {
                            h = sendCode[i];
                        } else {
                            l = sendCode[i];
                            pwdCode[pwdIndex++] = (char)(h * 15 + l);
                        }
                        k++;
                    }

                    Log.i(TAG, "pwdCode length: " + pwdIndex);
                    Log.i(TAG, new String(pwdCode));
            //        Log.i(TAG, Arrays.toString(pwdCode));
                    String strSSID = new String(ssidCode);
                    String strPWD = new String(pwdCode);
                    publishProgress(strSSID, strPWD);

                    dtmfIndex = 0;
                } else if (!isStartReceive && isReceiving) {
                    isStartReceive = true;
                    Log.i(TAG, "Start receive sth.");
                }

                isReceiving = false;

                //timer.schedule(timerTask, 1000);
                //Log.i(TAG, "timer 1s;");
            }
        };

        timer.schedule(timerTask, 1000, 1000);

        while (controller.isStarted()) {
            try {
                DataBlock dataBlock = blockingQueue.take();
                Spectrum spectrum = dataBlock.FFT();
                spectrum.normalize();
                StatelessRecognizer statelessRecognizer = new StatelessRecognizer(spectrum);
                char key = statelessRecognizer.getRecognizedKey();

                if (key != ' ') {
                    isReceiving = true;
                    dtmfCode[dtmfIndex++] = (byte)key;
                    if (dtmfIndex >= dtmfCodeMaxLength) {
                        dtmfIndex = 0;
                    }
                    //Log.i(TAG, "" + key);
                }

//                if (key != ' ') {
//                  //  Log.w(TAG, "" + key);
//
//
//                   // if (key != lastKey) {
//                    if (true) {
//                        Log.w(TAG, " " + resIdx + " " + key + " " + dataBlock.seq);
//                        res[resIdx] = (byte)key;
//                        resIdx ++;
//
////                        if (lastKey == '0') {
////
////                        }
////
////
////                        if (key == '0' && (resIdx >= 2)) {
////                            if
////                        }
////
//
//
//                        if ((key == '*') && (resIdx >= 2)) {
//                            if (//res[resIdx-3] == '*' &&
//                                    res[resIdx-2] == '3' &&
//                                    res[resIdx-1] == '*' ) {
//
//                                ssidStart = resIdx;
//                                findSSID = true;
//                            }
//                        }
//
//                        if ((key == '*') && (resIdx >= 4)) {
//                            if (res[resIdx-3] == '*' &&
//                                    res[resIdx-2] == '#' &&
//                                    res[resIdx-1] == '*' ) {
//
//                                ssidEnd = resIdx-4;
//                                pwdStart = resIdx;
//                                findSSID = true;
//                            }
//                        }
//
//                        if (key == '#' && (resIdx > 15)) {
//                            if (res[resIdx-3] == '#' &&
//                                res[resIdx-2] == '8' &&
//                                res[resIdx-1] == '#' ) {
//
//                                pwdEnd = resIdx-4;
//                                findPwd = true;
//                            }
//                        }
//
//                        if (findSSID && findPwd) {
//                            findSSID = false;
//                            findPwd = false;
//                            resIdx = 0;
//
//                            Log.w(TAG, "find SSID:" + ssidStart + " " + ssidEnd);
//                            Log.w(TAG, "find PWD:" + pwdStart + " " + pwdEnd);
//
//                            ssid = new byte[ssidEnd-ssidStart+2];
//                            pwd = new byte[pwdEnd-pwdStart+2];
//
//
//                            int k = 0;
//                            for (int i=ssidStart;i<=ssidEnd;i++) {
//                                if (res[i] >= '0' && res[i] <= '9') {
//                                    res[i] -= '0';
//                                }
//
//                                if (res[i] >= 'A' && res[i] <= 'D') {
//                                    res[i] = (byte)(res[i] - 'A' + 12);
//                                }
//
//                                if (res[i] == '*') {
//                                    res[i] = 10;
//                                }
//
//                                if (res[i] == '#') {
//                                    res[i] = 11;
//                                }
//
//                                if (k%2==0) {
//                                    int a = ssidStart + k;
//                                    if (res[a] == 8) {
//                                        Log.w(TAG, "888: " + a + " " + res[a] + " " + res[a+1] );
//                                        res[a] = res[a + 1];
//                                    }
//                                }
//                                k ++;
//                            }
//
//
//                            k=0;
//                            for (int i=pwdStart;i<=pwdEnd;i++) {
//                                if (res[i] >= '0' && res[i] <= '9') {
//                                    res[i] -= '0';
//                                }
//
//                                if (res[i] >= 'A' && res[i] <= 'D') {
//                                    res[i] = (byte)(res[i] - 'A' + 12);
//                                }
//
//                                if (res[i] == '*') {
//                                    res[i] = 10;
//                                }
//
//                                if (res[i] == '#') {
//                                    res[i] = 11;
//                                }
//
//                                if (k%2==0) {
//                                    int a = pwdStart + k;
//                                    if (res[a] == 8) {
//                                        Log.w(TAG, "888: " + a + " " + res[a] + " " + res[a+1] );
//                                        res[a] = res[a + 1];
//                                    }
//                                }
//                                k ++;
//                            }
//
//
//                            int j = 0;
//                            for (int i=ssidStart;i<=ssidEnd;i+=2) {
//                                ssid[j] = (byte)(res[i] * 16 + res[i+1]);
//                                Log.w(TAG, "SSID: " + (char)ssid[j]);
//                                j++;
//                            }
//
//                            j = 0;
//                            for (int i=pwdStart;i<=pwdEnd;i+=2) {
//                                pwd[j] = (byte)(res[i] * 16 + res[i+1]);
//                                Log.w(TAG, "PWD: " + (char)pwd[j]);
//                                j++;
//                            }
//
//                            String strSSID = new String(ssid);
//                            String strPWD  = new String(pwd);
//
//                            publishProgress(strSSID, strPWD);
//
//                            Log.w(TAG, new String(ssid));
//                            Log.w(TAG, new String(pwd));
//                        }
//
//                    }
//                    lastKey = key;
//                }
            } catch (InterruptedException e){
                Log.w("WWWWWWWWWW", "recognizer error");
            }

        }
        return null;
    }


    protected void onProgressUpdate(Object... progress)
    {
        String ssid = (String)progress[0];
        String pwd  = (String)progress[1];
        controller.setWifi(ssid, pwd);
    }
}
