import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class BranchTest extends AnyFlatSpec with ChiselScalatestTester {
  "Branches" should "work for all branch types" in {
    test(new Top("assembly/branch_test.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(80)

      def reg(r: Int) = dut.io.regFile(r).peekInt()

      // Program sets x10 to magic values if branch worked
      println("1. BEQ taken")
      assert(reg(10) == 1)   // BEQ taken
      println("2. BNE taken")
      assert(reg(11) == 1)   // BNE taken
      println("3. BLT taken")
      assert(reg(12) == 1)   // BLT taken
      println("4. BGE taken")
      assert(reg(13) == 1)   // BGE taken
      println("5. BLTU taken")
      assert(reg(14) == 1)   // BLTU taken
      println("6. BGEU taken")
      assert(reg(15) == 1)   // BGEU taken

      println("Branch test passed")
    }
  }
}