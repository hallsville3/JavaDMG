package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel2 implements SoundChannel {
    public int timer;
    public int dutyIndex;
    public int duty;
    public int[] dutyCycles;
    public Memory memory;

    public Channel2(Memory mem) {
        memory = mem;
        timer = 0;
        dutyIndex = 0;
        duty = 0;
        dutyCycles = new int[] {0b10000000, 0b10000001, 0b11000011, 0b11100111};
    }

    public byte doCycle(int cpuCycles) {
        timer -= cpuCycles;
        if (timer <= 0) {
            dutyIndex = (dutyIndex + 1) % 8;
            int x = memory.read(0xFF18) | ((memory.read(0xFF19) & 0b111) << 8);
            timer = 4 * (2048 - x);
        }

        duty = dutyCycles[memory.read(0xFF16) >> 6];
        boolean toggle = (duty & (1 << dutyIndex)) == 1 << dutyIndex;
        return (byte) (toggle ? 1 : -1);
    }
}
