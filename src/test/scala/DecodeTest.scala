import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DecodeTest extends AnyFlatSpec with ChiselScalatestTester {
  "Decode stage" should "extract addi fields" in {
    test(new Stage2_ID(fpga = false)) { dut =>
      val addi = "h11100093".U // addi x1, x0, 0x111

      dut.io.instr.poke(addi)

      dut.io.opcode.expect("b0010011".U)
      dut.io.rd.expect(1.U)
      dut.io.rs1.expect(0.U)
      dut.io.funct3.expect(0.U)
    }
  }
}