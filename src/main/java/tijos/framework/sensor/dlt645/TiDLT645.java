package tijos.framework.sensor.dlt645;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;
import tijos.framework.util.Formatter;
import tijos.framework.util.LittleBitConverter;
import tijos.framework.util.crc.CheckSum8;

/**
 * Hello world!
 */
public class TiDLT645 extends Thread {

	/***** data tags table, DI0 DI1 DI2 DI3 *******/
	public static final int DLT645_TAG_FORWARD_ACTIVE_POWER = 0x00010000; // 表读数-总（正向有功）
	public static final int DLT645_TAG_BACKWARD_ACTIVE_POWER = 0x00020000; // 表读数-总（反向有功）
	public static final int DLT645_TAG_INSTANT_ACTIVE_POWER = 0x02030000; // 瞬时总有功

	public static final int DLT645_TAG_GRID_PHASE_VOLTAGE_A = 0x02010100;
	public static final int DLT645_TAG_GRID_PHASE_VOLTAGE_B = 0x02010200; // 电网相电压[Max. 3路]
	public static final int DLT645_TAG_GRID_PHASE_VOLTAGE_C = 0x02010300;

	public static final int DLT645_TAG_GRID_PHASE_CURRENT_A = 0x02020100;
	public static final int DLT645_TAG_GRID_PHASE_CURRENT_B = 0x02020200; // 电网相电流[Max. 3路]
	public static final int DLT645_TAG_GRID_PHASE_CURRENT_C = 0x02020300;

	public static final int DLT645_TAG_GRID_PHASE_POWER_A = 0x02030100;
	public static final int DLT645_TAG_GRID_PHASE_POWER_B = 0x02030200; // 电网相功率[Max. 3路]
	public static final int DLT645_TAG_GRID_PHASE_POWER_C = 0x02030300;

	// 表读数-电价1/2/3/4（正向有功）
	public static final int DLT645_TAG_FORWARD_ACTIVE_POWER_1 = 0x00010100;
	public static final int DLT645_TAG_FORWARD_ACTIVE_POWER_2 = 0x00010200;
	public static final int DLT645_TAG_FORWARD_ACTIVE_POWER_3 = 0x00010300;
	public static final int DLT645_TAG_FORWARD_ACTIVE_POWER_4 = 0x00010400;

	// 表读数-电价1/2/3/4（反向有功）
	public static final int DLT645_TAG_BACKWARD_ACTIVE_POWER_1 = 0x00020100;
	public static final int DLT645_TAG_BACKWARD_ACTIVE_POWER_2 = 0x00020200;
	public static final int DLT645_TAG_BACKWARD_ACTIVE_POWER_3 = 0x00020300;
	public static final int DLT645_TAG_BACKWARD_ACTIVE_POWER_4 = 0x00020400;

	/***** comm type *******/
	public static final int DLT645_COMM_TYPE_MASK = 0xE0;
	public static final int DLT645_MASTER_QUERY = 0x00;
	public static final int DLT645_SLAVE_REPLY_NORMAL = 0x80;
	public static final int DLT645_SLAVE_REPLY_ERROR = 0xC0;

	/***** comm bytes *******/
	private static final int DLT645_LEADING_BYTE = 0xFE;
	private static final int DLT645_START_BYTE = 0x68;

	/***** function code *******/
	private static final int DLT645_FUNC_CODE_MASK = 0x1F;
	private static final int DLT645_PKT_TYPE_TIME_SYNC = 0x08;
	private static final int DLT645_PKT_TYPE_READ_DATA = 0x11;
	private static final int DLT645_PKT_TYPE_READ_DATA_LEFT = 0x12;
	private static final int DLT645_PKT_TYPE_WRITE_DATA = 0x14;
	private static final int DLT645_PKT_TYPE_READ_ADDRESS = 0x13;
	private static final int DLT645_PKT_TYPE_WRITE_ADDRESS = 0x15;
	private static final int DLT645_PKT_TYPE_CHANGE_COMM_SPEED = 0x17;

