package gameboy.hallsville3;

import gameboy.hallsville3.Sound.APU;

public class Memory {
    public char[] memory;
    public char[] cartridgeROM;
    public char[] cartridgeRAM;
    public boolean mbc1, mbc2;
    public boolean RAMEnabled;
    public int currentROMBank, currentRAMBank;
    Controller controller;
    APU apu;
    public boolean romBankingEnabled;

    public Memory(int memSize, Controller con, APU a) {
        memory = new char[memSize];
        controller = con;
        apu = a;
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

    public void changeLoROMBank(char value) {
        if (mbc2)
        {
            currentROMBank = value & 0xF;
            if (currentROMBank == 0) {
                currentROMBank++;
            }
            return;
        }

        char lower5 = (char)(value & 31);
        currentROMBank &= 224; // turn off the lower 5
        currentROMBank |= lower5 ;
        if (currentROMBank == 0) {
            currentROMBank++;
        }
    }

    public void changeHiROMBank(char value) {
        // turn off the upper 3 bits of the current rom
        currentROMBank &= 31;

        // turn off the lower 5 bits of the data
        value &= 224;
        currentROMBank |= value;
        if (currentROMBank == 0) {
            currentROMBank++;
        }
    }

    public void handleRAMEnable(char address, char value) {
        if (mbc2)
        {
            if ((address & 0b1000) == 0b1000) {
                return;
            }
        }

        char test = (char)(value & 0xF);
        if (test == 0xA) {
            RAMEnabled = true;
        } else if (test == 0x0) {
            RAMEnabled = false;
        }
    }

    public void changeRAMBank(char value) {
        currentRAMBank = value & 0x3 ;
    }

    public void changeRAMMode(char value) {
        char newValue = (char)(value & 0x1);
        romBankingEnabled = (newValue == 0);
        if (romBankingEnabled) {
            currentRAMBank = 0;
        }
    }

    public void handleBanking(char address, char value) {
        if (address < 0x2000) {
            // Ram enabling
            if (mbc1 || mbc2) {
                handleRAMEnable(address, value);
            }
        } else if (address < 0x4000) {
            if (mbc1 || mbc2) {
                changeLoROMBank(value);
            }
        } else if (address < 0x6000) {
            if (mbc1) {
                if (romBankingEnabled) {
                    changeHiROMBank(value);
                } else {
                    changeRAMBank(value);
                }
            }
        } else if (address < 0x8000) {
            changeRAMMode(value);
        }
    }

    public void write(char address, char value) {
        if (address < 0x8000) {handleBanking(address, value); return;} // No writes to fixed ROM, must be for banking
        if (address <= 0x9FFF) { // Can't write to VRAM in mode 3
            if ((memory[0xFF41] & 0b11) == 3) {
                return;
            }
        }

        if ((address >= 0xA000) && (address < 0xC000))
        {
            if (RAMEnabled)
            {
                char newAddress = (char)(address - 0xA000);
                cartridgeRAM[newAddress + (currentRAMBank*0x2000)] = value;
                return;
            }
        }

        if ((address >= 0xE000) && (address < 0xFE00)) { // Echo ram needs to echo to RAM
            memory[address] = value;
            write(address - 0x2000, value);
            return;
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
        } else if (address == 0xFF14 || address == 0xFF19 || address == 0xFF1E || address == 0xFF23) {
            apu.handleNR4(address, value);
            memory[address] = value;
        } else if (address == 0xFF11 || address == 0xFF16 || address == 0xFF1B) {
            apu.handleNR1(address);
            memory[address] = value;
        } else {
            memory[address] = value;
        }
    }

    public void write(int address, char value) {
        write((char)address, value);
    }

    public char read(char address) {
        if (address >= 0x4000 && address <= 0x7FFF) { // Banked ROM
            char newAddress = (char)(address - 0x4000);
            return cartridgeROM[newAddress + (currentROMBank * 0x4000)] ;
        } else if (address >= 0xA000 && address <= 0xBFFF) { // Banked RAM
            char newAddress = (char)(address - 0xA000);
            return cartridgeRAM[newAddress + (currentRAMBank * 0x2000)] ;
        }

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

    public void loadCartridge(byte[] gameData) {
        cartridgeROM = new char[gameData.length];
        for (int i = 0; i < gameData.length; i++) {
            cartridgeROM[i] = (char) (gameData[i] & 0xFF); // 0xFF to fix Java signed byte-age
            if (i < 0x8000) { // Load the first 0x8000 bytes into memory on the GameBoy
                memory[i] = cartridgeROM[i];
            }
        }

        // What MBC is available?
        mbc1 = false;
        mbc2 = false;

        char mbcRegister = cartridgeROM[0x147];
        if (mbcRegister == 1 || mbcRegister == 2 || mbcRegister == 3) {
            mbc1 = true;
        } else if (mbcRegister == 4 || mbcRegister == 5) {
            mbc2 = true;
        }

        currentROMBank = 1;

        // Now we must initialize the cartridge RAM
        cartridgeRAM = new char[0x8000]; // It can have up to 4 0x2000 size banks
        currentRAMBank = 0;

    }
}
