package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel1 extends SquareChannel {
    public Channel1(Memory mem) {
        super(mem, (char)(0xFF10));
    }

    public int getID() {
        return 0;
    }
}