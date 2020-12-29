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
    int duty2 = 0;
    int timer, timer2;
    int sampleFreq;

    public APU(Memory mem) {
        loc = 0;
        bufSize = 2048;
        buffer = new byte[bufSize];
        memory = mem;
        timer = 0;
        timer2 = 0;
        initialize();
    }

    public void doCycle(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            duty = (duty + 1) % 8;
            int x = memory.read(0xFF18) | ((memory.read(0xFF19) & 0b111) << 8);
            timer = 4 * (2048 - x);
        }

        timer2 -= cpuCycles;
        if (timer2 <= 0) {
            duty2 = (duty2 + 1) % 8;
            int x2 = memory.read(0xFF13) | ((memory.read(0xFF14) & 0b111) << 8);
            timer2 = 4 * (2048 - x2);
        }

        sampleFreq += cpuCycles;
        if (sampleFreq >= 87) {
            sampleFreq -= 87;
            addSample((byte) ((duty2 < 4 ? 30 : -30) + (duty < 4 ? 30 : -30)));
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

    public void addSample(byte f){
        buffer[loc] = f;
        loc++;
        if (loc == bufSize) {
            // Buffer is full
            play(loc);
            loc = 0;
        }
    }

    public void play(int loc) {
        line.write(buffer, 0, loc);
    }

    public void stop() {
        line.drain();
        line.stop();
        line.close();
    }
}
