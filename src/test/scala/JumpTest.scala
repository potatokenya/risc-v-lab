import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class JumpTest extends AnyFlatSpec with ChiselScalatestTester {
  "Jumps" should "execute JAL and JALR correctly" in {
    test(new Top("assembly/jump_test.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(80)

      def reg(r: Int) = dut.io.regFile(r).peekInt()

      // x1 = return address from JAL
      assert(reg(1) != 0)

      // x5 set by code after JAL
      assert(reg(5) == 123)

      // x6 set after JALR
      assert(reg(6) == 77)

      println("Jump test passed")
    }
  }
}