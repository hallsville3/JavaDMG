package gameboy.hallsville3;

public class Timer {
    public Memory memory;
    public int cycles, dividerCycles;
    public Timer(Memory memory) {
        this.memory = memory;
        cycles = 0;
        dividerCycles = 0;
    }

    public boolean isEnabled() {
        return (memory.read(0xFF07) & 0b100) == 0b100;
    }

    public int getCycles() {
        switch (memory.read(0xFF07) & 0b11) {
            case 0 -> {return 1024;}
            case 1 -> {return 16;}
            case 2 -> {return 64;}
            case 3 -> {return 256;}
        }
        return 1024;
    }

    public void updateDivider(int cpuCycles) {
        dividerCycles += cpuCycles;
        if (dividerCycles > 255) {
            dividerCycles -= 256;
            memory.memory[0xFF04] = (char)((memory.memory[0xFF04] + 1) & 0xFF);
        }
    }

    public void update(int cpuCycles) {
        updateDivider(cpuCycles);
        if (isEnabled()) {
            cycles += cpuCycles;
            while (cycles >= getCycles()) {
                cycles -= getCycles();
                memory.write(0xFF05, (char) ((memory.read(0xFF05) + 1) & 0xFF));
                if (memory.read(0xFF05) == 0) {
                    // Request an interrupt and reset to value in 0xFF06
                    memory.write(0xFF05, memory.read(0xFF06));
                    memory.setInterrupt(2);
                }
            }
        }
    }
}
