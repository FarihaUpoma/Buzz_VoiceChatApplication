 import java.io.BufferedReader;
    import java.io.DataOutputStream;
    import java.io.IOException;
    import java.io.InputStreamReader;
    import java.io.PrintWriter;
    import java.io.OutputStream;
    import java.net.InetAddress;
    import java.net.ServerSocket;
    import java.net.Socket;
    import java.net.UnknownHostException;
    import java.sql.Connection;
    import java.sql.PreparedStatement;
    import java.sql.ResultSet;
    import java.sql.SQLException;
    import java.sql.Statement;
    import java.util.ArrayList;
    import java.util.List;
    import java.util.StringTokenizer;
    import java.util.logging.Level;
    import java.util.logging.Logger;
 
            public class Server extends JDBCContext {
 
                private int serverPort = 2302;
                private ServerSocket welcomeSocket;
                private List<ConnectionService> connections = new ArrayList<ConnectionService>();
 
                private static JDBCContext _BaseDAO = new JDBCContext();
                private static Connection _Connection = null;
                private static PreparedStatement _PreparedStatement = null;
                private static Statement _Statement = null;
                private static ResultSet _ResultSet = null;
                private static String _SQL = "";    
 
                public Server() throws IOException
                {
 
                    welcomeSocket = new ServerSocket(serverPort);
                    while(true) {
                            System.out.println("INI");
                        Socket client = welcomeSocket.accept();
                        System.out.println("Accepted");
                        ConnectionService service = new ConnectionService(client);
                        //connections.add(service);
                        System.out.println("Under");
                        service.run();
                    }
                }
 
                class ConnectionService extends Thread
                {
                    private Socket socket;
                    private BufferedReader inputReader;
                    private DataOutputStream output;
                    private PrintWriter outputWriter;
                    private String id;
                    InetAddress IPaddress;
 
                    public ConnectionService(Socket client) throws IOException
                    {
                        this.socket = client;
                        inputReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        output = new DataOutputStream(socket.getOutputStream());
                        outputWriter = new PrintWriter(socket.getOutputStream(), true);
                        System.out.println("Const");
                    }
 
                    @Override
                    public void run()
                    {
                            boolean f=true;
                            System.out.println("RUN");
                        while(f) {
                            f=false;
                            try {
                                    System.out.println("Reading");
                                    //String message = inputReader.readUTF();
                                String message = inputReader.readLine();
                                System.out.println(message + " Read");
                                //IPaddress = InetAddress.getByName(socket.getRemoteSocketAddress().toString());
                                //String IP = IPaddress.toString();
                                String IP=socket.getRemoteSocketAddress().toString();
                                StringBuilder temp=new StringBuilder(IP);
                                int i;
                                for(i=temp.length()-1;i>=0;i--)
                                {
 
                                    if(temp.charAt(i)==':')
                                    {
                                            temp.deleteCharAt(i);
                                            break;
                                    }
                                    temp.deleteCharAt(i);
                                }
                                temp.deleteCharAt(0);
                                IP=temp.toString();
                                System.out.println("RUN "+IP);
                                //if(f==true)
                                    //continue;
                                if(message.equals("req")) {
                                    try {
                                        message = inputReader.readLine();                            
                                        this.Request(message, id, IP);
                                    } catch (IOException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (SQLException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ClassNotFoundException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                else if(message.equals("close")) {
                                    try {
                                        _SQL = "delete from online_list where online_ip= ?";
                                        _Connection = _BaseDAO.getConnection();                
                                        _PreparedStatement = _Connection.prepareStatement(_SQL);
                                        _PreparedStatement.setString(1, IP);
 
                                        this.UpdateDB(_PreparedStatement);
                                        this.SendToMany();
                                    } catch (SQLException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    } catch (ClassNotFoundException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                                else {
                                    try {                            
                                        String string = message;
                                        System.out.println("Here "+string);
                                        String[] parts = string.split("\\+");
                                        System.out.println(parts.length);
                                        if(parts.length==2){
                                                id = parts[0];
                                                String pass = parts[1];
                                                String reply = this.Authenticate(id, pass);
                                                System.out.println("Replied "+reply);
                                                if(reply.equals("valid")) {
                                                    //send online frnd list
                                                    //update online list
                                                    //send caller info
                                                	output.writeBytes("valid\n");
                                                    this.SendFriendList(id);
                                                    this.SendCallerInfo(id);
                                                    _SQL = "insert into online_list (online_user_id,online_ip,online_busy) values (?,?,?)";
                                                    //System.out.println(_SQL);
                                                    _Connection = _BaseDAO.getConnection();                
                                                    _PreparedStatement = _Connection.prepareStatement(_SQL);                                    
                                                    //_PreparedStatement.setString(1, "20");
                                                    _PreparedStatement.setString(1, id);
                                                    _PreparedStatement.setString(2, IP);
                                                    _PreparedStatement.setString(3, "1");
                                                    System.out.println("update sql: "+_SQL);
                                                   // output.flush();
                                                }
                                                else
                                                {
                                                    output.writeBytes("invalid\n");
                                                    output.flush();
                                                }
                                        }
                                                else{
                                                    String full=parts[0];
                                                    String user=parts[1];
                                                    String pas=parts[2];
                                                    String email=parts[3];
                                                    _SQL = "insert into caller (user_id,user_fullname,user_password,user_email) values (?,?,?,?)";
                                                    _Connection = _BaseDAO.getConnection();                
                                                    _PreparedStatement = _Connection.prepareStatement(_SQL);
                                                    _PreparedStatement.setString(1, user);
                                                    _PreparedStatement.setString(2, full);
                                                    _PreparedStatement.setString(3, pas);
                                                    _PreparedStatement.setString(4, email);
                                                    System.out.println("Insert sql: "+_SQL);
                                                }
                                                    this.UpdateDB(_PreparedStatement);
 
                                    } catch (SQLException | ClassNotFoundException ex) {
                                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            } catch (IOException ex) {
                                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    private void Request(String friendName, String callerName, String IP) throws SQLException, ClassNotFoundException
                    {
                        //find ip from database using name
                        //send ip, if not busy
                        //update db flag as busy
 
                        _SQL = "select online_ip from online_list where online_user_id=? and  online_busy=0";
                        System.out.println("HellB " + _SQL);
                        _Connection = _BaseDAO.getConnection();
                        System.out.println("HellC" );
                        _PreparedStatement = _Connection.prepareStatement(_SQL);
                       _PreparedStatement.setString(1, friendName);
                        //_PreparedStatement.setString(2, );
                        System.out.println("HellD");
                        _ResultSet = _PreparedStatement.executeQuery();
                        System.out.println("HellE");
 
                        String result = null;
                        System.out.println("HellF");
                        if(_ResultSet.next()) {
                            result = _ResultSet.getString("online_ip");                
                        }
                        outputWriter.println(result);
                        if(result != null) {
                            _SQL = "update online_list set online_busy=1 where online_user_id=?  and online_ip=? ";
                            _Connection = _BaseDAO.getConnection();                
                            _PreparedStatement = _Connection.prepareStatement(_SQL);                                
                            _PreparedStatement.setString(1, callerName);
                            _PreparedStatement.setString(2, IP);
                            this.UpdateDB(_PreparedStatement);
                            _SQL = "update online_list set online_busy=1 where online_user_id= ? and online_ip= ?";
                            _Connection = _BaseDAO.getConnection();                
                            _PreparedStatement = _Connection.prepareStatement(_SQL);
                            _PreparedStatement.setString(1, friendName);
                            _PreparedStatement.setString(2, result);
                            this.UpdateDB(_PreparedStatement);
                        }
                    }
 
                    private String Authenticate(String id, String pass) throws SQLException, ClassNotFoundException
                    {
 
 
                        _SQL = "select user_id from caller where user_id= ? and user_password= ?";
                        System.out.println("HellB " + _SQL);
                        _Connection = _BaseDAO.getConnection();
                        System.out.println("HellC" );
                        _PreparedStatement = _Connection.prepareStatement(_SQL);
                        _PreparedStatement.setString(1, id);
                        _PreparedStatement.setString(2, pass);
                        System.out.println("HellD");
                        _ResultSet = _PreparedStatement.executeQuery();
                        System.out.println("HellE");
 
                        String result = null;
                        System.out.println("HellF");
                        while(_ResultSet.next()) {
                            result = _ResultSet.getString("user_id");
                            System.out.println(result);
                        }
                        System.out.println("End");
                        String reply = "valid";
                        if(id == result) reply = "invalid";
                        return reply;
 
                        //check if username pass correct
                        //then assign ip
                        //update online list
                    }
 
                    private void SendCallerInfo(String id) throws SQLException, ClassNotFoundException, IOException
                    {
                        _SQL = "select user_fullname, user_email from caller where user_id= ?";
                        System.out.println("HellB " + _SQL);
                        _Connection = _BaseDAO.getConnection();
                        System.out.println("HellC" );
                        _PreparedStatement = _Connection.prepareStatement(_SQL);  
                        _PreparedStatement.setString(1, id);
                        System.out.println("HellD");
                        _ResultSet = _PreparedStatement.executeQuery();
                        System.out.println("HellE");
 
                        String name = null;
                        String email = null;
                        System.out.println("HellF");
                        while(_ResultSet.next()) {
                            name = _ResultSet.getString("user_fullname");
                            email = _ResultSet.getString("user_email");
                            System.out.println("iterating");
                            System.out.println(name);
                            name+="+";
                            //name.concat("+");
                            System.out.println(name);
                            //name.concat(email);
                            name+=email;
                            output.writeBytes(name+'\n');
                            //output.flush();
                           // name = _ResultSet.getString(0);
                           // System.out.println("name : "+name);
                            //email = _ResultSet.getString(2);
 
                            System.out.println("name : "+name+" email: "+email);
 
                            //outputWriter.println(name);
                            //outputWriter.println(email);
                        }
                        output.flush();
                        System.out.println("End");
                        //outputWriter.println("End");
                    }
 
                    private void SendFriendList(String id) throws SQLException, ClassNotFoundException, IOException
                    {
                        _SQL = "select online_user_id from online_list where online_user_id in (select friendlist_friend_user_id from friend_list where friendlist_user_id = ? )";
                        System.out.println("HellB " + _SQL);
                        _Connection = _BaseDAO.getConnection();
                        System.out.println("HellC" );
                        _PreparedStatement = _Connection.prepareStatement(_SQL);
                        _PreparedStatement.setString(1, id);
                        System.out.println("HellD");
                        _ResultSet = _PreparedStatement.executeQuery();
                        System.out.println("HellE");
 
                        String result = null;
                        System.out.println("HellF");
                        while(_ResultSet.next()) {
                            result = _ResultSet.getString("online_user_id");
                            output.writeBytes(result+'\n');
                            //outputWriter.println(result);
                        }
                        output.writeBytes("End\n");
                        output.flush();
                        //outputWriter.println("End");            
                    }
 
                    private void UpdateDB(PreparedStatement pp) throws SQLException, ClassNotFoundException
                    {
                         //_SQL = "select user_fullname ,user_id, user_email from caller where user_id= ?";
                        /*System.out.println("HellB " + _SQL);
                        _Connection = _BaseDAO.getConnection();
                        System.out.println("HellC" );
                        _PreparedStatement = _Connection.prepareStatement(_SQL);
                        //_PreparedStatement.setString(1, id);
                        System.out.println("HellD");
                        */
                        PreparedStatement ps = pp;
                        pp.executeUpdate();
                        System.out.println("HellE");
 
                    }
                    private void SendToMany() throws SQLException, ClassNotFoundException, IOException
                    {
                     for(ConnectionService connection : connections) {
                            connection.SendFriendList(connection.id);
                        }  
                    }
 
                }
                public static void main(String args[]) throws IOException, SQLException, ClassNotFoundException
                {
 
                    new Server();      
 
                }
            }