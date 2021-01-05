package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;

public class LengthCounter {
    public int timer;
    public int counter;
    public int lengthMask;

    public char nr1, nr4;

    public boolean enabled;

    public Memory memory;

    public LengthCounter(Memory mem, char nr0, int mask) {
        // lengthMask is either 0b111111 or 0b11111111 depending on channel type
        lengthMask = mask;
        memory = mem;
        nr1 = (char)(nr0 + 1);
        nr4 = (char)(nr0 + 4);
    }

    public void doCycle(int cpuCycles) {
        timer += cpuCycles;
        if (timer >= GameBoy.CLOCK_SPEED / 256) {
            timer -= GameBoy.CLOCK_SPEED / 256;
            if ((memory.read(nr4) & 0b01000000) == 0b01000000) { // Length Counter is enabled
                if (counter > 0) {
                    counter--;
                    if (counter == 0) {
                        enabled = false;
                    }
                }
            }
        }
    }

    public void load() {
        counter = memory.read(nr1) & lengthMask;
        if (counter > 0) {
            enabled = true;
        }
        timer = 0;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
