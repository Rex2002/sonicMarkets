package preDev;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.awt.*;
import java.util.Arrays;

public class Test {

    static int SAMPLE_RATE = 44100;

    public static void main(String[] args){
        System.out.println("Hello Sound");
        new Test();
    }

    public Test(){
        AudioFormat af = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
        try{
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            //byte[] cleanSine = createSine(440, 10, 127);
            //double[] lfo = createDoubleSine(1, 4, 1);
            byte[] sine = createSine(new double[]{440,  493.88,  523.25,  587.33,  659.25,  698.46,  783.99,  880.00}, 4, 127);
            //byte[] combined = multiplyArrays(sine, lfo);
            EventQueue.invokeLater(() -> {
                FrequencyChart c = new FrequencyChart(Arrays.copyOfRange(sine, 0, 44100), 1, "Raw Sine");
                //FrequencyChart c1 = new FrequencyChart(Arrays.copyOfRange(combined, 0, 44100*4), 100, "Combined");
                //FrequencyChart c2 = new FrequencyChart(Arrays.copyOfRange(lfo,0, 44100),1, "LFO");
                c.setVisible(true);
                //c1.setVisible(true);
//                c2.setVisible(true);
            });
            //play(sdl, cleanSine);
            play(sdl, sine);
            //play(sdl, sine);

            sdl.drain();
            sdl.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    // creates a sine wave of given frequency, duration and max amplitude
    private byte[] createSine(int freq, int duration, int amplitude){
        byte[] sin = new byte[duration * SAMPLE_RATE];
        double samplingInterval = (double) SAMPLE_RATE/freq ;
        //double lfoSamplingInterval = (double) SAMPLE_RATE/2;
        System.out.println("Frequency of signal: " + freq + " hz");
        System.out.println("Sampling interval: " + samplingInterval + " hz");
        for(int i = 0; i < sin.length; i++){
            double angle = ((2*Math.PI * i)/(samplingInterval));  // full circle: 2*Math.PI -> one step: divide by sampling interval
            //double lfo = ((2*Math.PI)/lfoSamplingInterval) * i;

            sin[i] = (byte) ((Math.sin(angle)) * (amplitude) );
        }
        return sin;
    }

    private byte[] createSine(double[] freq, int duration, int amplitude){
        byte[] sin = new byte[duration * SAMPLE_RATE];
        System.out.println("Frequency of signal: " + freq + " hz");
        double samplingInterval = 0;
        for(int i = 0; i < sin.length; i++){
            if((double) SAMPLE_RATE/freq[(int)( (double) i/sin.length * freq.length)] != samplingInterval) {
                samplingInterval = (double) SAMPLE_RATE / freq[(int) ((double)i / sin.length * (freq.length))];
                System.out.println("New sampling interval: " + samplingInterval);
            }

            double angle = ((2*Math.PI * i)/(samplingInterval));  // full circle: 2*Math.PI -> one step: divide by sampling interval
            sin[i] = (byte) ((Math.sin(angle)) * amplitude)
        }
        return sin;
    }

    // creates a sine wave in double format of given frequency, duration and max amplitude
    private double[] createDoubleSine(int freq, int duration, int amplitude){
        double[] sin = new double[duration * SAMPLE_RATE];
        double samplingInterval = (double) SAMPLE_RATE/freq;
        double lfoSamplingInterval = (double) SAMPLE_RATE/2;
        System.out.println("Frequency of signal: " + freq + " hz");
        System.out.println("Sampling interval: " + samplingInterval + " hz");
        for(int i = 0; i < sin.length; i++){
            double angle = ((2*Math.PI)/(samplingInterval)) * i;  // full circle: 2*Math.PI -> one step: divide by sampling interval
            //double lfo = ((2*Math.PI)/lfoSamplingInterval) * i;

            sin[i] = ((Math.sin(angle)) * (amplitude));
        }
        return sin;
    }
    private void play(SourceDataLine s, byte[] data){
        s.write(data, 0, data.length);
    }

    private static byte[] addArrays(byte[] first, byte[] second) {
        int maxLength = Math.max(first.length, second.length);
        int minLength = Math.min(first.length, second.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < minLength; i++) {
            result[i] = (byte) (first[i] + second[i]);
        }
        if(maxLength == first.length){
            System.arraycopy(first, minLength, result, minLength, maxLength - minLength);
        }
        else{
            System.arraycopy(second, minLength, result, minLength, maxLength - minLength);
        }

        return result;
    }

    private static byte[] multiplyArrays(byte[] first, double[] second) {
        int maxLength = Math.max(first.length, second.length);
        int minLength = Math.min(first.length, second.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < minLength; i++) {
            result[i] = (byte) (first[i] * second[i]);
        }
        if(maxLength == first.length){
            System.arraycopy(first, minLength, result, minLength, maxLength - minLength);
        }
        else{
            System.arraycopy(second, minLength, result, minLength, maxLength - minLength);
        }

        return result;
    }

    private static byte[] multiplyArrays(byte[] first, byte[] second) {
        int maxLength = Math.max(first.length, second.length);
        int minLength = Math.min(first.length, second.length);
        byte[] result = new byte[maxLength];

        for (int i = 0; i < minLength; i++) {
            result[i] = (byte) (first[i] * second[i]);
        }
        if(maxLength == first.length){
            System.arraycopy(first, minLength, result, minLength, maxLength - minLength);
        }
        else{
            System.arraycopy(second, minLength, result, minLength, maxLength - minLength);
        }

        return result;
    }
}
