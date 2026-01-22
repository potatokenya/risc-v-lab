import chisel3._
import chisel3.util._

import chisel3._
import chisel3.util._

class IFID extends Bundle {
  val pc = UInt(32.W)
  val instr = UInt(32.W)
}

class IDEX extends Bundle {
  val pc = UInt(32.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)
  val rd = UInt(5.W)
  val funct3 = UInt(3.W)
  val funct7 = UInt(7.W)
  val imm = UInt(32.W)
  val opcode = UInt(7.W)
  val regData1 = UInt(32.W)
  val regData2 = UInt(32.W)

  // Control signals
  val aluSrc = Bool()
  val memRead = Bool()
  val memWrite = Bool()
  val regWrite = Bool()
  val memToReg = Bool()
  val branch = Bool()
}

class EXMEM extends Bundle {
  val aluResult = UInt(32.W)
  val rd = UInt(5.W)
  val regData2 = UInt(32.W)
  val memRead = Bool()
  val memWrite = Bool()
  val regWrite = Bool()
  val memToReg = Bool()
  val branchTaken = Bool()
  val branchTarget = UInt(32.W)
}

class MEMWB extends Bundle {
  val aluResult = UInt(32.W)
  val memData = UInt(32.W)
  val rd = UInt(5.W)
  val regWrite = Bool()
  val memToReg = Bool()
}
