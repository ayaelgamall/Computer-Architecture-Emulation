public class HagaSa23aMIPS {
    static int[] Memory;
    static int[] Registers;
    static int PC;
    static final int Zero=0;
    static int programLength;
    static boolean decode=false;
    static boolean excute=false;
    static boolean zeroFlag;
    public static void main (String[] args){
        Assembler();
        runProgram();
    }

    private static void Assembler() {
        //todo read file
        Memory=new int[2048];
        Registers=new int[32];
        PC=0;//??
        programLength=0;//??
    }

    private static void runProgram() {
         int instruction =-1;
         Instruction toBeExcuted=null;
         Instruction toBeDecoded=null;
         Instruction toMemory = null;
         Instruction toWB = null;

        for (int cycle=0 ; ; cycle++)
        {
            writeBack(toWB);
            memory(toMemory);
            toWB=toMemory;
           if( excute) execute1(toBeExcuted);
           else{ execute2(toBeExcuted);toMemory=toBeExcuted;}
           if( decode) toBeDecoded=decode1(instruction);
           else{ decode2(toBeDecoded);toBeExcuted=toBeDecoded;}

            instruction=  fetch();
            if(cycle>6)
                break;

        }
    }

    private static void memory(Instruction i) {

    }


    private static void writeBack(Instruction i) {
    }



    public static int ALU(int operandA, int operandB, int operation) {

        int output = 0;
       int zeroFlag = 0;
        switch(operation ){
            case 0:output = operandA<<operandB;break;
            case 1:output= operandA & operandB;break;
            case 2:output=operandA+operandB;break;
            case 3:output=operandA*operandB;break;
            case 4:output=operandA>operandB?1:0;break;
            case 5:output= ~(operandA & operandB);break;
            case 6:output=operandA-operandB;break;
            default: ;
        }
        zeroFlag=output==0?1:0;
        // Complete the ALU body here...

        System.out.println("Operation = "+operation);
        System.out.println("First Operand = "+operandA);
        System.out.println("Second Operand = "+operandB);
        System.out.println("Result = "+output);
        System.out.println("Zero Flag = "+zeroFlag);

        return output;
    }


    private static void execute2(Instruction i) {
        if(i.Branch){
            PC=i.pc+i.immediate*4;
        }excute=false;
    }

    private static void execute1(Instruction instruction) {
        switch (instruction.opcode) {
            case 0: instruction.regData = Registers[instruction.r2] + Registers[instruction.r3]; //ADD
                break;
            case 1: instruction.regData = Registers[instruction.r2] - Registers[instruction.r3]; //SUB
                break;
            case 2: instruction.regData = Registers[instruction.r2] * instruction.immediate;//MULT imm
                break;
            case 3: instruction.regData = Registers[instruction.r2] + instruction.immediate;//ADD imm
                break;
            case 4: zeroFlag = (0 == (instruction.r2 - instruction.r1));//bne
                break;
            case 5: instruction.regData = Registers[instruction.r2] & instruction.immediate;//AND imm
                break;
            case 6: instruction.regData = Registers[instruction.r2] | instruction.immediate;//OR imm
                break;
            case 7: instruction.shiftRes = instruction.address << 2;//Jump
                break;
            case 8: instruction.regData = Registers[instruction.r2] << instruction.shamt;//Shift left logical
                break;
            case 9: instruction.regData = Registers[instruction.r2] >> instruction.shamt;//shift right logical
                break;
            case 10: case 11:instruction.shiftRes = instruction.r2+instruction.immediate;//Load/store word
                break;
            default:

        }

    }

    private static void decode2(Instruction i) {
        switch (i.opcode){
            case 0:
            case 1:
            case 2:
            case 9:
            case 8:i.RegDst =i.RegWrite=true;break;
            case 3:
            case 5:
            case 6:i.ALUSrc=i.RegWrite=true;break;
            case 4:i.RegDst=i.RegWrite=i.Branch=true;break;
            case 7:i.Branch=true;break;
            case 10:i.ALUSrc=i.RegWrite=i.MemRead=i.MemtoReg=true;break;
            case 11:i.ALUSrc=i.MemWrite=true;break;
        }
        decode=true;

    }

    private static Instruction decode1(int instruction) {
        if(instruction==-1)return null;
        int opcode = 0;  // bits31:28
        int r1 = 0;      // bits27:23
        int r2 = 0;      // bit22:18
        int r3 = 0;      // bits17:13
        int shamt = 0;   // bits12:0
        int imm = 0;     // bits17:0
        int address = 0; // bits27:0

        opcode = instruction & 0b11110000000000000000000000000000 ;//todo not negative
        r1     = instruction & 0b00001111100000000000000000000000 ;
        r2     = instruction & 0b00000000011111000000000000000000 ;
        r3     = instruction & 0b00000000000000111110000000000000 ;
        shamt  = instruction & 0b00000000000000000000111111111111 ;
        imm =    instruction & 0b00000000000000111111111111111111 ;
        address =instruction & 0b00001111111111111111111111111111 ;

        opcode = opcode >> 28;
        r1 = r1 >> 23;
        r2 = r2 >> 18;
        r3 = r3 >> 13;

        decode= false;
        return new Instruction(opcode,shamt,r1,r2,r3,imm,address);


    }

    private static int fetch() {
        int res = Memory[PC];
        PC++;
        return res;
    }


    static class Instruction{
        public int regData;
        public int shiftRes;
        int pc=PC;
        int opcode;
        int shamt;
        int r1;
        int r2;
        int r3;
        int immediate;
        int address;
        //        int ALUop;
        boolean RegDst;
        boolean ALUSrc ;
        boolean RegWrite;
        boolean MemRead;
        boolean MemWrite;
        boolean Branch ;
        boolean MemtoReg;

        public Instruction(int opcode, int shamt, int r1, int r2, int r3, int immediate, int address) {
            this.opcode = opcode;
            this.shamt = shamt;
            this.r1 = r1;
            this.r2 = r2;
            this.r3 = r3;
            this.immediate = immediate;
            this.address = address;

        }
    }
}

