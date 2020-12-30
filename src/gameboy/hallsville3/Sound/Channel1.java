package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel1 implements SoundChannel {
    public int timer;
    public int dutyIndex;
    public int duty;
    public int[] dutyCycles;

    public int soundLength;
    public Memory memory;

    public boolean enabled;

    public Channel1(Memory mem) {
        memory = mem;
        timer = 0;
        dutyIndex = 0;
        duty = 0;
        dutyCycles = new int[] {0b10000000, 0b10000001, 0b11000011, 0b11100111};
        enabled = false;
    }

    public void updateTimer(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            dutyIndex = (dutyIndex + 1) % 8;
            int x = memory.read(0xFF13) | ((memory.read(0xFF14) & 0b111) << 8);
            timer = 4 * (2048 - x);
            soundLength = memory.read(0xFF16) & 0b00111111;
        }
    }

    public void trigger() {

    }

    public byte doCycle(int cpuCycles) {
        updateTimer(cpuCycles);

        duty = dutyCycles[memory.read(0xFF11) >> 6];
        boolean toggle = (duty & (1 << dutyIndex)) == 1 << dutyIndex;

        enabled = (memory.read(0xFF12) & 0b11100000) != 0;

        int volume = 0;
        if (!enabled) {
            return 0;
        }
        return (byte) (toggle ? volume : 0);
    }
}
