package server;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.rmi.server.ServerNotActiveException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Queue;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import Exceptions.InvalidEmailException;
import Exceptions.InvalidPasswordException;
import database.QueriesManager;
import database.PredefinedSQLCode.Colonne;
import enumclass.QueryParameter;
import enumclass.ServerServicesName;
import interfaces.SocketService;
import objects.Account;
import objects.Album;
import objects.Song;
import utility.TimeFormatter;
import utility.WaithingAnimationThread;

public class ComunicationManager extends Thread implements SocketService, Serializable
{
	protected ArrayList<ConnectionHandler> clientsThread = new ArrayList<>();
	private HashMap<ServerServicesName, Function<HashMap<String, Object>,Object>> serverFunctions = new HashMap<>();
	private HashMap<ServerServicesName, String[]> functionParametreKeys = new HashMap<>();
	//private HashMap<Function<HashMap<String, Object>,Object>, Method> functionName = new HashMap<>();
	private HashMap<ServerServicesName, String> functionName = new HashMap<>();
	private HashMap<QueryParameter, Colonne> QueryParametre_to_Colonne = new HashMap<>();

	private Terminal terminal;
	private boolean exit = false;
	private int port;

	/**
	 * Costruttore della classe
	 * @param port
	 * @throws RemoteException
	 */
	public ComunicationManager(int port) throws RemoteException {
		this.terminal = Terminal.getInstance();
		this.port = port;
		//setDaemon(true);
		setPriority(MAX_PRIORITY);


		//hashMap che associa a ogni servizio la sua funzione
		/////////////////////////////////////////////////////////////
		//Account
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.ADD_ACCOUNT, this::addAccount);
		serverFunctions.put(ServerServicesName.GET_ACCOUNT, this::getAccount);
		serverFunctions.put(ServerServicesName.DELETE_ACCOUNT, this::deleteAccount);

