package cn.kindk.yjd.dtmfdecoder;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

/**
 * Created by yjd on 1/3/17.
 */

public class RecognizerTask extends AsyncTask<Void, Object, Void> {
    private Controller controller;
    private BlockingQueue<DataBlock> blockingQueue;
    private Recognizer recognizer;
    private final String TAG = "RecognizerTask";

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

        //res = new char[10000];
        res = new byte[10000];
//        ssid  = new byte[20];
//        pwd = new byte[20];

        resIdx = 0;
        Log.w("RecognizerTask", "new recognizer");
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.w("RecognizerTask", "start recognizer " + controller.isStarted());
        char lastKey = 0;
        while (controller.isStarted()) {
            try {
                DataBlock dataBlock = blockingQueue.take();
                Spectrum spectrum = dataBlock.FFT();
                spectrum.normalize();
                StatelessRecognizer statelessRecognizer = new StatelessRecognizer(spectrum);
                char key = statelessRecognizer.getRecognizedKey();

                if (key != ' ') {
                    if (key != lastKey) {
                        Log.w(TAG, " " + resIdx + " " + key);
                        res[resIdx] = (byte)key;
                        resIdx ++;

                        if ((key == '*') && (resIdx >= 2)) {
                            if (//res[resIdx-3] == '*' &&
                                    res[resIdx-2] == '3' &&
                                    res[resIdx-1] == '*' ) {

                                ssidStart = resIdx;
                                findSSID = true;
                            }
                        }

                        if ((key == '*') && (resIdx >= 4)) {
                            if (res[resIdx-3] == '*' &&
                                    res[resIdx-2] == '#' &&
                                    res[resIdx-1] == '*' ) {

                                ssidEnd = resIdx-4;
                                pwdStart = resIdx;
                                findSSID = true;
                            }
                        }

                        if (key == '#' && (resIdx > 15)) {
                            if (res[resIdx-3] == '#' &&
                                res[resIdx-2] == '8' &&
                                res[resIdx-1] == '#' ) {

                                pwdEnd = resIdx-4;
                                findPwd = true;
                            }
                        }

                        if (findSSID && findPwd) {
                            findSSID = false;
                            findPwd = false;
                            resIdx = 0;

                            Log.w(TAG, "find SSID:" + ssidStart + " " + ssidEnd);
                            Log.w(TAG, "find PWD:" + pwdStart + " " + pwdEnd);

                            ssid = new byte[ssidEnd-ssidStart+2];
                            pwd = new byte[pwdEnd-pwdStart+2];

                            int j = 0;
                            for (int i=ssidStart;i<=ssidEnd;i++) {
                                ssid[j] = res[i];
                                j++;
                                Log.w(TAG, "SSID: " + res[i]);
                            }

                            j = 0;
                            for (int i=pwdStart;i<=pwdEnd;i++) {
                                pwd[j] = res[i];
                                j++;
                                Log.w(TAG, "PWD: " + res[i]);
                            }

                            String strSSID = new String(ssid);
                            String strPWD  = new String(pwd);

                            publishProgress(strSSID, strPWD);

                            Log.w(TAG, new String(ssid));
                            Log.w(TAG, new String(pwd));
                        }

                    }
                    lastKey = key;
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
