package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public abstract class SquareChannel implements SoundChannel {
    public char nr0, nr1, nr2, nr3, nr4;
    public int timer;

    public int dutyIndex;
    public int duty;
    public int[] dutyCycles;

    public int soundLength;
    public Memory memory;
    public VolumeEnvelope envelope;

    public boolean enabled;

    public SquareChannel(Memory mem, char n) {
        memory = mem;
        timer = 0;
        dutyIndex = 0;
        duty = 0;
        soundLength = 0;
        dutyCycles = new int[] {0b00000001, 0b10000001, 0b10000111, 0b01111110};
        enabled = false;

        nr0 = n;
        nr1 = (char)(n+1);
        nr2 = (char)(n+2);
        nr3 = (char)(n+3);
        nr4 = (char)(n+4);

        envelope = new VolumeEnvelope(memory, nr2);
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            dutyIndex = (dutyIndex + 1) % 8;
            int x = memory.read(nr3) | ((memory.read(nr4) & 0b111) << 8);
            timer = 4 * (2048 - x);
            soundLength = memory.read(nr1) & 0b00111111;
        }
    }

    public void trigger() {
        envelope.trigger();
    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        envelope.doCycle(cpuCycles);

        duty = dutyCycles[memory.read(nr1) >> 6];
        boolean toggle = (duty & (1 << dutyIndex)) == 1 << dutyIndex;
        enabled = (memory.read(nr2) & 0b11100000) != 0;

        if (!enabled) {
            return 0;
        }
        return (byte) (toggle ? envelope.getVolume() : -envelope.getVolume());
    }
}
