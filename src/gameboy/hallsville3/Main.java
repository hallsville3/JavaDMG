package gameboy.hallsville3;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        GameBoy gb = new GameBoy();
        String game = "ROMs/01-read_timing.gb";
        //game = "ROMs/mooneye/acceptance/timer/tim00.gb";
        //game = "ROMs/zelda.gb";
        //game = "ROMs/mario.gb";
        //game = "ROMs/tetris.gb";
        //game = "ROMs/doctor.gb";
        //game = "ROMs/dmg-acid2.gb";
        gb.loadGame(game);
        gb.run();
    }
}
