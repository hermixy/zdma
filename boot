#!/opt/Xilinx/Vivado/2017.3/bin/xsdb
set img "/home/igalanommatis/work/zdma/image"
puts stderr "INFO: Compiling Device-Tree..."
exec -ignorestderr dtc -I dts -O dtb $img/dts/system-top.dts -o $img/zdma.dtb

puts stderr "INFO: Resetting all ARM targets..."
connect -url 127.0.0.1:3121
set items [split [targets] \n]
foreach line $items {
	set index [ scan $line %d]
	if {[string match "*ARM*" $line] == 1} {
		target $index
		rst
	}
}

puts stderr "INFO: Configuring the FPGA..."
puts stderr "INFO: Downloading bitstream to the target."
fpga "$img/zdma.bit"
after 2000
targets -set -nocase -filter {name =~ "arm*#0"}

source "$img/ps7_init.tcl" ; ps7_post_config
catch {stop}
set mctrlval [string trim [lindex [split [mrd 0xF8007080] :] 1]]
puts "mctrlval=$mctrlval"
puts stderr "INFO: Downloading ELF file to the target."
dow "$img/zynq-fsbl.elf"
after 2000
con
after 3000; stop
targets -set -nocase -filter {name =~ "arm*#0"}
puts stderr "INFO: Downloading ELF file to the target."
dow "$img/u-boot.elf"
after 2000
con
after 2000; stop
targets -set -nocase -filter {name =~ "arm*#0"}

rst -processor; after 2000
dow -data "$img/zdma.dtb" 0x02408000
after 2000
rwr r2 0x02408000
targets -set -nocase -filter {name =~ "arm*#0"}
dow -data "$img/zImage" 0x8000
after 2000
rwr pc 0x00008000
con
after 500
exit
