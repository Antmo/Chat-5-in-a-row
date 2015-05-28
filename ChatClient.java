import ChatApp.*;          // The package containing our stubs
import org.omg.CosNaming.*; // HelloClient will use the naming service.
import org.omg.CosNaming.NamingContextPackage.*;
import org.omg.CORBA.*;     // All CORBA applications need these classes.
import org.omg.PortableServer.*;   
import org.omg.PortableServer.POA;

import java.util.Scanner;

 
class ChatCallbackImpl extends ChatCallbackPOA
{
    private ORB orb;
    private String name = "Sirius";

    public void setORB(ORB orb_val) 
    {
        orb = orb_val;
    }

    public void callback(String notification)
    {
        System.out.println(notification);
    }

    public void setName(String name)
    {
	this.name = name;
    }
    
    public String getName()
    {
	return this.name;
    }
}

public class ChatClient
{
    static Chat chatImpl;
    static String name;

    public static void main(String args[])
    {
	try {
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null);

	    // create servant (impl) and register it with the ORB
	    ChatCallbackImpl chatCallbackImpl = new ChatCallbackImpl();
	    chatCallbackImpl.setORB(orb);

	    // get reference to RootPOA and activate the POAManager
	    POA rootpoa = 
		POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootpoa.the_POAManager().activate();
	    
	    // get the root naming context 
	    org.omg.CORBA.Object objRef = 
		orb.resolve_initial_references("NameService");
	    NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
	    
	    // resolve the object reference in naming
	    String name = "Chat";
	    chatImpl = ChatHelper.narrow(ncRef.resolve_str(name));
	    
	    // obtain callback reference for registration w/ server
	    org.omg.CORBA.Object ref = 
		rootpoa.servant_to_reference(chatCallbackImpl);
	    ChatCallback cref = ChatCallbackHelper.narrow(ref);
	    
	    // Application code goes below
	    String chat = chatImpl.say(cref, "\n  Hello....");
	    System.out.println(chat);
	    
	    String action;
	    Scanner in = new Scanner(System.in);
	    
	    while(true)
		{
		    action = in.nextLine();
		    executeCommand(cref, action);
		    //		    chatImpl.say(cref, action);
		}
	    
	    
	} catch(Exception e){
	    System.out.println("ERROR : " + e);
	    e.printStackTrace(System.out);
	}
    }

    private static void executeCommand(ChatCallback cref, String action)
    {
	Scanner in = new Scanner(System.in);
	// 	String msg = command.substring(command.indexOf(" "),command.length() );
	//	String action = command.substring(0,command.indexOf(" "));

	if( action.equals("join") )
	    {
		name = in.nextLine();
		if ( !chatImpl.join(cref, name) )
		    name = null;
	    }
	else if( action.equals("post") )
	    chatImpl.post(cref, in.nextLine(), name);
	else if( action.equals("leave") )
	    chatImpl.leave(cref, name);
	else if( action.equals("list") )
	    chatImpl.list(cref);
	else if( action.equals("play") )
	    {
		String marker = in.nextLine();
		chatImpl.play(cref, name, marker.charAt(0));
	    }
	else if( action.equals("quit") )
	    chatImpl.quit(cref, name);
	else if( action.equals("set") )
	    {
		String line = in.nextLine();
		String[] args = line.split(" ");
		chatImpl.set(cref,name,Integer.parseInt(args[0]),Integer.parseInt(args[1]));
	    }
	else
	    chatImpl.say(cref, "command not found");
	return;
    }
}

