import chisel3._
import chiseltest._
import lib.ReadAssembly
import org.scalatest.flatspec.AnyFlatSpec

class PipelineTest_addi extends AnyFlatSpec with ChiselScalatestTester {
  "Pipeline Test" should "pass" in {
    val FPGA = false
    val MEM_SIZE = 1024
    val FREQ = 50000000
    val BAUD = 9600
    val PROGRAM: Seq[Int] = ReadAssembly.readBin("assembly/addi5.bin")
    test(new Top(PROGRAM, FPGA, MEM_SIZE, FREQ, BAUD)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      while(dut.io.pc.peekInt <= 108) {
        if(dut.io.pc.peekInt === 28) {
          println("First Instruction Check")
          dut.io.regs(1).expect(1.S)
        }
        if(dut.io.pc.peekInt === 48) {
          println("Second Instruction Check")
          dut.io.regs(1).expect(3.S)
        }
        if(dut.io.pc.peekInt === 68) {
          println("Third Instruction Check")
          dut.io.regs(1).expect(6.S)
        }
        if(dut.io.pc.peekInt === 88) {
          println("Fourth Instruction Check")
          dut.io.regs(1).expect(10.S)
        }
        if(dut.io.pc.peekInt === 108) {
          println("Fifth Instruction Check")
          dut.io.regs(1).expect(15.S)
        }
        dut.clock.step()
      }
    }
  }
}
