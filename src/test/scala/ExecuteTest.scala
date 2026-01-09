import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteTest extends AnyFlatSpec with ChiselScalatestTester {
  "Execute stage" should "execute addi" in {
    test(new Stage3_EX(fpga = false)) { dut =>
      dut.io.rs1_val.poke(10.U)
      dut.io.imm.poke(5.U)
      dut.io.rd.poke(1.U)
      dut.io.opcode.poke("b0010011".U)
      dut.io.funct3.poke("b000".U)

      dut.io.alu_out.expect(15.U)
      dut.io.we_out.expect(true.B)
      dut.io.rd_out.expect(1.U)
    }
  }
}
