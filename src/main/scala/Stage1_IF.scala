import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class Stage1_IF(program: Seq[Int], fpga: Boolean) extends Module {
  val io = IO(new Bundle {
    val pc     = Output(UInt(32.W))
    val instr  = Output(UInt(32.W))
    val valid  = Output(Bool())
  })

  // -----------------------------
  // Program Counter
  // -----------------------------
  val pcReg = RegInit(0.U(32.W))
  pcReg := pcReg + 4.U

  io.pc := pcReg
  io.valid := true.B

  // -----------------------------
  // Instruction Memory (ROM)
  // -----------------------------
  val imem = SyncReadMem(1024, UInt(32.W))

  // Load program at elaboration time
  loadMemoryFromFile(imem, "src/main/resources/program_add.hex")

  // Word address = PC >> 2
  val addr = pcReg(31, 2)

  io.instr := imem.read(addr)
}
