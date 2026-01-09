import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopAddiTest extends AnyFlatSpec with ChiselScalatestTester {
  "Top" should "execute addi in pipeline" in {

    val program = Seq(
      0x00700093, // addi x1, x0, 7
      0x00508113  // addi x2, x1, 5 (will fail until WB + forwarding)
    )

    test(new Top(program, fpga = false))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

        // Just step and observe waveforms for now
        dut.clock.step(10)
      }
  }
}
