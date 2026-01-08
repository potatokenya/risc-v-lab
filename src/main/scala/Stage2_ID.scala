import chisel3._
import chisel3.util._

class Stage2_ID(fpga: Boolean) extends Module {
  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))

    val opcode = Output(UInt(7.W))
    val rd     = Output(UInt(5.W))
    val rs1    = Output(UInt(5.W))
    val rs2    = Output(UInt(5.W))
    val funct3 = Output(UInt(3.W))
    val funct7 = Output(UInt(7.W))
    val imm_i  = Output(UInt(32.W))
  })

  // Output
  io.opcode := io.instr(6,0)
  io.rd     := io.instr(11,7)
  io.funct3 := io.instr(14,12)
  io.rs1    := io.instr(19,15)
  io.rs2    := io.instr(24,20)
  io.funct7 := io.instr(31,25)

  // I-type immediate (addi)
  io.imm_i := Cat(Fill(20, io.instr(31)), io.instr(31,20))

}
