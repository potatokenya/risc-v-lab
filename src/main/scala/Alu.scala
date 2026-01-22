import chisel3._
import chisel3.util._

class ALU extends Module {
  val io = IO(new Bundle {
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val aluCtrl = Input(UInt(4.W))
    val out = Output(UInt(32.W))
    val zero = Output(Bool())
  })

  io.out := 0.U
  io.zero := false.B

  switch(io.aluCtrl) {
    is("b0000".U) { io.out := io.in1 + io.in2 } // ADD
    is("b1000".U) { io.out := io.in1 - io.in2 } // SUB
    is("b0001".U) { io.out := io.in1 << io.in2(4,0) } // SLL
    is("b0010".U) { io.out := (io.in1.asSInt < io.in2.asSInt).asUInt } // SLT
    is("b0011".U) { io.out := (io.in1 < io.in2).asUInt } // SLTU
    is("b0100".U) { io.out := io.in1 ^ io.in2 } // XOR
    is("b0101".U) { io.out := io.in1 >> io.in2(4,0) } // SRL
    is("b1101".U) { io.out := (io.in1.asSInt >> io.in2(4,0)).asUInt } // SRA
    is("b0110".U) { io.out := io.in1 | io.in2 } // OR
    is("b0111".U) { io.out := io.in1 & io.in2 } // AND
  }

  io.zero := (io.out === 0.U)
}