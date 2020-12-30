package gameboy.hallsville3;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        GameBoy gb = new GameBoy();
        String game = "ROMs/cpu_instrs.gb";
        game = "ROMs/zelda.gb";
        game = "ROMs/mario.gb";
        //game = "ROMs/mario2.gb";
        //game = "ROMs/tetris.gb";
        //game = "ROMs/doctor.gb";
        //game = "ROMs/dmg-acid2.gb";
        //game = "ROMs/cpu_instrs/individual/2.gb";
        //game = "ROMs/kirby.gb";
        //game = "ROMs/pokemon.gb";
        gb.loadGame(game);
        gb.run();
    }
}
