import chisel3._
import chiseltest._
import old.Pipeline
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {
  "Execute stage" should "execute addi" in {
    test(new Pipeline) { dut =>
      dut.clock.step(2)   // fetch + decode
      dut.clock.step(1)   // execute

      dut.io.dbg.alu_out.expect(0x111.U)
      dut.io.dbg.we.expect(true.B)
    }
  }
}
