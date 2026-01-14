import chisel3._
import chisel3.util._
import lib.peripherals.{MemoryMappedUart, StringStreamer}
import lib.peripherals.MemoryMappedUart.UartPins

class Top(program: Seq[Int], fpga: Boolean) extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(32.W))
    val instr = Output(UInt(32.W))

    // Debug taps (forwarded from Pipeline)
    val dbg_opcode = Output(UInt(7.W))
    val dbg_rd     = Output(UInt(5.W))
    val dbg_rs1    = Output(UInt(5.W))
    val dbg_funct3 = Output(UInt(3.W))
    val dbg_imm    = Output(UInt(32.W))
    val dbg_alu    = Output(UInt(32.W))
    val dbg_we     = Output(Bool())

    // Register file debug access
    val rf_addr = Input(UInt(5.W))
    val rf_data = Output(UInt(32.W))
  })

  // CPU
  val cpu = Module(new Pipeline)

  // Forward signals out
  io.pc    := cpu.io.pc
  io.instr := cpu.io.instr

  // Forward debug signals
  cpu.io.dbg.rf_addr := io.rf_addr
  io.rf_data     := cpu.io.dbg.rf_data

  //---------------------
  // Debug outputs
  //---------------------
  io.dbg_opcode := cpu.io.dbg.opcode
  io.dbg_rd     := cpu.io.dbg.rd
  io.dbg_rs1    := cpu.io.dbg.rs1
  io.dbg_funct3 := cpu.io.dbg.funct3
  io.dbg_imm    := cpu.io.dbg.imm
  io.dbg_alu    := cpu.io.dbg.alu_out
  io.dbg_we     := cpu.io.dbg.we
}
