package gameboy.hallsville3;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;

public class CPU {
    public char opcode;         //Current opcode
    public int opcodeCount = 0;
    public int opcodeCountLimit = 0;
    public Memory memory;       //RAM

    public char sp, pc;         //Stack and Program Counter Registers
    public char[] registers;    //Registers

    public char delay_timer;    //The delay timer (Counts down at 60hz)
    public char sound_timer;    //The sound timer (Counts down at 60hz, buzzes at 0))

    public final int B = 0;
    public final int C = 1;
    public final int D = 2;
    public final int E = 3;
    public final int H = 4;
    public final int L = 5;
    public final int F = 6;
    public final int A = 7;

    public final int Zf = 0;
    public final int Nf = 1;
    public final int Hf = 2;
    public final int Cf = 3;

    public boolean ime = true; // Interrupt Master Enable
    public boolean ime_scheduled = false; // IME will be re-enabled NEXT cycle

    public int breakpoint = 0;

    public long oldTime;

    public int cycles;

    public boolean halted = false; // Gets set to true when HALT is executed

    public CPU(Memory mem) {
        memory = mem;
        initialize();
    }

    public void initialize() {
        opcode = 0;

        pc = 0x100; //256

        registers = new char[8]; //BCDEHLFA
        setBootValues();

        delay_timer = 0;
        sound_timer = 0;

        oldTime = System.currentTimeMillis();
    }

    public void setBootValues() {

        registers[A] = 0x1;
        registers[C] = 0xD;
        registers[E] = 0xD8;
        registers[F] = 0xB0;
        registers[H] = 0x1;
        registers[L] = 0x4D;
        sp = 0xFFFE;

        memory.forceWrite(0xFF05, (char)(0x00)); // TIMA
        memory.forceWrite(0xFF06, (char)(0x00)); // TMA
        memory.forceWrite(0xFF07, (char)(0x00)); // TAC
        memory.forceWrite(0xFF10, (char)(0x80)); // NR10
        memory.forceWrite(0xFF11, (char)(0xBF)); // NR11
        memory.forceWrite(0xFF12, (char)(0xF3)); // NR12
        memory.forceWrite(0xFF14, (char)(0xBF)); // NR14
        memory.forceWrite(0xFF16, (char)(0x3F)); // NR21
        memory.forceWrite(0xFF17, (char)(0x00)); // NR22
        memory.forceWrite(0xFF19, (char)(0xBF)); // NR24
        memory.forceWrite(0xFF1A, (char)(0x7F)); // NR30
        memory.forceWrite(0xFF1B, (char)(0xFF)); // NR31
        memory.forceWrite(0xFF1C, (char)(0x9F)); // NR32
        memory.forceWrite(0xFF1E, (char)(0xBF)); // NR33
        memory.forceWrite(0xFF20, (char)(0xFF)); // NR41
        memory.forceWrite(0xFF21, (char)(0x00)); // NR42
        memory.forceWrite(0xFF22, (char)(0x00)); // NR43
        memory.forceWrite(0xFF23, (char)(0xBF)); // NR44
        memory.forceWrite(0xFF24, (char)(0x77)); // NR50
        memory.forceWrite(0xFF25, (char)(0xF3)); // NR51
        memory.forceWrite(0xFF26, (char)(0xF1)); // NR52
        memory.forceWrite(0xFF40, (char)(0x91)); // LCDC
        memory.forceWrite(0xFF42, (char)(0x00)); // SCY
        memory.forceWrite(0xFF43, (char)(0x00)); // SCX
        memory.forceWrite(0xFF45, (char)(0x00)); // LYC
        memory.forceWrite(0xFF47, (char)(0xFC)); // BGP
        memory.forceWrite(0xFF48, (char)(0xFF)); // OBP0
        memory.forceWrite(0xFF49, (char)(0xFF)); // OBP1
        memory.forceWrite(0xFF4A, (char)(0x00)); // WY
        memory.forceWrite(0xFF4B, (char)(0x00)); // WX
        memory.forceWrite(0xFFFF, (char)(0x00)); // IE
    }


    public void loadGame(String game) throws IOException {
        Path p = FileSystems.getDefault().getPath(game);
        byte[] gameData = Files.readAllBytes(p);

        memory.loadCartridge(gameData);
    }

    public boolean isNegative(char x) {
        return (((x >> 7) & 0b1) == 1);
    }

    public void registerDump() {
        System.out.println("A: " + Integer.toHexString(registers[7]));
        System.out.println("B: " + Integer.toHexString(registers[0]));
        System.out.println("C: " + Integer.toHexString(registers[1]));
        System.out.println("D: " + Integer.toHexString(registers[2]));
        System.out.println("E: " + Integer.toHexString(registers[3]));
        System.out.println("F: " + Integer.toHexString(registers[6]));
        System.out.println("H: " + Integer.toHexString(registers[4]));
        System.out.println("L: " + Integer.toHexString(registers[5]));
        System.out.println();
        System.out.println("Zf: " + (int)getFlag(Zf));
        System.out.println("Nf: " + (int)getFlag(Nf));
        System.out.println("Hf: " + (int)getFlag(Hf));
        System.out.println("Cf: " + (int)getFlag(Cf));
        System.out.println();
        System.out.println("PC: " + Integer.toHexString(pc));
        System.out.println("SP: " + Integer.toHexString(sp));
    }

