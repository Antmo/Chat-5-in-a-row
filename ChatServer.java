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
	
    }; /* end class User */

    class Game {
	protected int HEIGHT = 5;
	protected int WIDTH  = 5;
	protected int free_slots = HEIGHT*WIDTH;
	
	protected char def_mark = '+';
	protected char tm1_mark = 'X';
	protected char tm2_mark = 'O';
	protected char win_mark;

	protected int tm1_score = 0;
	protected int tm2_score = 0;

	protected char[][] gameBoard = new char[HEIGHT][WIDTH];

	public Game()
	{
	    clear_board();
	}

	public void clear_board()
	{
	    for(char[] row : gameBoard )
		Arrays.fill(row, def_mark);
	}

	public boolean set(int x, int y, char marker)
	{
	    int xpos = x-1;
	    int ypos = y-1;

	    if( x > WIDTH  || xpos < 0 || 
		y > HEIGHT || ypos < 0 )
		return false;

	    if (gameBoard[ypos][xpos] == def_mark)
		gameBoard[ypos][xpos] = marker;
	    else
		return false;
		
	    return true;
	}
	
	/* 
	*  Check for winners from the position that was just set.
	*  This should run in O(1) time.
	*/
	private boolean checkWinner(int x, int y, char marker)
	{

	    int xpos = x-1;
	    int ypos = y-1;
	    /* horizontal */
	    int horiz = xpos-4;
	    int step = 0;
	    for (int i = 0; i < 5; ++i )
		{
		    if ( horiz >= 0 && horiz < WIDTH )
			{
			    while ( gameBoard[ypos][horiz] == marker )
				{
				    /*basicly if we managed five in a row*/
				    if (horiz == xpos+step )
					return true;

				    if (++horiz > WIDTH-1 )
					break;
				}
			}
		    ++step;
		    horiz = xpos-4+step;
		}

	    /* vertical */
	    int verti = ypos-4;
	    step = 0;
	    for (int i = 0; i < 5; ++i )
		{
		    if ( verti >= 0 && verti < HEIGHT )
			{
			    while ( gameBoard[verti][xpos] == marker )
				{
				    /*basicly if we managed five in a row*/
				    if (verti == ypos+step )
					return true;

				    if (++verti > HEIGHT-1 )
					break;
				}
			}
		    ++step;
		    verti = ypos-4+step;
		}

	    /* Diagonal check */
	    verti = ypos-4;
	    horiz = xpos-4;
	    step = 0;
	    for ( int i = 0; i < 5; ++i )
		{
		    if ( verti >= 0 && verti < HEIGHT &&
			 horiz >= 0 && horiz < WIDTH )
			{
			    while( gameBoard[verti][horiz] == marker )
				{
				    /*basicly if we managed five in a row*/
				    if( (horiz == xpos+step) ) //no need to check both
					return true;

				    if ( ++horiz > WIDTH-1 || ++verti > HEIGHT-1 )
					break;
				}
			}
		    ++step;
		    horiz = xpos-4+step;
		    verti = ypos-4+step;
		}

	    /* Anti-diagonal */
	    verti = ypos+4;
	    horiz = xpos-4; //this is tricky havnt thought so hard on it
	    step = 0;
	    for ( int i = 0; i < 5; ++i )
		{
		    if ( verti >= 0 && verti < HEIGHT &&
			 horiz >= 0 && horiz < WIDTH )
			{
			    while( gameBoard[verti][horiz] == marker )
				{
				    /*basicly if we managed five in a row*/
				    if( (horiz == xpos+step))
					return true;

				    if ( --horiz < 0 || ++verti > HEIGHT-1 )
					break;
				}
			}
		    ++step;
		    horiz = xpos-4+step; //tricky tricky +4 because of arrays 0-9 etc yadayada not a generic solution
		    verti = ypos-4+step;
		}

	    return false;
	}	

	public void updateScore(char marker)
	{
	    if(marker == 'X')
		++tm1_score;
	    else
		++tm2_score;
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
	}
    }; /* end class Game*/

    private Game theGame = new Game();                /* one instance of this runs on the server */
    private Vector<User> USERS = new Vector<User>();  /* 'name-server' */
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
	    {
		/* Broadcast updated gameboard to all currently active players */
		for( User obj : USERS )
		    {
			if(obj.playing)
			    {
				obj.ref.callback("Game board updated:");
				obj.ref.callback(theGame.print());
			    }
		    }

		if ( theGame.checkWinner(xCoord,yCoord,usr.marker) )
		    {	
			theGame.updateScore(usr.marker);
			theGame.clear_board();

			/* Broadcast reset gameboard to all currently active players */
			for( User obj : USERS )
			    {
				if(obj.playing)
				    {
					obj.ref.callback("Team " + usr.marker + " has won");
					obj.ref.callback("Current score: [X:" + theGame.tm1_score + "] vs [O:" + theGame.tm2_score + "]");
					obj.ref.callback("Restarting the game ...");
					obj.ref.callback(theGame.print());
				    }
			    }
		    }
		elseif( --theGame.free_slots == 0 )
		{
			/* Reset board & broadcast to active players*/
		}
		
	    }
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

    /* 
     * CHAT related functions 
     */
    public String say(ChatCallback callobj, String msg)
    {
        callobj.callback(msg);
	return ("         ....Goodbye!\n");
    }

    public boolean join(ChatCallback callobj,String name)
    {

	User usr = findUser(name);
	if(usr != null)
	    {
		callobj.callback(name + " is taken");
		return false;
	    } 

	USERS.add(new User(callobj,name));
	broadcast(name + " has joined");

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
} /* end class ChatImpl */

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
