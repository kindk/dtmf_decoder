package cn.kindk.yjd.dtmfdecoder;

import android.util.Log;

import java.util.Arrays;

import math.FFT;

/**
 * Created by yjd on 1/3/17.
 */

public class DataBlock {
    private double[] block;

    public DataBlock(short[] buffer, int blockSize, int bufferReadSize) {
        block = new double[blockSize];
        for (int i = 0; i < blockSize && i < bufferReadSize; i++) {
            block[i] = (double) buffer[i];
        }
    }

    public int seq;

    public void setBlock(double[] block)
    {
        this.block = block;
    }

    public double[] getBlock()
    {
        return block;
    }

    public Spectrum FFT() {
        return new Spectrum(FFT.magnitudeSpectrum(block));
    }

    public void dump() {
        for (int i = 0; i < block.length; i++) {
            Log.i("DataBlock", Arrays.toString(block));
        }
    }
}
