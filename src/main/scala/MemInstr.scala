import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFile
import chisel3.util.experimental.loadMemoryFromFileInline

class InstrMem(file: String) extends Module {
  val io = IO(new Bundle {
    val addr  = Input(UInt(32.W))
    val instr = Output(UInt(32.W))
  })

  val mem = SyncReadMem(1024, UInt(32.W))
  loadMemoryFromFileInline(mem, file)

  val rd = mem.read(io.addr(31,2), true.B)
  io.instr := RegNext(rd, 0.U)
}

class DataMem extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val memRead = Input(Bool())
    val memWrite = Input(Bool())
    val readData = Output(UInt(32.W))
  })

  val dmem = SyncReadMem(1024, UInt(32.W))

  when(io.memWrite) {
    dmem.write(io.addr(31,2), io.writeData)
  }

  //io.readData := Mux(io.memRead, dmem(io.addr >> 2), 0.U)
  val readData = dmem.read(io.addr(31,2), io.memRead)
  val readReg = RegNext(readData, 0.U)
  io.readData := readReg
}
