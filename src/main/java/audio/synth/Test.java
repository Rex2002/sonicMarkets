package audio.synth;

// freq: 440,  493.88,  523.25,  587.33,  659.25,  698.46,  783.99,  880.00


import audio.Constants;
import audio.synth.envelopes.ADSR;
import audio.synth.fx.Effect;
import audio.synth.fx.FilterData;
import audio.synth.generators.PhaseAdvancers;
import audio.synth.generators.PhaseContainer;
import audio.synth.generators.SineWaveGenerator;
import audio.synth.generators.WaveGenerator;
import state.EventQueues;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.awt.*;
import java.util.Arrays;

import static audio.synth.Util.findMax;

public class Test {
    static String waveFileName = "./src/main/resources/Casio-MT-45-Beguine.wav";
    static String waveFileFunk = "./src/main/resources/Yamaha-PSS-280-Funk.wav";
    static String waveFileLoFi = "./src/main/resources/lofi_research.wav";

    public static void main(String[] args){
        System.out.println("Hello Sound");
        new Test();
    }

    public Test(){
        AudioFormat af = new AudioFormat(Constants.SAMPLE_RATE, 16, Constants.CHANNEL_NO, true, true);

        try{
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            ADSR adsr = new ADSR(0.2, 0.2, 0.5, 0.3);
            WaveGenerator generator = new SineWaveGenerator();

            short[] sineEcho = Effect.echo(generator.generate(new double[]{440,  493.88,  523.25,  587.33 }, 8, new short[]{16383}, adsr), new double[]{0.9}, new int[]{15000});
            short[] sine1 = generator.generate(new double[]{440,440, 493.88,  493.88, 440,440,  523.25,  587.33, 440, 440 }, 40, new short[]{16383}, adsr);
            short[] sine2 = generator.generate(new double[]{523.25,  587.33,  659.25,  698.46, }, 8, new short[]{12000}, adsr);
            short[] addedSine = addArrays(sine1, sine2);

            //short[] fft = generator.generate(880, 2, new short[]{16383});
            //short[] fft2 = generator.generate(440, 2, new short[]{16383});
            //short[] addedfftSine  = addArrays(fftSine2, fftSine);
            short[] fft = SampleLoader.loadSample(waveFileName);
            FilterData filterData = new FilterData();
            filterData.setCutoff(new double[] {1200, 6000});
            filterData.setBandwidth(new double[]{0.5});
            filterData.setHighPass(false);
            short[] addedFilteredSine = Effect.IIR(fft, filterData);
            Complex[] fftOfSine = Util.fft(Arrays.copyOfRange(fft,0 , 2048));
            Complex[] fftOfFilteredSine = Util.fft(Arrays.copyOfRange(addedFilteredSine, 0, 2048));
            // Currently stereo samples can be played, but sounds a bit weird and is only half the speed
            //short[] drumSample = SampleLoader.loadSample(waveFileName);

            InstrumentData instrData = new InstrumentData();
            instrData.setInstrument(InstrumentEnum.SYNTH_ONE);
            instrData.setVolume(new double[]{15000, 7000});
            instrData.setPitch(new int[]{69, 70, 80});
            instrData.setPan(new double[]{0});
            instrData.setFilterData(filterData);

            short[] synthLine = new SynthLine(instrData, 6).synthesize();


            // mod freq factor of 1.5 seems to resemble a clarinet - though rather rough, could not yet figure out how to add more harmonics
            // TODO add calculation to actually play given freq when modulation and not just gcd of carrier and modulation frequency
            short[] mSine = generator.generate(new double[]{900}, 4, new short[]{15000},  2/3f);
            //short[] combined = multiplyArrays(sine, lfo);
            EventQueue.invokeLater(() -> {
                FrequencyChart c = new FrequencyChart(Arrays.copyOfRange(fftOfSine, 0, fftOfSine.length), 1, "Unfiltered");
                FrequencyChart c0 = new FrequencyChart(Arrays.copyOfRange(fftOfFilteredSine, 0, fftOfFilteredSine.length), 1, "Filtered");
                FrequencyChart c1 = new FrequencyChart(Arrays.copyOfRange(addedSine, 0, 44100), 1, "Added");
                //FrequencyChart c2 = new FrequencyChart(Arrays.copyOfRange(sw,0, 44100),1, "Sawtooth");
                //c.setVisible(true);
                //c0.setVisible(true);
                //c1.setVisible(true);
                //c2.setVisible(true);
            });
            playWithControls(sdl, sine1);
            System.out.println("we have reached the point");
            sdl.drain();
            sdl.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    // creates a sine wave of given frequency, duration and max amplitude


    private short[] createSawtooth(double[] freq, int duration, int amplitude){
        short[] sw = new short[duration * Constants.SAMPLE_RATE * Constants.CHANNEL_NO];
        PhaseContainer phases = new PhaseContainer();
        phases.phase = 0;
        for(int i = 0; i < sw.length; i += 2){
            PhaseAdvancers.advancePhaseSawtooth(phases, freq[(int) (((double) i / sw.length) * freq.length)]);
            sw[i] = (short) (phases.ret * amplitude);
            sw[i+1] = (short) (phases.ret * amplitude);
        }
        return sw;
    }

    private short[] createSawtooth(double[] freq, int duration, int amplitude, ADSR env){
        short[] sw = new short[duration * Constants.SAMPLE_RATE * Constants.CHANNEL_NO];
        env.setSectionLen(sw.length/freq.length);
        PhaseContainer phases = new PhaseContainer();
        phases.phase = 0;
        for(int i = 0; i < sw.length; i += 2){
            PhaseAdvancers.advancePhaseSawtooth(phases, freq[(int) (((double) i / sw.length) * freq.length)]);

            sw[i] = (short) (phases.ret * amplitude * env.getAmplitudeFactor(i));
            sw[i+1] = (short) (phases.ret * amplitude * env.getAmplitudeFactor(i));
        }
        return sw;
    }

    // creates a sine wave in double format of given frequency, duration and max amplitude
    private double[] createDoubleSine(int freq, int duration, int amplitude){
        double[] sin = new double[duration * Constants.SAMPLE_RATE * Constants.CHANNEL_NO];
        double samplingInterval = (double) Constants.SAMPLE_RATE/freq;
        for(int i = 0; i < sin.length; i += 2){
            double angle = ((2*Math.PI)/(samplingInterval)) * i;  // full circle: 2*Math.PI -> one step: divide by sampling interval
            //double lfo = ((2*Math.PI)/lfoSamplingInterval) * i;

            sin[i] = ((Math.sin(angle)) * (amplitude));
            sin[i+1] = ((Math.sin(angle)) * (amplitude));
        }
        return sin;
    }
    public static void play(SourceDataLine s, short[] data){
        byte[] out = new byte[data.length * 2];
        for(int p = 0; p < data.length; p++){
            out[2*p] = (byte) ((data[p] >> 8) & 0xFF);
            out[2*p+1] = (byte) (data[p] & 0xFF);
        }
        s.write(out, 0, out.length);
    }

    public static void playWithControls(SourceDataLine s, short[] data){
        //TODO test / implement edge behaviour
        System.out.println("Entering playWithControls.");
        System.out.println("The audio playback can be controlled with the following commands: \n p: pause \n r: resume \n jf: jump forward 2s \n jb: jump backwards 2s \n");
        System.out.println("Please do not try to test edge-case behaviour. It is untested and may result in outOfBoundsExceptions");
        int playbackSampleSize = 4410;
        int positionPointer = 0;
        boolean paused = false;
        Thread playController = new Thread(new PlaybackController());
        playController.start();
        System.out.println("started thread for playback controller.");
        System.out.println("playBackSampleSize/data.length: " + data.length/playbackSampleSize);
        byte[] outBuffer = new byte[playbackSampleSize * 2];
        while(positionPointer < data.length/playbackSampleSize) {
            if(!paused){
                for (int i = 0; i < playbackSampleSize; i++) {
                    outBuffer[2 * i] = (byte) ((data[positionPointer * playbackSampleSize + i] >> 8) & 0xFF);
                    outBuffer[2 * i + 1] = (byte) (data[positionPointer * playbackSampleSize + i] & 0xFF);
                }
                s.write(outBuffer, 0, outBuffer.length);
                positionPointer++;
            }


            if(!EventQueues.toPlayback.isEmpty()){
                System.out.println("getting a value from the event queue:" + EventQueues.toPlayback.peek());
                int queueValue = 0;
                try {
                    queueValue = EventQueues.toPlayback.take();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                System.out.println("EventQueueValue: " + queueValue);
                switch (queueValue) {
                    case -1 -> paused = true;
                    case -2 -> paused = false;
                    case 1 -> positionPointer += 20;
                    case 2 -> positionPointer -= 20;
                }
            }
        }
        System.out.println("finished loop");
    }

    private void play(SourceDataLine s, byte[] data){
        s.write(data, 0, data.length);
    }


    private static short[] addArrays(short[] first, short[] second) {
        return addArrays(first, second, 0);
    }

    private static short[] addArrays(short[] first, short[] second, int start) {
        // if start is zero, it does not matter which array is longer.
        // if start is not zero, we assume that the second array is meant to be added at the given position
        if(start != 0 && first.length < second.length + start){
            throw new RuntimeException("Illegal array addition, length not matching!");
        }
        int maxLength = Math.max(first.length, second.length);
        int minLength = Math.min(first.length, second.length);
        double resizingFactor = (double) 16383 / Math.max(findMax(first), findMax(second));
        short[] result = new short[maxLength];

        for( int i = 0; i < start; i++){
            result[i] = (short) (first[i] * resizingFactor);
        }

        for (int i = start; i < minLength + start; i++) {
            short secondValue = (short) (second[i-start] * resizingFactor);
            result[i] = (short) (first[i] * resizingFactor + second[i-start] * resizingFactor);
        }
        if(maxLength == first.length){
            for(int i = minLength + start; i<maxLength; i++) {
                result[i] = (short) (first[i] * resizingFactor);
            }
        }
        else{
            for(int i = minLength; i<maxLength; i++) {
                result[i] = (short) (second[i] * resizingFactor);
            }

        }
        return result;
    }



    @Deprecated
    private static short[] multiplyArrays(short[] first, double[] second) {
        int maxLength = Math.max(first.length, second.length);
        int minLength = Math.min(first.length, second.length);
        short[] result = new short[maxLength];

        for (int i = 0; i < minLength; i++) {
            result[i] = (short) (first[i] * second[i]);
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