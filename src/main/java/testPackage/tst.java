package testPackage;

import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.MacAddress;

import net.floodlightcontroller.packet.Ethernet;

public class tst {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Ethernet l2=new Ethernet();
		//l2.setSourceMACAddress(MacAddress.of("00:00:00:00:00:01"));68-5D-43-7B-CE-15
		l2.setSourceMACAddress(MacAddress.of("68-5D-43-7B-CE-15"));
		l2.setDestinationMACAddress(MacAddress.BROADCAST);
		l2.setEtherType(EthType.IPv4);
		System.out.print(l2.serialize().toString()); 

	}

}
