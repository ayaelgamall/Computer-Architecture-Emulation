public class HagaSa23aMIPS {
    static int[] Memory;
    static int[] Registers;
    static int PC;
    static final int Zero=0;
    static int programLength;
    static boolean decode=false;
    static boolean fetch=true;
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

        for (int cycle=0 ;PC<Math.min(programLength,1024) ; cycle++)
        {
            writeBack(toWB);
            memory(toMemory);
            toWB=toMemory;
            if( excute) execute1(toBeExcuted);
            else{
                boolean Jump=execute2(toBeExcuted);
                if(Jump){
                    fetch=true;
                    toBeExcuted=null;
                    toBeDecoded=null;
                    toMemory = null;
                    continue;
                }
                toMemory=toBeExcuted;
            }
            if( decode) toBeDecoded=decode1(instruction);
            else{ decode2(toBeDecoded);toBeExcuted=toBeDecoded;}

            instruction= fetch?  fetch():-1 ;fetch=!fetch;

        }
    }
    private static void memory(Instruction i) {
        if(i==null)return;
        if(i.MemRead)
            i.valueLW=Memory[i.ALUOutput];
        else if(i.MemWrite)
            Memory[i.ALUOutput]=i.valueR1;
    }
    private static void writeBack(Instruction i) {
        if(i==null)return;
        if(i.RegWrite){
           Registers[i.r1]=i.MemtoReg? i.valueLW : i.ALUOutput;
        }

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
    private static boolean execute2(Instruction i) {
        if(i==null){
            return false;
        }
        if(i.Branch && zeroFlag){
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
            case 9: instruction.ALUOutput = instruction.valueR2 >> instruction.shamt;//shift right logical
                break;
            case 10: case 11:instruction.ALUOutput = instruction.r2+instruction.immediate;//Load/store word
                break;
            default:

        }

    }

    private static void decode2(Instruction i) {
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
        int res = Memory[PC];
        PC++;
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