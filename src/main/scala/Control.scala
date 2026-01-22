import chisel3._
import chisel3.util._

/* ============================================================
 * Control Unit
 * ============================================================ */
class Control extends Module {
  val io = IO(new Bundle {
    val opcode   = Input(UInt(7.W))

    val regWrite = Output(Bool())
    val memRead  = Output(Bool())
    val memWrite = Output(Bool())
    val memToReg = Output(Bool())
    val aluSrc   = Output(Bool())
    val branch   = Output(Bool())

    val jump     = Output(Bool())   // JAL
    val jumpReg  = Output(Bool())   // JALR
    val wbSelPC4 = Output(Bool())   // write PC+4
  })

  // defaults
  io.regWrite := false.B
  io.memRead  := false.B
  io.memWrite := false.B
  io.memToReg := false.B
  io.aluSrc   := false.B
  io.branch   := false.B
  io.jump     := false.B
  io.jumpReg  := false.B
  io.wbSelPC4 := false.B

  switch(io.opcode) {

    // R-type
    is("b0110011".U) {
      io.regWrite := true.B
    }

    // I-type ALU
    is("b0010011".U) {
      io.regWrite := true.B
      io.aluSrc   := true.B
    }

    // Load
    is("b0000011".U) {
      io.regWrite := true.B
      io.memRead  := true.B
      io.memToReg := true.B
      io.aluSrc   := true.B
    }

    // Store
    is("b0100011".U) {
      io.memWrite := true.B
      io.aluSrc   := true.B
    }

    // Branch
    is("b1100011".U) {
      io.branch := true.B
    }

    // JAL
    is("b1101111".U) {
      io.jump     := true.B
      io.regWrite := true.B
      io.wbSelPC4 := true.B
    }

    // JALR
    is("b1100111".U) {
      io.jumpReg  := true.B
      io.regWrite := true.B
      io.aluSrc   := true.B
      io.wbSelPC4 := true.B
    }
  }
}

/* ============================================================
 * ALU Control Unit
 * ============================================================ */
class ALUControl extends Module {
  val io = IO(new Bundle {
    val opcode  = Input(UInt(7.W))
    val funct3  = Input(UInt(3.W))
    val funct7  = Input(UInt(7.W))
    val aluCtrl = Output(UInt(4.W))
  })

  io.aluCtrl := "b0000".U // ADD default

