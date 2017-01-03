package cn.kindk.yjd.dtmfdecoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.BlockingQueue;

/**
 * Created by yjd on 1/3/17.
 */

public class RecordTask extends AsyncTask<Void, Object, Void> {
    int frequency = 16000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    int blockSize = 1024;

    Controller controller;
    BlockingQueue<DataBlock> blockingQueue;

    public RecordTask(Controller controller, BlockingQueue<DataBlock> blockingQueue) {
        this.controller = controller;
        this.blockingQueue = blockingQueue;
        Log.w("RecordTask", "new Record");
    }

    @Override
    protected Void doInBackground(Void... params) {
        //1280

        int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
        Log.w("RecordTask", "bufferSize is " + bufferSize);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                frequency, channelConfiguration, audioEncoding, bufferSize);

        try {
            short[] buffer = new short[blockSize];
            audioRecord.startRecording();

            while (controller.isStarted()) {
                int bufferReadSize = audioRecord.read(buffer, 0, blockSize);
                DataBlock dataBlock = new DataBlock(buffer, blockSize, bufferReadSize);
                blockingQueue.put(dataBlock);
            }
        } catch (Throwable t) {
            Log.e("AudioRecord", "Recording Failed");
        }

        audioRecord.stop();

        return null;
    }
}
