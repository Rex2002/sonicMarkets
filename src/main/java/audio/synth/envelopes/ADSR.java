package audio.synth.envelopes;

public class ADSR implements Envelope{
    private final double attack;
    private final double decay;
    private final double sustain;
    private final double release;
    private int envLen;

    public ADSR(double attack, double decay, double sustain, double release) {
        this.attack = attack;
        this.decay = decay;
        this.sustain = sustain;
        this.release = release;
        if (attack + decay + release > 0.99) {
            throw new RuntimeException("adsr envelope with invalid parameters initiated");
        }
    }


    public double getAmplitudeFactor(int pos) {
        pos = pos % envLen;
        double relPos = (double) pos / envLen;
        if (relPos > 1 || relPos < 0) {
            throw new RuntimeException("rel. position calculation failed: " + relPos);
        }
        if(relPos < attack){
            return relPos * 1/attack;
        }
        if(relPos < decay+attack){
            return relPos * (sustain-1)/(decay) + (1-(attack*sustain-attack)/(decay));
        }
        if(relPos < 1-release){
            return sustain;
        }
        else {
            return -sustain / release * relPos + sustain / release;
        }
    }
    public void setSectionLen(int envLen){
        this.envLen = envLen;
    }
}