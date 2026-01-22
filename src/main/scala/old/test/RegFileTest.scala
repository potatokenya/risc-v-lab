package old.test

import chisel3._
import chiseltest._
import old.RegisterFile
import org.scalatest.flatspec.AnyFlatSpec

class RegFileTest extends AnyFlatSpec with ChiselScalatestTester {
  "RegisterFile" should "write and read registers" in {
    test(new RegisterFile) { dut =>
      dut.io.we.poke(true.B)
      dut.io.rd.poke(1.U)
      dut.io.wd.poke(42.U)
      dut.clock.step()

      dut.io.rs1.poke(1.U)
      dut.io.rd1.expect(42.U)
    }
  }
}