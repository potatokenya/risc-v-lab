import chisel3._
import chisel3.util._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    val id_ex_memRead = Input(Bool())
    val id_ex_rd = Input(UInt(5.W))
    val if_id_rs1 = Input(UInt(5.W))
    val if_id_rs2 = Input(UInt(5.W))
    val stall = Output(Bool())
  })

  io.stall := io.id_ex_memRead && ((io.id_ex_rd === io.if_id_rs1) || (io.id_ex_rd === io.if_id_rs2))
}