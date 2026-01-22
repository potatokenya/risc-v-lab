import chisel3._
import chisel3.util._
import lib.peripherals.{MemoryMappedLeds, MemoryMappedUart, StringStreamer}
import lib.peripherals.MemoryMappedUart.UartPins

object Top extends App {
  emitVerilog(
    new Top("assembly/hello_world.hex"),
    Array("--target-dir", "generated")
  )
}

class Top(file: String) extends Module {
  val io = IO(new Bundle {
    val regFile = Output(Vec(32, UInt(32.W)))
    val stop = Output(Bool())
    val led  = Output(UInt(8.W))
    val dbg = new Bundle {
      val rf_addr = Input(UInt(5.W))
      val rf_data = Output(UInt(32.W))
    }
  })

  // =========================================================
  // Memories
  // =========================================================
  val imem = Module(new InstrMem(file))
  val dmem = Module(new DataMem)

  // =========================================================
  // PC
  // =========================================================
  val pc = RegInit(0.U(32.W))
  val pcNext = Wire(UInt(32.W))

  // =========================================================
  // Pipeline registers
  // =========================================================
  val ifid  = RegInit(0.U.asTypeOf(new IFID))
  val idex  = RegInit(0.U.asTypeOf(new IDEX))
  val exmem = RegInit(0.U.asTypeOf(new EXMEM))
  val memwb = RegInit(0.U.asTypeOf(new MEMWB))

  // =========================================================
  // Register file
  // =========================================================
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  io.dbg.rf_data := regs(io.dbg.rf_addr)
  io.regFile := regs

  // ============================
  // Hazard Detection
  // ============================
  val hazard = Module(new HazardUnit)
  hazard.io.id_ex_memRead := idex.memRead
  hazard.io.id_ex_rd      := idex.rd
  hazard.io.if_id_rs1     := ifid.instr(19,15)
  hazard.io.if_id_rs2     := ifid.instr(24,20)
  val stall = hazard.io.stall

  // =========================================================
  // IF stage
  // =========================================================
  imem.io.addr := pc

  pcNext := pc + 4.U
  when(exmem.branchTaken) { pcNext := exmem.branchTarget }
  when(exmem.jumpTaken)   { pcNext := exmem.jumpTarget   }

  when(exmem.branchTaken || exmem.jumpTaken) {
    ifid := 0.U.asTypeOf(new IFID)
    pc := pcNext
  }.elsewhen(!stall) {
    pc := pcNext
    ifid.pc := pc
    ifid.instr := imem.io.instr
  }

  // =========================================================
  // ID stage
  // =========================================================
  val opcode = ifid.instr(6,0)
  val rs1    = ifid.instr(19,15)
  val rs2    = ifid.instr(24,20)
  val rd     = ifid.instr(11,7)
  val funct3 = ifid.instr(14,12)
  val funct7 = ifid.instr(31,25)

  val regData1 = regs(rs1)
  val regData2 = regs(rs2)

  val control = Module(new Control)
  control.io.opcode := opcode

  val instrType = Wire(UInt(3.W))
  instrType := MuxLookup(opcode, 0.U, Seq(
    "b0010011".U -> 0.U, // I
    "b0000011".U -> 0.U,
    "b1100111".U -> 0.U,
    "b0100011".U -> 1.U, // S
    "b1100011".U -> 2.U, // B
    "b1101111".U -> 4.U  // J
  ))

  val imm = ImmGen.getImm(ifid.instr, instrType)

  idex.pc        := ifid.pc
  idex.rs1       := rs1
  idex.rs2       := rs2
  idex.rd        := rd
  idex.regData1 := regs(rs1)
  idex.regData2 := regs(rs2)
  idex.opcode   := opcode
  idex.funct3   := funct3
  idex.funct7   := funct7
  idex.imm      := imm

  idex.regWrite := control.io.regWrite
  idex.memRead  := control.io.memRead
  idex.memWrite := control.io.memWrite
  idex.memToReg := control.io.memToReg
  idex.aluSrc   := control.io.aluSrc
  idex.branch   := control.io.branch
  idex.jump     := control.io.jump
  idex.jumpReg  := control.io.jumpReg
  idex.wbSelPC4 := control.io.wbSelPC4