  switch(io.opcode) {

    // R-type
    is("b0110011".U) {
      switch(io.funct3) {
        is("b000".U) { io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1000".U, "b0000".U) } // SUB/ADD
        is("b001".U) { io.aluCtrl := "b0001".U } // SLL
        is("b010".U) { io.aluCtrl := "b0010".U } // SLT
        is("b011".U) { io.aluCtrl := "b0011".U } // SLTU
        is("b100".U) { io.aluCtrl := "b0100".U } // XOR
        is("b101".U) { io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1101".U, "b0101".U) } // SRA/SRL
        is("b110".U) { io.aluCtrl := "b0110".U } // OR
        is("b111".U) { io.aluCtrl := "b0111".U } // AND
      }
    }

    // I-type ALU
    is("b0010011".U) {
      switch(io.funct3) {
        is("b000".U) { io.aluCtrl := "b0000".U } // ADDI
        is("b010".U) { io.aluCtrl := "b0010".U } // SLTI
        is("b011".U) { io.aluCtrl := "b0011".U } // SLTIU
        is("b100".U) { io.aluCtrl := "b0100".U } // XORI
        is("b110".U) { io.aluCtrl := "b0110".U } // ORI
        is("b111".U) { io.aluCtrl := "b0111".U } // ANDI
        is("b001".U) { io.aluCtrl := "b0001".U } // SLLI
        is("b101".U) { io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1101".U, "b0101".U) } // SRAI/SRLI
      }
    }

    // Load / Store
    is("b0000011".U, "b0100011".U) {
      io.aluCtrl := "b0000".U
    }

    // Branch
    is("b1100011".U) {
      io.aluCtrl := "b1000".U // SUB
    }
  }
}

/* ============================================================
 * Immediate Generator
 * ============================================================ */
object ImmGen {
  def getImm(instr: UInt, instrType: UInt): SInt = {
    val imm = Wire(SInt(32.W))
    imm := 0.S

    switch(instrType) {
      is(0.U) { imm := instr(31,20).asSInt }  // I-type
      is(1.U) { imm := Cat(instr(31,25), instr(11,7)).asSInt } // S-type
      is(2.U) { // B-type
        imm := Cat(instr(31), instr(7), instr(30,25), instr(11,8), 0.U(1.W)).asSInt
      }
      is(4.U) { // J-type
        imm := Cat(Fill(11, instr(31)), instr(19,12), instr(20), instr(30,21), 0.U(1.W)).asSInt
      }
    }
    imm
  }
}

/* ============================================================
 * Branch Comparator
 * ============================================================ */
object BranchComp {
  def compare(funct3: UInt, a: UInt, b: UInt): Bool = {
    val res = WireDefault(false.B)
    switch(funct3) {
      is("b000".U) { res := a === b }               // BEQ
      is("b001".U) { res := a =/= b }               // BNE
      is("b100".U) { res := a.asSInt < b.asSInt }   // BLT
      is("b101".U) { res := a.asSInt >= b.asSInt }  // BGE
      is("b110".U) { res := a < b }                 // BLTU
      is("b111".U) { res := a >= b }                // BGEU
    }
    res
  }
}

/*
class Control extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val regWrite = Output(Bool())
    val memRead = Output(Bool())
    val memWrite = Output(Bool())
    val memToReg = Output(Bool())
    val aluSrc = Output(Bool())
    val branch = Output(Bool())
    val jump     = Output(Bool())   // JAL
    val jumpReg  = Output(Bool())   // JALR
    val wbSelPC4 = Output(Bool())   // write PC+4
  })

  io.regWrite := false.B
  io.memRead := false.B
  io.memWrite := false.B
  io.memToReg := false.B
  io.aluSrc := false.B
  io.branch := false.B
  io.jump     := false.B
  io.jumpReg  := false.B
  io.wbSelPC4 := false.B

  switch(io.opcode) {
    // R-type
    is("b0110011".U) { io.regWrite := true.B }
    // Load
    is("b0000011".U) { io.regWrite := true.B; io.memRead := true.B; io.memToReg := true.B; io.aluSrc := true.B }
    // Store
    is("b0100011".U) { io.memWrite := true.B; io.aluSrc := true.B }
    // I-type ALU
    is("b0010011".U) { io.regWrite := true.B; io.aluSrc := true.B }
    // branch instruction
    is("b1100011".U) { io.branch := true.B; io.aluSrc := false.B }
    // JAL
    is("b1101111".U) { io.jump := true.B; io.regWrite := true.B; io.wbSelPC4 := true.B}
    // JALR
    is("b1100111".U) {io.jumpReg := true.B; io.regWrite := true.B; io.aluSrc := true.B; io.wbSelPC4 := true.B }
  }
}

class ALUControl extends Module {
  val io = IO(new Bundle {
    val opcode  = Input(UInt(7.W))
    val funct3  = Input(UInt(3.W))
    val funct7  = Input(UInt(7.W))
    val aluCtrl = Output(UInt(4.W))
  })

  // Default: ADD
  io.aluCtrl := "b0000".U

  switch(io.opcode) {

    // R-type
    is("b0110011".U) {
      switch(io.funct3) {
        is("b000".U) { io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1000".U, "b0000".U) } // SUB / ADD
        is("b001".U) { io.aluCtrl := "b0001".U } // SLL
        is("b010".U) { io.aluCtrl := "b0010".U } // SLT
        is("b011".U) { io.aluCtrl := "b0011".U } // SLTU
        is("b100".U) { io.aluCtrl := "b0100".U } // XOR
        is("b101".U) { io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1101".U, "b0101".U) } // SRA / SRL
        is("b110".U) { io.aluCtrl := "b0110".U } // OR
        is("b111".U) { io.aluCtrl := "b0111".U } // AND
      }
    }

    // I-type ALU
    is("b0010011".U) {
      switch(io.funct3) {
        is("b000".U) { io.aluCtrl := "b0000".U } // ADDI
        is("b010".U) { io.aluCtrl := "b0010".U } // SLTI
        is("b011".U) { io.aluCtrl := "b0011".U } // SLTIU
        is("b100".U) { io.aluCtrl := "b0100".U } // XORI
        is("b110".U) { io.aluCtrl := "b0110".U } // ORI
        is("b111".U) { io.aluCtrl := "b0111".U } // ANDI
        is("b001".U) { io.aluCtrl := "b0001".U } // SLLI
        is("b101".U) {
          io.aluCtrl := Mux(io.funct7 === "b0100000".U, "b1101".U, "b0101".U) // SRAI / SRLI
        }
      }
    }

    // Load / Store use ADD for address
    is("b0000011".U) { io.aluCtrl := "b0000".U } // LW
    is("b0100011".U) { io.aluCtrl := "b0000".U } // SW

    // Branch: use SUB for comparison
    is("b1100011".U) { io.aluCtrl := "b1000".U } // BEQ/BNE/etc
  }
}

class DecodedInstr extends Bundle {
  val instrType = UInt(3.W)   // R, I, S, SB, U, UJ
  val isBranch  = Bool()
  val isJal     = Bool()
  val isJalr    = Bool()
  val rs1Valid  = Bool()
  val rs2Valid  = Bool()
  val imm       = SInt(32.W)
  val aluOp     = UInt(4.W)   // maps to your ALUControl codes
}

object Decode {

  def getImm(instr: UInt, instrType: UInt): SInt = {
    val imm = Wire(SInt(32.W))
    imm := 0.S
    switch(instrType) {
      is(0.U) { imm := (instr(31,20)).asSInt } // I-type
      is(1.U) { imm := (Cat(instr(31,25), instr(11,7))).asSInt } // S-type
      is(2.U) { imm := (Cat(instr(31), instr(7), instr(30,25), instr(11,8), 0.U(1.W))).asSInt } // SB-type
      is(3.U) { imm := (instr(31,12) ## Fill(12,0.U)).asSInt } // U-type
      is(4.U) { imm := (Fill(11,instr(31)) ## instr(19,12) ## instr(20) ## instr(30,21) ## 0.U(1.W)).asSInt } // UJ-type
    }
    imm
  }

  def decode(instr: UInt): DecodedInstr = {
    val dec = Wire(new DecodedInstr)
    dec := 0.U.asTypeOf(new DecodedInstr)

    val opcode = instr(6,0)
    val funct3 = instr(14,12)
    val funct7 = instr(31,25)

    switch(opcode) {

      // R-type
      is("b0110011".U) {
        dec.instrType := 0.U
        dec.rs1Valid := true.B
        dec.rs2Valid := true.B
        switch(funct3) {
          is("b000".U) { dec.aluOp := Mux(funct7 === "b0100000".U, "b1000".U, "b0000".U) } // SUB/ADD
          is("b001".U) { dec.aluOp := "b0001".U } // SLL
          is("b010".U) { dec.aluOp := "b0010".U } // SLT
          is("b011".U) { dec.aluOp := "b0011".U } // SLTU
          is("b100".U) { dec.aluOp := "b0100".U } // XOR
          is("b101".U) { dec.aluOp := Mux(funct7 === "b0100000".U, "b1101".U, "b0101".U) } // SRA/SRL
          is("b110".U) { dec.aluOp := "b0110".U } // OR
          is("b111".U) { dec.aluOp := "b0111".U } // AND
        }
      }

      // I-type ALU
      is("b0010011".U) {
        dec.instrType := 0.U
        dec.rs1Valid := true.B
        switch(funct3) {
          is("b000".U) { dec.aluOp := "b0000".U } // ADDI
          is("b010".U) { dec.aluOp := "b0010".U } // SLTI
          is("b011".U) { dec.aluOp := "b0011".U } // SLTIU
          is("b100".U) { dec.aluOp := "b0100".U } // XORI
          is("b110".U) { dec.aluOp := "b0110".U } // ORI
          is("b111".U) { dec.aluOp := "b0111".U } // ANDI
          is("b001".U) { dec.aluOp := "b0001".U } // SLLI
          is("b101".U) { dec.aluOp := Mux(funct7 === "b0100000".U, "b1101".U, "b0101".U) } // SRAI/SRLI
        }
      }

      // Branch
      is("b1100011".U) {
        dec.instrType := 2.U
        dec.isBranch := true.B
        dec.rs1Valid := true.B
        dec.rs2Valid := true.B
        dec.aluOp := "b1000".U   // SUB if you still want zero flag support
      }

      // JAL
      is("b1101111".U) {
        dec.instrType := 4.U
        dec.isJal := true.B
      }

      // JALR
      is("b1100111".U) {
        dec.instrType := 0.U
        dec.isJalr := true.B
        dec.rs1Valid := true.B
      }
    }

    dec.imm := getImm(instr, dec.instrType)
    dec
  }

  def compare(funct3: UInt, op1: UInt, op2: UInt): Bool = {
    val res = WireDefault(false.B)
    switch(funct3) {
      is("b000".U) { res := op1 === op2 }  // BEQ
      is("b001".U) { res := op1 =/= op2 }  // BNE
      is("b100".U) { res := op1.asSInt < op2.asSInt } // BLT
      is("b101".U) { res := op1.asSInt >= op2.asSInt } // BGE
      is("b110".U) { res := op1 < op2 }  // BLTU
      is("b111".U) { res := op1 >= op2 } // BGEU
    }
    res
  }

}

 */