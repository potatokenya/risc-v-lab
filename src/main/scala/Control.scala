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

