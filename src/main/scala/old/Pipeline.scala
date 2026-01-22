package old

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

  // Hazard signal (declare early)
  val loadUseHazard = Wire(Bool())
  loadUseHazard := false.B   // default

  // ---------------------------
  // IF STAGE
  // ---------------------------
  val pcReg = RegInit(0.U(32.W))
  //pcReg := pcReg + 4.U

  val pcValid = RegInit(false.B)

  when(!pcValid) {
    pcValid := true.B
    pcReg := pcReg // hold
  }.elsewhen(!loadUseHazard) {
    pcReg := pcReg + 4.U
  }
  io.pc := pcReg

  val imem = SyncReadMem(1024, UInt(32.W))
  //loadMemoryFromFile(imem, "src/main/resources/program_add.hex")
  loadMemoryFromFile(imem, "assembly/program.hex")
  val instr = imem.read(pcReg(31,2))
  val instrReg = RegNext(instr)

  // Fetch
  io.instr := instrReg

  // ---------------------------
  // IF/ID PIPELINE REGISTER
  // ---------------------------
  val ifId = new Bundle {
    val pc    = UInt(32.W)
    val instr = UInt(32.W)
  }
  val ifIdReg = RegInit(0.U.asTypeOf(ifId))

  when(!loadUseHazard) {
    ifIdReg.pc    := pcReg
    ifIdReg.instr := instrReg
  }.otherwise {
    ifIdReg := ifIdReg   // freeze
  }

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
  val imm_s = Cat(Fill(20, ifIdReg.instr(31)), ifIdReg.instr(31,25), ifIdReg.instr(11,7))

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
    val rs1    = UInt(5.W)
    val rs2    = UInt(5.W)
    val imm    = UInt(32.W)
    val rd     = UInt(5.W)
    val opcode = UInt(7.W)
    val funct3 = UInt(3.W)
    val funct7 = UInt(7.W)
  }

  val idExReg = RegInit(0.U.asTypeOf(idEx))

  when(loadUseHazard) {
    idExReg := 0.U.asTypeOf(idEx)   // bubble
  }.otherwise {
    idExReg.rs1Val := regFile.io.rd1
    idExReg.rs2Val := regFile.io.rd2
    idExReg.rs1    := rs1
    idExReg.rs2    := rs2

    idExReg.imm    := Mux(opcode === "b0100011".U, imm_s, imm_i)
    idExReg.rd     := rd
    idExReg.opcode := opcode
    idExReg.funct3 := funct3
    idExReg.funct7 := funct7
  }

  // ---------------------------
  // EX STAGE
  // ---------------------------
  val aluOut = WireDefault(0.U(32.W))
  val weOut  = WireDefault(false.B)

  // control signals for MEM stage
  val isLoad  = WireDefault(false.B)
  val isStore = WireDefault(false.B)

  // Default operands
  val exRs1 = Wire(UInt(32.W))
  val exRs2 = Wire(UInt(32.W))

  exRs1 := idExReg.rs1Val
  exRs2 := idExReg.rs2Val


  // ADDI
  when(idExReg.opcode === "b0010011".U && idExReg.funct3 === "b000".U) {
    // ADDI
    aluOut := exRs1 + idExReg.imm
    weOut  := true.B

  }.elsewhen(idExReg.opcode === "b0110011".U) {
    // R-type
    weOut := true.B
    switch(idExReg.funct3) {
      is("b000".U) {
        when(idExReg.funct7 === "b0000000".U) { aluOut := exRs1 + exRs2 } // ADD
          .elsewhen(idExReg.funct7 === "b0100000".U) { aluOut := exRs1 - exRs2 } // SUB
      }
      is("b111".U) { aluOut := exRs1 & exRs2 } // AND
      is("b110".U) { aluOut := exRs1 | exRs2 } // OR
      is("b100".U) { aluOut := exRs1 ^ exRs2 } // XOR
    }

  }.elsewhen(idExReg.opcode === "b0000011".U) {
    // LOAD
    aluOut  := exRs1 + idExReg.imm   // effective address
    weOut   := true.B                         // write back after MEM
    isLoad  := true.B

  }.elsewhen(idExReg.opcode === "b0100011".U) {
    // STORE
    aluOut   := exRs1 + idExReg.imm  // effective address
    weOut    := false.B                       // store does NOT write RF
    isStore  := true.B
  }

  io.alu := aluOut

  // ---------------------------
  // EX/MEM PIPELINE REGISTER
  // ---------------------------
  val exMem = new Bundle {
    val aluOut = UInt(32.W)
    val rd     = UInt(5.W)
    val we     = Bool()
    val rs2Val  = UInt(32.W)   // store data
    val isLoad  = Bool()
    val isStore = Bool()
  }
  val exMemReg = RegInit(0.U.asTypeOf(exMem))

  exMemReg.aluOut := aluOut
  exMemReg.rd     := idExReg.rd
  exMemReg.we     := weOut
  exMemReg.rs2Val  := exRs2
  exMemReg.isLoad  := isLoad
  exMemReg.isStore := isStore

  // EX/MEM forwarding
  when(exMemReg.we && (exMemReg.rd =/= 0.U) && (exMemReg.rd === idExReg.rs1)) {
    exRs1 := exMemReg.aluOut
  }
  when(exMemReg.we && (exMemReg.rd =/= 0.U) && (exMemReg.rd === idExReg.rs2)) {
    exRs2 := exMemReg.aluOut
  }

  // Detect the load-use hazard
  val idRs1 = ifIdReg.instr(19,15)
  val idRs2 = ifIdReg.instr(24,20)

  loadUseHazard := exMemReg.isLoad && (exMemReg.rd =/= 0.U) && ((exMemReg.rd === idRs1) || (exMemReg.rd === idRs2))


  // ---------------------------
  // MEM STAGE
  // ---------------------------
  val memStage = new Bundle {
    val rd     = UInt(5.W)
    val we     = Bool()
    val isLoad = Bool()
    val aluOut = UInt(32.W)
  }
  val memStageReg = RegInit(0.U.asTypeOf(memStage))
  val dmem = SyncReadMem(1024, UInt(32.W))

  // capture control for memory
  memStageReg.rd     := exMemReg.rd
  memStageReg.we     := exMemReg.we
  memStageReg.isLoad := exMemReg.isLoad
  memStageReg.aluOut := exMemReg.aluOut


  // Issue read: data arrives next cycle
  val memOut = dmem.read(exMemReg.aluOut(31,2))
  // register the memory output
  val memOutReg = RegNext(memOut)

  // Store
  when (exMemReg.isStore) {
    dmem.write(exMemReg.aluOut(31,2), exMemReg.rs2Val)
    // print to verify the store really happens
    printf(p"STORE addr=${exMemReg.aluOut} STORE data=${exMemReg.rs2Val}\n")
  }

  // ---------------------------
  // MEM/WB PIPELINE REGISTER
  // ---------------------------
  val memWb = new Bundle {
    //val wbData = UInt(32.W)
    val rd     = UInt(5.W)
    val we     = Bool()

    val isLoad = Bool()
    val aluOut = UInt(32.W)
    val memOut = UInt(32.W)

  }

  val memWbReg = RegInit(0.U.asTypeOf(memWb))

  // Register everything for WB
  memWbReg.rd     := exMemReg.rd
  memWbReg.we     := exMemReg.we
  memWbReg.isLoad := exMemReg.isLoad
  memWbReg.aluOut := exMemReg.aluOut
  memWbReg.memOut := RegNext(memOutReg)

  val wbData = Mux(memWbReg.isLoad, memWbReg.memOut, memWbReg.aluOut)
  printf(p"isLoad=${isLoad} exMemReg.isLoad=${exMemReg.isLoad} memWbReg.isLoad=${memWbReg.isLoad} \n")

  // MEM/WB forwarding
  when(memWbReg.we && (memWbReg.rd =/= 0.U) &&
    !(exMemReg.we && (exMemReg.rd === idExReg.rs1)) &&
    (memWbReg.rd === idExReg.rs1)) {
    exRs1 := wbData
  }

  when(memWbReg.we && (memWbReg.rd =/= 0.U) &&
    !(exMemReg.we && (exMemReg.rd === idExReg.rs2)) &&
    (memWbReg.rd === idExReg.rs2)) {
    exRs2 := wbData
  }

  // ---------------------------
  // WRITE BACK
  // ---------------------------

  regFile.io.rd := memWbReg.rd
  regFile.io.wd := wbData
  regFile.io.we := memWbReg.we && (memWbReg.rd =/= 0.U)


  // ---------------------------
  // Debug connections
  // ---------------------------
  io.dbg.opcode := opcode
  io.dbg.rd     := rd
  io.dbg.rs1    := rs1
  io.dbg.funct3 := funct3
  io.dbg.imm := idExReg.imm

  io.dbg.idex_rs1val := exRs1
  io.dbg.alu_out := exMemReg.aluOut
  io.dbg.we      := exMemReg.we

  printf(p"wbData: memWbReg.memOut=${memWbReg.memOut} memWbReg.aluOut=${memWbReg.aluOut}\n")
  printf(p"exmem rd=${exMemReg.rd}  memWB rd=${memWbReg.rd} data=${wbData} we=${memWbReg.we}\n")
  printf(p"rf_addr=${io.dbg.rf_addr} rf_data=${io.dbg.rf_data}\n")
}