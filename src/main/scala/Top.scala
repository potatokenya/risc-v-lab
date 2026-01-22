import chisel3._
import chisel3.util._
import lib.peripherals.{MemoryMappedUart, StringStreamer}
import lib.peripherals.MemoryMappedUart.UartPins


class Top(file: String = "assembly/program.hex") extends Module {
  val io = IO(new Bundle {
    val regFile = Output(Vec(32, UInt(32.W)))
    val stop = Output(Bool()) // signal program completion
    val led = Output(UInt(8.W))

    // Debug taps for tests
    val dbg = new Bundle {

      // Register file debug access
      val rf_addr = Input(UInt(5.W))
      val rf_data = Output(UInt(32.W))
    }
  })

  // === Memories ===
  val imem = Module(new InstrMem(file))
  val dmem = Module(new DataMem())

  // === Program counter ===
  val pc = RegInit(0.U(32.W))
  val pcNext = Wire(UInt(32.W))

  // === Pipeline registers ===
  val ifid = RegInit(0.U.asTypeOf(new IFID))
  val idex = RegInit(0.U.asTypeOf(new IDEX))
  val exmem = RegInit(0.U.asTypeOf(new EXMEM))
  val memwb = RegInit(0.U.asTypeOf(new MEMWB))

  // === Register file ===
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  // ------------------------
  // Debug tap
  // ------------------------
  io.dbg.rf_data := regs(io.dbg.rf_addr)

  // === hazard detection ===
  val hazard = Module(new HazardUnit)
  hazard.io.id_ex_memRead := idex.memRead
  hazard.io.id_ex_rd      := idex.rd
  hazard.io.if_id_rs1     := ifid.instr(19,15)
  hazard.io.if_id_rs2     := ifid.instr(24,20)

  val stall = hazard.io.stall

  // === Fetch Stage ===
  imem.io.addr := pc
  val instr = imem.io.instr

  // Simple PC logic: advance 4 unless branch
  pcNext := Mux(exmem.branchTaken, exmem.branchTarget, pc + 4.U)

  when(exmem.branchTaken) {
    ifid := 0.U.asTypeOf(new IFID)
  }.elsewhen(!stall) {
    pc := pcNext
    ifid.pc := pc
    ifid.instr := instr
  }

  // === ID Stage ===
  val opcode = ifid.instr(6,0)
  val rd     = ifid.instr(11,7)
  val rs1    = ifid.instr(19,15)
  val rs2    = ifid.instr(24,20)
  val funct3 = ifid.instr(14,12)
  val funct7 = ifid.instr(31,25)

  val imm_i = Cat(Fill(20, ifid.instr(31)), ifid.instr(31,20))
  val imm_s = Cat(Fill(20, ifid.instr(31)), ifid.instr(31,25), ifid.instr(11,7))

  val regData1 = regs(rs1)
  val regData2 = regs(rs2)

  val ctrl = Module(new Control)
  ctrl.io.opcode := opcode

  when(exmem.branchTaken) {
    idex := 0.U.asTypeOf(new IDEX)
  }

  when(exmem.branchTaken) {
    idex := 0.U.asTypeOf(new IDEX)
  }.elsewhen(stall) {
    idex := 0.U.asTypeOf(new IDEX)
  }.otherwise {
    idex.pc := ifid.pc
    idex.rs1 := rs1
    idex.rs2 := rs2
    idex.rd  := rd
    idex.regData1 := regData1
    idex.regData2 := regData2
    idex.imm := Mux(opcode === "b0100011".U, imm_s, imm_i)
    idex.opcode := opcode
    idex.funct3 := funct3
    idex.funct7 := funct7

    // Control signals (simple demo: addi/add/lw/sw)
    idex.aluSrc   := ctrl.io.aluSrc
    idex.memRead  := ctrl.io.memRead
    idex.memWrite := ctrl.io.memWrite
    idex.regWrite := ctrl.io.regWrite
    idex.memToReg := ctrl.io.memToReg
    idex.branch   := ctrl.io.branch
  }
  /*
  idex.aluSrc := true.B  // addi/lw uses imm
  idex.regWrite := true.B
  idex.memRead := false.B
  idex.memWrite := false.B
  idex.memToReg := false.B
  idex.branch := false.B */

