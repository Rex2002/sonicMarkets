package audio.mixer;

public class Mixer {
    /**
     * Mixes the given audio streams together.
     *
     * @param audioStreams array of double-arrays to be mixed
     * @return mixed audio stream as double-array
     */
    public static double[] mixAudioStreams(double[][] audioStreams, int numberEvLines, int indexBacking) {
        int maxLength = 0;
        for (double[] stream : audioStreams) {
            maxLength = Math.max(maxLength, stream.length);
        }

        double[] out = new double[maxLength];

        for (int i = 0; i < audioStreams.length; i++) {
            double[] audioStream = audioStreams[i];

            double mixFactor = 1;

            //reduce volume of event instruments
            if (i < numberEvLines) {
                mixFactor = 0.65;
            }
            // reduce volume of backing
            if (i == indexBacking) {
                mixFactor = 0.95;
            }

            for (int j = 0; j < audioStream.length; j++) {
                out[j] += mixFactor * audioStream[j];
            }
        }
        return out;
    }
}