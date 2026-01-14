import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class TopAddiTest extends AnyFlatSpec with ChiselScalatestTester {
  "Top" should "execute addi in pipeline" in {

    val program = Seq(
      0x11100093, // addi x1, x0, 0x111
      0x22200113, // addi x2, x0, 0x222
      0x00000013, // nop
      0x00000013, // nop
      0x00000013, // nop
      0x002081b3, // add x3, x1, x2
      0x00000013  // nop
    )
    test(new Top(program, fpga = false)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // step and observe waveforms for now
        dut.clock.step(10)

      }
  }
}
