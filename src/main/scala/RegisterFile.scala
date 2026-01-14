import chisel3._

class RegisterFile extends Module {
  val io = IO(new Bundle {
    val rs1 = Input(UInt(5.W))     // Source register 1 address
    val rs2 = Input(UInt(5.W))     // Source register 2 address
    val rd  = Input(UInt(5.W))     // Destination register address
    val wd  = Input(UInt(32.W))    // Write data (from WB stage)
    val we  = Input(Bool())        // Register write enable

    val rd1 = Output(UInt(32.W))   // Read data from rs1
    val rd2 = Output(UInt(32.W))   // Read data from rs2

    // Debug: read any register by index
    val dbg_addr = Input(UInt(5.W))
    val dbg_data = Output(UInt(32.W))
  })
  // Register file storage
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))


  // Reading from registers
  // Combination reads (x0 always reads as zero)
  io.rd1 := Mux(io.rs1.orR, regs(io.rs1), 0.U)
  io.rd2 := Mux(io.rs2.orR, regs(io.rs2), 0.U)

  // Writing to registers (Synchronous write on clock edge)
  when (io.we && io.rd =/= 0.U) {
    regs(io.rd) := io.wd
  }

  // Debug read
  io.dbg_data := Mux(io.dbg_addr.orR, regs(io.dbg_addr), 0.U)

}