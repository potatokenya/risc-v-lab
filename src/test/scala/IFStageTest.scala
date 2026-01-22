import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class IFStageTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipeline" should "fetch instructions correctly" in {
    test(new Top).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Step 1 cycle at a time and check PC increments by 4
      for(i <- 0 until 10) {
        dut.clock.step(1)
        println(s"Cycle $i: PC = ${dut.pc.peek()}")
        dut.pc.expect((i*4).U)
      }
    }
  }
}

