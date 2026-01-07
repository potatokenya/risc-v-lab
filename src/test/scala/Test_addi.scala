import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// IFStageTest
class Test_addi extends AnyFlatSpec with ChiselScalatestTester {
  "Stage1_IF Test" should "fetch instructions" in {
    test(new Stage1_IF())
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.clock.step(10)
      }
  }
}