import ChatApp.*;          // The package containing our stubs. 
import org.omg.CosNaming.*; // HelloServer will use the naming service. 
import org.omg.CosNaming.NamingContextPackage.*; // ..for exceptions. 
import org.omg.CORBA.*;     // All CORBA applications need these classes. 
import org.omg.PortableServer.*;   
import org.omg.PortableServer.POA;
 
import java.util.Vector;
import java.util.Arrays;


class ChatImpl extends ChatPOA
{
    class User {
    
	String name;
	ChatCallback ref;
	boolean playing = false;
	char marker = '+';

	public User(ChatCallback obj, String msg)
	{
	    name = msg;
	    ref = obj;
	}
	
    };

    class Game {
	// Can I use private in java? ...
	private int HEIGHT = 5;
	private int WIDTH  = 5;
	private char def_mark = '+';
	private char tm1_mark = 'X';
	private char tm2_mark = 'O';
	private char win_mark;

	char[][] gameBoard = new char[HEIGHT][WIDTH];

	public Game()
	{
	    for(char[] row : gameBoard )
		Arrays.fill(row, def_mark);
	}

	public boolean set(int x, int y, char marker)
	{
	    if( x-1 > WIDTH  || x-1 < 0 || 
		y-1 > HEIGHT || y-1 < 0 )
		return false;
	    if (gameBoard[x-1][y-1] == def_mark)
		gameBoard[x-1][y-1] = marker;
	    //	    else
	    //		return false;

	    //checkforwinners 
	    if( checkWinner() )
		{
		    // Broadcast to players that game is over
		    // Reset board
		}
		
	    return true;
	}
	
	private boolean checkWinner()
	{

	    /*  pair<char, int> doesn't exist in java.
	    *   stackoverflow told me to write a new class for it
	    *   but I don't really want to ...
	    *   As a result, alot of the stuff below is hardcoded and ugly
	    */

	    int xcnt = 0;
	    int ocnt = 0;

	    /* Horizontal check */
	    for(int i = 0; i < HEIGHT; ++i)
		{
		    for(int j = 0; i < WIDTH; ++j)
			{
			    if( thegame.gameBoard[i][j] == def_mark )
				xcnt = ocnt = 0;
			    else if( theGame.gameBoard[i][j] == tm1_mark )
				++xcnt;
			    else if( theGame.gameBoard[i][j] == tm2_mark )
				++ocnt;
			}
			
			/* TODO Change these two if's to one using a ternary operator */
		    if(xcnt == 5)
			{
			    this.win_mark = 'X';
			    return true;
			}
		    else if(ocnt == 5)
			{
			    this.win_mark = 'O';
			    return true;
			}
		}

	    /* Vertical check*/
	    for(int i = 0; i < HEIGHT; ++i)
		{
		    for(int j = 0; i < WIDTH; ++j)
			{
			    if( thegame.gameBoard[j][i] == def_mark )
				xcnt = ocnt = 0;
			    else if( theGame.gameBoard[j][i] == tm1_mark )
				++xcnt;
			    else if( theGame.gameBoard[j][i] == tm2_mark )
				++ocnt;
			}
		    if(xcnt == 5)
			{
			    this.win_mark = 'X';
			    return true;
			}
		    else if(ocnt == 5)
			{
			    this.win_mark = 'O';
			    return true;
			}
		}

	    /* Diagonal check */
	    // code here

	    /* Anti diagonal check */
	    //code here
	}
	public String print()
	{
	    String board = "";
	    for( int i = 0; i < HEIGHT; ++i ) 
		{
		    for( int j = 0; j < WIDTH; ++j ) 
			{
			    board += (gameBoard[i][j]) + " ";
			}
		    board += '\n';
		}
	    return board;
		    //		System.out.println(Arrays.toString(gameBoard));
	}
    };

    private Game theGame = new Game();
    private Vector<User> USERS = new Vector<User>();
    private ORB orb;

    public void setORB(ORB orb_val) {
        orb = orb_val;
    }
    
    public void printGameBoard(ChatCallback callobj)
    {
	callobj.callback(theGame.print());
    }

    public void set(ChatCallback callobj, String name, int xCoord, int yCoord)
    {
	User usr = findUser(name);
	if(usr != null && !usr.playing)
	    {
		say(callobj, "You're not in a game, type 'play {X, O}' to play");
		return;
	    }
	if( theGame.set(xCoord,yCoord,usr.marker) )
	    printGameBoard(callobj);
	else
	    say(callobj, "Invalid move");

	return;
    }

    public User findUser(String name)
    {
	for ( User obj : USERS )
	    {
		if (obj.name.equals(name))
		    return obj;
	    } 
	return null;
    }

    /* CHAT */
    public String say(ChatCallback callobj, String msg)
    {
        callobj.callback(msg);
	return ("         ....Goodbye!\n");
    }

    public boolean join(ChatCallback callobj, String name)
    {
	User usr = findUser(name);

	if(usr != null)
	    {
		callobj.callback(name + " is taken");
		return false;
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
	User usr = findUser(name);

	if(usr != null)
	    {
		USERS.remove(USERS.indexOf(usr));	    
		broadcast(name + " has left.");
	    }
	
	return;
    }
    
    public void list(ChatCallback callobj)
    {
	callobj.callback("Users online:");
	for ( User obj : USERS )
	    callobj.callback(obj.name);

	return;
    }

    public void play(ChatCallback callobj, String name, char marker)
    {
	User usr = findUser(name);

	if(usr == null)
	    {
		say(callobj, "Please join the chat before playing a game.");
	    }
	else if( usr.playing )
	    {
		say(callobj, "You're already in a game. Type 'quit' to leave");
	    }
	else
	    {
		usr.playing = true;

		if(marker == 'X' || marker == 'O')
		    usr.marker = marker;
		else
		    usr.marker = 'X';

		say(callobj, "Welcome to the game. Your marker is: " + usr.marker);
		printGameBoard(callobj);
	    }
	return;
    }
    
    public void quit(ChatCallback objref, String name)
    {
	User usr = findUser(name);

	if( usr == null )
	    {
		say(objref, "You're not in a chat");
	    }
	else if ( !usr.playing )
	    say(objref, "You're not in a game");
	else
	    {
		usr.playing = false;
		say(objref, "Left the game");
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
