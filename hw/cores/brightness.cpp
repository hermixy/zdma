#include "common.h"
#include "ap_int.h"

int32_t zdma_core(axi_stream_t& src, axi_stream_t& dst, int32_t brightness)
{
#pragma HLS INTERFACE axis port=src bundle=INPUT_STREAM
#pragma HLS INTERFACE axis port=dst bundle=OUTPUT_STREAM
#pragma HLS INTERFACE s_axilite port=brightness bundle=control offset=0x10
#pragma HLS INTERFACE s_axilite port=return bundle=control offset=0x1C
#pragma HLS INTERFACE ap_stable port=brightness
	axi_elem_t data_in, data_out;
	int32_t ret;
	int16_t val[AXI_TDATA_NBYTES];
	union {
		axi_data_t all;
		uint8_t at[AXI_TDATA_NBYTES];
	} pixel;
	
	ret = 0;
	do {
#pragma HLS loop_tripcount min=153600 max=518400
#pragma HLS pipeline
		src >> data_in;
		ret += AXI_TDATA_NBYTES;
		data_out = data_in;
		pixel.all = data_out.data;

		for (int px = 0; px < AXI_TDATA_NBYTES; px++) {
			val[px] = pixel.at[px] + brightness;
			if (val[px] > 255)
				pixel.at[px] = 255;
			else if (val[px] < 0)
				pixel.at[px] = 0;
			else
				pixel.at[px] = val[px];
		}

		data_out.data = pixel.all;
		dst << data_out;
	} while (!data_out.last);
	return ret;
}

