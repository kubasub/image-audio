package imajadio;

/**
 * @author Jakub Subczynski
 * @date April 12, 2014
 */
public class Harmonic {
    private final double frequency; //from 100Hz-10000Hz
    private final double amplitude;

    public Harmonic(double frequency, double amplitude) {
        this.frequency = frequency;
        this.amplitude = amplitude;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getAmplitude() {
        return amplitude;
    }
}
