import chisel3._
import chisel3.util._

class Stage3_EX(fpga: Boolean) extends Module {
  val io = IO(new Bundle {
    // Inputs from ID stage
    val rs1_val = Input(UInt(32.W))
    val imm     = Input(UInt(32.W))
    val rd      = Input(UInt(5.W))
    val opcode  = Input(UInt(7.W))
    val funct3  = Input(UInt(3.W))

    // Outputs to next stage
    val alu_out = Output(UInt(32.W))
    val rd_out  = Output(UInt(5.W))
    val we_out  = Output(Bool())
  })

  // Default outputs
  io.alu_out := 0.U
  io.rd_out  := io.rd
  io.we_out  := false.B

  // Decode (only ADDI for now)
  when (io.opcode === "b0010011".U && io.funct3 === "b000".U) {
    io.alu_out := io.rs1_val + io.imm
    io.we_out  := true.B
  }


  // Pipeline registers


  // ALU


  // Output


}