    public int doCBOpcode(char opcode) { // Executes 0xCB00 & opcode
        opcodeCount++;
        if (opcodeCountLimit > 0 && opcodeCount > opcodeCountLimit) {
            System.exit(0);
        }
        switch (opcode) {
            // Shifts
            case 0x3F: case 0x38: case 0x39: case 0x3A: case 0x3B: case 0x3C: case 0x3D: // SRL reg
            {
                resetFlags();
                int reg = opcode - 0x38;
                setFlag(Cf, registers[reg] & 0x1); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant bit
                registers[reg] = (char)((registers[reg] >> 1) & 0b01111111);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x3E: // SRL (HL)
            {
                resetFlags();
                setFlag(Cf, memory.read(get16BitRegister(H, L)) & 0x1); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant byte
                memory.write(get16BitRegister(H, L), (char)((memory.read(get16BitRegister(H, L)) >> 1) & 0b01111111));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            case 0x27: case 0x20: case 0x21: case 0x22: case 0x23: case 0x24: case 0x25: // SLA reg
            {
                resetFlags();
                int reg = opcode - 0x20;
                setFlag(Cf, (registers[reg] & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Shift one bit left
                registers[reg] = (char)((registers[reg] << 1) & 0xFF);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x26: // SLA (HL)
            {
                resetFlags();
                setFlag(Cf, (memory.read(get16BitRegister(H, L)) & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Shift one bit left
                // and Clear most significant byte
                memory.write(get16BitRegister(H, L), (char)((memory.read(get16BitRegister(H, L)) << 1) & 0xFF));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            case 0x2F: case 0x28: case 0x29: case 0x2A: case 0x2B: case 0x2C: case 0x2D: // SRA reg
            {
                resetFlags();
                int reg = opcode - 0x28;
                setFlag(Cf, registers[reg] & 0b1); // Set Carry flag to most sig bit
                // Shift one bit right
                registers[reg] = (char)(registers[reg] >> 1);

                // Bit 7 retains its original value
                registers[reg] = (char)(registers[reg] | ((registers[reg] & 0b01000000) << 1));

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x2E: // SRA (HL)
            {
                resetFlags();
                setFlag(Cf, memory.read(get16BitRegister(H, L)) & 0b1); // Set Carry flag to most sig bit
                // Shift one bit right
                // and Clear most significant byte
                memory.write(get16BitRegister(H, L), (char)(memory.read(get16BitRegister(H, L)) >> 1));

                // Bit 7 retains its original value
                char mem_hl = memory.read(get16BitRegister(H, L));
                memory.write(get16BitRegister(H, L), (char)(mem_hl | ((mem_hl & 0b01000000) << 1)));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            // Rotates

            case 0x07: case 0x00: case 0x01: case 0x02: case 0x03: case 0x04: case 0x05: // RLC reg
            {
                resetFlags();
                setFlag(Cf, (registers[opcode] & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Shift one bit left
                registers[opcode] = (char)(((registers[opcode] << 1) | getFlag(Cf)) & 0xFF);

                setFlag(Zf, registers[opcode] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x06: // RLC (HL)
            {
                resetFlags();
                setFlag(Cf, ((memory.read(get16BitRegister(H, L)) & 0b10000000) >> 7) & 0x1); // Set Carry flag to most sig bit
                // Shift one bit left and or the old msb into the lsb
                memory.write(get16BitRegister(H, L), (char)(((memory.read(get16BitRegister(H, L)) << 1) | getFlag(Cf)) & 0xFF));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            case 0x0F: case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C: case 0x0D: // RRC reg
            {
                int reg = opcode - 0x08;
                resetFlags();
                setFlag(Cf, registers[reg] & 0b00000001); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant bit
                registers[reg] = (char)(((registers[reg] >> 1)| (getFlag(Cf) << 7)) & 0xFF);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x0E: // RRC (HL)
            {
                resetFlags();
                setFlag(Cf, memory.read(get16BitRegister(H, L)) & 0b00000001); // Set Carry flag to most sig bit
                // Shift one bit left and or the old msb into the lsb
                memory.write(get16BitRegister(H, L), (char)(((memory.read(get16BitRegister(H, L)) >> 1) | (getFlag(Cf) << 7)) & 0xFF));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            case 0x17: case 0x10: case 0x11: case 0x12: case 0x13: case 0x14: case 0x15: // RL reg
            {
                int oldCarry = getFlag(Cf);
                int reg = opcode - 0x10;
                resetFlags();
                setFlag(Cf, (registers[reg] & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Shift one bit left
                registers[reg] = (char)(((registers[reg] << 1) | oldCarry) & 0xFF);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x16: // RL (HL)
            {
                int oldCarry = getFlag(Cf);
                resetFlags();
                setFlag(Cf, ((memory.read(get16BitRegister(H, L)) & 0b10000000) >> 7) & 0x1); // Set Carry flag to most sig bit
                // Shift one bit left and or the old msb into the lsb
                memory.write(get16BitRegister(H, L), (char)(((memory.read(get16BitRegister(H, L)) << 1) | oldCarry) & 0xFF));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            case 0x1F: case 0x18: case 0x19: case 0x1A: case 0x1B: case 0x1C: case 0x1D: // RR reg
            {
                int oldCarry = getFlag(Cf);
                int reg = opcode - 0x18;
                resetFlags();
                setFlag(Cf, registers[reg] & 0b00000001); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant bit
                registers[reg] = (char) (((registers[reg] >> 1) | (oldCarry << 7)) & 0xFF);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                pc++;
                cycles = 8;
                break;
            }

            case 0x1E: // RR (HL)
            {
                int oldCarry = getFlag(Cf);
                resetFlags();
                setFlag(Cf, memory.read(get16BitRegister(H, L)) & 0x1); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant byte
                memory.write(get16BitRegister(H, L), (char)((memory.read(get16BitRegister(H, L)) >> 1) & 0b01111111));
                // Or the old carry into ms bit
                memory.write(get16BitRegister(H, L), (char)(memory.read(get16BitRegister(H, L)) | (oldCarry << 7)));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                pc++;
                cycles = 16;
                break;
            }

            // Swaps
            case 0x37: case 0x30: case 0x31: case 0x32: case 0x33: case 0x34: case 0x35: // SWAP reg
            {
                resetFlags();
                int reg = opcode - 0x30;
                char lower_nibble = (char)(registers[reg] & 0xF);
                char higher_nibble = (char)((registers[reg] & 0xF0) >> 4);

                registers[reg] = (char)((lower_nibble << 4) | higher_nibble);

                setFlag(Zf, registers[reg] == 0 ? 1 : 0);

                pc++;
                cycles = 8;
                break;
            }

            case 0x36: // SWAP HL
            {
                resetFlags();
                char lower_nibble = (char)(memory.read(get16BitRegister(H, L)) & 0xF);
                char higher_nibble = (char)((memory.read(get16BitRegister(H, L)) & 0xF0) >> 4);

                memory.write(get16BitRegister(H, L), (char)((lower_nibble << 4) | higher_nibble));

                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);

                pc++;
                cycles = 16;
                break;
            }

            // BIT b, reg
            case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x47: case 0x48: case 0x49: case 0x4A: case 0x4B: case 0x4C: case 0x4D: case 0x4F:
            case 0x50: case 0x51: case 0x52: case 0x53: case 0x54: case 0x55: case 0x57: case 0x58: case 0x59: case 0x5A: case 0x5B: case 0x5C: case 0x5D: case 0x5F:
            case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x67: case 0x68: case 0x69: case 0x6A: case 0x6B: case 0x6C: case 0x6D: case 0x6F:
            case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75: case 0x77: case 0x78: case 0x79: case 0x7A: case 0x7B: case 0x7C: case 0x7D: case 0x7F:
            {
                int reg = opcode & 7;
                int bit = 1 << ((opcode >> 4)-0x4) * 2 + ((opcode >> 3) & 0b1);
                setFlag(Nf, 0);
                setFlag(Hf, 1);

                boolean cp = (registers[reg] & bit) == 0;// Is bit b 0?
                setFlag(Zf, cp ? 1: 0);
                pc++;
                cycles = 8;
                break;
            }

            // BIT b, (HL)
            case 0x46:  case 0x4E:
            case 0x56:  case 0x5E:
            case 0x66:  case 0x6E:
            case 0x76:  case 0x7E:
            {
                int bit = 1 << ((opcode >> 4)-0x4) * 2 + ((opcode >> 3) & 0b1);
                setFlag(Nf, 0);
                setFlag(Hf, 1);

                boolean cp = (memory.read(get16BitRegister(H, L)) & bit) == 0; // Is bit b 0?
                setFlag(Zf, cp ? 1: 0);
                pc++;
                cycles = 12;
                break;
            }

            // RES b, reg
            case 0x80: case 0x81: case 0x82: case 0x83: case 0x84: case 0x85: case 0x87: case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8F:
            case 0x90: case 0x91: case 0x92: case 0x93: case 0x94: case 0x95: case 0x97: case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9F:
            case 0xA0: case 0xA1: case 0xA2: case 0xA3: case 0xA4: case 0xA5: case 0xA7: case 0xA8: case 0xA9: case 0xAA: case 0xAB: case 0xAC: case 0xAD: case 0xAF:
            case 0xB0: case 0xB1: case 0xB2: case 0xB3: case 0xB4: case 0xB5: case 0xB7: case 0xB8: case 0xB9: case 0xBA: case 0xBB: case 0xBC: case 0xBD: case 0xBF:
            {
                int reg = opcode & 7;
                int bit = 1 << ((opcode >> 4)-0x8) * 2 + ((opcode >> 3) & 0b1);

                registers[reg] &= (~bit) & 0xFF;
                pc++;
                cycles = 8;
                break;
            }

            // RES b, (HL)
            case 0x86:  case 0x8E:
            case 0x96:  case 0x9E:
            case 0xA6:  case 0xAE:
            case 0xB6:  case 0xBE:
            {
                int bit = 1 << ((opcode >> 4)-0x8) * 2 + ((opcode >> 3) & 0b1);
                memory.write(get16BitRegister(H, L), (char)(memory.read(get16BitRegister(H, L)) & ((~bit) & 0xFF)));
                pc++;
                cycles = 16;
                break;
            }

            // SET b, reg
            case 0xC0: case 0xC1: case 0xC2: case 0xC3: case 0xC4: case 0xC5: case 0xC7: case 0xC8: case 0xC9: case 0xCA: case 0xCB: case 0xCC: case 0xCD: case 0xCF:
            case 0xD0: case 0xD1: case 0xD2: case 0xD3: case 0xD4: case 0xD5: case 0xD7: case 0xD8: case 0xD9: case 0xDA: case 0xDB: case 0xDC: case 0xDD: case 0xDF:
            case 0xE0: case 0xE1: case 0xE2: case 0xE3: case 0xE4: case 0xE5: case 0xE7: case 0xE8: case 0xE9: case 0xEA: case 0xEB: case 0xEC: case 0xED: case 0xEF:
            case 0xF0: case 0xF1: case 0xF2: case 0xF3: case 0xF4: case 0xF5: case 0xF7: case 0xF8: case 0xF9: case 0xFA: case 0xFB: case 0xFC: case 0xFD: case 0xFF:
            {
                int reg = opcode & 7;
                int bit = 1 << ((opcode >> 4)-0xC) * 2 + ((opcode >> 3) & 0b1);

                registers[reg] |= bit;
                pc++;
                cycles = 8;
                break;
            }

            // SET b, (HL)
            case 0xC6:  case 0xCE:
            case 0xD6:  case 0xDE:
            case 0xE6:  case 0xEE:
            case 0xF6:  case 0xFE:
            {
                int bit = 1 << ((opcode >> 4)-0xC) * 2 + ((opcode >> 3) & 0b1);
                memory.write(get16BitRegister(H, L), (char)(memory.read(get16BitRegister(H, L)) | bit));
                pc++;
                cycles = 16;
                break;
            }

            default:
                return 1;
        }
        return 0;
    }

    public int doOpcode(char opcode) {
        opcodeCount++;
        if (opcodeCountLimit > 0 && opcodeCount > opcodeCountLimit) {
            System.exit(0);
        }
        switch (opcode) {
            //8-Bit loads
            //Register to register
            case 0x40: case 0x41: case 0x42: case 0x43: case 0x44: case 0x45: case 0x47: // ld b,reg
            case 0x48: case 0x49: case 0x4a: case 0x4b: case 0x4c: case 0x4d: case 0x4f: // ld c,reg
            case 0x50: case 0x51: case 0x52: case 0x53: case 0x54: case 0x55: case 0x57: // ld d,reg
            case 0x58: case 0x59: case 0x5a: case 0x5b: case 0x5c: case 0x5d: case 0x5f: // ld e,reg
            case 0x60: case 0x61: case 0x62: case 0x63: case 0x64: case 0x65: case 0x67: // ld h,reg
            case 0x68: case 0x69: case 0x6a: case 0x6b: case 0x6c: case 0x6d: case 0x6f: // ld l,reg
            case 0x78: case 0x79: case 0x7a: case 0x7b: case 0x7c: case 0x7d: case 0x7f: // ld a,reg
            {
                int dst_register = opcode >> 3 & 7; //Destination register
                char src_value = registers[opcode & 7]; //Source value
                registers[dst_register] = src_value;
                pc++;
                cycles = 4;
                break;
            }

            //Register to (HL)
            case 0x70: case 0x71: case 0x72: case 0x73: case 0x74: case 0x75: case 0x77: //ld (HL), reg
            {
                char src_value = registers[opcode & 7]; //Source value
                memory.write(get16BitRegister(H, L), src_value);
                pc++;
                cycles = 8;
                break;
            }

            //Memory to (HL)
            case 0x36: {
                memory.write(get16BitRegister(H, L), memory.read(pc + 1));
                pc += 2;
                cycles = 12;
                break;
            } // ld (HL), d8

            //(HL) to register
            case 0x46: {
                registers[B] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld b, (HL)
            case 0x4E: {
                registers[C] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld c, (HL)
            case 0x56: {
                registers[D] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld d, (HL)
            case 0x5E: {
                registers[E] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld e, (HL)
            case 0x66: {
                registers[H] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld h, (HL)
            case 0x6E: {
                registers[L] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld l, (HL)
            case 0x7E: {
                registers[A] = memory.read(get16BitRegister(H, L));
                pc++;
                cycles = 8;
                break;
            } // ld a, (HL)

            //Memory to register
            case 0x06: {
                registers[B] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld b, d8
            case 0x0E: {
                registers[C] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld c, d8
            case 0x16: {
                registers[D] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld d, d8
            case 0x1E: {
                registers[E] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld e, d8
            case 0x26: {
                registers[H] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld h, d8
            case 0x2E: {
                registers[L] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld l, d8
            case 0x3E: {
                registers[A] = memory.read(pc + 1);
                pc += 2;
                cycles = 8;
                break;
            } // ld a, d8

            //Memory to A
            case 0x0A: {
                registers[A] = memory.read(get16BitRegister(B, C));
                pc++;
                cycles = 8;
                break;
            } //ld a, (bc)
            case 0x1A: {
                registers[A] = memory.read(get16BitRegister(D, E));
                pc++;
                cycles = 8;
                break;
            } //ld a, (de)
            case 0xFA: {
                registers[A] = memory.read(get16BitMemory(pc + 1, pc + 2));
                pc += 3;
                cycles = 16;
                break;
            } //ld a, (a16)

            //A to Memory
            case 0x02: {
                memory.write(get16BitRegister(B, C), registers[A]);
                pc++;
                cycles = 8;
                break;
            } //ld (bc), a
            case 0x12: {
                memory.write(get16BitRegister(D, E), registers[A]);
                pc++;
                cycles = 8;
                break;
            } //ld (de), a
            case 0xEA: {
                memory.write(get16BitMemory(pc + 1, pc + 2), registers[A]);
                pc += 3;
                cycles = 16;
                break;
            } //ld (a16), a

            //ld A, (C + 0xFF00) and ld (C + 0xFF00), A
            case 0xF2: {
                registers[A] = memory.read(registers[C] + 0xFF00);
                pc++;
                cycles = 8;
                break;
            }
            case 0xE2: {
                memory.write(registers[C] + 0xFF00, registers[A]);
                pc++;
                cycles = 8;
                break;
            }

            //(HL+) and (HL-) loads
            case 0x3A: { //LDD A, (HL)
                doOpcode((char) 0x7E);
                set16BitRegister(H, L, (char) (get16BitRegister(H, L) - 1));
                cycles = 8;
                break;
            }
            case 0x32: {
                doOpcode((char) 0x77);
                set16BitRegister(H, L, (char) (get16BitRegister(H, L) - 1));
                cycles = 8;
                break;
            }
            case 0x2A: { //LDI A, (HL)
                doOpcode((char) 0x7E);
                set16BitRegister(H, L, (char) (get16BitRegister(H, L) + 1));
                cycles = 8;
                break;
            }
            case 0x22: {
                doOpcode((char) 0x77);
                set16BitRegister(H, L, (char) (get16BitRegister(H, L) + 1));
                cycles = 8;
                break;
            }

            //LDH (n), A and LDH A, (n)
            case 0xE0: {
                memory.write(0xFF00 + memory.read(pc + 1), registers[A]);
                cycles = 12;
                pc += 2;
                break;
            } //LDH (n), A
            case 0xF0: {
                registers[A] = memory.read(0xFF00 + memory.read(pc + 1));
                cycles = 12;
                pc += 2;
                break;
            } //LDH A, (n)

            //16-Bit Loads

            //ld (BC, DE, HL, SP), d16
            case 0x01: {
                set16BitRegister(B, C, get16BitMemory(pc + 1, pc + 2));
                cycles = 12;
                pc += 3;
                break;
            }
            case 0x11: {
                set16BitRegister(D, E, get16BitMemory(pc + 1, pc + 2));
                cycles = 12;
                pc += 3;
                break;
            }
            case 0x21: {
                set16BitRegister(H, L, get16BitMemory(pc + 1, pc + 2));
                cycles = 12;
                pc += 3;
                break;
            }
            case 0x31: {
                sp = get16BitMemory(pc + 1, pc + 2);
                cycles = 12;
                pc += 3;
                break;
            }

            //ld SP, HL and ld HL, SP + n
            case 0xF9: {
                sp = get16BitRegister(H, L);
                cycles = 8;
                pc++;
                break;
            } //ld SP, HL
            case 0xF8: { //ld HL, SP + n
                resetFlags();
                char value = memory.read(pc + 1);

                if (halfCarryCheck(sp, value)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(sp, value)) { //Full carry check
                    setFlag(Cf, 1);
                }

                char temp_sp = sp;

                if (isNegative(value)) {
                    temp_sp -= ((~value + 1) & 0xFF);
                } else {
                    temp_sp += value;
                }

                set16BitRegister(H, L, temp_sp);
                cycles = 12;
                pc += 2;
                break;
            }

            //ld (nn), SP
            case 0x08: {
                memory.write(get16BitMemory(pc + 1, pc + 2), (char) (sp & 0x00FF));
                memory.write(get16BitMemory(pc + 1, pc + 2) + 1, (char) ((sp & 0xFF00) >> 8));
                cycles = 20;
                pc += 3;
                break;
            }

            //Push register pair onto the stack and decrement stack pointer twice
            case 0xF5: {
                memory.write(sp - 2, registers[F]);
                memory.write(sp - 1, registers[A]);
                sp -= 2;
                cycles = 16;
                pc++;
                break;
            }
            case 0xC5: {
                memory.write(sp - 2, registers[C]);
                memory.write(sp - 1, registers[B]);
                sp -= 2;
                cycles = 16;
                pc++;
                break;
            }
            case 0xD5: {
                memory.write(sp - 2, registers[E]);
                memory.write(sp - 1, registers[D]);
                sp -= 2;
                cycles = 16;
                pc++;
                break;
            }
            case 0xE5: {
                memory.write(sp - 2, registers[L]);
                memory.write(sp - 1, registers[H]);
                sp -= 2;
                cycles = 16;
                pc++;
                break;
            }

            //Pop register pair off the stack and increment stack pointer twice
            case 0xF1: {
                registers[F] = (char) (memory.read(sp) & 0xF0);
                registers[A] = memory.read(sp + 1);
                sp += 2;
                cycles = 12;
                pc++;
                break;
            }
            case 0xC1: {
                registers[C] = memory.read(sp);
                registers[B] = memory.read(sp + 1);
                sp += 2;
                cycles = 12;
                pc++;
                break;
            }
            case 0xD1: {
                registers[E] = memory.read(sp);
                registers[D] = memory.read(sp + 1);
                sp += 2;
                cycles = 12;
                pc++;
                break;
            }
            case 0xE1: {
                registers[L] = memory.read(sp);
                registers[H] = memory.read(sp + 1);
                sp += 2;
                cycles = 12;
                pc++;
                break;
            }

            //8-Bit ALU

            //ADD A, r
            case 0x80:
            case 0x81:
            case 0x82:
            case 0x83:
            case 0x84:
            case 0x85:
            case 0x87: {
                int src_register = opcode & 7; //Destination register
                resetFlags();

                if (halfCarryCheck(registers[A], registers[src_register])) { //Half carry check
                    setFlag(2, 1);
                }

                if (fullCarryCheck(registers[A], registers[src_register])) { //Full carry check
                    setFlag(3, 1);
                }

                registers[A] += registers[src_register];

                registers[A] %= 256; //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(0, 1);
                }

                cycles = 4;
                pc++;
                break;
            }

            case 0x86: { //ADD A, (HL)
                resetFlags();

                if (halfCarryCheck(registers[A], memory.read(get16BitRegister(H, L)))) { //Half carry check
                    setFlag(2, 1);
                }

                if (fullCarryCheck(registers[A], memory.read(get16BitRegister(H, L)))) { //Full carry check
                    setFlag(3, 1);
                }

                registers[A] += memory.read(get16BitRegister(H, L));

                registers[A] %= 256; //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(0, 1);
                }

                cycles = 8;
                pc++;
                break;
            }

            case 0xC6: { //ADD A, n
                int value = memory.read(pc + 1);
                resetFlags();

                if (halfCarryCheck(registers[A], value)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(registers[A], value)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] += value;

                registers[A] %= 256;

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc += 2;
                break;
            }

            // SUB A, n
            case 0x90:
            case 0x91:
            case 0x92:
            case 0x93:
            case 0x94:
            case 0x95:
            case 0x97: {
                int src_register = opcode - 0x90; //Comparison register
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], registers[src_register])) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], registers[src_register])) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (registers[A] - registers[src_register] == 0) { //Zero flag
                    setFlag(0, 1);
                }

                registers[A] -= registers[src_register];

                registers[A] = (char) ((registers[A] + 256) % 256);

                setFlag(Nf, 1);

                cycles = 4;
                pc++;
                break;
            }

            case 0x96: { //SUB A, (HL)
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], memory.read(get16BitRegister(H, L)))) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], memory.read(get16BitRegister(H, L)))) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (registers[A] - memory.read(get16BitRegister(H, L)) == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                registers[A] -= memory.read(get16BitRegister(H, L));

                registers[A] = (char) ((registers[A] + 256) % 256);

                setFlag(Nf, 1);

                cycles = 8;
                pc++;
                break;
            }

            case 0xD6: { //SUB A, n
                int value = memory.read(pc + 1);
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], value)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], value)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (registers[A] - value == 0) { //Zero flag
                    setFlag(0, 1);
                }

                registers[A] -= value;

                registers[A] = (char) ((registers[A] + 256) % 256);

                setFlag(Nf, 1);

                cycles = 8;
                pc += 2;
                break;
            }

            //SBC A, r
            case 0x98: case 0x99: case 0x9A: case 0x9B: case 0x9C: case 0x9D: case 0x9F: {
                int src_register = opcode - 0x98; //Destination register
                int oldCarry = getFlag(Cf);
                resetFlags();

                if (halfCarryCheck(registers[src_register], oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarrySubtractionCheck(registers[A], registers[src_register] + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(registers[src_register], oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], registers[src_register] + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] -= registers[src_register] + oldCarry;

                registers[A] = (char) ((registers[A] + 256) % 256); //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                setFlag(Nf, 1);

                cycles = 4;
                pc++;
                break;
            }

            case 0x9E: // SBC A, (HL)
            {
                int oldCarry = getFlag(Cf);
                resetFlags();

                if (halfCarryCheck(memory.read(get16BitRegister(H, L)), oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarrySubtractionCheck(registers[A], memory.read(get16BitRegister(H, L)) + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(memory.read(get16BitRegister(H, L)), oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], memory.read(get16BitRegister(H, L)) + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] -= memory.read(get16BitRegister(H, L)) + oldCarry;

                registers[A] = (char) ((registers[A] + 256) % 256); //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                setFlag(Nf, 1);

                cycles = 8;
                pc++;
                break;
            }

            case 0xDE: // SBC A, n
            {
                int oldCarry = getFlag(Cf);
                int value = memory.read(pc + 1);
                resetFlags();

                if (halfCarryCheck(value, oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarrySubtractionCheck(registers[A], value + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(value, oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], value + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] -= value + oldCarry;

                registers[A] = (char) ((registers[A] + 256) % 256); //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                setFlag(Nf, 1);

                cycles = 8;
                pc += 2;
                break;
            }


            //ADC A, r
            case 0x88: case 0x89: case 0x8A: case 0x8B: case 0x8C: case 0x8D: case 0x8F: {
                int src_register = opcode & 7; //Destination register
                int oldCarry = getFlag(Cf);
                resetFlags();

                if (halfCarryCheck(registers[src_register], oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarryCheck(registers[A], registers[src_register] + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(registers[src_register], oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarryCheck(registers[A], registers[src_register] + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] += registers[src_register] + oldCarry;

                registers[A] %= 256; //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                cycles = 4;
                pc++;
                break;
            }

            case 0x8E: { //ADC A, (HL)
                int oldCarry = getFlag(Cf);
                resetFlags();

                if (halfCarryCheck(memory.read(get16BitRegister(H, L)), oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarryCheck(registers[A], memory.read(get16BitRegister(H, L)) + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(memory.read(get16BitRegister(H, L)), oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarryCheck(registers[A], memory.read(get16BitRegister(H, L)) + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] += memory.read(get16BitRegister(H, L)) + oldCarry;

                registers[A] %= 256; //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc++;
                break;
            }

            case 0xCE: { //ADC A, n
                int value = memory.read(pc + 1);
                int oldCarry = getFlag(Cf);
                resetFlags();

                if (halfCarryCheck(value, oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (halfCarryCheck(registers[A], value + oldCarry)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarryCheck(value, oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (fullCarryCheck(registers[A], value + oldCarry)) { //Full carry check
                    setFlag(Cf, 1);
                }

                registers[A] += value + oldCarry;

                registers[A] %= 256; //Account for overflow

                if (registers[A] == 0) { //Zero flag
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc += 2;
                break;
            }

            // AND
            case 0xA0:
            case 0xA1:
            case 0xA2:
            case 0xA3:
            case 0xA4:
            case 0xA5:
            case 0xA7: {
                int src_register = opcode - 0xA0; //Comparison register
                resetFlags();

                setFlag(Hf, 1);

                registers[A] &= registers[src_register];

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 4;
                pc++;
                break;
            }

            case 0xA6: { //AND A, (HL)
                resetFlags();
                setFlag(Hf, 1);

                registers[A] &= memory.read(get16BitRegister(H, L));

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc++;
                break;
            }

            case 0xE6: { //AND A, n
                int value = memory.read(pc + 1);
                resetFlags();

                setFlag(Hf, 1);

                registers[A] &= value;

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc += 2;
                break;
            }

            // CP n
            case 0xBF:
            case 0xB8:
            case 0xB9:
            case 0xBA:
            case 0xBB:
            case 0xBC:
            case 0xBD: {
                int src_register = opcode - 0xB8; //Comparison register
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], registers[src_register])) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], registers[src_register])) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (registers[A] - registers[src_register] == 0) { //Zero flag
                    setFlag(0, 1);
                }

                setFlag(Nf, 1);

                cycles = 4;
                pc++;
                break;
            }

            case 0xBE: { //CP A, (HL)
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], memory.read(get16BitRegister(H, L)))) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (registers[A] < memory.read(get16BitRegister(H, L))) {
                    setFlag(Cf, 1);
                }

                if (registers[A] == memory.read(get16BitRegister(H, L))) { //Zero flag
                    setFlag(Zf, 1);
                }

                setFlag(Nf, 1);

                cycles = 8;
                pc++;
                break;
            }

            case 0xFE: // CP A, #
            {
                int value = memory.read(pc + 1);
                resetFlags();

                if (halfCarrySubtractionCheck(registers[A], value)) { //Half carry check
                    setFlag(Hf, 1);
                }

                if (fullCarrySubtractionCheck(registers[A], value)) { //Full carry check
                    setFlag(Cf, 1);
                }

                if (registers[A] - value == 0) { //Zero flag
                    setFlag(0, 1);
                }

                setFlag(Nf, 1);

                cycles = 8;
                pc += 2;
                break;
            }

            // OR A, reg
            case 0xB7:
            case 0xB0:
            case 0xB1:
            case 0xB2:
            case 0xB3:
            case 0xB4:
            case 0xB5: {
                resetFlags();
                int register = opcode - 0xB0;
                registers[A] |= registers[register];
                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }
                cycles = 4;
                pc++;
                break;
            }

            case 0xB6: { //OR A, (HL)
                resetFlags();

                registers[A] |= memory.read(get16BitRegister(H, L));

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc++;
                break;
            }

            case 0xF6: { //OR A, n
                int value = memory.read(pc + 1);
                resetFlags();

                registers[A] |= value;

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc += 2;
                break;
            }

            // XOR
            case 0xAF:
            case 0xA8:
            case 0xA9:
            case 0xAA:
            case 0xAB:
            case 0xAC:
            case 0xAD: {
                resetFlags();
                int register = opcode - 0xA8;
                registers[A] ^= registers[register];
                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }
                cycles = 4;
                pc++;
                break;
            }

            case 0xAE: { //XOR A, (HL)
                resetFlags();

                registers[A] ^= memory.read(get16BitRegister(H, L));

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc++;
                break;
            }

            case 0xEE: { //XOR A, n
                int value = memory.read(pc + 1);
                resetFlags();

                registers[A] ^= value;

                if (registers[A] == 0) {
                    setFlag(Zf, 1);
                }

                cycles = 8;
                pc += 2;
                break;
            }

            // Increments
            case 0x3C:
            case 0x04:
            case 0x0C:
            case 0x14:
            case 0x1C:
            case 0x24:
            case 0x2C: // INC reg
            {
                int reg = (opcode >> 3) & 7;
                setFlag(Hf, halfCarryCheck(registers[reg], 1) ? 1 : 0);
                registers[reg] = (char) ((registers[reg] + 1) % 256);
                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                setFlag(Nf, 0);
                pc++;
                cycles = 4;
                break;
            }

            case 0x34: // INC (HL)
            {
                setFlag(Hf, halfCarryCheck(memory.read(get16BitRegister(H, L)), 1) ? 1 : 0);
                memory.write(get16BitRegister(H, L), (char) ((memory.read(get16BitRegister(H, L)) + 1) % 256));
                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                setFlag(Nf, 0);
                pc++;
                cycles = 12;
                break;
            }

            // Decrements
            case 0x3D:
            case 0x05:
            case 0x0D:
            case 0x15:
            case 0x1D:
            case 0x25:
            case 0x2D: // DEC reg
            {
                int reg = (opcode >> 3) & 7;
                boolean hc = halfCarrySubtractionCheck(registers[reg], 1);
                setFlag(Hf, hc ? 1 : 0);
                registers[reg] = (char) ((registers[reg] - 1 + 256) % 256);
                setFlag(Zf, registers[reg] == 0 ? 1 : 0);
                setFlag(Nf, 1);

                pc++;
                cycles = 4;
                break;
            }

            case 0x35: {
                boolean hc = halfCarrySubtractionCheck(memory.read(get16BitRegister(H, L)), 1);
                setFlag(Hf, hc ? 1 : 0);
                memory.write(get16BitRegister(H, L), (char) ((memory.read(get16BitRegister(H, L)) - 1 + 256) % 256));
                setFlag(Zf, memory.read(get16BitRegister(H, L)) == 0 ? 1 : 0);
                setFlag(Nf, 1);

                pc++;
                cycles = 12;
                break;
            }

            // Rotates
            case 0x07: // RLCA
            {
                resetFlags();
                setFlag(Cf, (registers[A] & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Rotate one bit left
                registers[A] = (char) (((registers[A] << 1) | getFlag(Cf)) & 0xFF);

                pc++;
                cycles = 4;
                break;
            }

            case 0x17: // RLA
            {
                int oldCarry = getFlag(Cf);
                resetFlags();
                setFlag(Cf, (registers[A] & 0b10000000) >> 7); // Set Carry flag to most sig bit
                // Rotate one bit left
                registers[A] = (char) (((registers[A] << 1) | oldCarry) & 0xFF);

                pc++;
                cycles = 4;
                break;
            }

            case 0x0F: // RRCA
            {
                resetFlags();
                setFlag(Cf, registers[A] & 0b00000001); // Set Carry flag to most sig bit
                // Rotate one bit left
                registers[A] = (char) (((registers[A] >> 1) | (getFlag(Cf) << 7)) & 0xFF);

                pc++;
                cycles = 4;
                break;
            }

            case 0x1F: // RRA
            {
                int oldCarry = getFlag(Cf);
                resetFlags();
                setFlag(Cf, registers[A] & 0x1); // Set Carry flag to least sig bit
                // Shift one bit right
                // and Clear most significant bit
                registers[A] = (char) ((registers[A] >> 1) & 0b01111111);
                // Or the old carry into ms bit
                registers[A] = (char) (registers[A] | (oldCarry << 7));

                pc++;
                cycles = 4;
                break;
            }

            // Jumps

            case 0xC3: // JP nn
            {
                pc = get16BitMemory(pc + 1, pc + 2);

                cycles = 16;
                break;
            }

            case 0xE9: // JP (HL)
            {
                pc = get16BitRegister(H, L);

                cycles = 4;
                break;
            }

            case 0x18: // JR n
            {
                char val = memory.read(pc + 1);
                if (isNegative(val)) {
                    pc -= ((~val + 1) & 0xFF);
                } else {
                    pc += val;
                }
                pc += 2;
                cycles = 12;
                break;
            }

            case 0x20:
            case 0x28:
            case 0x30:
            case 0x38: // JR cc n
            {
                boolean cc;
                if (opcode == 0x20) {
                    cc = getFlag(Zf) == 0; // NZ
                } else if (opcode == 0x28) {
                    cc = getFlag(Zf) == 1; // Z
                } else if (opcode == 0x30) {
                    cc = getFlag(Cf) == 0; // NC
                } else {
                    cc = getFlag(Cf) == 1; // C
                }
                if (cc) {
                    char val = memory.read(pc + 1);
                    if (isNegative(val)) {
                        pc -= ((~val + 1) & 0xFF);
                    } else {
                        pc += val;
                    }
                    cycles = 12;
                } else {
                    cycles = 8;
                }
                pc += 2;
                break;
            }

            case 0xC2:
            case 0xCA:
            case 0xD2:
            case 0xDA: // JP cc nn
            {
                boolean cc;
                if (opcode == 0xC2) {
                    cc = getFlag(Zf) == 0; // NZ
                } else if (opcode == 0xCA) {
                    cc = getFlag(Zf) == 1; // Z
                } else if (opcode == 0xD2) {
                    cc = getFlag(Cf) == 0; // NC
                } else {
                    cc = getFlag(Cf) == 1; // C
                }
                if (cc) {
                    pc = get16BitMemory(pc + 1, pc + 2);
                    cycles = 16;
                } else {
                    pc += 3;
                    cycles = 12;
                }
                break;
            }

            // Call
            case 0xCD: // Call nn
            {
                memory.write(sp - 1, (char) (((pc + 3) >> 8) & 0xFF));
                memory.write(sp - 2, (char) ((pc + 3) & 0xFF));
                sp -= 2;
                pc = get16BitMemory(pc + 1, pc + 2);
                cycles = 24;

                break;
            }

            case 0xC4:
            case 0xCC:
            case 0xD4:
            case 0xDC: // CALL cc nn
            {
                boolean cc;
                if (opcode == 0xC4) {
                    cc = getFlag(Zf) == 0; // NZ
                } else if (opcode == 0xCC) {
                    cc = getFlag(Zf) == 1; // Z
                } else if (opcode == 0xD4) {
                    cc = getFlag(Cf) == 0; // NC
                } else {
                    cc = getFlag(Cf) == 1; // C
                }
                if (cc) {
                    memory.write(sp - 1, (char) (((pc + 3) >> 8) & 0xFF));
                    memory.write(sp - 2, (char) ((pc + 3) & 0xFF));
                    sp -= 2;

                    pc = get16BitMemory(pc + 1, pc + 2);
                    cycles = 24;
                } else {
                    pc += 3;
                    cycles = 12;
                }
                break;
            }

            // Return
            case 0xC9: // RET
            {
                pc = get16BitMemory(sp, sp + 1);
                sp += 2;
                cycles = 16;
                break;
            }

            case 0xC0:
            case 0xC8:
            case 0xD0:
            case 0xD8: // RET cc
            {
                boolean cc;
                if (opcode == 0xC0) {
                    cc = getFlag(Zf) == 0; // NZ
                } else if (opcode == 0xC8) {
                    cc = getFlag(Zf) == 1; // Z
                } else if (opcode == 0xD0) {
                    cc = getFlag(Cf) == 0; // NC
                } else {
                    cc = getFlag(Cf) == 1; // C
                }
                if (cc) {
                    pc = get16BitMemory(sp, sp + 1);
                    sp += 2;
                    cycles = 20;
                } else {
                    pc++;
                    cycles = 8;
                }
                break;
            }

            case 0xC7:
            case 0xCF:
            case 0xD7:
            case 0xDF:
            case 0xE7:
            case 0xEF:
            case 0xF7:
            case 0xFF: // RST n
            {
                memory.write(sp - 1, (char) (((pc + 1) >> 8) & 0xFF));
                memory.write(sp - 2, (char) ((pc + 1) & 0xFF));
                sp -= 2;
                pc = (char) (opcode - 0xC7);

                cycles = 16;
                break;
            }

            // RETI
            case 0xD9: {
                pc = get16BitMemory(sp, sp + 1);
                sp += 2;
                cycles = 16;
                ime = true;
                break;
            }

            // 16-bit ALU
            // Increments
            case 0x03:
            case 0x13:
            case 0x23: // INC nn
            {
                int r1 = ((opcode >> 4) & 0xF) * 2;
                int r2 = r1 + 1;
                set16BitRegister(r1, r2, (char) ((get16BitRegister(r1, r2) + 1) % (256 * 256)));
                pc++;
                cycles = 8;
                break;
            }

            case 0x33: // INC SP
            {
                sp = (char) ((sp + 1) % (256 * 256));
                pc++;
                cycles = 8;
                break;
            }

            // Decrements
            case 0x0B:
            case 0x1B:
            case 0x2B: // DEC nn
            {
                int r1 = ((opcode >> 4) & 0xF) * 2;
                int r2 = r1 + 1;
                set16BitRegister(r1, r2, (char) ((get16BitRegister(r1, r2) - 1 + 256 * 256) % (256 * 256)));
                pc++;
                cycles = 8;
                break;
            }

            case 0x3B: // DEC SP
            {
                sp = (char) ((sp - 1 + 256 * 256) % (256 * 256));
                pc++;
                cycles = 8;
                break;
            }

            // ADD
            case 0x09:
            case 0x19:
            case 0x29: // ADD HL rr
            {
                int r1 = ((opcode >> 4) & 0xF) * 2;
                int r2 = r1 + 1;
                setFlag(Hf, halfCarryCheck16Bit(get16BitRegister(H, L), get16BitRegister(r1, r2)) ? 1 : 0);
                setFlag(Cf, fullCarryCheck16Bit(get16BitRegister(H, L), get16BitRegister(r1, r2)) ? 1 : 0);
                setFlag(Nf, 0);
                set16BitRegister(H, L, (char) ((get16BitRegister(H, L) + get16BitRegister(r1, r2)) % (256 * 256)));
                pc++;
                cycles = 8;
                break;
            }

            case 0x39: // ADD HL SP
            {
                setFlag(Hf, halfCarryCheck16Bit(get16BitRegister(H, L), sp) ? 1 : 0);
                setFlag(Cf, fullCarryCheck16Bit(get16BitRegister(H, L), sp) ? 1 : 0);
                setFlag(Nf, 0);
                set16BitRegister(H, L, (char) ((get16BitRegister(H, L) + sp) % (256 * 256)));
                pc++;
                cycles = 8;
                break;
            }

            case 0xE8: // ADD SP d8
            {
                char value = memory.read(pc + 1);

                setFlag(Hf, halfCarryCheck(sp, value) ? 1 : 0);
                setFlag(Cf, fullCarryCheck(sp, value) ? 1 : 0);
                setFlag(Nf, 0);
                setFlag(Zf, 0);
                if (isNegative(value)) {
                    sp -= ((~value + 1) & 0xFF);
                } else {
                    sp += value;
                }
                sp %= 256 * 256;
                pc += 2;
                cycles = 16;
                break;
            }

            // Miscellaneous
            case 0xF3: // DI
            {
                // Disable Interrupts
                ime = false;
                pc++;
                cycles = 4;
                break;
            }

            case 0xFB: // EI
            {
                // Enable Interrupts after next instruction
                ime_scheduled = true;
                pc++;
                cycles = 4;
                break;
            }

            case 0x27: // DAA
            {
                if (getFlag(Nf) == 0) {  // after an addition, adjust if (half-)carry occurred or if result is out of bounds
                    if (getFlag(Cf) == 1 || registers[A] > 0x99) {
                        registers[A] += 0x60;
                        setFlag(Cf, 1);
                    }
                    if (getFlag(Hf) == 1 || (registers[A] & 0x0f) > 0x09) {
                        registers[A] += 0x6;
                    }
                } else {  // after a subtraction, only adjust if (half-)carry occurred
                    if (getFlag(Cf) == 1) {
                        registers[A] -= 0x60;
                    }
                    if (getFlag(Hf) == 1) {
                        registers[A] -= 0x6;
                    }
                }
                registers[A] = (char) ((registers[A] + 256) % 256);
                // these flags are always updated
                setFlag(Zf, registers[A] == 0 ? 1 : 0);
                setFlag(Hf, 0);
                pc++;
                cycles = 4;
                break;
            }

            case 0x2F: // CPL A
            {
                registers[A] = (char) (~registers[A] & 0xFF); // Flip all bits of A
                setFlag(Nf, 1);
                setFlag(Hf, 1);
                cycles = 4;
                pc++;
                break;
            }

            case 0x3F: // CCF
            {
                setFlag(Cf, (~getFlag(Cf)) & 0x1); // Flip the carry
                setFlag(Nf, 0);
                setFlag(Hf, 0);
                cycles = 4;
                pc++;
                break;
            }

            case 0x37: // SCF
            {
                setFlag(Cf, 1); // Flip the carry
                setFlag(Nf, 0);
                setFlag(Hf, 0);
                cycles = 4;
                pc++;
                break;
            }

            case 0x00: // NOP
            {
                cycles = 4;
                pc++;
                break;
            }

            // TODO Add Halt Bug
            case 0x76: // HALT
            {
                cycles = 4;
                pc++;
                if (ime) {
                    halted = true; // Version 1
                } else {
                    char IF = memory.read(0xFF0F);
                    char IE = memory.read(0xFFFF);
                    if ((IE & IF) > 0) {
                        halted = true; // Version 2
                    } else {
                        // Halt bug
                        System.out.println("Halt bug!");
                    }
                }
                break;
            }

            // TODO Implement stop
            case 0x10: // STOP
            {
                pc += 2;
                cycles = 4;
                break;
            }

            case 0xD3: case 0xDD: case 0xDB:
            case 0xE3: case 0xE4: case 0xEB: case 0xEC: case 0xED:
            case 0xF4: case 0xFC: case 0xFD: // Unassigned opcodes
            {
                pc++;
                cycles = 4;
                break;
            }

            default: //Unknown opcode
                return 1;

        }
        return 0;
    }

    public void serviceInterrupt(int interrupt) {
        ime = false;
        // We need to reset the bit in IF for interrupt
        char IF = memory.read(0xFF0F);
        memory.write(0xFF0F, (char)(IF & ~(1 << interrupt)));

        // Push PC onto the stack
        memory.write(sp - 1, (char) ((pc >> 8) & 0xFF));
        memory.write(sp - 2, (char) (pc & 0xFF));
        sp -= 2;

        // Jump to the interrupt handler
        switch (interrupt)
        {
            case 0 -> pc = 0x40;
            case 1 -> pc = 0x48;
            case 2 -> pc = 0x50;
            case 4 -> pc = 0x60;
        }
    }

    public int handleInterrupts() {
        if (ime) { // Interrupts are enabled
            char IF = memory.read(0xFF0F); // Interrupts requested
            char IE = memory.read(0xFFFF); // Interrupts enabled
            if (IF > 0) { // Some bit is set
                for (int i = 0; i < 5; i++) {
                    if (((IF & (1 << i)) >> i) == 1) {
                        // The interrupt flag is set
                        if (((IE & (1 << i)) >> i) == 1) {
                            // The interrupt is enabled
                            serviceInterrupt(i);
                            // We also need to let the cpu continue if it is halted
                            halted = false;
                            return 20; // 20 cycles
                        }
                    }
                }
            }
        }
        return 0;
    }

    public void doCycle() {
        if (halted) {
            char IF = memory.read(0xFF0F); // Interrupts requested
            char IE = memory.read(0xFFFF); // Interrupts enabled
            if ((IF & IE) > 0) {
                halted = false;
            } else {
                return;
            }
        }
        boolean enable_ime = ime_scheduled; // Save the value for after the next opcode
        ime_scheduled = false;
        char opcode = memory.read(pc);
        /*if (!executed.contains(opcode)) {
            executed.add(opcode);
            System.out.println(Integer.toHexString(opcode));
            System.out.println(Integer.toHexString(pc));
        }*/
        if (breakpoint != 0 && pc == breakpoint || breakpoint == -1) {
            registerDump();
            System.out.println("Executing 0x" + Integer.toHexString(opcode) + " at 0x" + Integer.toHexString(pc));
            Scanner s = new Scanner(System.in);
            System.out.print("Next Breakpoint: ");
            String result = s.nextLine();
            if (result.equals("-")) { // Next instruction
                breakpoint = -1;
            } else if (!result.equals("0")) {
                breakpoint = Integer.parseInt(result, 16);
            }
        }
        //registerDump();
        //System.out.println("Executing 0x" + Integer.toHexString(opcode) + " at 0x" + Integer.toHexString(pc));

        if (opcode == 0xCB) {
            pc++;
            opcode = memory.read(pc);
            if (doCBOpcode(opcode) == 1) {
                pc--;
                registerDump();
                System.out.println("Unknown Opcode 0xCB" + Integer.toHexString(opcode) + " at 0x" + Integer.toHexString(pc));
                System.exit(0);
            }
        } else if (doOpcode(opcode) == 1) {
            if (opcode != 0x00) {
                registerDump();
                System.out.println("Unknown Opcode 0x" + Integer.toHexString(opcode) + " at 0x" + Integer.toHexString(pc));
                System.exit(0);
            }
            pc++;
        }
        if (enable_ime) {
            ime = true;
        }
        //registerDump();
    }

    public char get16BitRegister(int first, int second) {
        return (char)((registers[first] << 8) | registers[second]);
    }

    public char get16BitMemory(int first, int second) {
        return (char)((memory.read(second) << 8) | memory.read(first));
    }

    public void set16BitRegister(int first, int second, char value) {
        registers[first] = (char)((value & 0xFF00) >> 8);
        registers[second] = (char)(value & 0xFF);
    }

    public boolean halfCarryCheck(int a, int b) {
        return (((a & 0xf) + (b & 0xf)) & 0x10) > 0xf;
    }

    public boolean halfCarryCheck16Bit(int a, int b) {
        return (((a & 0xfff) + (b & 0xfff)) & 0x1000) > 0xfff;
    }

    public boolean halfCarrySubtractionCheck(int a, int b) {
        return (b & 0xF) > (a & 0xF);
    }

    public boolean fullCarryCheck(int a, int b) {
        return ((a & 0xFF) + (b & 0xFF)) > 0xFF;
    }

    public boolean fullCarryCheck16Bit(int a, int b) {
        return ((a & 0xFFFF) + (b & 0xFFFF)) > 0xFFFF;
    }

    public boolean fullCarrySubtractionCheck(int a, int b) {
        return b > a;
    }

    public void setFlag(int i, int val) {
        /* |0|1|2|3|
           ---------
           |Z|N|H|C| */
        registers[F] = (char)(registers[F] & ~(1 << (7-i))); //Reset the relevant flag
        if (val == 1) {
            registers[F] = (char)(registers[F] | (1 << (7-i)));
        }
    }

    public char getFlag(int i) {
        /* |0|1|2|3|
           ---------
           |Z|N|H|C| */
        return (char)((registers[F] & (1 << (7-i))) >> (7-i)); //Get the relevant flag
    }

    public void resetFlags() {
        registers[6] = 0x0;
    }
}
