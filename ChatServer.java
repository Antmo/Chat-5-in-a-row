import ChatApp.*;          // The package containing our stubs. 
import org.omg.CosNaming.*; // HelloServer will use the naming service. 
import org.omg.CosNaming.NamingContextPackage.*; // ..for exceptions. 
import org.omg.CORBA.*;     // All CORBA applications need these classes. 
import org.omg.PortableServer.*;   
import org.omg.PortableServer.POA;
 
import java.util.Vector;

class ChatImpl extends ChatPOA
{
    class User {
    
	String name;
	ChatCallback ref;

	public User(ChatCallback obj, String msg)
	{
	    name = msg;
	    ref = obj;
	}
	
    };
    
    private Vector<User> USERS = new Vector<User>();
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }

    public String say(ChatCallback callobj, String msg)
    {
        callobj.callback(msg);
        return ("         ....Goodbye!\n");
    }

    public boolean join(ChatCallback callobj, String name)
    {
	for ( User obj : USERS )
	    {
		System.out.println(obj.name);
		if (obj.name.equals(name))
		    {
			callobj.callback(name + " is taken");
			return false;
		    }
	    } 
	USERS.add(new User(callobj,name));
	broadcast(name + " has joined");

	callobj.setName(name);
	return true;
    }

    public void post(ChatCallback callobj, String msg, String name)
    {
	broadcast(name +" says: "+  msg);
    }

    public void leave(ChatCallback callobj, String name)
    {
	for( User obj : USERS )
	    {
		if(obj.name.equals(name))
		    {
			//			unsigned index = USERS.indexOf(obj);
			USERS.remove(USERS.indexOf(obj));
			broadcast(name + " has left.");
			return;
		    }
	    }
    }
    private void broadcast(String msg)
    {
	for( User obj : USERS )
	    {
		obj.ref.callback(msg);
	    }
    }
}

public class ChatServer 
{
    public static void main(String args[]) 
    {
	try { 
	    // create and initialize the ORB
	    ORB orb = ORB.init(args, null); 

	    // create servant (impl) and register it with the ORB
	    ChatImpl chatImpl = new ChatImpl();
	    chatImpl.setORB(orb);
	    
	    // get reference to rootpoa & activate the POAManager
	    POA rootpoa = 
		POAHelper.narrow(orb.resolve_initial_references("RootPOA"));  
	    rootpoa.the_POAManager().activate(); 

	    // get the root naming context
	    org.omg.CORBA.Object objRef = 
		           orb.resolve_initial_references("NameService");
	    NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

	    // obtain object reference from the servant (impl)
	    org.omg.CORBA.Object ref = 
		rootpoa.servant_to_reference(chatImpl);
	    Chat cref = ChatHelper.narrow(ref);

	    // bind the object reference in naming
	    String name = "Chat";
	    NameComponent path[] = ncRef.to_name(name);
	    ncRef.rebind(path, cref);

	    // Application code goes below
	    System.out.println("ChatServer ready and waiting ...");
	    
	    // wait for invocations from clients
	    orb.run();
	}
	    
	catch(Exception e) {
	    System.err.println("ERROR : " + e);
	    e.printStackTrace(System.out);
	}

	System.out.println("ChatServer Exiting ...");
    }

}
