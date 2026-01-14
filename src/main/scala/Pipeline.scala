import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile

class Pipeline extends Module {
  val io = IO(new Bundle {
    val pc    = Output(UInt(32.W))
    val instr = Output(UInt(32.W))
    val alu   = Output(UInt(32.W))

    // Debug taps for tests
    val dbg = new Bundle {
      val opcode = Output(UInt(7.W))
      val rd     = Output(UInt(5.W))
      val rs1    = Output(UInt(5.W))
      val funct3 = Output(UInt(3.W))
      val imm    = Output(UInt(32.W))

      val idex_rs1val = Output(UInt(32.W))
      val alu_out     = Output(UInt(32.W))
      val we          = Output(Bool())

      // Register file debug access
      val rf_addr = Input(UInt(5.W))
      val rf_data = Output(UInt(32.W))
    }
  })

  // ---------------------------
  // IF STAGE
  // ---------------------------
  val pcReg = RegInit(0.U(32.W))
  pcReg := pcReg + 4.U
  io.pc := pcReg

  val imem = SyncReadMem(1024, UInt(32.W))
  loadMemoryFromFile(imem, "src/main/resources/program_add.hex")
  val instr = imem.read(pcReg(31,2))

  // Fetch
  io.instr := instr

  // ---------------------------
  // IF/ID PIPELINE REGISTER
  // ---------------------------
  val ifId = new Bundle {
    val pc    = UInt(32.W)
    val instr = UInt(32.W)
  }
  val ifIdReg = RegInit(0.U.asTypeOf(ifId))

  ifIdReg.pc    := pcReg
  ifIdReg.instr := instr

  // ---------------------------
  // ID STAGE
  // ---------------------------
  val opcode = ifIdReg.instr(6,0)
  val rd     = ifIdReg.instr(11,7)
  val rs1    = ifIdReg.instr(19,15)
  val rs2    = ifIdReg.instr(24,20)
  val funct3 = ifIdReg.instr(14,12)
  val funct7 = ifIdReg.instr(31,25)


  val imm_i = Cat(Fill(20, ifIdReg.instr(31)), ifIdReg.instr(31,20))

  // Register File
  val regFile = Module(new RegisterFile)
  regFile.io.rs1 := rs1
  regFile.io.rs2 := rs2
  regFile.io.rd  := 0.U
  regFile.io.wd  := 0.U
  regFile.io.we  := false.B

  // Debug pass through
  regFile.io.dbg_addr := io.dbg.rf_addr
  io.dbg.rf_data      := regFile.io.dbg_data

  // ---------------------------
  // ID/EX PIPELINE REGISTER
  // ---------------------------
  val idEx = new Bundle {
    val rs1Val = UInt(32.W)
    val rs2Val = UInt(32.W)
    val imm    = UInt(32.W)
    val rd     = UInt(5.W)
    val opcode = UInt(7.W)
    val funct3 = UInt(3.W)
    val funct7 = UInt(7.W)
  }
  val idExReg = RegInit(0.U.asTypeOf(idEx))

  idExReg.rs1Val := regFile.io.rd1
  idExReg.rs2Val := regFile.io.rd2
  idExReg.imm    := imm_i
  idExReg.rd     := rd
  idExReg.opcode := opcode
  idExReg.funct3 := funct3
  idExReg.funct7 := funct7

  // ---------------------------
  // EX STAGE
  // ---------------------------
  val aluOut = WireDefault(0.U(32.W))
  val weOut  = WireDefault(false.B)

  // ADDI
  when(idExReg.opcode === "b0010011".U && idExReg.funct3 === "b000".U) {
    aluOut := idExReg.rs1Val + idExReg.imm
    weOut  := true.B
  // ADD (R-type)
  }.elsewhen(idExReg.opcode === "b0110011".U && idExReg.funct3 === "b000".U) {
    aluOut := idExReg.rs1Val + idExReg.rs2Val
    weOut  := true.B
  }

  io.alu := aluOut

  // ---------------------------
  // EX/MEM PIPELINE REGISTER
  // ---------------------------
  val exMem = new Bundle {
    val aluOut = UInt(32.W)
    val rd     = UInt(5.W)
    val we     = Bool()
  }
  val exMemReg = RegInit(0.U.asTypeOf(exMem))

  exMemReg.aluOut := aluOut
  exMemReg.rd     := idExReg.rd
  exMemReg.we     := weOut

  // ---------------------------
  // MEM STAGE (temporary, just forwards ALU results)
  // ---------------------------
  val memWbData = exMemReg.aluOut
  val memWbRd   = exMemReg.rd
  val memWbWe   = exMemReg.we

  // ---------------------------
  // MEM/WB PIPELINE REGISTER
  // ---------------------------
  val memWb = new Bundle {
    val wbData = UInt(32.W)
    val rd     = UInt(5.W)
    val we     = Bool()
  }
  val memWbReg = RegInit(0.U.asTypeOf(memWb))

  memWbReg.wbData := exMemReg.aluOut
  memWbReg.rd     := exMemReg.rd
  memWbReg.we     := exMemReg.we

  // ---------------------------
  // WRITE BACK
  // ---------------------------
  // Temporary direct write back:
  //regFile.io.rd := idExReg.rd
  //regFile.io.wd := aluOut
  //regFile.io.we := weOut
  // ---------------------------
  regFile.io.rd := memWbReg.rd
  regFile.io.wd := memWbReg.wbData
  regFile.io.we := memWbReg.we

  // ---------------------------
  // Debug connections
  // ---------------------------
  io.dbg.opcode := opcode
  io.dbg.rd     := rd
  io.dbg.rs1    := rs1
  io.dbg.funct3 := funct3
  io.dbg.imm    := imm_i

  io.dbg.idex_rs1val := idExReg.rs1Val
  io.dbg.alu_out := exMemReg.aluOut
  io.dbg.we      := exMemReg.we
}
