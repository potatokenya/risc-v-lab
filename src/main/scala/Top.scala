import chisel3._
import chisel3.util._

import lib.peripherals.{MemoryMappedUart, StringStreamer}
import lib.peripherals.MemoryMappedUart.UartPins

class Top(program: Seq[Int], fpga: Boolean) extends Module {
  val io = IO(new Bundle {
    // Debug outputs for now
    val pc    = Output(UInt(32.W))
    val instr = Output(UInt(32.W))

    // ---- DEBUG ----
    val dbg_imm     = Output(UInt(32.W))
    val dbg_rs1_val = Output(UInt(32.W))
    val dbg_alu_out = Output(UInt(32.W))
  })

  // --------------------
  // IF stage
  // --------------------
  val ifStage = Module(new Stage1_IF(program, fpga))
  io.pc := ifStage.io.pc
  io.instr := ifStage.io.instr

  // --------------------
  // IF/ID
  // --------------------
  val ifid = Module(new IF_IDRegister)
  ifid.io.pc_in    := ifStage.io.pc
  ifid.io.instr_in := ifStage.io.instr

  // --------------------
  // Decode
  // --------------------
  val id = Module(new Stage2_ID(fpga))
  id.io.instr := ifid.io.instr_out

  // --------------------
  // Register File
  // --------------------
  val rf = Module(new RegisterFile)
  rf.io.rs1 := id.io.rs1
  rf.io.rs2 := id.io.rs2

  // TEMP: no writeback yet
  rf.io.we := false.B
  rf.io.rd := 0.U
  rf.io.wd := 0.U

  // --------------------
  // ID/EX
  // --------------------
  val idex = Module(new ID_EXRegister)
  idex.io.rs1_val_in := rf.io.rd1
  idex.io.imm_in     := id.io.imm_i
  idex.io.rd_in      := id.io.rd
  idex.io.opcode_in  := id.io.opcode
  idex.io.funct3_in  := id.io.funct3

  // --------------------
  // Execute
  // --------------------
  val ex = Module(new Stage3_EX(fpga))
  ex.io.rs1_val := idex.io.rs1_val_out
  ex.io.imm     := idex.io.imm_out
  ex.io.rd      := idex.io.rd_out
  ex.io.opcode  := idex.io.opcode_out
  ex.io.funct3  := idex.io.funct3_out

  //---------------------
  // Debug outputs
  //---------------------
  io.dbg_imm     := idex.io.imm_out
  io.dbg_rs1_val := idex.io.rs1_val_out
  io.dbg_alu_out := ex.io.alu_out
}