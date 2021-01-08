package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public abstract class NoiseChannel extends SoundChannel{
    public int timer;

    public Memory memory;
    public VolumeEnvelope envelope;
    public LengthCounter counter;
    public LFSR lfsr;

    public boolean enabled;

    public NoiseChannel(Memory mem, char n) {
        super(n);
        memory = mem;
        timer = 0;
        enabled = false;

        envelope = new VolumeEnvelope(memory, nr0);
        counter = new LengthCounter(memory, nr0, 0b00111111);

        lfsr = new LFSR(memory, nr0);
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        while (timer <= 0) {
            int x = getShiftedDivisor();
            timer += x;
            lfsr.tick();
        }
    }

    public void trigger() {
        lfsr.reset();
        envelope.trigger();
    }

    public void load() {
        counter.load();
    }

    public int getDivisor() {
        int divisorCode = memory.read(nr3) & 0b111;
        if (divisorCode == 0) {
            return 8;
        }
        return 16 * divisorCode;
    }

    public int getShiftedDivisor() {
        return getDivisor() << ((memory.read(nr3) & 0b11110000) >> 4);
    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        envelope.doCycle(cpuCycles);
        counter.doCycle(cpuCycles);

        enabled = (memory.read(nr2) & 0b11110000) != 0;

        if (!enabled) {
            return 0;
        }

        int volume = envelope.getVolume();

        if (!counter.isEnabled()) {
            return 0;
        }

        return (byte) (lfsr.getLastBit() == 0 ? volume : -volume);
    }
}
