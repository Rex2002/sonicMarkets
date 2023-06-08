package audio.synth.generators;

import static audio.Constants.SAMPLE_RATE;

public class PhaseAdvancers {


    public static double advancePhaseSine(double phase, double freq){
        phase += 2 * Math.PI * freq/SAMPLE_RATE;
        while (phase > 2 * Math.PI){
            phase -= 2* Math.PI;
        }
        while (phase < 0){
            phase += 2 * Math.PI;
        }
        return phase;
    }
}