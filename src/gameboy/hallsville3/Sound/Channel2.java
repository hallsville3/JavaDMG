package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel2 extends SquareChannel {
    public Channel2(Memory mem) {
        super(mem, (char)(0xFF15));
    }
}