  // ============================
  // Forwarding
  // ============================
  val forward = Module(new ForwardUnit)
  forward.io.ex_rs1 := idex.rs1
  forward.io.ex_rs2 := idex.rs2
  forward.io.mem_rd := exmem.rd
  forward.io.wb_rd  := memwb.rd
  forward.io.mem_regWrite := exmem.regWrite
  forward.io.wb_regWrite  := memwb.regWrite

  val forwardA = MuxLookup(forward.io.forwardA, idex.regData1, Seq(
    1.U -> Mux(memwb.memToReg, memwb.memData, memwb.aluResult),
    2.U -> exmem.aluResult
  ))

  val forwardB = MuxLookup(forward.io.forwardB, idex.regData2, Seq(
    1.U -> Mux(memwb.memToReg, memwb.memData, memwb.aluResult),
    2.U -> exmem.aluResult
  ))

  // =========================================================
  // Execute stage
  // =========================================================
  val alu = Module(new ALU)
  alu.io.in1 := forwardA
  alu.io.in2 := Mux(idex.aluSrc, idex.imm.asUInt, forwardB)

  val aluCtrl = Module(new ALUControl)
  aluCtrl.io.opcode := idex.opcode
  aluCtrl.io.funct3 := idex.funct3
  aluCtrl.io.funct7 := idex.funct7

  alu.io.aluCtrl := aluCtrl.io.aluCtrl
  //alu.io.in1 := idex.regData1
  //alu.io.in2 := Mux(idex.aluSrc, idex.imm.asUInt, idex.regData2)

  //val branchCond   = BranchComp.compare(idex.funct3, idex.regData1, idex.regData2)
  val branchCond = BranchComp.compare(idex.funct3, forwardA, forwardB)
  val branchTaken  = idex.branch && branchCond
  val branchTarget = (idex.pc.asSInt + idex.imm).asUInt

  val jalTarget  = idex.pc + idex.imm.asUInt
  //val jalrTarget = (idex.regData1 + idex.imm.asUInt) & "hFFFFFFFE".U
  val jalrTarget = (forwardA + idex.imm.asUInt) & "hFFFFFFFE".U
  val jumpTaken  = idex.jump || idex.jumpReg
  val jumpTarget = Mux(idex.jumpReg, jalrTarget, jalTarget)


  exmem.aluResult   := alu.io.out
  exmem.rd          := idex.rd
  //exmem.regData2    := idex.regData2
  exmem.regData2 := forwardB
  exmem.regWrite   := idex.regWrite
  exmem.memRead    := idex.memRead
  exmem.memWrite   := idex.memWrite
  exmem.memToReg   := idex.memToReg
  exmem.branchTaken:= branchTaken
  exmem.branchTarget:= branchTarget
  exmem.jumpTaken  := jumpTaken
  exmem.jumpTarget := jumpTarget
  exmem.pcPlus4    := idex.pc + 4.U
  exmem.wbSelPC4   := idex.wbSelPC4

  // =========================================================
  // Memory stage
  // =========================================================
  dmem.io.addr      := exmem.aluResult
  dmem.io.writeData:= exmem.regData2
  dmem.io.memRead  := exmem.memRead
  dmem.io.memWrite := exmem.memWrite

  memwb.aluResult := exmem.aluResult
  memwb.memData  := dmem.io.readData
  memwb.rd       := exmem.rd
  memwb.regWrite := exmem.regWrite
  memwb.memToReg := exmem.memToReg
  memwb.pcPlus4  := exmem.pcPlus4
  memwb.wbSelPC4 := exmem.wbSelPC4

  // =========================================================
  // WB stage
  // =========================================================
  val wbData = Mux(memwb.wbSelPC4, memwb.pcPlus4, Mux(memwb.memToReg, memwb.memData, memwb.aluResult)
  )

  when(memwb.regWrite && memwb.rd =/= 0.U) {
    regs(memwb.rd) := wbData
  }

  io.regFile := regs
  io.stop := false.B
  io.led := 0.U
}