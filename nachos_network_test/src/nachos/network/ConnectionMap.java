package nachos.network;

import nachos.machine.Lib;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Map of <ConnectionState, Connections> where ConnectionState represents constant 
 * variables in Connection (CLOSED, SYN_SENT, etc...) and Connections represent a list of 
 * Connections (currently using LinkedList). Unfortunately, this does not guarantee 
 * synchronization as there are unknown sync bugs to fix.
 * 
 * Each Connection is unique throughout the map, so no multiple same Connection would exist 
 * in the map.
 * connectionState表示常量的<connectionState，connections>的映射
 * 连接（关闭、syn_sent等）和连接中的变量表示
 * *连接（当前正在使用LinkedList）。不幸的是，这不能保证
 * *同步，因为有未知的同步错误要修复。
 * *
 * *在map中每个连接都是唯一的，因此不会存在多个相同的连接。
 * *在地图上。
 */
public class ConnectionMap {
	/**
	 * Allocate a new map
	 */
	public ConnectionMap() {
		map = new HashMap<Integer, LinkedList<Connection>>();
	}
	
	/**
	 * Add the specified connection to the map only if the connection doesn't exist in the map.
	 * 只有在地图中不存在连接时，才将指定的连接添加到映射中
	 * @param	c	the connection to add. Must not be <tt>null</tt>.
	 */
	public void add(Connection c) {
		Lib.assertTrue(c != null && c.validState());
		
		// Get the connection state
		//获取连接状态
		Integer connectionState = c.state;
		
		// Get the corresponding LinkedList
		//获取相应的LinkedList
		LinkedList<Connection> connections;
		// Remove the LinkedList from the map if it exists
		//如果存在，从地图中删除链接列表
		if(map.containsKey(connectionState)) {
			connections = map.get(connectionState);
			map.remove(connectionState);	// temporary removal for replacement
		}
		// Create a new LinkedList if it doesn't exist
		else
			connections = new LinkedList<Connection>();
		
		// Check for duplicates (if it exists, then do nothing and reinsert)
		//检查重复（如果存在，则不做任何操作，重新插入）
		if(!connections.isEmpty()) {
			for(Connection con : connections) {
				// Duplicate found. Insert the list back to the map
				//找到重复项将列表插入回map
				if(con.equals(c)) {
					// Insert the list back to the map
					map.put(connectionState, connections);
					return;
				}
			}
		}
		
		// Duplicate not found. Insert Connection into the list
		//找不到副本。将连接插入列表
		connections.add(c);
		
		// Insert the list back to the map
		map.put(connectionState, connections);
	}
	
	/**
     * Close (remove) a connection in the CLOSING state. Returns null if c is not found.
     *关闭（移除）处于关闭状态的连接如果找不到c，则返回null。
     * @param	c	the connection to close. Must not be <tt>null</tt>.
     * @return	the element removed from the map.
     */
    public Connection close(Connection c) {
    	Lib.assertTrue(c != null && c.state == Connection.CLOSING);
    	
    	// Return null if there are no CLOSING connections
		//如果没有关闭连接，则返回null
    	if(!map.containsKey(Connection.CLOSING)) {
    		return null;
    	}
    	
    	// Get the CLOSING Connection list
		//获取关闭连接列表
    	LinkedList<Connection> connections = map.get(Connection.CLOSING);
    	Integer connectionState = c.state;

    	// Remove the connection if exists
		//如果存在，请删除连接
    	for(Connection con : connections) {
			if(con.equals(c)) {
				// Remove the connection from the list
				connections.remove(c);
				
				// Insert (back) into the map
				map.put(connectionState, connections);
				return c;
			}
		}
		
    	// Connection doesn't exist. Reinsert (back) into the map and return null
		//连接不存在。重新插入（返回）到映射中并返回空值
		map.put(connectionState, connections);

		return null;
    }
    
