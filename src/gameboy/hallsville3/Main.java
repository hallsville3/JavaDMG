package gameboy.hallsville3;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        GameBoy gb = new GameBoy();
        String game = "ROMs/instr_timing.gb";
        //game = "ROMs/zelda.gb";
        //game = "ROMs/mario.gb";
        //game = "ROMs/tetris.gb";
        //game = "ROMs/doctor.gb";
        //game = "ROMs/dmg-acid2.gb";
        //game = "ROMs/cpu_instrs/individual/2.gb";
        gb.loadGame(game);
        gb.run();
    }
}
