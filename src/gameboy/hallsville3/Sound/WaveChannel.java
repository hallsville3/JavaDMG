package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public abstract class WaveChannel extends SoundChannel {
    public int timer;

    public int waveTableIndex;

    public Memory memory;

    public LengthCounter counter;

    public WaveChannel(Memory mem, char n) {
        super(n);
        memory = mem;
        timer = 0;
        waveTableIndex = 0;

        counter = new LengthCounter(memory, nr0, 0b11111111);
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        while (timer <= 0) {
            waveTableIndex = (waveTableIndex + 1) % 32; // Increment the waveTableIndex

            int x = memory.read(nr3) | ((memory.read(nr4) & 0b111) << 8);
            timer += 2 * (2048 - x);
        }
    }

    public void trigger() {
        waveTableIndex = 0;
    }

    public void load() {
        counter.load();
    }

    public int getVolume() {
        return (memory.read(nr2) >> 5) & 0b11;
    }

    public int getVolumeShift(int volume) {
        int shift = 0;
        switch (volume) {
            case 0 -> shift = 4;
            case 1 -> shift = 0;
            case 2 -> shift = 1;
            case 3 -> shift = 2;
        }
        return shift;
    }

    public byte getWaveTableEntry() {
        int address = waveTableIndex / 2;
        int offset = waveTableIndex % 2;
        int sample = memory.read(0xFF30 + address);
        if (offset == 0) {
            sample = sample >> 4;
        }
        return (byte)(sample & 0b1111);
    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        counter.doCycle(cpuCycles);

        int volume;
        boolean DAC = (memory.read(nr0) & 0b10000000) == 0b10000000;
        if (counter.isEnabled() && DAC) {
            volume = getVolume();
        } else {
            volume = 0;
        }

        int volumeShift = getVolumeShift(volume);

        return (byte)(getWaveTableEntry() >> volumeShift);
    }
}