	private static final int DLT645_PKT_TYPE_WRITE_DATA_RSP = 0x94;
	private static final int DLT645_PKT_TYPE_WRITE_DATA_ERR = 0xD4;

	/***** package length *******/
	private static final int DLT645_HEAD_TAIL_LEN = 16; // 4 leading bytes, 2 start bytes, 6 address, 1 func code, 1
														// data len, 1 cs, 1 end byte
	private static final int DLT645_PRE_LEADING_LEN = 4; // 4 0xEF
	private static final int DLT645_DATA_TAG_LEN = 4; // data identification
	private static final int DLT645_ADDRESS_LEN = 6; // meter address
	private static final int DLT645_MAX_DATA_LEN = 12; // max data 4+8
	private static final int DLT645_MIN_DATA_LEN = 6; // min data 4+2
	private static final int DLT645_POWER_READING_LEN = 4; // power data len
	private static final int DLT645_PASSWORD_LEN = 4; // password len
	private static final int DLT645_OPERATOR_LEN = 4; // operator len
	private static final int DLT645_FIXED_LEN = 10; // 2leading bytes，6 meter address，1func code，1 data len
	private static final int DLT645_gPHASE_VC_LEN = 2; // E V data len
	private static final int DLT645_gPHASE_P_LEN = 3; // three-phase power len
	private static final int DLT645_SEND_LEN = 20; //
	private static final int DLT645_RECV_LEN = 30;
	private static final int DLT645_ERROR_LEN = 1; // error message
	private static final int DLT645_EXTRA_LEN = 4; // read message extra length

	// Input stream for UART
	InputStream input;

	TiUART uart;

	/*
	 * meter address
	 */
	byte[] MeterAddress = new byte[DLT645_ADDRESS_LEN];

	// Keep the UART read thread running
	private boolean keeprunning = true;

	IDeviceEventListener eventLisener = null;

	/**
	 * Initialize with Uart
	 * 
	 * @param uart
	 */
	public TiDLT645(TiUART uart) {
		this.uart = uart;
		this.input = new BufferedInputStream(new TiUartInputStream(uart), 256);

		initMeterAddress();
	}

