package tijos.framework.sensor.dlt645;

import java.io.IOException;

import tijos.framework.devicecenter.TiUART;

/**
 * Hello world!
 */
public class TiDLT645Sample {

	public static void main(String[] args) throws IOException {
		System.out.println("Hello World!");

		int[] All_Meter_Data = new int[] { TiDLT645.DLT645_TAG_FORWARD_ACTIVE_POWER,
				TiDLT645.DLT645_TAG_BACKWARD_ACTIVE_POWER, TiDLT645.DLT645_TAG_INSTANT_ACTIVE_POWER,
				TiDLT645.DLT645_TAG_GRID_PHASE_VOLTAGE_A, TiDLT645.DLT645_TAG_GRID_PHASE_VOLTAGE_B,
				TiDLT645.DLT645_TAG_GRID_PHASE_VOLTAGE_C, TiDLT645.DLT645_TAG_GRID_PHASE_CURRENT_A,
				TiDLT645.DLT645_TAG_GRID_PHASE_CURRENT_B, TiDLT645.DLT645_TAG_GRID_PHASE_CURRENT_C,
				TiDLT645.DLT645_TAG_GRID_PHASE_POWER_A, TiDLT645.DLT645_TAG_GRID_PHASE_POWER_B,
				TiDLT645.DLT645_TAG_GRID_PHASE_POWER_C, TiDLT645.DLT645_TAG_FORWARD_ACTIVE_POWER_1,
				TiDLT645.DLT645_TAG_FORWARD_ACTIVE_POWER_2, TiDLT645.DLT645_TAG_FORWARD_ACTIVE_POWER_3,
				TiDLT645.DLT645_TAG_FORWARD_ACTIVE_POWER_4, TiDLT645.DLT645_TAG_BACKWARD_ACTIVE_POWER_1,
				TiDLT645.DLT645_TAG_BACKWARD_ACTIVE_POWER_2, TiDLT645.DLT645_TAG_BACKWARD_ACTIVE_POWER_3,
				TiDLT645.DLT645_TAG_BACKWARD_ACTIVE_POWER_4 };

		TiUART uart = TiUART.open(3);
		uart.setWorkParameters(8, 1, TiUART.PARITY_EVEN, 2400);

		TiDLT645 dlt645 = new TiDLT645(uart);
		
		byte [] addr = dlt645.getMeterAddress();
		
		for(int tag:All_Meter_Data)
		{
			double reading = MeterReading_Get(dlt645, tag);
			System.out.println("tag: " + tag + " reading: " + reading);
		}
	}

	static double MeterReading_Get(TiDLT645 dlt645, int meterdata) throws IOException {
		double reading;
		reading = dlt645.queryMeterReading(meterdata); // read meter data

		return reading;
	}
}
