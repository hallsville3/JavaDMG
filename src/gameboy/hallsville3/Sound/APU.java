package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;

import javax.sound.sampled.*;

public class APU {
    public int loc;
    int bufSize;
    public int sampleRate = 48000;
    byte[] buffer;

    AudioFormat af;
    DataLine.Info info;
    public SourceDataLine line;

    Memory memory;

    SoundChannel[] soundChannels;
    int sampleFreq;

    int oversample;

    public APU() {
        loc = 0;
        oversample = 1;
        initialize();
    }

    public void setMemory(Memory mem) {
        memory = mem;
        soundChannels = new SoundChannel[] {new Channel1(memory), new Channel2(memory), new Channel3(memory), new Channel4(memory)};
    }

    public void doCycle(int cpuCycles) {
        sampleFreq += cpuCycles;

        byte left = 0;
        byte right = 0;
        if ((memory.read(0xFF26) & 0b10000000) != 0) { // Volume is enabled
            for (SoundChannel channel : soundChannels) { // Adjust for signed output
                int output = channel.doCycle(cpuCycles) / 4;
                boolean rightChannel = (memory.read(0xFF25) >> (channel.getID()) & 0b1) == 0b1;
                boolean leftChannel = (memory.read(0xFF25) >> (4 + channel.getID()) & 0b1) == 0b1;

                if (rightChannel) {
                    right += output;
                }

                if (leftChannel) {
                    left += output;
                }
            }
        }

        // Now scale left and right by their respective volumes
        right *= memory.read(0xFF24) & 0b111;
        left *= (memory.read(0xFF24) >> 4) & 0b111;

        while (sampleFreq >= GameBoy.CLOCK_SPEED / sampleRate / oversample) {
            sampleFreq -= GameBoy.CLOCK_SPEED / sampleRate / oversample;
            addSample(left, right);
        }
    }

    public void initialize() {
        af = new AudioFormat(sampleRate, 16, 2, true, true);
        info = new DataLine.Info(SourceDataLine.class, af);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af, 20000);
            bufSize = line.getBufferSize();
            buffer = new byte[bufSize];
        } catch (LineUnavailableException e) {
            System.out.println("Could not acquire sound device.");
            System.exit(1);
        }
        line.start();
    }

    public void addSample(byte left, byte right){
        // Since we are using 16 bit output
        buffer[loc] = left; // Left Channel
        buffer[loc + 1] = 0;

        buffer[loc + 2] = right; // Right Channel
        buffer[loc + 3] = 0;
        loc += 4;
        if (loc >= bufSize) { // If we are going beyond the end of the buffer, dump the audio for safety
            loc = 0;
        }
    }

    public static byte interpolateLinear(int x0, int x1, double t) {
        return (byte)(t * x0 + (1 - t) * x1);
    }

    public byte[] getInterpolatedBuffer() {
        byte[] interp = new byte[buffer.length];
        int s = 0;
        for (double i = 0; i < loc - oversample * 4; i += oversample * 4) { // 4 for 2 channel 16 bit audio
            double t = i / loc; // Ranges from 0 to 1 through original samples
            int l = (int) (t * loc) / 4 * 4;
            int r = l + 4;
            t = t * loc - l;

            interp[4 * s] = interpolateLinear(buffer[l], buffer[r], t);
            interp[4 * s + 1] = (byte)0;
            interp[4 * s + 2] = interpolateLinear(buffer[l + 2], buffer[r + 2], t);
            interp[4 * s + 3] = (byte)0;
            s++;
        }
        return interp;
    }

    public byte[] dropInterpolate() {
        byte[] interp = new byte[buffer.length];
        int s = 0;
        for (double i = 0; i < loc; i += oversample * 4) { // 4 for 2 channel 16 bit audio
            double t = i / loc; // Ranges from 0 to 1 through original samples
            interp[4 * s] = buffer[(int) (t * loc) / 4 * 4];
            interp[4 * s + 1] = buffer[(int) (t * loc) / 4 * 4 + 1];
            interp[4 * s + 2] = buffer[(int) (t * loc) / 4 * 4 + 2];
            interp[4 * s + 3] = buffer[(int) (t * loc) / 4 * 4 + 3];
            s++;
        }
        return interp;
    }

    public void play() {
        line.write(dropInterpolate(), 0, loc / oversample / 4 * 4);
        loc = 0;
    }

    public void stop() {
        line.drain();
        line.stop();
        line.close();
    }

    public void handleNR4(char address, char value) {
        // Checks if any channels need to be triggered
        if (address == 0xFF14) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[0].trigger();
            }
        } else if (address == 0xFF19) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[1].trigger();
            }
        } else if (address == 0xFF1E) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[2].trigger();
            }
        } else if (address == 0xFF23) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[3].trigger();
            }
        }
    }

    public void handleNR1(char address) {
        //Checks if any channel needs to be reloaded
        if (address == 0xFF11) {
            soundChannels[0].load();
        } else if (address == 0xFF16) {
            soundChannels[1].load();
        } else if (address == 0xFF1B) {
            soundChannels[2].load();
        } else if (address == 0xFF20) {
            soundChannels[3].load();
        }

    }
}
