
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class HagaSa23aMIPS {
    static int[] Memory;
    static int[] Registers;
    static int PC;
    static final int R0=0;
    static int programLength;
    static boolean decode=false;
    static boolean fetch=true;
    static boolean excute=false;
    static boolean zeroFlag;
    public static void main (String[] args){
        Assembler("program1");
        runProgram();
    }
     private static void Assembler(String Name) {
        Memory = new int[2048];
        int programLength=0;
        Registers=new int[32];
        PC=0;//??
        String fileName = "src/" + Name+".txt";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder stringBuilder = new StringBuilder();//not needed ??
            String line = null;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                if (!(line.equals(""))) {
                    Memory[programLength++]=(int)Long.parseLong(getBinary(line.split(" ")),2);
                }
        }
            reader.close();
        }
        catch(IOException e) {
            System.out.println("FILE NOT FOUND");
        }
    }
    private static String getBinary(String [] x) {
        StringBuilder output = new StringBuilder();
        boolean immediate=false;
        boolean sll=false;
        boolean mem=false;
        int r;
        switch(x[0].toUpperCase()) {
            case "ADD" : output.append("0000"); break ;
            case "SUB" : output.append("0001"); break ;
            case "MULI" : output.append("0010"); immediate=true; break ;
            case "ADDI" : output.append("0011"); immediate=true;break ;
            case "BNE" : output.append("0100"); immediate=true;break ;
            case "ANDI" : output.append("0101");immediate=true; break ;
            case "ORI" : output.append("0110"); immediate=true;break ;
            case "J" : output.append("0111"); output.append(String.format("%28s", Integer.toBinaryString(Integer.parseInt(x[1]))).replaceAll(" ", "0"));return output.toString() ;
            case "SLL" : output.append("1000"); sll=true; break ;
            case "SRL" : output.append("1001");sll=true; break ;
            case "LW" : output.append("1010");mem=true; break ;
            case "SW" :output.append("1011");mem=true; break ;
            default: System.out.println("Something went wrong!!"); break;
        }
        r=Integer.parseInt(x[1].substring(1,x[1].length()));
        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
        if (mem) {
            String imm="";
            for (int i =0;i<x[2].length();i++) {
                if(("(").equals(x[2].charAt(i)+"")) {
                    output.append(String.format("%5s", Integer.toBinaryString(Integer.parseInt(x[2].substring(i+2,x[2].length()-1)))).replaceAll(" ", "0"));
                    output.append(String.format("%18s", Integer.toBinaryString(Integer.parseInt(imm))).replaceAll(" ", "0"));
                    return output.toString();
                }
                imm+=x[2].charAt(i)+"";
            }}
        r=Integer.parseInt(x[2].substring(1,x[2].length()));
        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
        if (immediate) {
            output.append(String.format("%18s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0"));
            return output.toString();
        }
        else {
            if (sll) {
                output.append("00000");
                output.append(String.format("%13s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0"));
                return output.toString();
            }
            r=Integer.parseInt(x[3].substring(1,x[3].length()));
            output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
            output.append("0000000000000");
            return output.toString();
        }
    }
    private static void runProgram() {
        int instruction =-1;
        Instruction toBeExcuted=null;
        Instruction toBeDecoded=null;
        Instruction toMemory = null;
        Instruction toWB = null;
        int limit = 7+ (programLength-1)*2;
        for (int cycle=1 ;; cycle++)
        {
            System.out.println("Clock Cycle : "+cycle);
            if(  writeBack(toWB))break;
            memory(toMemory);
            toWB=toMemory;
            if( excute) execute1(toBeExcuted);
            else{
                boolean Jump=execute2(toBeExcuted);
                if(Jump){
                    fetch=true;
                    toBeExcuted=null;
                    toBeDecoded=null;
                    toMemory = toBeExcuted;
                    continue;
                }
                toMemory=toBeExcuted;
            }
            if( decode) toBeDecoded=decode1(instruction);
            else{ decode2(toBeDecoded);toBeExcuted=toBeDecoded;}

            instruction= fetch?  fetch():-1 ;fetch=!fetch;

        }
        System.out.println("The Stages are finished");
        System.out.println("The Registers Content is :" +printReg());
        System.out.println("The Memory Content is :"+ Arrays.toString(Memory));
    }

    private static String printReg() {
        StringBuilder s = new StringBuilder("R0=" + R0);
        for (int i = 1; i < Registers.length; i++)
            s.append(" , R").append(i).append("=").append(Registers[i]);
        s.append(" , PC=" );s.append(PC);
        return s.toString();
    }

    private static void memory(Instruction i) {
        if(i==null)return;
        System.out.println("At Memory Stage : Instruction "+i.pc);
        System.out.println("   Inputs: MemRead="+i.MemRead  +" ,Data From ALU= " +i.ALUOutput + "Write Data ="+ i.valueR1 );

        if(i.MemRead)
            i.valueLW=Memory[i.ALUOutput];

        else if(i.MemWrite){
            System.out.println("    Memory index " +i.ALUOutput+ " has changed from : "+Memory[i.ALUOutput]);
            Memory[i.ALUOutput]=i.valueR1;
            System.out.println("    to : "+Memory[i.ALUOutput]);
        }
    }
    private static boolean writeBack(Instruction i) {
        if(i==null) return false;
        System.out.println("At Execute Stage : Instruction "+i.pc);
        System.out.println("   Inputs: RegWrite="+i.RegWrite + " , MemToReg="+i.MemtoReg+" , DataFromMemory ="+i.valueLW +" ,Data From ALU= " +i.ALUOutput + "WriteReg ="+ i.r1 +"\n" );
        if(i.RegWrite){
            System.out.println("    Register R" +i.r1+ " has changed from : "+Registers[i.r1]);
            if(i.r1!=0)
            Registers[i.r1]=i.MemtoReg? i.valueLW : i.ALUOutput;
            System.out.println("    to : "+Registers[i.r1]);

        }

        if(i.pc==programLength)return true;

        return false;
    }
    private static boolean execute2(Instruction i) {
        if(i==null){
            return false;
        }
        if(i.Branch && !zeroFlag){
            PC = i.pc + i.ALUOutput;
            return true;
        }
        if (i.Jump){
            PC = i.address;
            return true;
        }
        excute=false;
        return false;
    }

    private static void execute1(Instruction instruction) {
        if(instruction==null)return;
        switch (instruction.opcode) {
            case 0: instruction.ALUOutput = instruction.valueR2 + instruction.valueR3; //ADD
                break;
            case 1: instruction.ALUOutput = instruction.valueR2 - instruction.valueR3; //SUB
                break;
            case 2: instruction.ALUOutput = instruction.valueR2 * instruction.immediate;//MULT imm
                break;
            case 3: instruction.ALUOutput = instruction.valueR2 + instruction.immediate;//ADD imm
                break;
            case 4: zeroFlag = (0 == (instruction.valueR2 - instruction.valueR1));//bne
                break;
            case 5: instruction.ALUOutput = instruction.valueR2 & instruction.immediate;//AND imm
                break;
            case 6: instruction.ALUOutput = instruction.valueR2 | instruction.immediate;//OR imm
                break;
            case 7: // instruction.shiftRes = instruction.address << 2;//Jump
                break;
            case 8: instruction.ALUOutput = instruction.valueR2 << instruction.shamt;//Shift left logical
                break;
            case 9: instruction.ALUOutput = instruction.valueR2 >>> instruction.shamt;//shift right logical
                break;
            case 10: case 11:instruction.ALUOutput = instruction.r2+instruction.immediate;//Load/store word
                break;
            default:

        }

    }

    private static void decode2(Instruction i) {
        if(i==null)return;
        switch (i.opcode){
            case 0:
            case 1:
            case 9:
            case 8:i.RegDst =i.RegWrite=true;break;
            case 2:
            case 3:
            case 5:
            case 6:i.ALUSrc=i.RegWrite=true;break;
            case 4:i.Branch=true;break;
            case 7:i.Jump =true;break;
            case 10:i.ALUSrc=i.RegWrite=i.MemRead=i.MemtoReg=true;break;
            case 11:i.ALUSrc=i.MemWrite=true;break;
        }
        decode=true;

    }

    private static Instruction decode1(int instruction) {
        if(instruction==-1)return null;
        int opcode;  // bits31:28
        int r1 ;      // bits27:23
        int r2 ;      // bit22:18
        int r3 ;      // bits17:13
        int shamt;   // bits12:0
        int imm ;     // bits17:0
        int address; // bits27:0
        int valueR2;
        int valueR3;

        opcode = instruction & 0b11110000000000000000000000000000 ;
        r1     = instruction & 0b00001111100000000000000000000000 ;
        r2     = instruction & 0b00000000011111000000000000000000 ;
        r3     = instruction & 0b00000000000000111110000000000000 ;
        shamt  = instruction & 0b00000000000000000000111111111111 ;
        imm =    instruction & 0b00000000000000111111111111111111 ;
        address =instruction & 0b00001111111111111111111111111111 ;
        int pcBits = PC & 0b11110000000000000000000000000000;

        if(opcode<0) opcode= (int) ((2 * (long) Integer.MAX_VALUE + 2 + opcode) >>28);
        else opcode = opcode >> 28 ;

        r1 = r1 >> 23;
        r2 = r2 >> 18;
        r3 = r3 >> 13;
        address= address | pcBits;

        int valueR1 = Registers[r1];
        valueR2 = Registers[r2];
        valueR3 = Registers[r3] ;

        decode= false;
        return new Instruction(opcode,shamt,r1,r2,r3,imm,address,valueR1,valueR2,valueR3);


    }

    private static int fetch() {
        if(PC==programLength)return -1;
        int res = Memory[PC];
        PC++;
        System.out.println();
        return res;
    }


    static class Instruction{
        int pc=PC;
        int opcode;
        int shamt;
        int r1;
        int r2;
        int r3;
        int valueR1;
        int valueR2;
        int valueR3;
        int immediate;
        int address;
        boolean RegDst; //todo not used I think in our architecture delete it
        boolean ALUSrc ;
        boolean RegWrite;
        boolean MemRead;
        boolean MemWrite;
        boolean Branch ;
        boolean MemtoReg;
        boolean Jump;
        int ALUOutput;
        int valueLW;


        public Instruction( int opcode, int shamt, int r1, int r2, int r3, int immediate, int address ,int valueR1,int valueR2,int valueR3) {
            this.opcode = opcode;
            this.shamt = shamt;
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.immediate = immediate;
            this.address = address;
            this.valueR2=valueR2;
            this.valueR1=valueR1;
            this.valueR3=valueR3;

        }
    }
}