		/////////////////////////////////////////////////////////////
		//SONGs
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.GET_MOST_POPULAR_SONGS, this::getMostPopularSongs);
		serverFunctions.put(ServerServicesName.SEARCH_SONGS, this::searchSongs);
		serverFunctions.put(ServerServicesName.GET_SONG_BY_IDS, this::getSongByIDs);
		serverFunctions.put(ServerServicesName.GET_ALBUM_SONGS, this::getAlbumsSongs);
		
		
		/////////////////////////////////////////////////////////////
		//ALBUMs
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.SEARCH_ALBUMS, this::searchAlbums);
		serverFunctions.put(ServerServicesName.GET_RECENT_PUPLISCED_ALBUMS, this::getRecentPublischedAlbum);
		serverFunctions.put(ServerServicesName.GET_ALBUM_BY_ID, this::getAlbumByID);
		
		
		/////////////////////////////////////////////////////////////
		//ARTISTs
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.GET_ARTIST_SONGS, this::getArtistSongs);		
		serverFunctions.put(ServerServicesName.GET_ARTIST_ALBUMS, this::getArtistAlbums);
		serverFunctions.put(ServerServicesName.SEARCH_ARTISTS, this::searchArtists);
		serverFunctions.put(ServerServicesName.GET_ARTIST_BY_ID, this::getArtistsByIDs);


		


		/////////////////////////////////////////////////////////////
		//PLAYLIST
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.ADD_PLAYLIST, this::addPlaylist);
		serverFunctions.put(ServerServicesName.GET_ACCOUNT_PLAYLIST, this::getAccountsPlaylists);
		serverFunctions.put(ServerServicesName.ADD_SONG_PLAYLIST, this::addSongToPlaylist);
		serverFunctions.put(ServerServicesName.REMOVE_SONG_PLAYLIST, this::removeSongFromPlaylist);
		serverFunctions.put(ServerServicesName.RENAME_PLAYLIST, this::renamePlaylist);
		serverFunctions.put(ServerServicesName.DELETE_PLAYLIST, this::deletePlaylist);
		serverFunctions.put(ServerServicesName.GET_PLAYLIST_SONGS, this::getPlaylistSongs);
		
		/////////////////////////////////////////////////////////////
		//EMOTIONS
		////////////////////////////////////////////////////////////
		serverFunctions.put(ServerServicesName.ADD_EMOTION, this::addEmotion);
		serverFunctions.put(ServerServicesName.REMOVE_EMOTION, this::deleteEmotion);
		serverFunctions.put(ServerServicesName.GET_SONG_EMOTION, this::getSongEmotion);
		serverFunctions.put(ServerServicesName.GET_ACCOUNT_EMOTIONS, this::getAccountEmotion);
		
		
		//============================================================================================================//
		//hashMap che associa a ogni servizio i parametri richiesti
		/////////////////////////////////////////////////////////////
		//Account
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.ADD_ACCOUNT, 					new String[] {QueryParameter.NAME.toString(), QueryParameter.USERNAME.toString(), QueryParameter.USER_ID.toString(), QueryParameter.CODICE_FISCALE.toString(), QueryParameter.EMAIL.toString(), QueryParameter.PASSWORD.toString(), QueryParameter.CIVIC_NUMBER.toString(), QueryParameter.VIA_PIAZZA.toString(), QueryParameter.CAP.toString(), QueryParameter.COMMUNE.toString(), QueryParameter.PROVINCE.toString()});  
	    functionParametreKeys.put(ServerServicesName.GET_ACCOUNT, 					new String[]{QueryParameter.EMAIL.toString(), QueryParameter.PASSWORD.toString()}); 
		functionParametreKeys.put(ServerServicesName.DELETE_ACCOUNT, 				new String[]{QueryParameter.ACCOUNT_ID.toString()});
		
		
		/////////////////////////////////////////////////////////////
		//SONGs
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.GET_MOST_POPULAR_SONGS, 		new String[]{QueryParameter.LIMIT.toString(), QueryParameter.OFFSET.toString()});
		functionParametreKeys.put(ServerServicesName.SEARCH_SONGS, 					new String[]{QueryParameter.SEARCH_STRING.toString(), QueryParameter.LIMIT.toString(), QueryParameter.OFFSET.toString(), QueryParameter.MODE.toString()}); 
		functionParametreKeys.put(ServerServicesName.GET_SONG_BY_IDS, 				new String[]{QueryParameter.ID.toString()});
		
		
		/////////////////////////////////////////////////////////////
		//ALBUMs
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.GET_RECENT_PUPLISCED_ALBUMS, 	new String[]{QueryParameter.LIMIT.toString(), QueryParameter.OFFSET.toString(), QueryParameter.THRESHOLD.toString()}); 
		functionParametreKeys.put(ServerServicesName.SEARCH_ALBUMS, 				new String[]{QueryParameter.SEARCH_STRING.toString(), QueryParameter.LIMIT.toString(), QueryParameter.OFFSET.toString()}); 
		functionParametreKeys.put(ServerServicesName.GET_ALBUM_SONGS, 				new String[]{QueryParameter.ALBUM_ID.toString()}); 
		functionParametreKeys.put(ServerServicesName.GET_ALBUM_BY_ID, 				new String[]{QueryParameter.ID.toString()});
		
		
		/////////////////////////////////////////////////////////////
		//ARTISTs
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.GET_ARTIST_BY_ID, 				new String[]{QueryParameter.ARTIST_ID.toString()});
		functionParametreKeys.put(ServerServicesName.GET_ARTIST_ALBUMS, 			new String[]{QueryParameter.ARTIST_ID.toString()});
		functionParametreKeys.put(ServerServicesName.GET_ARTIST_SONGS, 				new String[]{QueryParameter.ARTIST_ID.toString()});
		functionParametreKeys.put(ServerServicesName.SEARCH_ARTISTS, 				new String[]{QueryParameter.SEARCH_STRING.toString(), QueryParameter.LIMIT.toString(), QueryParameter.OFFSET.toString()});



		/////////////////////////////////////////////////////////////
		//PLAYLIST
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.ADD_PLAYLIST, 					new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.PLAYLIST_NAME.toString()});
		functionParametreKeys.put(ServerServicesName.GET_ACCOUNT_PLAYLIST, 			new String[]{QueryParameter.ACCOUNT_ID.toString()});
		functionParametreKeys.put(ServerServicesName.ADD_SONG_PLAYLIST, 			new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.PLAYLIST_ID.toString(), QueryParameter.SONG_ID.toString()});
		functionParametreKeys.put(ServerServicesName.REMOVE_SONG_PLAYLIST, 			new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.PLAYLIST_ID.toString(), QueryParameter.SONG_ID.toString()});
		functionParametreKeys.put(ServerServicesName.RENAME_PLAYLIST, 				new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.PLAYLIST_ID.toString(), QueryParameter.NEW_NAME.toString()});
		functionParametreKeys.put(ServerServicesName.GET_PLAYLIST_SONGS, 			new String[]{QueryParameter.PLAYLIST_ID.toString()});


		/////////////////////////////////////////////////////////////
		//EMOTIONS
		////////////////////////////////////////////////////////////
		functionParametreKeys.put(ServerServicesName.ADD_EMOTION, 					new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.SONG_ID.toString(), QueryParameter.EMOZIONE.toString(), QueryParameter.COMMENT.toString(), QueryParameter.VAL_EMOZIONE.toString()});
		functionParametreKeys.put(ServerServicesName.REMOVE_EMOTION, 				new String[]{QueryParameter.ID.toString()});
		functionParametreKeys.put(ServerServicesName.GET_COMMENTS_SONG_FOR_ACCOUNT, new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.SONG_ID.toString()});
		functionParametreKeys.put(ServerServicesName.DELETE_PLAYLIST, 				new String[]{QueryParameter.ACCOUNT_ID.toString(), QueryParameter.PLAYLIST_ID.toString()});
		functionParametreKeys.put(ServerServicesName.GET_SONG_EMOTION, 				new String[]{QueryParameter.SONG_ID.toString()});
		functionParametreKeys.put(ServerServicesName.GET_ACCOUNT_EMOTIONS, 			new String[]{QueryParameter.ACCOUNT_ID.toString()});

		
		
		functionParametreKeys.put(ServerServicesName.GET_COMMENTS_SONG, 			new String[]{QueryParameter.SONG_ID.toString()});
		

		try {
			//hashMap che associa a ogni servizio il nome della sua funzione
			functionName.put(ServerServicesName.ADD_ACCOUNT, "addAccount");
			functionName.put(ServerServicesName.GET_ACCOUNT, "getAccount");
			functionName.put(ServerServicesName.GET_MOST_POPULAR_SONGS, "getMostPopularSongs");
			functionName.put(ServerServicesName.GET_RECENT_PUPLISCED_ALBUMS, "getRecentPublischedAlbum");
			functionName.put(ServerServicesName.SEARCH_SONGS, "searchSongs");
			functionName.put(ServerServicesName.SEARCH_ALBUMS, "searchAlbums");
			functionName.put(ServerServicesName.GET_SONG_BY_IDS, "getSongByIDs");
			functionName.put(ServerServicesName.GET_ALBUM_SONGS, "getAlbumsSongs");
			functionName.put(ServerServicesName.ADD_PLAYLIST, "addPlaylist");
			functionName.put(ServerServicesName.GET_ACCOUNT_PLAYLIST, "getAccountsPlaylistsBy");
			functionName.put(ServerServicesName.ADD_SONG_PLAYLIST, "addSongToPlaylist");
			functionName.put(ServerServicesName.REMOVE_SONG_PLAYLIST, "removeSongFromPlaylist");
			functionName.put(ServerServicesName.RENAME_PLAYLIST, "renamePlaylist");
			functionName.put(ServerServicesName.ADD_EMOTION, "addEmotion");
			functionName.put(ServerServicesName.REMOVE_EMOTION, "deleteComment");
			functionName.put(ServerServicesName.GET_COMMENTS_SONG_FOR_ACCOUNT, "getAccountComments");
			functionName.put(ServerServicesName.GET_COMMENTS_SONG, "getSongComments");
			functionName.put(ServerServicesName.GET_ACCOUNT_EMOTIONS, "getAccountComments");
			functionName.put(ServerServicesName.GET_SONG_EMOTION, "getSongEmotion");
			functionName.put(ServerServicesName.DELETE_PLAYLIST, "deletePlaylist");
			functionName.put(ServerServicesName.DELETE_ACCOUNT, "deleteAccount");
			functionName.put(ServerServicesName.GET_ARTIST_SONGS, "getArtistSongs");
			functionName.put(ServerServicesName.GET_PLAYLIST_SONGS, "getPlaylistsSongs");
			functionName.put(ServerServicesName.GET_ALBUM_BY_ID, "getAlbumsByIDs");
			functionName.put(ServerServicesName.GET_ARTIST_ALBUMS, "getArtistAlbums");
			functionName.put(ServerServicesName.SEARCH_ARTISTS, "searchArtist");
			functionName.put(ServerServicesName.GET_ARTIST_BY_ID, "getArtistByID");
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		QueryParametre_to_Colonne.put(QueryParameter.ACCOUNT_ID, Colonne.ACCOUNT_ID_REF);
		QueryParametre_to_Colonne.put(QueryParameter.SONG_ID, Colonne.SONG_ID_REF);
		QueryParametre_to_Colonne.put(QueryParameter.EMOZIONE, Colonne.TYPE);
		QueryParametre_to_Colonne.put(QueryParameter.COMMENT, Colonne.COMMENTO);
		QueryParametre_to_Colonne.put(QueryParameter.VAL_EMOZIONE, Colonne.VALUE);

	}

	private HashMap<Colonne, Object> convertFromQueryParametre2Colonne(final HashMap<String, Object> argsTable, boolean addID_colum)
	{
		HashMap<Colonne, Object> ColonneValore = new HashMap<Colonne, Object>();

		for(String key : argsTable.keySet()) {
			//System.out.println(key);
			ColonneValore.put(QueryParametre_to_Colonne.get(QueryParameter.valueOf(key)), argsTable.get(key));
		}
			

		if(addID_colum)
			ColonneValore.put(Colonne.ID, QueriesManager.generate_ID_from_Time());

		return ColonneValore;
	}

	/**
	 * Funzione che serve per eseguire i servizi del server
	 * @param name Nome del servizio
	 
	 * @param clientIP L'IP dell'host
	 * @return
	 */
	//* @param params Parametri del servizio ( HashMap<String, Object> )
	public Object executeServerServiceFunction(final ServerServicesName name, final HashMap<String, Object> params, final String clientIP) 
	{
		Function<HashMap<String, Object>,Object> function = this.serverFunctions.get(name);
		double startTime = System.nanoTime();
		double end = 0;
		
		
		//verifico se tutti i parametri sono corretti
		if(!testParametre(params, functionParametreKeys.get(name))) 
			return new Exception("Missing argument");

		//eseguo le operazioni
		try {
			//printFunctionArgs(function, clientIP);	
			
			Object output =  function.apply(params);

			if(output instanceof SQLException) {
				System.out.println((SQLException) output);
				//terminal.printErrorln(((SQLException) output).toString());
			}
			
			if(output instanceof Exception) {
				throw (Exception) output;
			}
			return output;
		}
		catch (Exception e) {
			printError(e);
            e.printStackTrace();
			return e; 
        }
		finally {
			end = System.nanoTime();
			printFunctionExecutionTime(functionName.get(name), clientIP,  end - startTime);
		}
	}


	public static String getMachineIP()
	{
		String IP = "";
		try(final DatagramSocket socket = new DatagramSocket()){
			socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
			IP = socket.getLocalAddress().getHostAddress();
		}
		catch(Exception e) {

		}
		return IP;
	}


	
	public void run() 
	{
		ServerSocket server = null;
		String IP = getMachineIP();


		


		terminal.printInfoln("Start comunication inizilization");
		terminal.startWaithing(Terminal.MessageType.INFO + " Starting server...");
		try {Thread.sleep(ThreadLocalRandom.current().nextInt(400, 1000));} catch (Exception e) {}

		try {
			if(IP == "")
				IP = getPrivateIPv4();
			//IP = InetAddress.getLocalHost().getHostAddress();
		} 
		catch (Exception e) {
			terminal.printErrorln(e.toString());
			e.printStackTrace();
			return;
		}

		
		terminal.printInfoln("Start SOCKET configuration:");
		terminal.printInfoln("ServerSocket creation on port " + port);
		
		try {Thread.sleep(ThreadLocalRandom.current().nextInt(400, 1000));} catch (Exception e) {}
		try {
			server = new ServerSocket(port);
			server.setSoTimeout(500);

			//IP = server.getInetAddress()
		} 
		catch (Exception e) {
			terminal.printErrorln(e.toString());
			e.printStackTrace();
			terminal.stopWaithing();
			return;
		}

		terminal.stopWaithing();
		terminal.printSeparator();
		terminal.printSuccesln(Terminal.Color.GREEN_BOLD_BRIGHT + "Server initialization complete" + Terminal.Color.RESET);
		terminal.printSeparator();
		terminal.printInfoln("Server listening on "+ Terminal.Color.MAGENTA + IP + " : " + port + Terminal.Color.RESET);
		terminal.printInfoln("press ENTER to stop the server");
		terminal.setAddTime(true);
		terminal.startWaithing(Terminal.MessageType.INFO + " Server Running", WaithingAnimationThread.Animation.DOTS);
		terminal.printLine();

			
		while (!exit) 
		{
			try {
				//ho impostato un timeout di 500ms
				Socket clientSocket = server.accept();
				if(clientSocket == null) 
					continue;

				terminal.printInfoln("Connection established with: " + Terminal.Color.MAGENTA + clientSocket.getInetAddress().getHostAddress() + Terminal.Color.RESET);
				
				ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket, this);
				connectionHandler.start();
				clientsThread.add(connectionHandler);
			
			} 
			catch (java.io.InterruptedIOException e) {
				continue;
			}
			catch (IOException e) {
				terminal.printErrorln(e.toString());
				e.printStackTrace();
			}

		}	
		
		terminal.printInfoln("Closing Server...");

		//faccio una copia per evitare errori
		ArrayList<ConnectionHandler> temp = (ArrayList<ConnectionHandler>) clientsThread.clone();
		for(ConnectionHandler client : clientsThread)
			client.terminate();

		if(temp.size() > 0)
			terminal.printInfoln("waith for clients thread...");

		for(ConnectionHandler client : temp)
			while(client.isAlive());
		
			
		try {
			while(!server.isClosed()) {
				server.close();
			}	
		} 
		catch (IOException e) {
			terminal.printErrorln(e.toString());
			e.printStackTrace();
			return;
		}
		
		terminal.printInfoln("server is close: " + server.isClosed());
		terminal.stopWaithing();
		terminal.setAddTime(false);	
	}

	public void terminate() {
		this.exit = true;
		Thread.currentThread().interrupt();
	}

	protected void removeClientSocket(ConnectionHandler connectionHandler) {
		this.clientsThread.remove(connectionHandler);
		new Thread(() -> {
			terminal.printInfoln("host disconected: " + Terminal.Color.MAGENTA + connectionHandler.getSocket().getInetAddress().getHostAddress() + Terminal.Color.RESET);
		}).start();
	}
