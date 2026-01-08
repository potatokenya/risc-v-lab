import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// IFStage Test Addi
class test_IFID extends AnyFlatSpec with ChiselScalatestTester {
  "IF/ID Test" should "verify register" in {
    test(new IF_IDRegister) { dut =>
      dut.io.pc_in.poke(4.U)
      dut.io.instr_in.poke("h12300093".U)
      dut.clock.step()
      dut.io.pc_out.expect(4.U)
      dut.io.instr_out.expect("h12300093".U)
    }
  }
}
