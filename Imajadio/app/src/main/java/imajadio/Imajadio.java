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
    private final int NUMBER_OF_FREQUENCIES = 4000 - 100; //frequency range from 100-10000 Hz
    private final int SAMPLE_RATE = 8000;

    private final Bitmap IMAGE;
    private final int IMAGE_HEIGHT;
    private final int IMAGE_WIDTH;
    private final int MAX_AMPLITUDE;

    private byte[] DATA;
    private double highestAmplitude;

    private double grainDuration;

    private AudioTrack audio; //contains the audio to play


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
    public void bitmapToAudio() {
        double[] samples = new double[(int) (grainDuration * SAMPLE_RATE)];
        byte[] generatedSnd = new byte[IMAGE_WIDTH * 2 * samples.length];
        int idx = 0;

        audio = new AudioTrack(AudioManager.STREAM_MUSIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, 2 * samples.length * IMAGE_WIDTH,
                AudioTrack.MODE_STATIC);

        for (int column = 1; column <= IMAGE_WIDTH; column++) { //for each column in the image
            //Convert a column to samples
            int[] pixels = new int[IMAGE_HEIGHT];
            IMAGE.getPixels(pixels, 0, 1, column - 1, 0, 1, IMAGE_HEIGHT); //extract a column of pixels
            //Log.e("COLUMN #", String.valueOf(column));
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
        audio.write(generatedSnd, 0, generatedSnd.length);
    }

    private double[] columnToSamples(int[] column, int columnIndex) {
        Harmonic[] harmonics = new Harmonic[column.length];

        //converts each pixel in a column to its associated harmonic
        for (int verticalOffset = 0; verticalOffset < column.length; verticalOffset++) {
            harmonics[verticalOffset] = pixelToHarmonic(column[verticalOffset], verticalOffset);
            //Log.e("Harm #"+String.valueOf(verticalOffset), "freq: "+ String.valueOf(harmonics[verticalOffset].getFrequency()) + "; amp: " + String.valueOf(harmonics[verticalOffset].getAmplitude()));
        }

        int numSamples = (int) (grainDuration * SAMPLE_RATE);
        double[] samples = new double[numSamples];
        double amplitude;

        for (int sampleIndex = 0; sampleIndex < numSamples; sampleIndex++) { //for each sample in the output table
            amplitude = 0;

            for (Harmonic h : harmonics) { //add the amplitude of each harmonic

                // the "((numSamples*columnIndex)+sampleIndex)" is used to make each frequency continue off from where it was in the last column
                //amplitude += h.getAmplitude() * Math.sin(w * h.getFrequency() * ((numSamples * columnIndex) + sampleIndex));
                amplitude += h.getAmplitude() * Math.sin(2 * Math.PI * ((numSamples * columnIndex) + sampleIndex) / (SAMPLE_RATE / h.getFrequency()));


                //TESTING OUTPUT
                if((columnIndex*numSamples)+sampleIndex >= 522 && (columnIndex*numSamples)+sampleIndex <= 1372 && h.getAmplitude() != 0) {
                    //Log.e("TEST", "Sample: " + String.valueOf((columnIndex*numSamples)+sampleIndex) + "\t Harmonic: " + String.valueOf(h.getFrequency()) + "\t Amplitude: " + String.valueOf(h.getAmplitude()));
                }


            }
            samples[sampleIndex] = amplitude;

            //highest amplitude is stored for later normalization
            if (Math.abs(samples[sampleIndex]) > highestAmplitude) {
                highestAmplitude = Math.abs(samples[sampleIndex]);
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

    public void play() {

        if (audio.getPlayState() == 3) {
            audio.stop();
            audio.reloadStaticData();
        }

        audio.play();


    }

    public void stop(){
        audio.stop();
        audio.reloadStaticData();
    }


    public void onPlaybackStopped(AudioTrack.OnPlaybackPositionUpdateListener listener){

        audio.setNotificationMarkerPosition((int) (SAMPLE_RATE * IMAGE_WIDTH*grainDuration));
        audio.setPlaybackPositionUpdateListener(listener);

    }


    public byte[] getDATA() {
        return DATA;
    }

    public short getAudioChannelCount() {
        return (short) audio.getChannelCount();
    }

    public int getAudioSampleRate() {
        return audio.getSampleRate();
    }

    public void normalizeAudio() {

        double multiplier = MAX_AMPLITUDE / highestAmplitude; //what to multiply every sample by.

        for (int i = 0; i < DATA.length; i = i + 2) {

            int k = (int) ((twoBytesToAmplitude(DATA[i], DATA[i + 1])) * multiplier);

            // Log.e("iiiii...", String.valueOf(i));
            // Log.e("First...", String.valueOf(twoBytesToAmplitude(DATA[i],DATA[i+1])));

            DATA[i] = (byte) (k & 0x00ff);
            DATA[i + 1] = (byte) ((k & 0xff00) >>> 8);

            // Log.e("After...", String.valueOf(twoBytesToAmplitude(DATA[i],DATA[i+1])));
        }
        //audio.write(DATA, 0, DATA.length);

    }//normalizeAudio

    private static double twoBytesToAmplitude(byte b1, byte b2) {
        return ((b2 << 8) | (b1 & 0xFF));
    }


}//Imajadio
