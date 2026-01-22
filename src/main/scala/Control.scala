import chisel3._
import chisel3.util._

class Control extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val regWrite = Output(Bool())
    val memRead = Output(Bool())
    val memWrite = Output(Bool())
    val memToReg = Output(Bool())
    val aluSrc = Output(Bool())
    val branch = Output(Bool())
  })

  io.regWrite := false.B
  io.memRead := false.B
  io.memWrite := false.B
  io.memToReg := false.B
  io.aluSrc := false.B
  io.branch := false.B

  switch(io.opcode) {
    is("b0110011".U) { io.regWrite := true.B } // R-type
    is("b0000011".U) { io.regWrite := true.B; io.memRead := true.B; io.memToReg := true.B; io.aluSrc := true.B } // Load
    is("b0100011".U) { io.memWrite := true.B; io.aluSrc := true.B } // Store
    is("b0010011".U) { io.regWrite := true.B; io.aluSrc := true.B } // I-type ALU
    is("b1100011".U) { io.branch := true.B; io.aluSrc := false.B } // branch instruction
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