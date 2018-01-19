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
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
//net.floodlightcontroller.ndn.CIDTrack

//net.floodlightcontroller.forwarding.Forwarding
//net.floodlightcontroller.learningswitch.LearningSwitch
//net.floodlightcontroller.routing.RoutingManager
public class CIDTrackARP implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected IStaticEntryPusherService entrypushers;
	protected static Logger logger;
	private boolean pusher_added =false;
	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 1;
	protected static TableId FLOWMOD_DEFAULT_TABLE_ID = TableId.ZERO;
	
	/////
	// more flow-mod defaults
		
		protected static short FLOWMOD_PRIORITY = 100;
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		
		return CIDTrackARP.class.getSimpleName();
		
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
	    	ArrayList<OFAction> actions = new ArrayList<OFAction>();
	    	actions.add(myFactory.actions().buildOutput()
	    	.setPort(OFPort.of(2)) // raw types replaced with objects for type-checking and readability
	    	.build()); // list of immutable OFAction objects


	    	 OFFactory my13Factory = OFFactories.getFactory(OFVersion.OF_13);
	    	
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
	     	 
	        return Command.CONTINUE;
	    }
	
	}
    

    
    
    
    
    
    
    
    
    
