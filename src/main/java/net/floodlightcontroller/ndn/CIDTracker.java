package net.floodlightcontroller.ndn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.projectfloodlight.openflow.protocol.OFBucket;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
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
import net.floodlightcontroller.mactracker.MACTracker;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.staticentry.IStaticEntryPusherService;
//net.floodlightcontroller.ndn.CIDTracker
public class CIDTracker implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected IStaticEntryPusherService entrypushers;
	protected static Logger logger;
	private boolean pusher_added =false;
	public static int FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static int FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static int FLOWMOD_DEFAULT_PRIORITY = 1;
	protected static TableId FLOWMOD_DEFAULT_TABLE_ID = TableId.ZERO;
	
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		
		return CIDTracker.class.getSimpleName();
		
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
		DatapathId datapathid = DatapathId.of("00:00:00:00:00:00:00:01");
		//check if pusher not added => add
		if(!this.pusher_added){
		if(sw.getId().equals(datapathid)){
			this.addStaticFlow(sw);
			System.out.println("static pusher added on "+sw.getId().toString());
			this.pusher_added=true;
		}
		}
		       
        return Command.CONTINUE;
	}
	
    public void addStaticFlow(IOFSwitch mySwitch){
    	OFFactory myFactory=mySwitch.getOFFactory();
		OFVersion myOFVersion = myFactory.getVersion();
		//System.out.println(myOFVersion.toString());
		
		
		//first we will add flowmod to direct packet to group to duplicate packet
		Match myMatch=myFactory.buildMatch()
				//.setExact(MatchField.IN_PORT, OFPort.IN_PORT)//or OFPort.of(1)
				.setExact(MatchField.IN_PORT, OFPort.of(1))
				.setExact(MatchField.ETH_TYPE, EthType.IPv4)
				.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("10.0.0.0/24"))
			    .setExact(MatchField.IP_PROTO, IpProtocol.of((short)250))
			    //.setExact(MatchField.UDP_DST, arg1)
				.build();
		if(!MatchField.IPV4_SRC.arePrerequisitesOK(myMatch))
			System.out.println("error in myMatch");
		
		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		ArrayList<OFInstruction> instructionList =new ArrayList<OFInstruction>();
		OFInstructions instructions = myFactory.instructions();
		OFActions actions = myFactory.actions();
		
		//OFActionGroup actgroup=actions.group(OFGroup.of(1));
		
		/*
		 *
		 * ***example flowmod from forwrding class
		fmb.setCookie(cookie)
        .setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
        .setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
        .setBufferId(OFBufferId.NO_BUFFER) 
        .setMatch(m)
        .setPriority(FLOWMOD_DEFAULT_PRIORITY);*/
		
				
		
		/*
		OFActionSetDlDst setDlDst = actions.buildSetDlDst()
			    .setDlAddr(MacAddress.of("ff:ff:ff:ff:ff:ff"))
			    .build();
			actionList.add(setDlDst);
		OFActionGroup actgroup=actions.group(OFGroup.of(1));
		
		//may be use output in action
			/*
			 OFActionOutput output = actions.buildOutput()
    .setMaxLen(0xFFffFFff)
    .setPort(OFPort.of(1))
    .build();
    actionList.add(output);*/
		
		    ArrayList<OFAction> actionGroup_B1 = new ArrayList<OFAction>();
		    OFActions actionsinGroup1 = myFactory.actions();
		    ArrayList<OFAction> actionGroup_B2 = new ArrayList<OFAction>();
		    OFActions actionsinGroup2 = myFactory.actions();
		    OFOxms oxms =myFactory.oxms();
		  
		    ///////////  action to set cache ip 
		    OFActionSetField setNwDst = actions.buildSetField()
		    	    .setField(
		    	        oxms.buildIpv4Dst()
		    	        .setValue(IPv4Address.of("10.0.0.3"))//here set cache ip 
		    	        //or we can set output port to port cache in new openflow action output
		    	        
		    	        .build()
		    	    )
		    	    .build();
		    
		   OFActionOutput outputbucket1=actionsinGroup1.buildOutput()
				   .setPort(OFPort.ALL)///if switch doesn't support normal , we have to select portnumber[.of()]
				   .setMaxLen(Integer.MAX_VALUE)
				   .build();
		   
		   
		   actionGroup_B1.add(outputbucket1);///here we must make new output to cache port
		   actionGroup_B1.add(setNwDst);
		   actionGroup_B2.add(outputbucket1);
		    
		    
			ArrayList<OFBucket> bucketList = new ArrayList<OFBucket>();
			//ArrayList<OFBucket> singleBucket = new ArrayList<OFBucket>();
			
			OFBucket myBucket1 = myFactory.buildBucket()
				    .setActions(actionGroup_B1)
				    .setWatchGroup(OFGroup.ANY) /* ANY --> don't care / wildcarded. */
				    .setWatchPort(OFPort.ANY) /* ANY --> don't care / wildcarded. */
				    .build();
			OFBucket myBucket2 = myFactory.buildBucket()
				    .setActions(actionGroup_B2)
				    .setWatchGroup(OFGroup.ANY) /* ANY --> don't care / wildcarded. */
				    .setWatchPort(OFPort.ANY) /* ANY --> don't care / wildcarded. */
				    .build();
			bucketList.add(myBucket1);
			bucketList.add(myBucket2);
			
			
			
			OFActionGroup actgroup=actions.buildGroup().setGroup(OFGroup.of(1)).build();//if this stmt doesn't work use stmt before
			
			OFInstructionApplyActions applyActions = instructions.buildApplyActions()
				    .setActions(actionList)
				    .build();
			instructionList.add(applyActions);
			
			//ArrayList<OFGroupMod> groupMods = new ArrayList<OFGroupMod>();
			OFGroupAdd addGroup = myFactory.buildGroupAdd()
				    .setGroup(OFGroup.of(1))
				    .setGroupType(OFGroupType.ALL)
				    .setBuckets(bucketList) /* Will be included with the new OFGroup. */
				    .build();
			
			OFFlowAdd flowAdd = myFactory.buildFlowAdd()
					.setBufferId(OFBufferId.NO_BUFFER)
					.setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
					.setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT)
					//.setPriority(FLOWMOD_DEFAULT_PRIORITY)//1
					.setPriority(1)
					.setMatch(myMatch)
					.setInstructions(instructionList)
					//.setTableId(FLOWMOD_DEFAULT_TABLE_ID)//0
					.setTableId(TableId.of(0))
					.build();
			//groupMods.add(addGroup);
			//OFActionGroup setGroup=actions.g
			 
			// 00:00:00:00:00:00:00:01 
		
		
		
			//IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(1));
			//mySwitch.write(groupMods);
			
			//mySwitch.write((OFMessage) groupMods);
			
			/// add static by entrypusher  --proactivly
			//entrypushers.addFlow("CID", flowAdd, mySwitch.getId());
			//entrypushers.addGroup("group_cid", addGroup, mySwitch.getId());
			
			//or we can add reactivly by write on switch
			mySwitch.write(flowAdd);
			mySwitch.write(addGroup);
		
	}

}
