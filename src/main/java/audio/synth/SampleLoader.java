package audio.synth;

//import com.groupdocs.metadata.Metadata;
//import com.groupdocs.metadata.core.WavRootPackage;
//import com.groupdocs.metadata.internal.c.a.s.internal.nb.Au;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SampleLoader {
    public static double[] loadSample(String filename) {

        try{
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(filename));
            AudioFormat af = audioStream.getFormat();
            if(af.getFrameSize() != 4 || af.getSampleRate() != 44100 || af.isBigEndian()){
                throw new RuntimeException("Illegal Audioformat in sample" + filename);
            }
            double[] out = new double[(int) (audioStream.getFrameLength() * 2)];
            byte[] frame = new byte[4];
            for(int pos = 0; pos < audioStream.getFrameLength(); pos ++){
                audioStream.read(frame);
                out[2*pos] = frame[1]  << 8 | frame[0] & 0xFF;
                out[2*pos+1] = frame[3] << 8 | frame[2] & 0xFF;
            }
            System.out.println("Array length: " + out.length);
            return out;
        }catch (IOException | UnsupportedAudioFileException e){
            e.printStackTrace();
            throw new RuntimeException("error while reading sample file: ");
        }

    }
// INFO: only needed for debugging, only works when adding groupdocs to mvn and after uncommenting respective imports
//    public static void printAudioFormat(String filename){
//        try (Metadata metadata = new Metadata(filename))
//        {
//            WavRootPackage root = metadata.getRootPackageGeneric();
//            System.out.println(root.getWavPackage().getBitsPerSample()); // Bits per Sample
//            System.out.println(root.getWavPackage().getBlockAlign()); // Block Align
//            System.out.println(root.getWavPackage().getByteRate()); // Byte Rate
//            System.out.println(root.getWavPackage().getNumberOfChannels()); // No. of Channels
//            System.out.println(root.getWavPackage().getAudioFormat()); // Audio Format
//            System.out.println(root.getWavPackage().getSampleRate()); // Sample Rate
//        }
//    }
}