  // === forwarding ===
  val forward = Module(new ForwardUnit)
  forward.io.ex_rs1 := idex.rs1
  forward.io.ex_rs2 := idex.rs2
  forward.io.mem_rd := exmem.rd
  forward.io.wb_rd  := memwb.rd
  forward.io.mem_regWrite := exmem.regWrite
  forward.io.wb_regWrite  := memwb.regWrite

  val forwardA = Wire(UInt(32.W))
  val forwardB = Wire(UInt(32.W))

  forwardA := MuxLookup(forward.io.forwardA, idex.regData1, Seq(
    1.U -> Mux(memwb.memToReg, memwb.memData, memwb.aluResult),
    2.U -> exmem.aluResult
  ))

  forwardB := MuxLookup(forward.io.forwardB, idex.regData2, Seq(
    1.U -> Mux(memwb.memToReg, memwb.memData, memwb.aluResult),
    2.U -> exmem.aluResult
  ))

  // === EX Stage ===
  val alu = Module(new ALU)
  val aluIn2 = Mux(idex.aluSrc, idex.imm, forwardB)
  alu.io.in1 := forwardA
  alu.io.in2 := aluIn2
  //val aluIn2 = Mux(idex.aluSrc, idex.imm, idex.regData2)
  //alu.io.in1 := idex.regData1
  //alu.io.in2 := aluIn2

  val aluCtrlUnit = Module(new ALUControl)
  aluCtrlUnit.io.opcode := idex.opcode
  aluCtrlUnit.io.funct3 := idex.funct3
  aluCtrlUnit.io.funct7 := idex.funct7

  alu.io.aluCtrl := aluCtrlUnit.io.aluCtrl
  //alu.io.aluCtrl := "b0000".U // ADD demo

  // EX stage branch comparator inputs (must use forwarded values!)
  val branchOpA = forwardA
  val branchOpB = forwardB

  val beq  = branchOpA === branchOpB
  val bne  = branchOpA =/= branchOpB
  val blt  = branchOpA.asSInt < branchOpB.asSInt
  val bge  = branchOpA.asSInt >= branchOpB.asSInt
  val bltu = branchOpA < branchOpB
  val bgeu = branchOpA >= branchOpB

  val branchCond = Wire(Bool())
  branchCond := false.B

  switch(idex.funct3) {
    is("b000".U) { branchCond := beq  }  // BEQ
    is("b001".U) { branchCond := bne  }  // BNE
    is("b100".U) { branchCond := blt  }  // BLT
    is("b101".U) { branchCond := bge  }  // BGE
    is("b110".U) { branchCond := bltu }  // BLTU
    is("b111".U) { branchCond := bgeu }  // BGEU
  }

  //val branchTaken = idex.branch && alu.io.zero
  //val branchTarget = idex.pc + idex.imm
  val branchTaken = idex.branch && branchCond
  val branchTarget = idex.pc + idex.imm

  // EX/MEM
  exmem.aluResult := alu.io.out
  exmem.rd := idex.rd
  exmem.regData2 := idex.regData2
  exmem.regWrite := idex.regWrite
  exmem.memToReg := idex.memToReg
  exmem.memRead := idex.memRead
  exmem.memWrite := idex.memWrite
  exmem.branchTaken := branchTaken
  exmem.branchTarget := branchTarget

  // === MEM Stage ===
  dmem.io.addr := exmem.aluResult
  dmem.io.writeData := exmem.regData2
  dmem.io.memRead := exmem.memRead
  dmem.io.memWrite := exmem.memWrite

  memwb.aluResult := exmem.aluResult
  memwb.memData := dmem.io.readData
  memwb.rd := exmem.rd
  memwb.regWrite := exmem.regWrite
  memwb.memToReg := exmem.memToReg

  // === WB Stage ===
  when(memwb.regWrite && memwb.rd =/= 0.U) {
    regs(memwb.rd) := Mux(memwb.memToReg, memwb.memData, memwb.aluResult)
  }

  // === Debug outputs ===
  io.regFile := regs
  io.stop := false.B
  io.led := 0.U

}