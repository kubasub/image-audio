package imajadio;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Jakub Subczynski
 * @date April 12, 2014
 */
public class Imajadio {
    private final int NUMBER_OF_FREQUENCIES = 10000 - 100; //frequency range from 100-10000 Hz
    private final int SAMPLE_RATE = 8000;

    private final Bitmap IMAGE;
    private final int IMAGE_HEIGHT;
    private final int IMAGE_WIDTH;
    private final int MAX_AMPLITUDE;

    private byte[] DATA;
    private double highestAmplitude;


    private double grainDuration;


    public Imajadio(Bitmap image) {
        this(image, 16, 1);
    }

    public Imajadio(Bitmap image, int bitDepth) {
        this(image, bitDepth, 1);
    }

    public Imajadio(Bitmap image, int bitDepth, double grainDuration) {

        this.IMAGE = image;
        this.IMAGE_HEIGHT = IMAGE.getHeight();
        this.IMAGE_WIDTH = IMAGE.getWidth();
        this.MAX_AMPLITUDE = (int) Math.pow(2, bitDepth - 1); //max amplitude is based on bit depth

        this.grainDuration = grainDuration;
        this.highestAmplitude = 0;
    }

    public double getGrainDuration() {
        return grainDuration;
    }

    public void setGrainDuration(double grainDuration) {
        this.grainDuration = grainDuration;
    }

    /**
     * Converts the entire bitmap image to audio.
     * <p/>
     * It writes out the samples to the AudioTrack so that the returned AudioTrack just needs to be
     * played.
     *
     * @return AudioTrack   an instance which has the audio written out to it
     */
    public AudioTrack bitmapToAudio() {
        double[] samples = new double[(int) (grainDuration * SAMPLE_RATE)];
        byte[] generatedSnd = new byte[IMAGE_WIDTH * 2 * samples.length];
        int idx = 0;

        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * samples.length * IMAGE_WIDTH,
                AudioTrack.MODE_STATIC);

        for (int column = 1; column <= IMAGE_WIDTH; column++) { //for each column in the image
            //Convert a column to samples
            int[] pixels = new int[IMAGE_HEIGHT];
            IMAGE.getPixels(pixels, 0, 1, column - 1, 0, 1, IMAGE_HEIGHT); //extract a column of pixels
            Log.e("COLUMN #", String.valueOf(column));
            samples = columnToSamples(pixels, column - 1);

            // convert to 16 bit pcm sound array
            // assumes the sample buffer is normalised.
            for (double dVal : samples) {
                short val = (short) ((dVal));
                // in 16 bit wav PCM, first byte is the low order byte
                generatedSnd[idx++] = (byte) (val & 0x00ff);
                generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

            }
            DATA = generatedSnd;
        }
        audioTrack.write(generatedSnd, 0, generatedSnd.length);

        return audioTrack;
    }

    private double[] columnToSamples(int[] column, int columnIndex) {
        Harmonic[] harmonics = new Harmonic[column.length];

        for (int verticalOffset = 0; verticalOffset < column.length; verticalOffset++) {
            harmonics[verticalOffset] = pixelToHarmonic(column[verticalOffset], verticalOffset);
            //Log.e("Harm #"+String.valueOf(verticalOffset), "freq: "+ String.valueOf(harmonics[verticalOffset].getFrequency()) + "; amp: " + String.valueOf(harmonics[verticalOffset].getAmplitude()));
        }

        int numSamples = (int) (grainDuration * SAMPLE_RATE);
        double[] samples = new double[numSamples];

        double w = 2 * Math.PI / numSamples;


        for (int sampleIndex = 0; sampleIndex < numSamples; sampleIndex++) { //for each sample in the output table
            double amplitude = 0;

            for (Harmonic h : harmonics) { //add the amplitude of each harmonic

                //equations which produce a stutter between columns
                //amplitude+= h.getAmplitude() * Math.sin(w * h.getFrequency() * sampleIndex);
                //amplitude += h.getAmplitude() * Math.sin(2 * Math.PI * sampleIndex / (SAMPLE_RATE/h.getFrequency()));

                //equations which do not produce a stutter between columns
                // the "((numSamples*columnIndex)+sampleIndex)" is used to make each frequency continue off from where it was in the last column
                amplitude += h.getAmplitude() * Math.sin(w * h.getFrequency() * ((numSamples * columnIndex) + sampleIndex));
                //amplitude += h.getAmplitude() * Math.sin(2 * Math.PI * ((numSamples*columnIndex)+sampleIndex) / (SAMPLE_RATE/h.getFrequency()));
            }

            samples[sampleIndex] = amplitude; //rounds amplitude to an integer

            //hers stev
            if(samples[sampleIndex] > highestAmplitude  ){
                highestAmplitude = samples[sampleIndex];
            }

        }


        return samples;
    }


    /**
     * Converts a pixel into a harmonic of the column it is in.
     * <p/>
     * Calculates the frequency of the harmonic using the pixels vertical offset in the bitmap
     * column.
     * <p/>
     * Calculates the amplitude of the harmonic using the intensity of the color of the pixel.
     *
     * @param pixel          the Bitmap pixel to be converted
     * @param verticalOffset the pixel's position in the column
     * @return Harmonic         the harmonic representation of the pixel
     */
    private Harmonic pixelToHarmonic(int pixel, int verticalOffset) {
        double frequency = (double) (IMAGE_HEIGHT - verticalOffset) *
                NUMBER_OF_FREQUENCIES / (IMAGE_HEIGHT + 1);

        double amplitude = ((double) (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel))) / 765 *
                MAX_AMPLITUDE / IMAGE_HEIGHT;

        return new Harmonic(frequency, amplitude);
    }

    public byte[] getDATA(){
        return DATA;
    }


    

}//Imajadio


