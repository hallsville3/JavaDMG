package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel4 extends NoiseChannel {
    public Channel4(Memory mem) {
        super(mem, (char)(0xFF1F));
    }

    public int getID() {
        return 3;
    }
}
