
import java.io.*;
import java.util.ArrayList;
public class HagaSa23aMIPS {
    static int[] Memory;
    static int[] Registers;
    static int PC;
    static final int R0=0;
    static int programLength=0;
    static boolean decode=true;
    static boolean odd =true;
    static boolean execute =true;
    static boolean zeroFlag;
    static  PrintWriter pw;
//    static ArrayList<Integer>a;

    public static void main (String[] args) throws FileNotFoundException {
        pw = new PrintWriter("branchFarPrinting.txt");
        Assembler("branchFarNext");
        runProgram();
        pw.flush();
        pw.close();
//        a=new ArrayList();
    }
    private static void Assembler(String Name) {
        Memory = new int[2048];
        Registers=new int[32];
        PC=0;//??
        String fileName = "src/" + Name+".txt";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            ArrayList<String[]> assm= new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                if (!(line.equals(""))) {
                    assm.add(line.split(" "));
                }
            }
            reader.close();
            for (int i =0;i< assm.size();i++){
	            Memory[programLength++]=(int)Long.parseLong(getBinary(assm.get(i)),2);
            }
        }
        catch(IOException e) {
            pw.println("FILE NOT FOUND");
        }
    }
    private static String getBinary(String [] x) {
        StringBuilder output = new StringBuilder();
        boolean immediate=false;
        boolean sll=false;
        int r;
        switch(x[0].toUpperCase()) {
            case "ADD" : output.append("0000"); break ;
            case "SUB" : output.append("0001"); break ;
            case "MULI" : output.append("0010"); immediate=true; break ;
            case "ADDI" : output.append("0011"); immediate=true;break ;
            case "BNE" : output.append("0100"); immediate=true;break ;
            case "ANDI" : output.append("0101");immediate=true; break ;
            case "ORI" : output.append("0110"); immediate=true;break ;
            case "J" : output.append("0111");
            output.append(String.format("%28s", Integer.toBinaryString(Integer.parseInt(x[1])))
                    .replaceAll(" ", "0"));
            return output.toString() ;
            case "SLL" : output.append("1000"); sll=true; break ;
            case "SRL" : output.append("1001");sll=true; break ;
            case "LW" : output.append("1010");immediate=true; break ;
            case "SW" :output.append("1011");immediate=true;break ;
            default: pw.println("Something went wrong!!"); break;
        }
        r=Integer.parseInt(x[1].substring(1,x[1].length()));
        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
        r=Integer.parseInt(x[2].substring(1,x[2].length()));
        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
        if (immediate) {
            String immed =String.format("%18s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0");
            immed=immed.substring(immed.length()-18);
            output.append(immed);
//            if (Integer.parseInt(x[3])<0){
//                a.add((int)Long.parseLong(output.toString()));
//            }
            return output.toString();
        }
        else {
            if (sll) {
                output.append("00000");
                String immed =String.format("%18s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0");
                immed=immed.substring(immed.length()-18);
                output.append(immed);
//                if (Integer.parseInt(x[3])<0){
//                    a.add((int)Long.parseLong(output.toString()));
//                }
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
//        int limit = 7 + (programLength-1)*2;
        for (int cycle=1 ; ; cycle++)
        {boolean Jump = false;
            pw.println("Clock Cycle : "+cycle+"\n");
            if(odd)
             if(writeBack(toWB)) break;
            if(!odd) memory(toMemory);
            toWB = toMemory;toMemory=null;
            if(execute) execute1(toBeExcuted);
            else{
               Jump =execute2(toBeExcuted);
                toMemory=toBeExcuted;
                toBeExcuted=null;
            }
            if(decode) toBeDecoded=decode1(instruction);
            else{decode2(toBeDecoded);toBeExcuted=toBeDecoded;}

            instruction= odd ?  fetch():-1 ;
            odd =!odd;
            if(Jump){
                instruction=-1;
                toBeExcuted=null;
                toBeDecoded=null;
            }
            pw.println("________________________________________________");
        }
         pw.println("________________________________________________");

        pw.println("The Stages are finished\n");
        pw.println("The Registers Content is :\n" +printReg());
        pw.println("\nThe Memory Content is :");
        printMem();
    }

    private static void printMem() {
        for (int i = 0; i < Memory.length; i++) {
            pw.println("Index "+i +" = "+Memory[i]);
        }
    }

    private static String printReg() {
        StringBuilder s = new StringBuilder("R0=" + R0);
        for (int i = 1; i < Registers.length; i++)
            s.append("\nR").append(i).append("=").append(Registers[i]);
        s.append("\nPC=" );s.append(PC);
        return s.toString();
    }

    private static void memory(Instruction i) {
        if(i==null)return;
        pw.println("At Memory Stage : Instruction "+i.pc);
        pw.println("   Inputs: MemRead="+i.MemRead  + " ,MemWrite="+i.MemWrite  +" ,Data From ALU= " +i.ALUOutput + " ,Write Data ="+ i.valueR1 );

        if(i.MemRead)
            i.valueLW=Memory[i.ALUOutput];

        else if(i.MemWrite){
            pw.print("    Memory index " +i.ALUOutput+ " has changed from : "+Memory[i.ALUOutput]);
            Memory[i.ALUOutput]=i.valueR1;
            pw.println(" to : "+Memory[i.ALUOutput]);
        }
        pw.println();
    }
    private static boolean writeBack(Instruction i) {
        if(i==null) return false;
        pw.println("At Write Back Stage : Instruction "+i.pc +i.binary);
        pw.println("   Inputs: RegWrite="+i.RegWrite + " , MemToReg="+i.MemtoReg+" , DataFromMemory ="+i.valueLW +" ,Data From ALU= " +i.ALUOutput + " ,WriteReg ="+ i.r1  );

        if(i.RegWrite){
            pw.print("    Register R" +i.r1+ " has changed from : "+Registers[i.r1]);
            if(i.r1!=0)
            Registers[i.r1]=i.MemtoReg? i.valueLW : i.ALUOutput;
            pw.println(" to : "+Registers[i.r1]);

        }
       pw.println();
        if(i.pc==programLength)return true;

        return false;
    }
    private static boolean execute2(Instruction i) {

        if(i==null){
            return false;
        }
        execute =true;
        pw.println("At Execute Stage : Instruction "+i.pc +i.binary);
        pw.println("   Inputs: Branch="+i.Branch + " , Zero Flag="+ zeroFlag +" , Jump ="+i.Jump+", PC ="+PC+", ALU output="+i.ALUOutput );

        if(i.Branch && !zeroFlag){
            pw.println("   Branch Instruction");
            pw.print("   PC value changed from "+ PC );
            PC = i.pc + i.ALUOutput;
            pw.println(" to "+ PC +"\n");
            return true;
        }
        if (i.Jump){
            pw.println("   Jump Instruction");
            pw.println("   Address="+i.address);
            pw.print("   PC value changed from "+ PC );
            PC = i.address;
            pw.println(" to "+ PC +"\n");
            return true;
        }
        pw.println();
        return false;
    }

    private static void execute1(Instruction i) {
        if(i==null)return;
        pw.println("At Execute Stage : Instruction "+i.pc+i.binary);
        pw.print("   Inputs: ALUControl="+i.opcode+" , ALUSrc="+i.ALUSrc+
                ", Read data 1="+i.valueR2+((!i.ALUSrc)?(" , Read data 2="+i.valueR3):(" , immediate value="+i.immediate)));

        switch (i.opcode) {
            case 0: i.ALUOutput = i.valueR2 + i.valueR3; //ADD
                break;
            case 1: i.ALUOutput = i.valueR2 - i.valueR3; //SUB
                break;
            case 2: i.ALUOutput = i.valueR2 * i.immediate;//MULT imm
                break;
            case 3: i.ALUOutput = i.valueR2 + i.immediate;//ADD imm
                break;
            case 4: zeroFlag = (0 == (i.valueR2 - i.valueR1));
                    i.ALUOutput = i.immediate-1; //bne //new code
                break;
            case 5: i.ALUOutput = i.valueR2 & i.immediate;//AND imm
                break;
            case 6: i.ALUOutput = i.valueR2 | i.immediate;//OR imm
                break;
            case 7: // instruction.shiftRes = instruction.address << 2;//Jump
                break;
            case 8: i.ALUOutput = i.valueR2 << i.shamt;//Shift left logical
                pw.print(", shift amount="+i.shamt);
                break;
            case 9: i.ALUOutput = i.valueR2 >>> i.shamt;//shift right logical
                pw.print(", shift amount="+i.shamt);
                break;
            case 10: case 11:i.ALUOutput = i.r2+i.immediate;//Load/store word
                break;
            default:
        }
        execute=false;

        pw.println("   Outputs: "+i.ALUOutput+", Zero Flag="+zeroFlag);
        pw.println();
    }

    private static void decode2(Instruction i) {
        if(i==null)return;
        pw.println("At Decode Stage : Instruction "+i.pc+i.binary);
        pw.println("   Inputs: Opcode="+i.opcode);

        switch (i.opcode){
            case 0:
            case 1:
            case 9:
            case 8:i.RegWrite=true;
                break;
            case 2:
            case 3:
            case 5:
            case 6:i.ALUSrc=i.RegWrite=true;break;
            case 4:i.Branch=true;break;
            case 7:i.Jump =true;break;
            case 10:i.ALUSrc=i.RegWrite=i.MemRead=i.MemtoReg=true;break;
            case 11:i.ALUSrc=i.MemWrite=true;break;
        }
        pw.print("   Outputs: ");
        pw.print(", Jump="+i.Jump);
        pw.print(", Branch="+i.Branch);
        pw.print(", MemRead="+i.MemRead);
        pw.print(", MemtoReg="+i.MemtoReg);
        pw.print(", ALUControl="+i.opcode);
        pw.print(", MemWrite="+i.MemWrite);
        pw.print(", ALUSrc="+i.ALUSrc);
        pw.print(", RegWrite="+i.RegWrite);
        pw.println("\n");
        decode=true;

    }

    private static Instruction decode1(int i) {
        if(i==-1)return null;
//        pw.println("At Decode Stage : Instruction "+(PC-1));
        int opcode;  // bits31:28
        int r1 ;      // bits27:23
        int r2 ;      // bit22:18
        int r3 ;      // bits17:13
        int shamt;   // bits12:0
        int imm ;     // bits17:0
        int address; // bits27:0
        int valueR2;
        int valueR3;
        pw.println("At Decode Stage : Instruction "+PC+" ( "+Integer.toBinaryString(i)+" )");
        pw.println("   Input: Instruction="+Integer.toBinaryString(i));

        opcode = i & 0b11110000000000000000000000000000 ;
        r1     = i & 0b00001111100000000000000000000000 ;
        r2     = i & 0b00000000011111000000000000000000 ;
        r3     = i & 0b00000000000000111110000000000000 ;
        shamt  = i & 0b00000000000000000000111111111111 ;
        imm =    i & 0b00000000000000111111111111111111 ;
        address =i & 0b00001111111111111111111111111111 ;
        int pcBits = PC & 0b11110000000000000000000000000000;

        if(opcode<0) opcode= (int) ((2 * (long) Integer.MAX_VALUE + 2 + opcode) >>28);
        else opcode = opcode >> 28 ;
        if (opcode!=10&&opcode!=11) {
            if (String.format("%18s", Integer.toBinaryString(imm).replaceAll(" ", "0")).charAt(0)=='1') {
                    imm= (int)Long.parseLong(String.format("%14s", Integer.toBinaryString(1)).replaceAll(" ", "1")+String.format("%18s", Integer.toBinaryString(imm).replaceAll(" ", "0")),2);
                }
        }
//        if (a.contains(instruction)) {
//            imm*=-1;
//        }

        r1 = r1 >> 23;
        r2 = r2 >> 18;
        r3 = r3 >> 13;
        address= address | pcBits;

        int valueR1 = Registers[r1];
        valueR2 = Registers[r2];
        valueR3 = Registers[r3] ;

        decode= false;
        Instruction inst = new Instruction(opcode,shamt,r1,r2,r3,imm,address,valueR1,valueR2,valueR3," ( "+Integer.toBinaryString(i)+" )");
        pw.print("   Outputs: Opcode="+inst.opcode);
        pw.print(", shift amount="+inst.opcode);
        pw.print(", Opcode="+inst.shamt);
        pw.print(", Read Register 1="+inst.r1);
        pw.print(", Read Register 2="+inst.r2);
        pw.print(", Register 3="+inst.r3);
        pw.print(", Immediate Value="+inst.immediate);
        pw.print(", Address="+inst.address);
        pw.println();
         return inst;


    }

    private static int fetch() {
        if(PC==programLength)return -1;
        int res = Memory[PC];
        PC++;
        pw.println( "At Fetch Stage : Instruction "+ PC + " ( "+Integer.toBinaryString(res)+" )");
        pw.println("   PC is incremented to "+PC);
        pw.println("   Output : "+ Integer.toBinaryString(res) +"\n");
        return res;
    }

    static class Instruction{
        String binary;
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

        boolean ALUSrc ;
        boolean RegWrite;
        boolean MemRead;
        boolean MemWrite;
        boolean Branch ;
        boolean MemtoReg;
        boolean Jump;
        int ALUOutput;
        int valueLW;

        public Instruction(int opcode, int shamt, int r1, int r2, int r3, int immediate, int address, int valueR1, int valueR2, int valueR3, String s) {
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
            this.binary=s;

        }
    }

//    private static void Assembler(String Name) {
//        Memory = new int[2048];
//        Registers=new int[32];
//        PC=0;//??
//        String fileName = "src/" + Name+".txt";
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(fileName));
//            StringBuilder stringBuilder = new StringBuilder();
//            String line = null;
//            ArrayList<String[]> assm= new ArrayList<>();
//            String [] labels= new String [1024];
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line);
//                if (!(line.equals(""))) {
//                    assm.add(line.split(" "));
//                }
//            }
//            reader.close();
//	            for (int i =0;i<assm.size();i++){
//	                    if (assm.get(i)[0].equalsIgnoreCase("BNE")){
//	                        labels[i] = assm.get(i)[3];
//	                    }
//	                    if (assm.get(i)[0].equalsIgnoreCase("J")){
//	                        labels[i] = assm.get(i)[1];
//	                    }
//	            }
//            for (int i =0;i< assm.size();i++){
//	                if (!(assm.get(i)[0].equalsIgnoreCase("ADD")||assm.get(i)[0].equalsIgnoreCase("SUB")||assm.get(i)[0].equalsIgnoreCase("MULI")||assm.get(i)[0].equalsIgnoreCase("BNE")||assm.get(i)[0].equalsIgnoreCase("ANDI")||assm.get(i)[0].equalsIgnoreCase("ORI")||assm.get(i)[0].equalsIgnoreCase("J")||assm.get(i)[0].equalsIgnoreCase("SLL")||assm.get(i)[0].equalsIgnoreCase("SRL")||assm.get(i)[0].equalsIgnoreCase("LW")||assm.get(i)[0].equalsIgnoreCase("SW"))){
//	                        for (int j=0;j<labels.length;j++){
//	                            if (labels[j]!=null){
//	                                if (labels[j].equals(assm.get(i)[0])){
//
//	                                if (Memory[j]==7){
//	                                    Memory[j]=Integer.parseInt(String.format("%4s", Integer.toBinaryString(Memory[j])).replaceAll(" ", "0")+""+String.format("%28s", Integer.toBinaryString(programLength)).replaceAll(" ", "0"),2);
//	                                }
//	                                else{
//	                                    Memory[j]=Integer.parseInt(String.format("%14s", Integer.toBinaryString(Memory[j])).replaceAll(" ", "0")+""+String.format("%18s", Integer.toBinaryString(programLength-j)).replaceAll(" ", "0"),2);
//	}}}}}
//	                else {
//                Memory[programLength++]=(int)Long.parseLong(getBinary(assm.get(i)),2);
//	                }
//            }
//        for (int i =0;i<6;i++){
//                pw.println(String.format("%32s", Integer.toBinaryString(Memory[i])).replaceAll(" ", "0")+"   "+i);
//            }
//        }
//        catch(IOException e) {
//            pw.println("FILE NOT FOUND");
//        }
//    }
//    private static String getBinary(String [] x) {
//        StringBuilder output = new StringBuilder();
//        boolean immediate=false;
//        boolean sll=false;
//        boolean mem=false;
//        int r;
//	        boolean branch= false;
//	        if ((x[0].toUpperCase()).equals("BNE"))
//	            branch=true;
//        switch(x[0].toUpperCase()) {
//            case "ADD" : output.append("0000"); break ;
//            case "SUB" : output.append("0001"); break ;
//            case "MULI" : output.append("0010"); immediate=true; break ;
//            case "ADDI" : output.append("0011"); immediate=true;break ;
//            case "BNE" : output.append("0100"); immediate=true;break ;
//            case "ANDI" : output.append("0101");immediate=true; break ;
//            case "ORI" : output.append("0110"); immediate=true;break ;
//            case "J" : output.append("0111"); output.append(String.format("%28s", Integer.toBinaryString(Integer.parseInt(x[1]))).replaceAll(" ", "0"));return output.toString() ;
//            case "SLL" : output.append("1000"); sll=true; break ;
//            case "SRL" : output.append("1001");sll=true; break ;
//            case "LW" : output.append("1010");immediate=true; break ;
//            case "SW" :output.append("1011");immediate=true;break ;
//            default: pw.println("Something went wrong!!"); break;
//        }
//        r=Integer.parseInt(x[1].substring(1,x[1].length()));
//        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
//	        if (mem) {
//	            String imm="";
//	            for (int i =0;i<x[2].length();i++) {
//	                if(("(").equals(x[2].charAt(i)+"")) {
//	                    output.append(String.format("%5s", Integer.toBinaryString(Integer.parseInt(x[2].substring(i+2,x[2].length()-1)))).replaceAll(" ", "0"));
//	                    output.append(String.format("%18s", Integer.toBinaryString(Integer.parseInt(imm))).replaceAll(" ", "0"));
//	                    return output.toString();
//	                }
//	                imm+=x[2].charAt(i)+"";
//	            }}
//        r=Integer.parseInt(x[2].substring(1,x[2].length()));
//        output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
//        if (immediate) {
//	            if (branch){
//	                return output.toString();
//	            }
//            output.append(String.format("%18s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0"));
//            return output.toString();
//        }
//        else {
//            if (sll) {
//                output.append("00000");
//                output.append(String.format("%13s", Integer.toBinaryString(Integer.parseInt(x[3]))).replaceAll(" ", "0"));
//                return output.toString();
//            }
//            r=Integer.parseInt(x[3].substring(1,x[3].length()));
//            output.append(String.format("%5s", Integer.toBinaryString(r)).replaceAll(" ", "0"));
//            output.append("0000000000000");
//            return output.toString();
//        }
//    }
}