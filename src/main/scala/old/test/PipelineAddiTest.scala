package old.test

import chisel3._
import chiseltest._
import old.Pipeline
import org.scalatest.flatspec.AnyFlatSpec

class PipelineAddiTest extends AnyFlatSpec with ChiselScalatestTester {
  "Pipeline" should "execute addi and add correctly" in {
    test(new Pipeline).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // Let the pipeline run
      dut.clock.step(8)

      // Check x1
      dut.io.dbg.rf_addr.poke(1.U)
      dut.clock.step(1)
      println("1. Instruction Check")
      dut.io.dbg.rf_data.expect("h111".U)

      // Check x2
      dut.io.dbg.rf_addr.poke(2.U)
      dut.clock.step(1)
      println("2. Instruction Check")
      dut.io.dbg.rf_data.expect("h222".U)

      dut.clock.step(5) // ensure memory operations have completed

      // Check x3
      dut.io.dbg.rf_addr.poke(3.U)
      dut.clock.step(1)
      println("3. Instruction Check")
      dut.io.dbg.rf_data.expect("h333".U)
    }
  }
}