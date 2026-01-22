import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class PipelineSimTest extends AnyFlatSpec with ChiselScalatestTester {
  "PipelineCPU" should "run program from hex file and print registers" in {
    test(new Top("assembly/program.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      val maxCycles = 100
      for (i <- 0 until maxCycles) {
        dut.clock.step(1)
        println(s"Cycle $i:")
        for (r <- 0 until 32) {
          val v = dut.io.regFile(r).peekInt()
          print(f"x$r%02d = 0x$v%08x  ")
          if ((r+1) % 4 == 0) println()
        }
        println("-------------------------------------------------")
      }
    }
  }
}