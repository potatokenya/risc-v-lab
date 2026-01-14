import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class DecodeTest extends AnyFlatSpec with ChiselScalatestTester {
  "Decode stage" should "extract addi fields" in {
    test(new Pipeline) { dut =>
      // wait one cycle so instruction is fetched
      dut.clock.step()

      dut.io.dbg.opcode.expect("b0010011".U)
      dut.io.dbg.rd.expect(1.U)
      dut.io.dbg.rs1.expect(0.U)
      dut.io.dbg.funct3.expect(0.U)
    }
  }
}
