package py.db;


import java.sql.*;
import java.util.logging.*;
import java.util.Hashtable;

/*
 * DBOperator provide APIs to execute SQL statements on databases.
 *
 * The way to use it is:
 *  DBOperator dbOperator = new DBOperator(host, user, passwd);
 *  dbOperator.use(dbName);
 *  dbOperator.executeSQL();
 */
public class DBOperator {
	private static String SYNC_OBJ = "sync";
	private static Hashtable hashDbNames = new Hashtable();
	private static Hashtable hashConnections = new Hashtable();
	private String encodingString = "useUnicode=true&characterEncoding=utf8";
	private String hostName = null;
	private String userName = null;
	private String password = null;
	private String userAndPasswd = null;
	private String prefixConnectionString = null;

	// private static DBOperator dbOperator;
	private Connection connection;
	private static Logger logger = Logger.getLogger(DBOperator.class.getName());

	// private Statement statement;
	// private ResultSet resultSet;

	public enum SQLTypes {
		INSERT, UPDATE, SELECT
	}

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (Exception e) {
			System.out.println("can't load com.mysql.jdbc.driver class, check your classpath");
			System.exit(1);
		}
	}

	public DBOperator(String host, String user, String passwd) {
	  hostName = host;
	  userName = user;
	  password = passwd;
	  userAndPasswd = "user=" + user + "&password=" + password;
	  prefixConnectionString = "jdbc:mysql://" + hostName + "/";
	}

	public boolean use(String dbName) {
		if (dbName == null)
			return false;
		
		synchronized (SYNC_OBJ) {
			connection = (Connection) hashConnections.get(dbName);

			if (connection == null) {
				connection = getConnection(dbName);
				if (connection == null)
					return false;
				else
					hashConnections.put(dbName, connection);
			}
			return true;
		}
	}

	public void closeDB(String dbName) {
		
		synchronized (SYNC_OBJ) {
			connection = (Connection) hashConnections.get(dbName);
		}

		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			logger.log(Level.WARNING, "can't close the connection to " + dbName);
			e.printStackTrace();
		}
	}

	public ResultSet executeSQL(String sqlStatement, SQLTypes types) throws SQLException {
		return executeSQL(sqlStatement, types, 0);
	}

	public ResultSet executeSQL(String sqlStatement, SQLTypes types, int fetchSize)
			throws SQLException {
		if (connection == null)
			return null;

		try {
			// if (statement != null)
			// statement.close();

			Statement statement = connection.createStatement(
					ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);

			if (fetchSize > 0) {
				statement.setFetchSize(fetchSize);
			}
			switch (types) {
			case INSERT:
			case UPDATE:
				statement.executeUpdate(sqlStatement);
				statement.close();
				return null;
			case SELECT:
				ResultSet resultSet = statement.executeQuery(sqlStatement);
				return resultSet;
			default:
				return null;
			}
		} catch (SQLException ex) {
			// handle any errors
			System.out.println("something wrong with the sqlStatement string: " + sqlStatement);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			throw ex;
		}
	}

	private Connection getConnection(String dbName) {
		String connectionString = null;
		try {
		        connectionString = prefixConnectionString + dbName + "?"
						+ userAndPasswd + "&" + encodingString;
			logger.info("connection string is : " + connectionString);
			Connection conn = DriverManager.getConnection(connectionString);
			return conn;

		} catch (SQLException ex) {
			// handle any errors
			System.out.println("something wrong with the connection String"
					+ connectionString);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		}
	}

	/*
	 * public void release() { try { if (statement != null) { statement.close();
	 * statement = null; } if (resultSet != null) { resultSet.close(); resultSet =
	 * null; } } catch (SQLException e) { System.out.println(e); } }
	 */

	static public void main(String[] args) {

		DBOperator db = new DBOperator("localhost", "root", "6729lc");
		db.use("universities");
		ResultSet srs;
		try {
			// srs = DbException.executeSQL("insert into stores values (4, \"?????????\",
			// \"?????????48\")");
			srs = db.executeSQL("SELECT url FROM departments",
					DBOperator.SQLTypes.SELECT);

			if (srs != null) {
				while (srs.next()) {
					String url = srs.getString("url");
					System.out.println(url);
				}
			}

			srs.close();

			/*
			 * ResultSet srs = DbException .executeSQL("insert into stores values (3,
			 * \"8848\", \"8848\")");
			 * 
			 * ResultSet srs = DbException .executeSQL("update stores set
			 * storename=\"joyo\", storedbname =\"joyo\" where storeid = 2");
			 * 
			 * srs = DbException .executeSQL("update stores set storename=\"dangdang\",
			 * storedbname =\"dangdang\" where storeid = 1");
			 */
			
	//		ResultSet srs = DbException .executeSQL("insert into stores values (3, \"8848\", \"8848\")");
			 
			db.closeDB("universities");
		} catch (SQLException e) {
			return;
		}

	}
}
