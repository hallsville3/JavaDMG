package gameboy.hallsville3;


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

    int duty = 0;
    int timer;
    int sampleFreq;

    public APU(Memory mem) {
        loc = 0;
        bufSize = 2048;
        buffer = new byte[bufSize];
        memory = mem;
        timer = 0;
        initialize();
    }

    public void doCycle(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            duty = (duty + 1) % 8;
            int x = memory.read(0xFF18) | ((memory.read(0xFF19) & 0b111) << 8);
            timer = 4 * (2048 - x);
        }
        sampleFreq += cpuCycles;
        if (sampleFreq >= 87) {
            sampleFreq -= 87;
            addSample((byte) (duty < 4 ? 30 : -30));
        }
    }

    public void test() {
        for (int i = 0; i < 4; i++) {
            for (int f = 55; f < 1760; f *= 2) {
                addFrequency(f, 4800);
            }
            for (int f = 1760; f > 55; f /= 2) {
                addFrequency(f, 4800);
            }
        }
    }

    public void initialize() {
        af = new AudioFormat(sampleRate, bitDepth, 1, true, false);
        info = new DataLine.Info(SourceDataLine.class, af);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(af, 48000 * 4);
        } catch (LineUnavailableException e) {
            System.out.println("Could not acquire sound device.");
            System.exit(1);
        }

        line.start();
    }

    public void addFrequency(int f, int duration) {
        for (int i = 0; i < duration; i++) {
            if ((i % (48000 / f)) < 48000 / f / 2) {
                addSample((byte)30);
            } else {
                addSample((byte)-30);
            }
        }
    }

    public void addSample(byte f){
        buffer[loc] = f;
        loc++;
        if (loc == bufSize) {
            // Buffer is full
            loc = 0;
            play();
        }
    }

    public void play() {
        line.write(buffer, 0, buffer.length);
    }

    public void stop() {
        line.drain();
        line.stop();
        line.close();
    }
}
