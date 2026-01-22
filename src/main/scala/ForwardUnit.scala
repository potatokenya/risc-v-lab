import chisel3._
import chisel3.util._

class ForwardUnit extends Module {
  val io = IO(new Bundle {
    val ex_rs1 = Input(UInt(5.W))
    val ex_rs2 = Input(UInt(5.W))
    val mem_rd = Input(UInt(5.W))
    val wb_rd = Input(UInt(5.W))
    val mem_regWrite = Input(Bool())
    val wb_regWrite = Input(Bool())
    val forwardA = Output(UInt(2.W))
    val forwardB = Output(UInt(2.W))
  })

  io.forwardA := 0.U
  io.forwardB := 0.U

  when(io.mem_regWrite && io.mem_rd =/= 0.U && io.mem_rd === io.ex_rs1) { io.forwardA := 2.U }
    .elsewhen(io.wb_regWrite && io.wb_rd =/= 0.U && io.wb_rd === io.ex_rs1) { io.forwardA := 1.U }

  when(io.mem_regWrite && io.mem_rd =/= 0.U && io.mem_rd === io.ex_rs2) { io.forwardB := 2.U }
    .elsewhen(io.wb_regWrite && io.wb_rd =/= 0.U && io.wb_rd === io.ex_rs2) { io.forwardB := 1.U }
}