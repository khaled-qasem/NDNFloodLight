package net.floodlightcontroller.ndn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFGroupAdd;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionGroup;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
//import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.Match.Builder;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.match.Prerequisite;
import org.projectfloodlight.openflow.protocol.oxm.OFOxms;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IListener.Command;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.learningswitch.LearningSwitch;
import net.floodlightcontroller.mactracker.MACTracker;
import net.floodlightcontroller.packet.CR;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.PacketParsingException;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
//net.floodlightcontroller.ndn.CIDTrack

//net.floodlightcontroller.forwarding.Forwarding
//net.floodlightcontroller.learningswitch.LearningSwitch
//net.floodlightcontroller.routing.RoutingManager
public class CIDTrack implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected IStaticEntryPusherService entrypushers;
	protected static Logger logger;
	private boolean pusher_added =false;
	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 1;
	protected static TableId FLOWMOD_DEFAULT_TABLE_ID = TableId.ZERO;
	protected boolean exist_cr =false;
	
	/////
	// more flow-mod defaults
		
		protected static short FLOWMOD_PRIORITY = 100;
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		
		return CIDTrack.class.getSimpleName();
		
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		// TODO Auto-generated method stub
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(IStaticEntryPusherService.class);
		    return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		entrypushers=context.getServiceImpl(IStaticEntryPusherService.class);
	    //the collection is kept sorted
	  //  entrypushers.addFlow("aa", fm, swDpid);


	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		// TODO Auto-generated method stub
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	
		

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {
		// TODO Auto-generated method stub
		
		OFFactory myFactory = sw.getOFFactory(); 
     	 OFFactory my13Factory = OFFactories.getFactory(OFVersion.OF_13);
		
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		 
        /* Various getters and setters are exposed in Ethernet */
        /* 
         * Check the ethertype of the Ethernet frame and retrieve the appropriate payload.
         * Note the shallow equality check. EthType caches and reuses instances for valid types.
         */
		int ss=eth.getEtherType().getValue();
        System.out.println("ether value type ="+ss+"--------------");
        if (eth.getEtherType() == EthType.IPv4) {
            /* We got an IPv4 packet; get the payload from Ethernet */
            IPv4 ipv4 = (IPv4) eth.getPayload();
            int ss1=ipv4.getProtocol().getIpProtocolNumber();
            System.out.println("ip protocol ="+ss1+"***********************");
            /* Various getters and setters are exposed in IPv4 */
           // byte[] ipOptions = ipv4.getOptions();
            //IPv4Address dstIp = ipv4.getDestinationAddress();
             
            /* 
             * Check the IP protocol version of the IPv4 packet's payload.
             */
            ///*******************************************************
            if (ipv4.getProtocol().getIpProtocolNumber() ==250 ) {
                /* We got a TCP packet; get the payload from IPv4 */
               // CR cr = (CR) ipv4.getPayload();
            	System.out.println(ipv4.getProtocol().getIpProtocolNumber());
            	byte[] bytearray;
            	bytearray=ipv4.getPayload().serialize();
            	
            	//UDP udp =(UDP) ipv4.getPayload();
            	UDP udp =new UDP();
            	CR cr =new CR();
            	try {
					cr.deserialize(bytearray, 0, bytearray.length);
				} catch (PacketParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                /* Various getters and setters are exposed in CR */
               TransportPort srcPort = cr.getSourcePort();
               TransportPort dstPort = cr.getDestinationPort();
                /* Your logic here! */
                
               byte[] serializedData = eth.serialize();
               
               
		    	 if(exist_cr){
		    		 ArrayList<OFAction> actionList = new ArrayList<OFAction>();
				    	 actionList.add(myFactory.actions().buildOutput()
					 		    	.setPort(OFPort.of(3)) // raw types replaced with objects for type-checking and readability
					 		    	.build()); // list of immutable OFAction objects
               OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
            		    .setData(serializedData)
            		    .setActions(actionList)
            		    //.setInPort(OFPort.CONTROLLER)
            		    .build();
               sw.write(po);
		    	 }
		    	 else {
		    		 ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		               actionList.add(myFactory.actions().buildOutput()
				 		    	.setPort(OFPort.of(2)) // raw types replaced with objects for type-checking and readability
				 		    	.build()); // list of immutable OFAction objects
				    	 actionList.add(myFactory.actions().buildOutput()
					 		    	.setPort(OFPort.of(3)) // raw types replaced with objects for type-checking and readability
					 		    	.build()); // list of immutable OFAction objects
             OFPacketOut po = sw.getOFFactory().buildPacketOut() /* mySwitch is some IOFSwitch object */
          		    .setData(serializedData)
          		    .setActions(actionList)
          		    //.setInPort(OFPort.CONTROLLER)
          		    .build();
                   sw.write(po);
		    		 
		    	 }                
            }	
     
            ////******************************************************         
            if (ipv4.getProtocol().getIpProtocolNumber() ==251 ) {
                /* We got a TCP packet; get the payload from IPv4 */
               // CR cr = (CR) ipv4.getPayload();
            	System.out.println(ipv4.getProtocol().getIpProtocolNumber());
            	byte[] bytearray;
            	bytearray=ipv4.getPayload().serialize();
            	exist_cr=!exist_cr;
            	//UDP udp =(UDP) ipv4.getPayload();
            	UDP udp =new UDP();
            	CR cr =new CR();
            	try {
					cr.deserialize(bytearray, 0, bytearray.length);
				} catch (PacketParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                /* Various getters and setters are exposed in CR */
               TransportPort srcPort = cr.getSourcePort();
               TransportPort dstPort = cr.getDestinationPort();
                /* Your logic here! */
                            
            }	
		
		
        }
            
	    	 
	    	
	    	ArrayList<OFAction> actions = new ArrayList<OFAction>();
	    	actions.add(myFactory.actions().buildOutput()
	    	.setPort(OFPort.of(2)) // raw types replaced with objects for type-checking and readability
	    	.build()); // list of immutable OFAction objects
	    	
	    	 Match myMatch = myFactory.buildMatch()
	    	   		 	.setExact(MatchField.IN_PORT, OFPort.of(1))
	    	   		 	.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	    	   		 	.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1"))
	    	   		 	.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
	    	   		 	.build();
		    	 
	    	 OFFlowAdd flowAdd = my13Factory.buildFlowAdd()
	    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
	    	   		 	.setMatch(myMatch)
	    	   		 	.setActions(actions)
	    	   		 	.setPriority(10)
	    	   		 	.build();


	    	sw.write(flowAdd);
	    	
	       /////////////////////////[1]/
	    	ArrayList<OFAction> actions2 = new ArrayList<OFAction>();
	    	actions2.add(myFactory.actions().buildOutput()
	    	.setPort(OFPort.of(1)) // raw types replaced with objects for type-checking and readability
	    	.build()); // list of immutable OFAction objects
	    	 	    	
	    	 Match myMatch2 = myFactory.buildMatch()
	    	   		 	.setExact(MatchField.IN_PORT, OFPort.of(2))//in port
	    	   		 	.setExact(MatchField.ETH_TYPE, EthType.IPv4)
	    	   		 	.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.2"))
	    	   		 	.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
	    	   		 	.build();
	    	 	    	 
	    	 OFFlowAdd flowAdd2 = my13Factory.buildFlowAdd()
	    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
	    	   		 	.setMatch(myMatch2)
	    	   		 	.setActions(actions2)
	    	   		 	.setPriority(10)
	    	   		 	.build();
	    	
	    	
	    	 sw.write(flowAdd2);
	    	
	    	///////////////////////////[2]/
	    	OFFactory myFactory3 = sw.getOFFactory(); 
	     	ArrayList<OFAction> actions3 = new ArrayList<OFAction>();
	     	actions3.add(myFactory3.actions().buildOutput()
	     	.setPort(OFPort.of(2)) // raw types replaced with objects for type-checking and readability
	     	.build()); // list of immutable OFAction objects

	     	 Match myMatch3 = myFactory.buildMatch()
	  	   		 	.setExact(MatchField.IN_PORT, OFPort.of(1))
	  	   		 	.setExact(MatchField.ETH_TYPE, EthType.ARP)
	  	   		 	.build();     	 
	     	 
	     	 OFFlowAdd flowAdd3 = my13Factory.buildFlowAdd()
	  	   		 	.setBufferId(OFBufferId.NO_BUFFER)
	  	   		 	.setMatch(myMatch3)
	  	   		 	.setActions(actions3)
	  	   		 	.setPriority(10)
	  	   		 	.build();
	     	 
	     	sw.write(flowAdd3);
	     	
	     	///////////////////////////[3]/
	    	ArrayList<OFAction> actions4 = new ArrayList<OFAction>();
	    	actions4.add(myFactory.actions().buildOutput()
	    	.setPort(OFPort.of(1)) // raw types replaced with objects for type-checking and readability
	    	.build()); // list of immutable OFAction objects
	    	
	    	
	    	 Match myMatch4 = myFactory.buildMatch()
	    	   		 	.setExact(MatchField.IN_PORT, OFPort.of(2))//in port
	    	   		 	.setExact(MatchField.ETH_TYPE, EthType.ARP)
	    	   		 	.build();
	    	 
	    	 OFFlowAdd flowAdd4 = my13Factory.buildFlowAdd()
	    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
	    	   		 	.setMatch(myMatch4)
	    	   		 	.setActions(actions4)
	    	   		 	.setPriority(10)
	    	   		 	.build();
	    	
	    	
	    	 sw.write(flowAdd4);
	    	 
	    	 ///////////////////////////////////[4]
		    	ArrayList<OFAction> actions5 = new ArrayList<OFAction>();
		    	actions5.add(myFactory.actions().buildOutput()
		    	.setPort(OFPort.of(2)) // raw types replaced with objects for type-checking and readability
		    	.build()); // list of immutable OFAction objects
		    	
		    	
		    	 Match myMatch5 = myFactory.buildMatch()
		    	   		 	.setExact(MatchField.IN_PORT, OFPort.of(3))//in port
		    	   		 	.setExact(MatchField.ETH_TYPE, EthType.ARP)
		    	   		 	.build();
		    	 
		    	 OFFlowAdd flowAdd5 = my13Factory.buildFlowAdd()
		    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
		    	   		 	.setMatch(myMatch5)
		    	   		 	.setActions(actions5)
		    	   		 	.setPriority(10)
		    	   		 	.build();
		    	
		    	
		    	 sw.write(flowAdd5);
		    	 
	    	 
	    	 /////////////////////// now lets implement out project 
	    	// TODO Auto-generated method stub
	    	 
	    	 ArrayList<OFAction> actions_cr = new ArrayList<OFAction>();
		    	actions_cr.add(myFactory.actions().buildOutput()
		    	.setPort(OFPort.CONTROLLER) // raw types replaced with objects for type-checking and readability
		    	.build()); // list of immutable OFAction objects
		    	
		    	 Match myMatch_cr = myFactory.buildMatch()
		    	   		 	.setExact(MatchField.IN_PORT, OFPort.of(1))
		    	   		 	.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		    	   		 	.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1"))
		    	   		 	.setExact(MatchField.IP_PROTO, IpProtocol.of((short)250))
		    	   		 	.build();
			
		    	 
		    	 
		    	 OFFlowAdd flowAdd_cr = my13Factory.buildFlowAdd()
		    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
		    	   		 	.setMatch(myMatch_cr)
		    	   		 	.setActions(actions_cr)
		    	   		 	.setPriority(10)
		    	   		 	.build();


		   // sw.write(flowAdd_cr);
	    	///____________________________________________________________________________________________ 
	    	 ///-------------------------------------------------------------------------------------------
		    	 
		    	 ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		    	 Match MatchCR=myFactory.buildMatch()
		 				.setExact(MatchField.IN_PORT, OFPort.of(1))
		 				.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		 				.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1"))
		 				.setExact(MatchField.IP_PROTO, IpProtocol.of((short)250))
		 				.build();
	    	 
		    	 actionList.add(myFactory.actions().buildOutput()
		 		    	.setPort(OFPort.CONTROLLER) // raw types replaced with objects for type-checking and readability
		 		    	.setMaxLen(Integer.MAX_VALUE)
		 		    	.build()); // list of immutable OFAction objects
		    	 /*
		    	 actionList.add(myFactory.actions().buildOutput()
			 		    	.setPort(OFPort.of(3)) // raw types replaced with objects for type-checking and readability
			 		    	.build()); // list of immutable OFAction objects
		    	 */
		    	 OFFlowAdd flowAddCR = my13Factory.buildFlowAdd()
		    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
		    	   		 	.setMatch(MatchCR)
		    	   		 	.setActions(actionList)
		    	   		 	.setPriority(10)
		    	   		 	.build();
	    	 
		    	 sw.write(flowAddCR);
	    	 
	    	 /////____________________________________________________________________________
		    	 //-------------------------------------------------------------------------
		    	 ArrayList<OFAction> actionList1 = new ArrayList<OFAction>();
		    	 Match MatchCR1=myFactory.buildMatch()
		 				.setExact(MatchField.IN_PORT, OFPort.of(3))
		 				.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		 				.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.3"))
		 				.setExact(MatchField.IP_PROTO, IpProtocol.of((short)251))
		 				.build();
	    	 
		    	 actionList1.add(myFactory.actions().buildOutput()
		 		    	.setPort(OFPort.CONTROLLER) // raw types replaced with objects for type-checking and readability
		 		    	.setMaxLen(Integer.MAX_VALUE)
		 		    	.build()); // list of immutable OFAction objects
		    	 
		    	 OFFlowAdd flowAddCR1 = my13Factory.buildFlowAdd()
		    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
		    	   		 	.setMatch(MatchCR1)
		    	   		 	.setActions(actionList1)
		    	   		 	.setPriority(10)
		    	   		 	.build();
	    	 
		    	 sw.write(flowAddCR1);
	    	 ///_____________________________________________________________
		   //// ------------------------------------------------------------
		    	 ArrayList<OFAction> actionListCache1 = new ArrayList<OFAction>();
		    	 
		    	 Match MatchCache1=myFactory.buildMatch()
		 				.setExact(MatchField.IN_PORT, OFPort.of(1))
		 				.setExact(MatchField.ETH_TYPE, EthType.IPv4)
		 				.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.1"))
		 				.setExact(MatchField.IP_PROTO, IpProtocol.of((short)250))		 				
		 				.setExact(MatchField.CR_SRC, TransportPort.of(256))
		 				.setExact(MatchField.CR_DST, TransportPort.of(256))
		 				.build();
		    	 
		    	 actionListCache1.add(myFactory.actions().buildOutput()	
		 		    	.setPort(OFPort.of(3)) // raw types replaced with objects for type-checking and readability
		 		    	.build()); // list of immutable OFAction objects
		    	 
		    	 OFFlowAdd flowAddCache1 = my13Factory.buildFlowAdd()
		    	   		 	.setBufferId(OFBufferId.NO_BUFFER)
		    	   		 	.setMatch(MatchCache1)
		    	   		 	.setActions(actionListCache1)
		    	   		 	.setPriority(12)
		    	   		 	.build();
	    	 
		    	// sw.write(flowAddCache1); 
	     	 
	        return Command.CONTINUE;
	    }
	
	
}
    

    
    
    
    
    
    
    
    
    
