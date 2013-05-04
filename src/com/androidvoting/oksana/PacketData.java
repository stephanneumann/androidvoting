/**
 * Essential data got from packets
 */

package com.oksana;

import com.murati.smartdkg.communication.SDKGMessage;
import com.murati.smartdkg.dkg.SDKGPlayer;

public class PacketData {
	public PacketData(SDKGPlayer to, SDKGPlayer from, SDKGMessage msg) {
		this.to = to;
		this.from = from;
		this.msg = msg;
	}
	private SDKGPlayer from, to; //player's indices
	private SDKGMessage msg;
	public SDKGPlayer getTo() {
		return to;
	}
	public SDKGPlayer getFrom() {
		return from;
	}
	public SDKGMessage getMsg() {
		return msg;
	}
}
