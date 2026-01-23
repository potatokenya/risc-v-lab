package lib.peripherals

import chisel3._
import chisel3.util._

import lib.Bus
class MemoryMappedSevenSeg extends Module {
  val io = IO(new Bundle {
    val port = Bus.RespondPort()
    val pins = new Bundle {
      val an  = Output(UInt(4.W))
      val seg = Output(UInt(7.W))
      val dp  = Output(Bool())
    }
  })

  // Register holding value to display
  val reg = RegInit(0.U(16.W))  // 4 hex digits

  // MMIO write
  when(io.port.write) {
    reg := io.port.wrData(15,0)
  }

  io.port.rdData := reg

  // Digit multiplexing
  val refreshCounter = RegInit(0.U(16.W))
  refreshCounter := refreshCounter + 1.U
  val digitSel = refreshCounter(15,14) // 2-bit → 4 digits

  val currentNibble = MuxLookup(digitSel, 0.U, Seq(
    0.U -> reg(3,0),
    1.U -> reg(7,4),
    2.U -> reg(11,8),
    3.U -> reg(15,12)
  ))

  // Active-low anodes
  io.pins.an := ~(1.U(4.W) << digitSel)

  // Hex decoder (active low segments)
  def hexTo7Seg(x: UInt): UInt = {
    MuxLookup(x, "b1111111".U, Seq(
      "h0".U -> "b1000000".U,
      "h1".U -> "b1111001".U,
      "h2".U -> "b0100100".U,
      "h3".U -> "b0110000".U,
      "h4".U -> "b0011001".U,
      "h5".U -> "b0010010".U,
      "h6".U -> "b0000010".U,
      "h7".U -> "b1111000".U,
      "h8".U -> "b0000000".U,
      "h9".U -> "b0010000".U,
      "hA".U -> "b0001000".U,
      "hB".U -> "b0000011".U,
      "hC".U -> "b1000110".U,
      "hD".U -> "b0100001".U,
      "hE".U -> "b0000110".U,
      "hF".U -> "b0001110".U
    ))
  }

  io.pins.seg := hexTo7Seg(currentNibble)
  io.pins.dp  := true.B  // off
}
