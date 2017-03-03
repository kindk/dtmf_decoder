package cn.kindk.yjd.dtmfdecoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static android.os.SystemClock.sleep;

/**
 * Created by yjd on 1/3/17.
 */

public class Controller {
    private static final String TAG = "Controller";
    private MainActivity mainActivity;

    private boolean started = false;

    int frequency = 16000;  //Can't change
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 1024;


    private RecordTask recordTask;
    private RecognizerTask recognizerTask;

    BlockingQueue<DataBlock> blockingQueue;

    public Controller(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public void changeState() {
        if (started == false) {
            started = true;
            Log.w(TAG, "start");

            blockingQueue = new LinkedBlockingQueue<DataBlock>();
            mainActivity.start();
           // recognizerTask = new RecognizerTask(this, blockingQueue);
            //recognizerTask.execute();

            new Thread(new Runnable() {
                @Override
                public void run() {

                    int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                    Log.w("RecordTask", "11bufferSize is " + bufferSize);

                    AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            frequency, channelConfiguration, audioEncoding, bufferSize);

                    int seq = 0;

                    try {
                        short[] buffer = new short[blockSize];
                        audioRecord.startRecording();

                        while (isStarted()) {
                            Log.i(TAG, "read 0");
                            int bufferReadSize = audioRecord.read(buffer, 0, blockSize);
                            Log.i(TAG, "read 1");
                            DataBlock dataBlock = new DataBlock(buffer, blockSize, bufferReadSize);
                            Spectrum spectrum = dataBlock.FFT();
                            spectrum.normalize();
                            StatelessRecognizer statelessRecognizer = new StatelessRecognizer(spectrum);
                            char key = statelessRecognizer.getRecognizedKey();
                            Log.i(TAG, "read 2");
                            if (key != ' ') {
                                Log.i(TAG, "=====1== " + key);
                               // key = ' ';
                                //dataBlock.dump();
                            }
                            sleep(5);
                        }
                    } catch (Throwable t) {
                        Log.e("AudioRecord", "Recording Failed");
                    }
                }
            }).start();
        } else {
            Log.w(TAG, "stop");
            mainActivity.stop();

            recognizerTask.cancel(true);
            recordTask.cancel(true);

            started = false;
        }
    }

    public void clear() {
       // Log.w(TAG, "clear");
        mainActivity.clear();
    }

    public boolean isStarted() { return started; }

    public void setWifi(String ssid, String pwd) {
        mainActivity.setSSID(ssid);
        mainActivity.setPWD(pwd);

    }
}
