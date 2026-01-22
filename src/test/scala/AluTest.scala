import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class AluTest extends AnyFlatSpec with ChiselScalatestTester {
  "ALU" should "execute all ALU instructions correctly" in {
    test(new Top("assembly/alu_test.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(50)

      def reg(r: Int) = dut.io.regFile(r).peekInt()

      // Example expectations (match your assembly)
      println("1. ADDI")
      assert(reg(1) == 10)        // ADDI
      println("2. ADDI")
      assert(reg(2) == 20)
      println("3. ADD")
      assert(reg(3) == 30)        // ADD
      println("4. SUB")
      assert(reg(4) == 20)        // SUB
      println("5. SLL")
      assert(reg(5) == 40)        // SLL
      println("6. SLT")
      assert(reg(6) == 1)         // SLT
      println("7. SLTU")
      assert(reg(7) == 0)         // SLTU
      println("8. OR")
      assert(reg(8) == 30)        // OR
      println("9. AND")
      assert(reg(9) == 20)        // AND
      println("10. XOR")
      assert(reg(10) == 10)       // XOR

      println("ALU test passed")
    }
  }
}