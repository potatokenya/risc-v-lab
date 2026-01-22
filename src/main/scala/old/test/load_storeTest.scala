package old.test

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
/*
class load_storeTest extends AnyFlatSpec with ChiselScalatestTester {

  "Pipeline" should "execute SW then LW correctly" in {

    test(new Top("assembly/program.hex")).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // Step the clock enough cycles to fetch and write-back the first few instructions
      // 5-stage pipeline: IF -> ID -> EX -> MEM -> WB
      dut.clock.step(8)  // let the first few instructions flow through the pipeline

      // -----------------------
      // Check x1 (addi x1, x0, 0x111)
      // -----------------------
      dut.io.dbg.rf_addr.poke(1.U) // select x1
      dut.clock.step(1)            // allow regFile output to update
      println("\n 1. Checking x1 after ADDI \n")
      dut.io.dbg.rf_data.expect("h111".U)

      // -----------------------
      // Check x2 (addi x2, x0, 0x222)
      // -----------------------
      dut.io.dbg.rf_addr.poke(2.U) // select x2
      dut.clock.step(1)
      println("\n 2. Checking x2 after ADDI \n")
      dut.io.dbg.rf_data.expect("h222".U)

      // -----------------------
      // Step more cycles to allow SW and LW instructions to execute
      // -----------------------
      dut.clock.step(5) // ensure memory operations have completed

      // -----------------------
      // Check x3 (loaded value from memory)
      // -----------------------
      dut.io.dbg.rf_addr.poke(3.U) // select x3
      dut.clock.step(2)
      println("\n 3. Checking x3 after LW \n ")
      dut.io.dbg.rf_data.expect("h222".U) // matches value stored by SW

      dut.clock.step(5) // ensure memory operations have completed

      // -----------------------
      // Check x4 (if used in test program)
      // -----------------------
      dut.io.dbg.rf_addr.poke(4.U)
      dut.clock.step(1)
      println("4. Checking x4")
      dut.io.dbg.rf_data.expect("h333".U) // whatever value your program expects

    }
  }
}

 */