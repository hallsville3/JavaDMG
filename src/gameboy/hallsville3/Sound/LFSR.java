package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class LFSR {
    public int lfsr;
    public char nr3;
    public Memory memory;
    public LFSR(Memory mem, char nr0) {
        nr3 = (char)(nr0 + 3);
        memory = mem;
        reset();
    }

    public void reset() {
        lfsr = 0x7FFF;
    }

    public void tick() {
        int xor = ((lfsr & 0b10) >> 1) ^ (lfsr & 0b01);
        lfsr = lfsr >> 1;
        lfsr = lfsr | (xor << 14);
        if ((memory.read(nr3) & 0b1000) == 0b1000) {
            // Mode width 7
            lfsr = (lfsr & ~(0b1 << 6)) | (xor << 6);
        }
    }

    public int getLastBit() {
        return ~lfsr & 0b1;
    }
}
