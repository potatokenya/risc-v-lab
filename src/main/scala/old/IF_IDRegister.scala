package old


import chisel3._
class IF_IDRegister extends Module {
  val io = IO(new Bundle {
    val pc_in    = Input(UInt(32.W))
    val instr_in = Input(UInt(32.W))

    val pc_out    = Output(UInt(32.W))
    val instr_out = Output(UInt(32.W))
  })

  val pcReg    = RegInit(0.U(32.W))
  val instrReg = RegInit(0.U(32.W))

  pcReg    := io.pc_in
  instrReg := io.instr_in

  io.pc_out    := pcReg
  io.instr_out := instrReg
}