	@Override
	public void run() {

		while (true) {
			try {
				while (keeprunning) {
					Delay.msDelay(10);

					while (input.available() < 10) {
						Delay.msDelay(10);
						continue;
					}

					int val = input.read();

					// head
					if (val == DLT645_START_BYTE) {

						CheckSum8 checksum = new CheckSum8();
						checksum.update(val);

						byte[] address = new byte[DLT645_ADDRESS_LEN];
						input.read(address);

						checksum.update(address);

						val = input.read();
						if (val != DLT645_START_BYTE) {
							checksum.update(val);
							continue;
						}

						checksum.update(val);

						int funCode = input.read();
						int dataLen = input.read();

						checksum.update(funCode, dataLen);

						int leftLen = dataLen + 2;
						while (input.available() < leftLen) {
							Delay.msDelay(10);
							continue;
						}

						byte[] buffer = new byte[leftLen];
						input.read(buffer);

						checksum.update(buffer, 0, buffer.length - 2);

						if (checksum.getValue() != (byte) (buffer[leftLen - 2] & 0xFF)) {
							// throw new IOException("Invalid CheckSum");
						}

						if (dataLen > 0) {
							for (int i = 0; i < dataLen; ++i) {
								buffer[i] = (byte) (buffer[i] - 0x33);
							}
						}

						if (funCode == DLT645_PKT_TYPE_WRITE_DATA_RSP || funCode == DLT645_PKT_TYPE_WRITE_DATA_ERR) {
							byte[] data = new byte[dataLen];
							System.arraycopy(buffer, 0, data, 0, dataLen);
							if (eventLisener != null) {
								eventLisener.onDataArrived(funCode, 0, data);
							}

						} else {
							byte[] tag = new byte[DLT645_DATA_TAG_LEN];
							System.arraycopy(buffer, 0, tag, 0, DLT645_DATA_TAG_LEN);

							byte[] data = new byte[dataLen];
							System.arraycopy(buffer, DLT645_DATA_TAG_LEN, data, 0, dataLen - DLT645_DATA_TAG_LEN);

							if (eventLisener != null) {
								int dataTag = LittleBitConverter.ToInt32(tag, 0);
								eventLisener.onDataArrived(funCode, dataTag, data);
							}
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initialize meter address
	 */
	private void initMeterAddress() {
		for (int i = 0; i < MeterAddress.length; i++) {
			MeterAddress[i] = (byte) 0xaa;
		}
	}

	/**
	 * Event listener for data arrived from remote node
	 *
	 * @param listener
	 */
	public void setEventListener(IDeviceEventListener listener) {
		this.eventLisener = listener;
	}

	/**
	 * Get meter address
	 * 
	 * @return
	 * @throws IOException
	 */
	public byte[] readMeterAddress() throws IOException {
		return queryMeterReading(DLT645_PKT_TYPE_READ_ADDRESS, 0);
	}

	/**
	 * query meter reading by data tag
	 * 
	 * @param dataTag
	 * @return
	 * @throws IOException
	 */
	public byte[] readMeterData(int dataTag) throws IOException {
		return queryMeterReading(DLT645_PKT_TYPE_READ_DATA, dataTag);
	}

	/**
	 * write the specified tag data to meter with password and operator
	 * 
	 * @param password
	 * @param operator
	 * @param dataTag
	 * @param data
	 * @throws IOException
	 */
	public void writeMeterData(byte[] password, byte[] operator, int dataTag, byte[] data) throws IOException {
		if (password.length != 4 || operator.length != 4)
			throw new IOException("invalid password or oeprator length");

		byte[] pkt = createWriteRequest(DLT645_PKT_TYPE_WRITE_DATA, password, operator, dataTag, data);
		sendPkt(pkt);

		int expectRecvLen = DLT645_HEAD_TAIL_LEN;
		byte[] recvPkt = getReply(expectRecvLen, 500);

		if (recvPkt.length == 0) {
			this.clearBuff();
			throw new IOException("DLT645 Receive meter reading reply failed!");
		}

		decodePkt(DLT645_PKT_TYPE_WRITE_DATA, recvPkt, dataTag);

		this.clearBuff();
	}

	/**
	 * Send meter data tag reading request, the result should be returned from
	 * 
	 * @param dataTag
	 * @throws IOException
	 */
	public void sendMeterReadingRequest(int dataTag) throws IOException {
		byte[] pkt = createSendPkt(DLT645_PKT_TYPE_READ_DATA, dataTag); // format of message

		sendPkt(pkt);
	}

	/**
	 * Send write command data request
	 * 
	 * @param password 4 bytes password
	 * @param operator 4 bytes operator
	 * @param dataTag
	 * @param data
	 * @throws IOException
	 */
	public void writeMeterDataRequest(byte[] password, byte[] operator, int dataTag, byte[] data) throws IOException {
		if (password.length != 4 || operator.length != 4)
			throw new IOException("invalid password or oeprator length");

		byte[] pkt = createWriteRequest(DLT645_PKT_TYPE_WRITE_DATA, password, operator, dataTag, data);
		sendPkt(pkt);
	}

	/**
	 * Alarm response
	 * 
	 * @param dataTag
	 * @throws IOException
	 */
	public void writeAlarmDataResponse(int dataTag) throws IOException {
		byte[] pkt = createSendPkt(DLT645_PKT_TYPE_READ_DATA_LEFT, dataTag);

		sendPkt(pkt);
	}

	/**
	 * Convert BCD to reading
	 * 
	 * @param input
	 * @param decimal
	 * @return
	 */
	public double BCD2Double(byte[] input, int decimal) {
		return BCD2Double(input, 0, input.length, decimal);
	}

	public double BCD2Double(byte[] input, int start, int len, int decimal) {
		double reading = 0;
		double coef = 1;

		if (start + len > input.length)
			return Double.NaN;

		/* result is in BCD format XXXXXX.XX, little endian */
		for (int i = 0; i < len; ++i) {
			reading += (input[start + i] & 0x0f) * coef;
			reading += (input[start + i] >> 4) * 10 * coef;
			coef *= 100;
		}

		for (int i = 0; i < decimal; i++) {
			reading /= 10.0;
		}

		return reading;
	}

	/**
	 * query meter reading by data tag
	 * 
	 * @param dataTag
	 * @return
	 * @throws IOException
	 */
	private byte[] queryMeterReading(int funCode, int dataTag) throws IOException {
		int expectRecvLen = DLT645_HEAD_TAIL_LEN + DLT645_DATA_TAG_LEN + DLT645_POWER_READING_LEN + DLT645_EXTRA_LEN;

		byte[] pkt = createSendPkt(funCode, dataTag); // format of message

		sendPkt(pkt);

		byte[] recvPkt = getReply(expectRecvLen, 500);

		if (recvPkt.length == 0) {
			this.clearBuff();
			throw new IOException("DLT645 Receive meter reading reply failed!");
		}

		byte[] meterData = decodePkt(funCode, recvPkt, dataTag);

		this.clearBuff();

		return meterData;
	}

	/**
	 * Create send packet by type
	 * 
	 * @param funCode
	 * @param dataTag
	 * @return
	 */
	private byte[] createSendPkt(int funCode, int dataTag) throws IOException {
		int datalen = 0;
		int pktLen;
		byte[] data = new byte[DLT645_MAX_DATA_LEN];

		switch (funCode) {
		case DLT645_PKT_TYPE_READ_DATA:
		case DLT645_PKT_TYPE_READ_DATA_LEFT:
			datalen = DLT645_DATA_TAG_LEN;

			byte[] tag = LittleBitConverter.GetBytes(dataTag);
			System.arraycopy(tag, 0, data, 0, tag.length);

			break;
		case DLT645_PKT_TYPE_READ_ADDRESS:
			datalen = 0;
			break;

		default:
			throw new IOException("Invalid type");
		}

		pktLen = datalen + DLT645_HEAD_TAIL_LEN;

		byte[] pkt = new byte[pktLen];

		/* add 4 leading bytes */
		pkt[0] = (byte) DLT645_LEADING_BYTE;
		pkt[1] = (byte) DLT645_LEADING_BYTE;
		pkt[2] = (byte) DLT645_LEADING_BYTE;
		pkt[3] = (byte) DLT645_LEADING_BYTE;

		pkt[4] = DLT645_START_BYTE; // start byte

		System.arraycopy(MeterAddress, 0, pkt, 5, MeterAddress.length);

		pkt[11] = DLT645_START_BYTE;
		pkt[12] = (byte) (DLT645_MASTER_QUERY | funCode); // function code
		pkt[13] = (byte) datalen;

		if (datalen > 0) {
			for (int i = 0; i < datalen; ++i) {
				pkt[14 + i] = (byte) (data[i] + 0x33);
			}
		}

		pkt[pktLen - 2] = (byte) getChecksum(pkt, 4, pktLen - 6); // get the checksum excluding the leading bytes and
																	// end byte
		pkt[pktLen - 1] = 0x16;

		return pkt;
	}

	private byte[] createWriteRequest(int funCode, byte[] password, byte[] operator, int dataTag, byte[] data) {

		int expectRecvLen = DLT645_HEAD_TAIL_LEN + DLT645_DATA_TAG_LEN + DLT645_PASSWORD_LEN + DLT645_OPERATOR_LEN
				+ DLT645_EXTRA_LEN;

		int dataLen = 04 + 04 + 04 + data.length;
		int pktLen = expectRecvLen;
		int pos = 0;

		byte[] pkt = new byte[pktLen];

		/* add 4 leading bytes */
		pkt[pos++] = (byte) DLT645_LEADING_BYTE;
		pkt[pos++] = (byte) DLT645_LEADING_BYTE;
		pkt[pos++] = (byte) DLT645_LEADING_BYTE;
		pkt[pos++] = (byte) DLT645_LEADING_BYTE;

		pkt[pos++] = DLT645_START_BYTE; // start byte

		System.arraycopy(MeterAddress, 0, pkt, pos, MeterAddress.length);
		pos += DLT645_ADDRESS_LEN;

		pkt[pos++] = DLT645_START_BYTE;
		pkt[pos++] = (byte) funCode;
		pkt[pos++] = (byte) dataLen;

		byte[] tag = LittleBitConverter.GetBytes(dataTag);
		System.arraycopy(tag, 0, pkt, pos, tag.length);
		pos += DLT645_DATA_TAG_LEN;

		System.arraycopy(password, 0, pkt, pos, password.length);
		pos += 4;

		System.arraycopy(operator, 0, pkt, pos, operator.length);
		pos += 4;

		System.arraycopy(data, 0, pkt, pos, data.length);
		pos += data.length;

		if (dataLen > 0) {
			for (int i = 0; i < dataLen; ++i) {
				pkt[14 + i] = (byte) (pkt[14 + i] + 0x33);
			}
		}

		pkt[pos++] = (byte) getChecksum(pkt, 4, pktLen - 6); // get the checksum excluding the leading bytes and end
																// byte
		pkt[pos++] = 0x16;

		return pkt;
	}

	/**
	 * Decode the tag data from received packet
	 * 
	 * @param funCode    function code
	 * @param Pkt
	 * @param match_data
	 * @return
	 * @throws IOException
	 */
	private byte[] decodePkt(int funCode, byte[] Pkt, int match_data) throws IOException {
		int check_len;

		/* delete all leading bytes */
		int startPos = 0;
		while (Pkt[startPos] != DLT645_START_BYTE && startPos < Pkt.length - 1) {
			startPos++;
		}

		if (startPos >= Pkt.length || Pkt[startPos] != DLT645_START_BYTE) {
			throw new IOException("DLT645 Decode: receive all FE packet or the start byte is not 68!");
		}

		// L
		int data_len = Pkt[startPos + 9];

		check_len = DLT645_FIXED_LEN + data_len;
		int expCS = Pkt[startPos + check_len] & 0xFF;
		int CS = getChecksum(Pkt, startPos, check_len);// excluding cs and end byte
		if (expCS != CS) {
			throw new IOException("DLT645 Decode: Checksum mismatch! CS " + CS + " exp CS " + expCS);
		}

		/* check if the receive pkt and the send pkt types match */
		int controlCode = Pkt[startPos + 8];
		if ((funCode & DLT645_FUNC_CODE_MASK) != (controlCode & DLT645_FUNC_CODE_MASK)) {
			throw new IOException("DLT645 Decode: Send and receive package types mismatch!");
		}

		for (int i = 0; i < data_len; ++i) // data area should subtract 0x33 to get the real values
		{
			Pkt[DLT645_FIXED_LEN + i + startPos] -= 0x33;
		}

		byte[] Type_Match = LittleBitConverter.GetBytes(match_data);

		switch (funCode) {
		case DLT645_PKT_TYPE_READ_ADDRESS:
			if (data_len < DLT645_ADDRESS_LEN) {
				throw new IOException("DLT645 Decode: receive read address data len mismatch!");
			}

			if (memcmp(Pkt, startPos + 1, Pkt, startPos + 10, DLT645_ADDRESS_LEN) != 0) {
				throw new IOException("DLT645 receive read address data  mismatch!");
			}

			System.arraycopy(Pkt, startPos + 10, MeterAddress, 0, DLT645_ADDRESS_LEN);

			return this.MeterAddress;

		case DLT645_PKT_TYPE_READ_DATA:
			if (memcmp(MeterAddress, 0, Pkt, startPos + 1, DLT645_ADDRESS_LEN) != 0) {
				throw new IOException("DLT645 Decode: receive reply reading meter address mismatch!");
			}

			if ((controlCode & DLT645_COMM_TYPE_MASK) == DLT645_SLAVE_REPLY_ERROR) {
				throw new IOException("DLT645 Decode: receive reply reading control code is D1!");
			}

			if (memcmp(Type_Match, 0, Pkt, startPos + 10, 4) != 0) {
				throw new IOException("DLT645 Decode: receive reply reading data identification mismatch!");
			}

			if (!((controlCode & DLT645_COMM_TYPE_MASK) == DLT645_SLAVE_REPLY_NORMAL)) {
				throw new IOException("DLT645 Decode: receive reply reading control code is not read function!");
			}

			if (data_len < DLT645_MIN_DATA_LEN || data_len > DLT645_MAX_DATA_LEN) {
				throw new IOException("DLT645 Decode: receive reply reading data length mismatch!");
			}

			// 4 bytes reading follows N bytes data tag in the data area
			byte[] meterData = new byte[data_len - DLT645_DATA_TAG_LEN];
			System.arraycopy(Pkt, startPos + DLT645_FIXED_LEN + DLT645_DATA_TAG_LEN, meterData, 0, meterData.length);

			return meterData;

		case DLT645_PKT_TYPE_WRITE_DATA:

			if (memcmp(MeterAddress, 0, Pkt, startPos + 1, DLT645_ADDRESS_LEN) != 0) {
				throw new IOException("DLT645 Decode: receive reply reading meter address mismatch!");
			}

			if (controlCode != 0x94) {
				throw new IOException("DLT645 Decode: receive reply reading control code is not 0x94!");
			}

			return null;

		default:
			throw new IOException("DLT645 Decode: control code type unknown!");
		}
	}

	/**
	 * Generate checksum from data buffer
	 * 
	 * @param data  data buffer
	 * @param start start pos
	 * @param len   length
	 * @return checksum value
	 */
	private static int getChecksum(byte[] data, int start, int len) {
		CheckSum8 checksum = new CheckSum8();
		checksum.update(data, start, len);

		return checksum.getValue();
	}

	/**
	 * Memory compare
	 * 
	 * @param b1
	 * @param startB1
	 * @param b2
	 * @param startB2
	 * @param sz
	 * @return
	 */
	private int memcmp(byte b1[], int startB1, byte b2[], int startB2, int sz) {
		for (int i = 0; i < sz; i++) {
			int v1 = b1[i + startB1];
			int v2 = b2[i + startB2];

			if (v1 != v2) {
				if ((v1 >= 0 && v2 >= 0) || (v1 < 0 && v2 < 0))
					return v1 - v2;
				if (v1 < 0 && v2 >= 0)
					return 1;
				if (v2 < 0 && v1 >= 0)
					return -1;
			}
		}
		return 0;
	}

	/**
	 * Send packet to uart
	 * 
	 * @param pkt
	 * @throws IOException
	 */
	private void sendPkt(byte[] pkt) throws IOException {
		System.out.println("send " + Formatter.toHexString(pkt));
		this.uart.write(pkt, 0, pkt.length);
	}

	/**
	 * Receive packet from uart within timeout
	 * 
	 * @param expLen  expected length
	 * @param timeOut timeout to read
	 * @return
	 * @throws IOException
	 */
	private byte[] getReply(int expLen, int timeOut) throws IOException {
		while (this.input.available() < expLen) {
			Delay.msDelay(100);
			timeOut -= 100;
			if (timeOut <= 0)
				break;
		}

		byte[] reply = new byte[this.input.available()];

		this.input.read(reply);

		return reply;
	}

	/**
	 * clear UART buffer
	 * 
	 * @throws IOException
	 */
	private void clearBuff() throws IOException {
		this.uart.clear(TiUART.BUFF_WR);
		while (this.input.read() > 0)
			;
	}


	// FEFEFEFE6866666666666668911F33335033650F9F6B33333333333333338733333343553533933933337538331516
	public static void main(String[] args) throws IOException {

		TiDLT645 dlt = new TiDLT645(null);
		byte[] password = new byte[] { 0x00, 0x00, 0x00, 0x00 };
		byte[] operator = new byte[] { (byte) 0xC1, (byte) 0XC2, (byte) 0XC3, (byte) 0XC4 };
		int dataTag = 0x0400010A;

		byte[] data = new byte[] { 01 };

		byte[] pkt = dlt.createWriteRequest(DLT645_PKT_TYPE_WRITE_DATA, password, operator, dataTag, data);

	}
}
