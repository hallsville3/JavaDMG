package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public abstract class SquareChannel extends SoundChannel {
    public int timer;

    public int dutyIndex;
    public int duty;
    public int[] dutyCycles;

    public Memory memory;
    public VolumeEnvelope envelope;
    public LengthCounter counter;

    public boolean enabled;

    public SquareChannel(Memory mem, char n) {
        super(n);
        memory = mem;
        timer = 0;
        dutyIndex = 0;
        duty = 0;
        dutyCycles = new int[] {0b00000001, 0b10000001, 0b10000111, 0b01111110};
        enabled = false;

        envelope = new VolumeEnvelope(memory, nr0);
        counter = new LengthCounter(memory, nr0, 0b00111111);
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            dutyIndex = (dutyIndex + 1) % 8;
            int x = memory.read(nr3) | ((memory.read(nr4) & 0b111) << 8);
            timer = 4 * (2048 - x);
        }
    }

    public void trigger() {
        envelope.trigger();
    }

    public void load() {
        counter.load();
    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        envelope.doCycle(cpuCycles);
        counter.doCycle(cpuCycles);

        int volume;
        if (counter.isEnabled()) {
            volume = envelope.getVolume();
        } else {
            volume = 0;
        }

        duty = dutyCycles[memory.read(nr1) >> 6];
        boolean toggle = (duty & (1 << dutyIndex)) == 1 << dutyIndex;
        enabled = (memory.read(nr2) & 0b11110000) != 0;

        if (!enabled) {
            return 0;
        }
        return (byte) (toggle ? volume : -volume);
    }
}
