package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;
import jdk.swing.interop.SwingInterOpUtils;

import javax.sound.sampled.*;

public class APU {
    public int loc;
    int bufSize;
    public int sampleRate = 44100;
    byte[] buffer;

    AudioFormat af;
    DataLine.Info info;
    public SourceDataLine line;

    Memory memory;

    SoundChannel[] soundChannels;
    int sampleFreq;

    public APU() {
        loc = 0;
        initialize();
    }

    public void setMemory(Memory mem) {
        memory = mem;
        soundChannels = new SoundChannel[] {new Channel1(memory), new Channel2(memory)};
    }

    public void doCycle(int cpuCycles) {
        sampleFreq += cpuCycles;

        byte left = 0;
        byte right = 0;
        byte volume = 1;
        for (SoundChannel channel: soundChannels) { // Adjust for signed output
            int output = channel.doCycle(cpuCycles) * volume / 4;
            boolean leftChannel = (memory.read(0xFF25) >> (channel.getID()) & 0b1) == 0b1;
            boolean rightChannel = (memory.read(0xFF25) >> (4 + channel.getID()) & 0b1) == 0b1;

            if (leftChannel) {
                left += output;
            }

            if (rightChannel) {
                right += output;
            }
        }

        if (sampleFreq >= GameBoy.CLOCK_SPEED / sampleRate) {
            sampleFreq -= GameBoy.CLOCK_SPEED / sampleRate;
            if ((memory.read(0xFF26) & 0b10000000) != 0) { // Volume is enabled
                addSample(left, right);
            } else {
                addSample((byte)0, (byte)0);
            }
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

    public void play() {
        line.write(buffer, 0, loc);
        loc = 0;
    }

    public void stop() {
        line.drain();
        line.stop();
        line.close();
    }

    public void handleNR4(char address, char value) {
        // Checks if any channels need to be triggered
        if (address == 0xFF19) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[1].trigger();
            }
        } else if (address == 0xFF14) {
            if ((value & 0b10000000) == 0b10000000) {
                // This is a trigger
                soundChannels[0].trigger();
            }
        }
    }
}
