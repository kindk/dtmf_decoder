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
    private final int recvCodeMaxLength = 200;
    private final int ssidCodeMaxLength = 100;
    private final int pwdCodeMaxLength = 100;
    boolean isStartReceive = false;
    boolean isReceiving = false;

    private final int zeroNumMin = 5;


    final private byte prefixCode = 0;
    final private int prefixLength = 5;
    final private byte divideCode = 15;


    char[] ssidCode;
    char[] pwdCode;

    byte[] dtmfCode;
    byte[] recvCode;
    int dtmfIndex;
    int recvIndex = 0;
    int ssidIndex;
    int pwdIndex;

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
        recvCode = new byte[recvCodeMaxLength];
        ssidCode = new char[ssidCodeMaxLength];
        pwdCode = new char[pwdCodeMaxLength];

        //res = new char[10000];
        res = new byte[10000];
//        ssid  = new byte[20];
//        pwd = new byte[20];

        resIdx = 0;
        dtmfIndex = 0;
        recvIndex = 0;

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

        Log.i(TAG, "(dtmfCode2Digital) Digital Code Length:" + length);
        Log.i(TAG, Arrays.toString(code));
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.w("RecognizerTask", "start recognizer " + controller.isStarted());
        char lastKey = 0;

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() { //run every second

                if (!isStartReceive && isReceiving) { //Start receive Sth.
                    isStartReceive = true;
                    Log.i(TAG, "Start receive sth.");
                } else if (isStartReceive && !isReceiving) {
                    //Receive finished
                    //Data:   dtmfCode[]
                    //Length: dtmfIndex
                    isStartReceive = false;

                    dtmfCode2Digital(dtmfCode, dtmfIndex);
                    //Now, dtmfCode include many duplicate code than original code
                    // as recording rate is not same as tone playing rate

                    //哨兵,Only for finding the pwdEndIndex, not equal to 0
                    dtmfCode[dtmfIndex++] = 1;


                    //Start parse duplicate dtmfCode to recvCode
                    recvCode[0] = dtmfCode[0]; //TODO clear recvCode
                    recvIndex = 1;

                    ssidStartIndex = 0;
                    ssidEndIndex = 0;
                    pwdStartIndex = 0;
                    pwdEndIndex = 0;

                    byte prevCode;
                    byte currCode;
                    prevCode = dtmfCode[0];
                    int repeatedCnt = 1;
                    for (int i = 1; i < dtmfIndex; i++) {
                        currCode = dtmfCode[i];

                        if (currCode == prevCode) {
                            repeatedCnt ++;
                        } else {
                            if (prevCode == prefixCode && repeatedCnt >= prefixLength) {
                                //Now meet ssid start or pwd start
                                //At DTMF_Encoder, there are 5 prefixCode
                                for (int j = 0; j < 4; j++) {
                                    recvCode[recvIndex++] = prefixCode;
                                }

                                if (ssidStartIndex == 0) {
                                    ssidStartIndex = recvIndex;
                                } else if (pwdStartIndex == 0) {
                                    ssidEndIndex = recvIndex - 6;
                                    pwdStartIndex = recvIndex;
                                } else if (pwdEndIndex == 0) {
//                                    Log.i(TAG, )
                                    pwdEndIndex = recvIndex - 6;
                                } else {
                                    Log.i(TAG, "Wrong Index");
                                }
                            } else if (repeatedCnt == 1) {
                                //If we get a code which only occurs once, suppose it
                                //is a wrong code and discards it.
                                //Because sampling rate is much larger than tone playing rate,
                                //every code we received should duplicate 3 or 4 times.
                                //Of course, this related to tone playPeriod, playInternal,
                                //and record sleep time.
                                Log.e(TAG, "=====Index: " + recvIndex + " " + i + " code: " + prevCode);

                                recvCode[recvIndex-1] = currCode;
                                prevCode = currCode;
                                repeatedCnt = 1;

                                continue;
                            }


                            prevCode = currCode;
                            repeatedCnt = 1;

                            recvCode[recvIndex++] = currCode;
                        }
                    }

                    Log.i(TAG, "recvCode Index: " + ssidStartIndex + " " + ssidEndIndex + " " + pwdStartIndex + " " + pwdEndIndex);
                    Log.i(TAG, Arrays.toString(recvCode));


                    // recvCode -------> ssidCode

                    ssidIndex = 0;
                    byte h = 0,l = 0;
                    int k = 0;
                    for (int i = ssidStartIndex; i <= ssidEndIndex; i++) {
                        if (recvCode[i] == divideCode) {
                            continue;
                        }

                        if (k % 2 == 0) {
                            h = recvCode[i];
                        } else {
                            l = recvCode[i];
                            ssidCode[ssidIndex++] = (char)(h * 15 + l);
                        }
                        k++;
                    }
                    
                    Log.i(TAG, "ssidCode length: " + ssidIndex);
                    Log.i(TAG, new String(ssidCode));

                    pwdIndex = 0;
                    h = 0;
                    l = 0;
                    k = 0;
                    for (int i = pwdStartIndex; i <= pwdEndIndex; i++) {
                        if (recvCode[i] == 15) {
                            continue;
                        }

                        if (k % 2 == 0) {
                            h = recvCode[i];
                        } else {
                            l = recvCode[i];
                            pwdCode[pwdIndex++] = (char)(h * 15 + l);
                        }
                        k++;
                    }

                    Log.i(TAG, "pwdCode length: " + pwdIndex);
                    Log.i(TAG, new String(pwdCode));

                    String strSSID = new String(ssidCode);
                    String strPWD = new String(pwdCode);
                    publishProgress(strSSID, strPWD);

                    int i;
                    for (i = 0; i < ssidIndex; i++) {
                        ssidCode[i] = 0;
                    }
                    for (i = 0; i < pwdIndex; i++) {
                        pwdCode[i] = 0;
                    }

                    dtmfIndex = 0;
                }

                isReceiving = false;
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
                }
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
