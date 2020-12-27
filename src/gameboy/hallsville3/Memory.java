package gameboy.hallsville3;

public class Memory {
    public char[] memory;
    Controller controller;

    public Memory(int memSize, Controller con) {
        memory = new char[memSize];
        controller = con;
    }

    public void doDMATransfer(char value) {
        char address = (char)(value << 8);
        for (int i = 0; i < 0xA0; i++) {
            memory[0xFE00 + i] = read(address + i);
        }
    }

    public void setInterrupt(int i) {
        write(0xFF0F, (char) (read(0xFF0F) | 1 << i));
    }

    public void forceWrite(int address, char value) {
        forceWrite((char) address, value);
    }
    public void forceWrite(char address, char value) {
        memory[address] = value;
    }

    public void write(char address, char value) {
        if (address >= 0x150 && address <= 0x3FFF) { // No writes to the rom allowed
            return;
        }
        if (address >= 0x8000 && address <= 0x9FFF) { // VRAM
            if ((memory[0xFF41] & 0b11) == 3) {
                return;
            }
        }

        if (address >= 0xFE00 && address <= 0xFE9F) { // OAM
            if ((memory[0xFF41] & 0b11) == 2 || (memory[0xFF41] & 0b11) == 3) {
                return;
            }
        }

        if (address == 0xFF41) {
            memory[address] &= 0x07;
            memory[address] |= value;
        }


        if (address == 0xFF00) {
            memory[0xFF00] &= 0x0F;
            memory[0xFF00] |= value & 0xF0;
        }

        if (address == 0xFF44) {
            memory[address] = 0;
        } else if (address == 0xFF04) { // Divider Register
            memory[0xFF04] = 0;
            memory[0xFF05] = 0; // Also reset the regular timer
        } else if (address == 0xFF46) {
            doDMATransfer(value);
        } else {
            memory[address] = value;
        }
    }

    public void write(int address, char value) {
        write((char)address, value);
    }

    public char read(char address) {
        if (address == 0xFF00) {
            return controller.get(memory[0xFF00]);
        } else if (address == 0xFF0F) {
            return (char)(0xF0 | memory[0xFF0F]);
        }
        return memory[address];
    }

    public char read(int address) {
        return read((char)address);
    }
}
