package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;

import javax.sound.sampled.*;

public class APU {
    int loc;
    int bufSize;
    int sampleRate = 48000;
    int bitDepth = 8;
    byte[] buffer;

    AudioFormat af;
    DataLine.Info info;
    SourceDataLine line;

    Memory memory;

    SoundChannel[] soundChannels;
    int sampleFreq;

    public APU() {
        loc = 0;
        bufSize = 2048;
        buffer = new byte[bufSize];
        initialize();
    }

    public void setMemory(Memory mem) {
        memory = mem;
        soundChannels = new SoundChannel[] {new Channel1(memory), new Channel2(memory)};
    }

    public void doCycle(int cpuCycles) {
        sampleFreq += cpuCycles;

        byte output = 0;
        byte volume = 1;
        for (SoundChannel channel: soundChannels) {
            output += channel.doCycle(cpuCycles) * volume / 4;
        }

        if (sampleFreq >= GameBoy.CLOCK_SPEED / sampleRate) {
            sampleFreq -= GameBoy.CLOCK_SPEED / sampleRate;
            addSample(output);
        }
    }

    public void initialize() {
        af = new AudioFormat(sampleRate, bitDepth, 1, false, false);
        info = new DataLine.Info(SourceDataLine.class, af);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af, 2048);
        } catch (LineUnavailableException e) {
            System.out.println("Could not acquire sound device.");
            System.exit(1);
        }

        line.start();
    }

    public void addSample(byte s){
        buffer[loc] = s;
        loc++;
        if (loc == bufSize / 2) {
            // Buffer is full
            play();
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
