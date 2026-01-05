import chisel3._
import chisel3.util._

class Stage1_IF(program: Seq[Int] = Seq(0x12300093), fpga: Boolean = true) extends Module {
  val io = IO(new Bundle {
    val pc     = Output(UInt(32.W))
    val instr  = Output(UInt(32.W))
    val valid  = Output(Bool())
  })

  // -----------------------------
  // Program Counter Circuit
  // -----------------------------
  val pcReg = RegInit(0.U(32.W))

  // Increment PC by 4 every cycle
  pcReg := pcReg + 4.U

  // Output
  io.pc := pcReg
  io.valid := true.B

  // -----------------------------
  // Instruction Memory (ROM)
  // -----------------------------
  // Convert program Seq[Int] to UInt literals
  val rom = VecInit(program.map(_.U(32.W)))

  // Word address = PC >> 2
  val romAddr = pcReg(31, 2)

  // Guard against out-of-bounds access
  val instr = WireDefault(0.U(32.W))
  when (romAddr < rom.length.U) {
    instr := rom(romAddr)
  }

  io.instr := instr
}