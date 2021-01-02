package gameboy.hallsville3;

import gameboy.hallsville3.Sound.APU;

import java.io.IOException;

public class GameBoy {
    Memory memory;
    CPU cpu;
    PPU ppu;
    APU apu;
    Timer timer;
    Controller controller;
    Window window;

    public static int CLOCK_SPEED = 4194304;

    public GameBoy() {
        int memSize = 0xFFFF + 1;
        int scale = 7;
        controller = new Controller();
        apu = new APU();
        memory = new Memory(memSize, controller, apu);

        // Both controller and APU need access to the memory, but the memory needs them to. (Circular dependency)
        controller.setMemory(memory);
        apu.setMemory(memory);
        memory.memory[0xFF00] = 0xF;

        cpu = new CPU(memory);
        ppu = new PPU(memory, scale);
        timer = new Timer(memory);
        window = new Window(ppu, scale);

        window.frame.addKeyListener(controller);
    }

    public void loadGame(String game) throws IOException {
        cpu.loadGame(game);
    }

    public void run() {
        long time = 0;
        while (cpu.pc < 0xFFFF) {
            // Emulate one instruction
            int interruptCycles = cpu.handleInterrupts();
            cpu.doCycle();

            int cycles = interruptCycles + cpu.cycles;
            timer.update(cycles);
            ppu.doCycle(cycles);
            apu.doCycle(cycles);
            // The following code block helps to synchronize the video at the cost of smoothness
            //if (System.currentTimeMillis() - time > 17) {
            //    System.out.println(System.currentTimeMillis() - time);
            //    window.repaint();
            //    time = System.currentTimeMillis();
            //}
        }
    }
}
