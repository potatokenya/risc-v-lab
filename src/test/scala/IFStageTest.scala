import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// IFStage Test
class IFStageTest extends AnyFlatSpec with ChiselScalatestTester {
  "Stage1_IF Test" should "fetch instructions" in {

    val FPGA = false
    val PROGRAM: Seq[Int] = Seq(
      0x11100093, // addi x1, x0, 0x111
      0x22200113, // addi x2, x0, 0x222
      0x00000013, // nop
      0x00000013, // nop
      0x00000013, // nop
      0x002081b3, // add x3, x1, x2
      0x00000013  // nop
    )
    test(new Stage1_IF(PROGRAM, FPGA)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      for (i <- PROGRAM.indices) {
        dut.io.pc.expect((i * 4).U)
        dut.clock.step()
        dut.io.instr.expect(PROGRAM(i).U)
      }
    }
  }
}