// ==================================== UTILITY ====================================//

	@SuppressWarnings("unused")
	public static String getPrivateIPv4() throws UnknownHostException, SocketException 
	{
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		String ipToReturn = null;
		while(e.hasMoreElements())
		{
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements())
			{
				InetAddress i = (InetAddress) ee.nextElement();
				String currentAddress = i.getHostAddress();
				Terminal.getInstance().printInfoln("IP address "+currentAddress+ " found");

				//i.isSiteLocalAddress()&&!i.isLoopbackAddress() && validate(currentAddress)
				if(validate(currentAddress)){
					ipToReturn = currentAddress;    
				}else{
					//System.out.println("Address not validated as public IPv4");
				}

			}
		}

		return ipToReturn;
	}

	private static final Pattern IPv4RegexPattern = Pattern.compile(
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

	public static boolean validate(final String ip) {
		return IPv4RegexPattern.matcher(ip).matches();
	}

	private void printFunctionExecutionTime(String function, String clientHost, double dt) {
		new Thread(() ->{
			//Method f = this.functionName.get(function);
			terminal.printInfoln(formatFunctionRequestTime(clientHost, function, dt));
		}).start();
	}

	private void printFunctionArgs(Function functionName, String clientHost) {

		/*new Thread(() ->{
			try {
				//terminal.printRequestln(formatFunctionRequest(clientHost, "MostPopularSongs("+limit+ ", " + offset +")"));
				//terminal.printRequestln(formatFunctionRequest(clientHost, functionName));
				
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}).start();*/
	}


	private String formatFunctionRequest(String clientHost, String function) {
		return "Host " + Terminal.Color.MAGENTA + clientHost + Terminal.Color.RESET + " requested function:" + Terminal.Color.CYAN_BOLD_BRIGHT + "\"" + function + "\"" + Terminal.Color.RESET;
	}

	private String formatFunctionRequestTime(String clientHost, String function, double dt) {

		String timeStr = "";
		Terminal.Color color = null;
		double seconds = (double) (dt / 1000000000.0);

		if(seconds <= 0.400) 
			color = Terminal.Color.GREEN_BOLD_BRIGHT;
		else if(seconds <= 0.800) 
			color = Terminal.Color.YELLOW_BOLD_BRIGHT;
		else if(seconds >= 0.800) 
			color = Terminal.Color.RED_BOLD_BRIGHT;

		timeStr = "  executed in " + color + TimeFormatter.formatTime(dt) + Terminal.Color.RESET;
		return "Host: " + Terminal.Color.MAGENTA + clientHost + Terminal.Color.RESET + ", function " + Terminal.Color.CYAN_BOLD_BRIGHT + "\"" + function + "\"" + Terminal.Color.RESET + timeStr;
	}


	private void printError(Exception e) {
		new Thread(() -> {
			try {
				Terminal.getInstance().printErrorln("Host " + Terminal.Color.MAGENTA + RemoteServer.getClientHost() + Terminal.Color.RESET + " error: " + e);
			} catch (ServerNotActiveException e1) {
				e1.printStackTrace();
			}
		}).start();
	}




	//====================================== SOCKET SERVICES ======================================//
	
	/**
     * Tests whether the provided arguments match the expected keys and have the correct size.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing arguments for an operation.
     * @param keys An array of keys expected in the argsTable.
     * @return {@code true} if the arguments are valid, {@code false} otherwise.
     */
	private boolean testParametre(HashMap<String, Object> argsTable, String[] keys) {
		
		if(argsTable.size() != keys.length)
			return false;
		
		//verifico che ci siano tutti i parametri necessari
		for (String str : keys) {
			if (!argsTable.containsKey(str)) {
				new Thread(() -> {
					terminal.printErrorln("Missing argument: " + str);
				}).start();
				return false;
			}
		}
		return true;
	}
	
	
	//---------------------------- operazioni con Account ---------------------------- //
	/**
     * Adds a new account based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for adding an account.
     * @return An object representing the result of the operation or an error message.
     */
	@Override
	public Object addAccount(final HashMap<String, Object> argsTable) 
	{
		try {
			HashMap<Colonne, Object> colonne_account   = new HashMap<Colonne, Object>();
			HashMap<Colonne, Object> colonne_residenza = new HashMap<Colonne, Object>();
			Account account = null;

			account = QueriesManager.getAccountByEmail((String) argsTable.get(QueryParameter.EMAIL.toString()));
			
			if(account != null) {
				return enumclass.ErrorString.INVALID_EMAIL.name();
			}

			account = QueriesManager.getAccountByNickname((String) argsTable.get(QueryParameter.USER_ID.toString()));

			if(account != null) {
				return enumclass.ErrorString.INVALID_NICKNAME.name();
			}

		
			colonne_account.put(Colonne.NAME, argsTable.get(QueryParameter.NAME.toString()));
			colonne_account.put(Colonne.SURNAME, argsTable.get(QueryParameter.USERNAME.toString()));
			colonne_account.put(Colonne.NICKNAME, argsTable.get(QueryParameter.USER_ID.toString()));
			colonne_account.put(Colonne.FISCAL_CODE, argsTable.get(QueryParameter.CODICE_FISCALE.toString()));
			colonne_account.put(Colonne.EMAIL, argsTable.get(QueryParameter.EMAIL.toString()));
			colonne_account.put(Colonne.PASSWORD, DigestUtils.sha256Hex((String) argsTable.get(QueryParameter.PASSWORD.toString())));
			//colonne_account.put(Colonne.RESIDENCE_ID_REF, resd_ID);
			
			//colonne_residenza.put(Colonne.ID, resd_ID);
			colonne_residenza.put(Colonne.VIA_PIAZZA, argsTable.get(QueryParameter.VIA_PIAZZA.toString()));
			colonne_residenza.put(Colonne.CIVIC_NUMER, Integer.parseInt((String)argsTable.get(QueryParameter.CIVIC_NUMBER.toString())));
			colonne_residenza.put(Colonne.PROVINCE_NAME, argsTable.get(QueryParameter.PROVINCE.toString()));
			colonne_residenza.put(Colonne.COUNCIL_NAME, argsTable.get(QueryParameter.COMMUNE.toString()));
			colonne_residenza.put(Colonne.CAP, (String)argsTable.get(QueryParameter.CAP.toString()));

			QueriesManager.addAccount_and_addResidence(colonne_account, colonne_residenza);

			HashMap<String, Object> temp = new HashMap<String, Object>();
			temp.put(QueryParameter.EMAIL.toString(), argsTable.get(QueryParameter.EMAIL.toString()));
			temp.put(QueryParameter.PASSWORD.toString(), argsTable.get(QueryParameter.PASSWORD.toString()));

			return getAccount(temp);
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
     * Gets an account based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for getting an account.
     * @return An object representing the result of the operation or an error message.
     */
	@Override
	@SuppressWarnings("unchecked")
	public Object getAccount(final HashMap<String, Object> argsTable) 
	{
		try { 
			//cerco se esiste un account con quell'email
			Account account = QueriesManager.getAccountByEmail((String) argsTable.get(QueryParameter.EMAIL.toString()));

			//verifico se l'ho trovato
			if(account == null)
				return enumclass.ErrorString.INVALID_EMAIL.name();

			//verifico se le password combaciano
			if(!account.getPassword().equals(DigestUtils.sha256Hex((String) argsTable.get(QueryParameter.PASSWORD.toString()))))
				return enumclass.ErrorString.INVALID_PASSWORD.name();

			return account;
		} 
		catch (Exception e) {
			return e;
        }	
	}

	//---------------------------- operazioni con SONG ---------------------------- //
	/**
     * Gets the most popular songs based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for getting popular songs.
     * @return An object representing the result of the operation or an error message.
     */
	@Override
	@SuppressWarnings("unchecked")
	public Object getMostPopularSongs(final HashMap<String, Object> argsTable) 
	{
		try {
			return QueriesManager.getTopPopularSongs((long)argsTable.get(QueryParameter.LIMIT.toString()), (long)argsTable.get(QueryParameter.OFFSET.toString()));
		} 
		catch (Exception e) {
			return e;
        }
	}

	
	/**
     * Searches for songs based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for searching songs.
     * @return An object representing the result of the operation or an error message.
     */
	@Override
	@SuppressWarnings("unchecked")
	public Object searchSongs(final HashMap<String, Object> argsTable) 
	{
        try {
			String key = (String)argsTable.get(QueryParameter.SEARCH_STRING.toString());
			long limit = (long)argsTable.get(QueryParameter.LIMIT.toString());
			long offset = (long)argsTable.get(QueryParameter.OFFSET.toString());
			int mode = (int)argsTable.get(QueryParameter.MODE.toString());

            Object[] result = QueriesManager.searchSong_and_countElement(key, limit, offset, mode);
		
			
			return result;
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
     * Gets the most recently published albums based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for getting recent albums.
     * @return An object representing the result of the operation or an error message.
     */
	@Override
	@SuppressWarnings("unchecked")
	public Object getRecentPublischedAlbum(final HashMap<String, Object> argsTable) 
	{
		try {
			ArrayList<Album> result = QueriesManager.getRecentPublischedAlbum((long)argsTable.get(QueryParameter.LIMIT.toString()), (long)argsTable.get(QueryParameter.OFFSET.toString()), (int)argsTable.get(QueryParameter.THRESHOLD.toString()));
			return result;
		}
		catch (Exception e) {
			return e;
        }
	}


	/**
     * Searches for albums based on a search string, limit, and offset.
     *
     * @param argsTable A HashMap containing search parameters.
     * @return An array of search results or an error string.
     */
	@Override
	@SuppressWarnings("unchecked")
	public Object searchAlbums(final HashMap<String, Object> argsTable) 
	{
        try {
            return QueriesManager.searchAlbum((String)argsTable.get(QueryParameter.SEARCH_STRING.toString()), (long)argsTable.get(QueryParameter.LIMIT.toString()), (long)argsTable.get(QueryParameter.OFFSET.toString()));
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
     * Deletes an account based on the provided parameters.
     *
     * @param argsTable A {@code HashMap<String, Object>} containing parameters for deleting an account.
     * @return {@code true} if the account is deleted successfully, {@code false} otherwise.
     */
	@Override
	public Object deleteAccount(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.deleteAccount((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()));
			return true;
		} 
		catch (Exception e) {
			return e;
		}
	}

	@Override
	public Object getSongByIDs(final HashMap<String, Object> argsTable) {
		try {
            return QueriesManager.searchSongByIDs((String[])argsTable.get(QueryParameter.ID.toString()));
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
     * Searches for albums based on a search string, limit, and offset.
     *
     * @param argsTable A HashMap containing search parameters.
     * @return An array of search results or an error string.
     */
	@Override
	public Object getAlbumsSongs(final HashMap<String, Object> argsTable) 
	{
        try {
            ArrayList<Song> result = QueriesManager.getAlbumSongs((String)argsTable.get(QueryParameter.ALBUM_ID.toString()));
			//terminal.printInfoln("element: " + result.size());
			return result;
		} 
		catch (Exception e) {
			return e;
        }
	}

	@Override
	public Object getArtistSongs(final HashMap<String, Object> argsTable) {
		try {
			ArrayList<Song> result = QueriesManager.getArtistSong((String)argsTable.get(QueryParameter.ARTIST_ID.toString()));
			//terminal.printInfoln("element: " + result.size());
			return result;
		} 
		catch (Exception e) {
			return e;
		}
	}

	@Override
	public Object getPlaylistSongs(final HashMap<String, Object> argsTable) {
		try {
			ArrayList<Song> result = QueriesManager.getPlaylistSong((String)argsTable.get(QueryParameter.PLAYLIST_ID.toString()));
			//terminal.printInfoln("element: " + result.size());
			return result;
		} 
		catch (Exception e) {
			return e;
		}
	}

	@Override
	public Object renamePlaylist(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.renamePlaylist((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()), (String)argsTable.get(QueryParameter.PLAYLIST_ID.toString()), (String)argsTable.get(QueryParameter.NEW_NAME.toString()));
			return true;        
		} 
		catch (Exception e) {
			return e;
		}
	}

	 /**
     * Retrieves an album by its ID.
     *
     * @param argsTable A HashMap containing the album ID.
     * @return An Album object or an error string.
     */
	@Override
	public Object getAlbumByID(final HashMap<String, Object> argsTable) {
		try {
			//System.out.println(argsTable.get(QueryParameter.ID.toString()).getClass());
            return QueriesManager.getAlbumByID((String)argsTable.get(QueryParameter.ID.toString()));
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
	 * Retrieves albums associated with a specific artist.
	 *
	 * @param argsTable A HashMap containing the artist ID.
	 * @return A list of albums or an error string.
	 */
	@Override
	public Object getArtistAlbums(final HashMap<String, Object> argsTable) {
		try {
			//ArrayList<Album> result = QueriesManager.getArtistAlbums((String)argsTable.get(QueryParameter.ARTIST_ID.toString()));
			//terminal.printInfoln("element: " + result.size());
			return null;
		} 
		catch (Exception e) {
			return e;
		}
	}

	/**
	 * Searches for artists based on a search string, limit, and offset.
	 *
	 * @param argsTable A HashMap containing search parameters.
	 * @return An array of search results or an error string.
	 */
	@Override
	public Object searchArtists(final HashMap<String, Object> argsTable) {
		try {
			String key = (String)argsTable.get(QueryParameter.SEARCH_STRING.toString());
			long limit = (long)argsTable.get(QueryParameter.LIMIT.toString());
			long offset = (long)argsTable.get(QueryParameter.OFFSET.toString());

            Object[] result = QueriesManager.searchArtists(key, limit, offset);

			return result;
		} 
		catch (Exception e) {
			return e;
		}
	}

	/**
	 * Retrieves artists based on their IDs.
	 *
	 * @param argsTable A HashMap containing artist IDs.
	 * @return An array of artists or an error string.
	 */
	@Override
	public Object getArtistsByIDs(HashMap<String, Object> argsTable) {
		try {
			
			return QueriesManager.getArtistByID((String)argsTable.get(QueryParameter.ARTIST_ID.toString()));
		} 
		catch (Exception e) {
			return e;
		}
	}

	/**
	 * Adds a new playlist for a specific account.
	 *
	 * @param argsTable A HashMap containing account ID and playlist name.
	 * @return True if the playlist was added successfully, false or an error string otherwise.
	 */
	@Override
	public Object addPlaylist(final HashMap<String, Object> argsTable) {
		try {
			//Aggiungere eventuale immagine
			QueriesManager.addPlaylist((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()), (String)argsTable.get(QueryParameter.PLAYLIST_NAME.toString()));
			return true;        
		} 
		catch (Exception e) {
			return e;
        }
	}

	/**
	 * Deletes a playlist for a specific account.
	 *
	 * @param argsTable A HashMap containing account ID and playlist ID.
	 * @return True if the playlist was deleted successfully, false or an error string otherwise.
	 */
	@Override
	public Object deletePlaylist(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.deletePlaylist((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()), (String)argsTable.get(QueryParameter.PLAYLIST_ID.toString()));
			return true;
		} 
		catch (Exception e) {
			return e;     
		} 
	}

	/**
	 * Removes a song from a playlist for a specific account.
	 *
	 * @param argsTable A HashMap containing account ID, playlist ID, and song ID.
	 * @return True if the song was removed successfully, false or an error string otherwise.
	 */
	@Override
	public Object removeSongFromPlaylist(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.removeSongFromPlaylist((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()), (String)argsTable.get(QueryParameter.PLAYLIST_ID.toString()), (String)argsTable.get(QueryParameter.SONG_ID.toString()));
			return true;        
		} 
		catch (Exception e) {
			return e;
		}
	}

	/**
	 * Adds a song to a playlist for a specific account.
	 *
	 * @param argsTable A HashMap containing account ID, playlist ID, and song ID.
	 * @return True if the song was added successfully, false or an error string otherwise.
	 */
	@Override
	public Object addSongToPlaylist(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.addSongToPlaylist((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()), (String)argsTable.get(QueryParameter.PLAYLIST_ID.toString()), (String)argsTable.get(QueryParameter.SONG_ID.toString()));
			return true;        
		} 
		catch (Exception e) {
			return e;
		}
	}

	/**
	 * Retrieves playlists associated with a specific account.
	 *
	 * @param argsTable A HashMap containing account ID.
	 * @return A list of playlists or an error string.
	 */
	@Override
	public Object getAccountsPlaylists(final HashMap<String, Object> argsTable) {
		try {
			return QueriesManager.getAccountsPlaylists((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()));
		} 
		catch (Exception e) {
			return e;
		}
	}


	/////////////////////////////////////////////////////////////////////////////////
	//EMOTION
	/////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a new emotion for a specific song.
	 *
	 * @param argsTable A HashMap containing emotion parameters.
	 * @return True if the emotion was added successfully, false or an error string otherwise.
	 */
	@Override
	public Object addEmotion(final HashMap<String, Object> argsTable) {
		try {
			QueriesManager.addEmotion(convertFromQueryParametre2Colonne(argsTable, true));
		} 
		catch (Exception e) {
			return e;
		}
		return true;
	}

	/**
	 * Retrieves emotions associated with a specific song.
	 *
	 * @param argsTable A HashMap containing song ID.
	 * @return A list of emotions or an error string.
	 */
	@Override
	public Object getSongEmotion(final HashMap<String, Object> argsTable) {
		try {
			return QueriesManager.getSongEmotion((String)argsTable.get(QueryParameter.SONG_ID.toString()));
		} 
		catch (Exception e) {
			return e;     
		} 
	}

	/**
	 * Deletes an emotion based on its ID.
	 *
	 * @param argsTable A HashMap containing emotion ID.
	 * @return True if the emotion was deleted successfully, false or an error string otherwise.
	 */
	@Override
	public Object deleteEmotion(final HashMap<String, Object> argsTable) {
		try {
			
			QueriesManager.deleteEmotion((String)argsTable.get(QueryParameter.ID.toString()));
			return true;
		} 
		catch (Exception e) {
			return e;     
		}

	}

	/**
	 * Retrieves emotions associated with a specific account.
	 *
	 * @param argsTable A HashMap containing account ID.
	 * @return A list of emotions or an error string.
	 */
	@Override
	public Object getAccountEmotion(HashMap<String, Object> argsTable) {
		try {
			return QueriesManager.getAccountEmotions((String)argsTable.get(QueryParameter.ACCOUNT_ID.toString()));
		} 
		catch (Exception e) {
			return e;     
		}
	}

	

	

	
	
	

	

	
	
	
}