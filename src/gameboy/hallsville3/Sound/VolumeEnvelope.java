package gameboy.hallsville3.Sound;

import gameboy.hallsville3.GameBoy;
import gameboy.hallsville3.Memory;

public class VolumeEnvelope {
    public int timer;
    public int mode;
    public int sweep;

    public int volume;

    public boolean finished;

    public char nr2;


    public Memory memory;

    public VolumeEnvelope(Memory mem, char nr0) {
        memory = mem;
        nr2 = (char)(nr0 + 2);
        volume = 15;
    }

    public void doCycle(int cpuCycles) {
        if (finished) {
            return;
        }
        timer += cpuCycles;
        if (timer >= GameBoy.CLOCK_SPEED / 64 * sweep) {
            timer -= GameBoy.CLOCK_SPEED / 64 * sweep;
            volume += mode;
            if (volume == 16) {
                finished = true;
                volume = 15;
            } else if (volume == -1) {
                finished = true;
                volume = 0;
            }
        }
    }

    public void trigger() {
        char envelopeRegister = memory.read(nr2);
        volume = envelopeRegister >> 4;
        mode = (envelopeRegister & 0b00001000) == 0b00001000 ? 1 : -1;
        sweep = envelopeRegister & 0b111;
        finished = false;
        timer = 0;
    }

    public int getVolume() {
        return volume;
    }
}