    /**
     * Switch the existing connection to different state. c must exist in the map.
     * 将现有连接切换到不同状态。C必须存在于地图中。
     * @param	newState	new state for the connection
     * @param	c			connection that wants to move. Must not be <tt>null</tt>.
     * @return	true if successful, false otherwise
     */
    public boolean switchConnection(int newState, Connection c) {
    	Lib.assertTrue(c != null && c.validState() && Connection.validState(newState));
    	
    	// Get the original connection state
		//获取原始连接状态
    	Integer originalConnState = c.state;
    	
    	// Get the original state's LinkedList
		//获取原始状态的LinkedList
    	LinkedList<Connection> originalConns;
    	// Remove the LinkedList from the map if it exists
		//如果存在，从地图中删除链接列表
    	if(map.containsKey(originalConnState)) {
			originalConns = map.get(originalConnState);
			map.remove(originalConnState);	// temporary removal for replacement
		}
    	// Create a new LinkedList if it doesn't exist
		//如果不存在，则创建一个新的链接列表
		else
			originalConns = new LinkedList<Connection>();
    	
    	// Get the new connection state
		//获取新连接状态
    	Integer newConnState = newState;
    	
    	// Get the new state's LinkedList
    	LinkedList<Connection> newConns;
    	// Remove the LinkedList from the map if it exists
    	if(map.containsKey(newConnState)) {
			newConns = map.get(newConnState);
			map.remove(newConnState);	// temporary removal for replacement
		}
    	// Create a new LinkedList if it doesn't exist
		else
			newConns = new LinkedList<Connection>();
    	
    	// Find the connection in the original list
    	for(Connection con : originalConns) {
			// Connection found. Remove it from the original list and insert into
    		// new list
    		if(con.equals(c)) {
				// Remove connection from the original list
				originalConns.remove(c);
				
				// Insert the original list back to the map
				map.put(originalConnState, originalConns);
				
				// Insert Connection to the new list
				newConns.add(c);
				
				// Insert the new list back into the map
				map.put(newConnState, newConns);
				return true;
			}
		}
    	
    	// Connection doesn't exist in original list. Reinsert both original 
    	// and new list back to where they belong
		//连接不存在于原始列表中。将原始和新列表重新插入到它们所属的位置
		map.put(originalConnState, originalConns);
		map.put(newConnState, newConns);
    	return false;
    }
    
    /**
     * Get the connection state with given parameters. Returns CLOSED if not found.
     * 获取给定参数的连接状态。如果找不到则返回closed。
     * @param	srcLink	source address
     * @param	srcPort	source port
     * @return	state of the connection (CLOSED or -1 if not found)
     */
    public int getConnectionState(int dstLink, int dstPort, int srcLink, int srcPort) {
    	// Search through all connection states
    	for(Integer connectionState : map.keySet())
    		// Search through all Connections in the list
    		for(Connection c : map.get(connectionState))
    			// Return the connection state if the connection is found
    			if(c.srcLink == srcLink && c.srcPort == srcPort && c.dstLink == dstLink && c.dstPort == dstPort)
    				return connectionState;
    	
    	// Connection doesn't exist. Conclude as closed
    	return Connection.CLOSED;
    }
    
    /**
     * Find a port's waiting connection if exists. Returns null if not found.
     * 如果存在端口，查找端口的等待连接。如果找不到，则返回null。
     * @param	srcLink	source address
     * @param	srcPort soruce port
     * @return	corresponding connection (null if not found)
     */
    public Connection findWaitingConnection(int srcLink, int srcPort) {
		// Check if there are any SYN_RCVD (or waiting) Connections
    	if(map.get(Connection.SYN_RCVD) != null && !map.get(Connection.SYN_RCVD).isEmpty()) {
    		// Find a connection that matches srcLink and srcPort
    		for(Connection c : map.get(Connection.SYN_RCVD)) {
				if(c.srcLink == srcLink && c.srcPort == srcPort) {
					// Remove the connection from the list
					LinkedList<Connection> connections = map.remove(Connection.SYN_RCVD);
					connections.remove(c);
					
					// Reinsert the list to the map
					map.put(Connection.SYN_RCVD, connections);
					return c;
				}
			}
    	}
    	
    	// Connection doesn't exist. Return null
		return null;
	}
	
	private HashMap<Integer, LinkedList<Connection>> map;
}