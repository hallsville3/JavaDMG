package gameboy.hallsville3;

public class Timer {
    public Memory memory;
    public int cycles;
    public Timer(Memory memory) {
        this.memory = memory;
        cycles = 0;
    }

    public boolean isEnabled() {
        return (memory.read(0xFF07) & 0b100) == 0b100;
    }

    public int getFrequency() {
        switch (memory.read(0xFF07) & 0b11) {
            case 0 -> {return 4096;}
            case 1 -> {return 262144;}
            case 2 -> {return 65536;}
            case 3 -> {return 16382;}
        }
        return 4096;
    }

    public void update(int cpuCycles) {
        if (isEnabled()) {
            cycles += cpuCycles;
            if (cycles >= 4194304 / getFrequency()) {
                cycles = 0;
                memory.write(0xFF05, (char) ((memory.read(0xFF05) + 1) % 256));
                if (memory.read(0xFF05) == 0) {
                    // Request an interrupt and reset to value in 0xFF06
                    memory.write(0xFF05, memory.read(0xFF06));
                    memory.setInterrupt(2);
                }
            }
        }
    }
}
