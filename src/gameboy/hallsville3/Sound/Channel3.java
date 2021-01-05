package gameboy.hallsville3.Sound;

import gameboy.hallsville3.Memory;

public class Channel3 extends WaveChannel {
    public Channel3(Memory mem) {
        super(mem, (char)(0xFF1A));
    }

    public int getID() {
        return 2;
    }
}
