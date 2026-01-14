import chisel3._
import chiseltest._
import old.Stage1_IF
import org.scalatest.flatspec.AnyFlatSpec

// IFStage Test Addi
class Test_addi extends AnyFlatSpec with ChiselScalatestTester {
  "Addi Test" should "fetch instructions" in {
    val FPGA = false
    val PROGRAM: Seq[Int] = Seq(0x12300093)
    test(new Stage1_IF(PROGRAM, FPGA)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.clock.step(5)
    }
  }
}