import chisel3._

class ID_EXRegister extends Module {
  val io = IO(new Bundle {
    val rs1_val_in = Input(UInt(32.W))
    val imm_in     = Input(UInt(32.W))
    val rd_in      = Input(UInt(5.W))
    val opcode_in  = Input(UInt(7.W))
    val funct3_in  = Input(UInt(3.W))

    val rs1_val_out = Output(UInt(32.W))
    val imm_out     = Output(UInt(32.W))
    val rd_out      = Output(UInt(5.W))
    val opcode_out  = Output(UInt(7.W))
    val funct3_out  = Output(UInt(3.W))
  })

  io.rs1_val_out := RegNext(io.rs1_val_in)
  io.imm_out     := RegNext(io.imm_in)
  io.rd_out      := RegNext(io.rd_in)
  io.opcode_out  := RegNext(io.opcode_in)
  io.funct3_out  := RegNext(io.funct3_in)
}
