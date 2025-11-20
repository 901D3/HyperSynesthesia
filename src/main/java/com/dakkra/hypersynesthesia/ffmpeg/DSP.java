package com.dakkra.hypersynesthesia.ffmpeg;

import com.tambapps.fft4j.FastFouriers;

import java.util.Arrays;

class DSP {

    private int peakLoudness = 0;

    private double rms = 0;

    private double[] spectrum = null;

    // private boolean interleavedEffect = false;

    // Skips the fft
    public void processLight(int[] samples) {
        final int samplesLength = samples.length;

        double sum = 0;

        for (int i = 0; i < samplesLength; i++) {
            int sample = samples[i];

            int absSample = Math.abs(sample);
            if (absSample > peakLoudness) {
                peakLoudness = absSample;
            }

            // Calculate RMS
            double d = sample;
            sum += d * d;
        }

        rms = Math.sqrt(sum / samplesLength);
    }

    public void processFull(int[] samples) {
        processLight(samples);

        // samples.length is basically FFT size
        final int fftSize = samples.length;

        // Calculate FFT
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];

        for (int i = 0; i < fftSize; i++) {
            real[i] = samples[i];
        }

        // for (int i = 0; i < fftSize; i++) imag[i] = 0;

        double[] outputReal = new double[fftSize];
        double[] outputImag = new double[fftSize];

        FastFouriers.BASIC.transform(real, imag, outputReal, outputImag);

        // Create spectrum
        // In the future HS may have customizable FFT size
        spectrum = new double[(int) Math.floor(fftSize / 2)];

        final int halfFftSize = spectrum.length;

        // Keep this if HS will add "Interleaved effect" in the future
        /*
         * if (interleavedEffect) {
         * double[] realTemp = Arrays.copyOfRange(outputReal, 0, halfFftSize);
         * double[] imagTemp = Arrays.copyOfRange(outputImag, 0, halfFftSize);
         * 
         * for (int i = 0; i < halfFftSize; i++) {
         * double realValue = realTemp[i];
         * double realValueShifted = realTemp[i + 1];
         * double imagValue = imagTemp[i];
         * 
         * int i2 = i * 2;
         * outputReal[i2] = Math.sqrt(realValueShifted * realValueShifted + imagValue *
         * imagValue);
         * outputImag[i2 + 1] = Math.sqrt(realValue * realValue + imagValue *
         * imagValue);
         * }
         * }
         */
        for (int i = 0; i < halfFftSize; i++) {
            double realValue = outputReal[i];
            double imagValue = outputImag[i];

            spectrum[i] = Math.sqrt(realValue * realValue + imagValue * imagValue);
        }

        int skippedBuckets = 4;
        spectrum = Arrays.copyOfRange(spectrum, skippedBuckets, halfFftSize);

        // Convert to dB
        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] = 20 * Math.log10(spectrum[i]);
        }

        // Compute max
        double max = 0;
        for (int i = 0; i < halfFftSize; i++) {
            double value = spectrum[i];

            if (value > max) {
                max = value;
            }
        }

        // Expand spectrum
        // Precompute scaled max value
        double scaledMax = max * 0.85;
        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] -= scaledMax;
        }

        // ~~Compute max again~~
        // spectrum[] is scaled so do the max value
        max -= scaledMax;

        /*
         * max = 0;
         * for (int i = 0; i < halfFftSize; i++) {
         * double value = spectrum[i];
         * 
         * if (value > max) {
         * max = value;
         * }
         * }
         */

        // Normalize spectrum
        for (int i = 0; i < halfFftSize; i++) {
            spectrum[i] /= max;
        }
    }

    public int getPeak() {
        return peakLoudness;
    }

    public double getRMS() {
        return rms;
    }

    public double[] getSpectrum() {
        return spectrum;
    }

    public double getRMSLoudness() {
        return (double) rms / Integer.MAX_VALUE;
    }

    public double getPeakLoudness() {
        return (double) peakLoudness / Integer.MAX_VALUE;
    }

    public void reset() {
        peakLoudness = 0;
        spectrum = null;
    }
}
