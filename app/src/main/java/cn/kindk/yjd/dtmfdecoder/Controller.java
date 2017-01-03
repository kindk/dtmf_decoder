package cn.kindk.yjd.dtmfdecoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by yjd on 1/3/17.
 */

public class Controller {
    String TAG = "Controller";
    private boolean started = false;

    int frequency = 16000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 1024;


    private MainActivity mainActivity;
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
//            recordTask = new RecordTask(this, blockingQueue);
            recognizerTask = new RecognizerTask(this, blockingQueue);

//            recordTask.execute();
//            Log.w(TAG, "recorderTask execute");

            recognizerTask.execute();
            Log.w(TAG, "recognizerTask execute");


            new Thread(new Runnable() {
                @Override
                public void run() {

                    int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
                    Log.w("RecordTask", "bufferSize is " + bufferSize);

                    AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            frequency, channelConfiguration, audioEncoding, bufferSize);

                    try {
                        short[] buffer = new short[blockSize];
                        audioRecord.startRecording();

                        while (isStarted()) {
                            int bufferReadSize = audioRecord.read(buffer, 0, blockSize);
                            DataBlock dataBlock = new DataBlock(buffer, blockSize, bufferReadSize);
                            blockingQueue.put(dataBlock);
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
