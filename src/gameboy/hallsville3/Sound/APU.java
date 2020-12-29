package gameboy.hallsville3.Sound;

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
    Channel2 ch2;
    int sampleFreq;

    public APU(Memory mem) {
        loc = 0;
        bufSize = 2048;
        buffer = new byte[bufSize];
        memory = mem;
        initialize();

        soundChannels = new SoundChannel[] {new Channel1(memory), new Channel2(memory)};
    }

    public void doCycle(int cpuCycles) {
        sampleFreq += cpuCycles;

        byte output = 0;
        for (SoundChannel channel: soundChannels) {
            output += channel.doCycle(cpuCycles) / 4;
        }

        if (sampleFreq >= 4194304 / sampleRate) {
            sampleFreq -= 4194304 / sampleRate;
            addSample(output);
        }
    }

    public void initialize() {
        af = new AudioFormat(sampleRate, bitDepth, 1, true, false);
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
}
