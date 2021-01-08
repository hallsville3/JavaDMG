package gameboy.hallsville3;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        GameBoy gb = new GameBoy();
        String game = "ROMS/kirby.gb";
        //game = "ROMs/castlevania2.gb";
        //game = "ROMs/zelda.gb";
        //game = "ROMs/mario.gb";
        //game = "ROMs/sound/08-len ctr during power.gb";
        //game = "ROMs/halt_bug.gb";
        //game = "ROMs/mario2.gb";
        //game = "ROMs/tetris.gb";
        //game = "ROMs/doctor.gb";
        //game = "ROMs/dmg-acid2.gb";
        //game = "ROMs/pokemon.gb";
        gb.loadGame(game);
        gb.run();
    }
}
