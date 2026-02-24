import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import './AdminChatPanel.css';

const AdminChatPanel = ({ adminId, adminUsername }) => {
    const [activeSessions, setActiveSessions] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [isConnected, setIsConnected] = useState(false);
    const [selectedSession, setSelectedSession] = useState(null);
    const [chatMessages, setChatMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [isSendingMessage, setIsSendingMessage] = useState(false);
    
    const stompClient = useRef(null);
    const messagesEndRef = useRef(null);
    const currentSessionSubscription = useRef(null);

    useEffect(() => {
        console.log('AdminChatPanel mounted with:', { adminId, adminUsername });
        connectWebSocket();
        fetchActiveSessions();
        
        // Refresh sessions every 30 seconds as backup
        const interval = setInterval(fetchActiveSessions, 30000);
        
        return () => {
            clearInterval(interval);
            if (currentSessionSubscription.current) {
                currentSessionSubscription.current.unsubscribe();
            }
            if (stompClient.current) {
                stompClient.current.disconnect();
            }
        };
    }, []);

    useEffect(() => {
        scrollToBottom();
    }, [chatMessages]);

    const connectWebSocket = () => {
        console.log('AdminChatPanel connecting to WebSocket...');
        const socket = new SockJS('http://localhost/ws');
        stompClient.current = Stomp.over(socket);
        
        stompClient.current.connect({}, (frame) => {
            console.log('AdminChatPanel connected to WebSocket:', frame);
            setIsConnected(true);
            
            // Subscribe to admin notifications
            stompClient.current.subscribe('/topic/admin/notifications', (message) => {
                console.log('Admin notification received:', message.body);
                const notification = JSON.parse(message.body);
                console.log('Parsed notification:', notification);
                
                // Refresh sessions when admin support is requested
                fetchActiveSessions();
            });
            
            // Subscribe to admin chat for general updates
            stompClient.current.subscribe('/topic/admin/chat', (message) => {
                console.log('Admin chat message received:', message.body);
                const chatMessage = JSON.parse(message.body);
                
                // Refresh sessions if it's a user message (new activity)
                if (chatMessage.senderType === 'USER') {
                    console.log('User message detected, refreshing sessions');
                    fetchActiveSessions();
                }

            });
            
        }, (error) => {
            console.error('AdminChatPanel WebSocket connection error:', error);
            setIsConnected(false);
        });
    };

    const fetchActiveSessions = async () => {
        try {
            console.log('Fetching active sessions...');
            const response = await fetch('http://localhost/api/support/sessions/active');
            if (response.ok) {
                const sessions = await response.json();
                console.log('Active sessions:', sessions);
                setActiveSessions(sessions);
            } else {
                console.error('Failed to fetch sessions:', response.status);
            }
        } catch (error) {
            console.error('Error fetching active sessions:', error);
        } finally {
            setIsLoading(false);
        }
    };

    const takeOverSession = async (sessionId) => {
        try {
            console.log('Taking over session:', sessionId);
            const response = await fetch(`http://localhost/api/support/sessions/${sessionId}/take-over?adminId=${adminId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (response.ok) {
                console.log('Successfully took over session');
                fetchActiveSessions();
                // Auto-select the session after takeover
                const session = activeSessions.find(s => s.sessionId === sessionId);
                if (session) {
                    selectSession(session);
                }
            } else {
                console.error('Failed to take over session:', response.status);
            }
        } catch (error) {
            console.error('Error taking over session:', error);
        }
    };

    const selectSession = async (session) => {
        console.log('Selecting session:', session);
        
        // Unsubscribe from previous session if exists
        if (currentSessionSubscription.current) {
            currentSessionSubscription.current.unsubscribe();
            currentSessionSubscription.current = null;
        }
        
        setSelectedSession(session);
        setChatMessages([]);
        
        // Fetch chat history for this session
        try {
            const response = await fetch(`http://localhost/api/support/chat/history/${session.sessionId}`);
            if (response.ok) {
                const history = await response.json();
                console.log('Chat history loaded:', history);
                setChatMessages(history);
            } else {
                console.error('Failed to fetch chat history:', response.status);
            }
        } catch (error) {
            console.error('Error fetching chat history:', error);
        }
        
        // Subscribe to this session's chat updates if connected
        if (stompClient.current && isConnected) {
            currentSessionSubscription.current = stompClient.current.subscribe(`/topic/chat/${session.sessionId}`, (message) => {
                console.log('Session chat message received:', message.body);
                const chatMessage = JSON.parse(message.body);
                console.log('Admin panel - Timestamp format:', typeof chatMessage.timestamp, chatMessage.timestamp);
                
                setChatMessages(prev => {

                    const exists = prev.some(msg => {
                        // Check by ID if available
                        if (msg.id && chatMessage.id && msg.id === chatMessage.id) {
                            return true;
                        }

                        return msg.message === chatMessage.message && 
                               msg.senderType === chatMessage.senderType &&
                               msg.username === chatMessage.username &&
                               Math.abs(new Date(msg.timestamp) - new Date(chatMessage.timestamp)) < 1000; // Within 1 second
                    });
                    
                    if (!exists) {
                        console.log('Adding new message to admin chat:', chatMessage);
                        return [...prev, chatMessage];
                    } else {
                        console.log('Duplicate message detected in admin chat, skipping:', chatMessage);
                        return prev;
                    }
                });
            });
        }
    };

    const sendAdminMessage = async () => {
        if (!newMessage.trim() || !selectedSession || isSendingMessage) return;
        
        setIsSendingMessage(true);
        
        try {
            const messageData = {
                sessionId: selectedSession.sessionId,
                userId: adminId,
                username: adminUsername,
                message: newMessage.trim()
            };

            const response = await fetch('http://localhost/api/support/chat/admin', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(messageData)
            });

            if (response.ok) {
                console.log('Admin message sent successfully');
                setNewMessage('');
                // Message will be received via WebSocket
            } else {
                console.error('Failed to send admin message:', response.status);
            }
        } catch (error) {
            console.error('Error sending admin message:', error);
        } finally {
            setIsSendingMessage(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendAdminMessage();
        }
    };

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) {
            return '--:--';
        }
        
        try {
            let date;
            
            // Handle different timestamp formats
            if (typeof timestamp === 'string') {

                date = new Date(timestamp);
            } else if (typeof timestamp === 'number') {

                date = new Date(timestamp);
            } else if (timestamp instanceof Date) {
                date = timestamp;
            } else {
                console.warn('Unknown timestamp format:', timestamp);
                return '--:--';
            }
            
            if (isNaN(date.getTime())) {
                console.warn('Invalid date created from timestamp:', timestamp);
                return '--:--';
            }
            
            return date.toLocaleTimeString([], { 
                hour: '2-digit', 
                minute: '2-digit' 
            });
        } catch (error) {
            console.error('Error formatting timestamp:', timestamp, error);
            return '--:--';
        }
    };

    const getSenderDisplayName = (message) => {
        if (message.senderType === 'SYSTEM') {
            return 'Support Bot';
        }
        if (message.senderType === 'ADMIN') {
            return `Admin (${message.username})`;
        }
        return message.username;
    };

    const getMessageClass = (message) => {
        if (message.senderType === 'USER') {
            return 'message user-message';
        }
        if (message.senderType === 'SYSTEM') {
            return 'message bot-message';
        }
        return 'message admin-message';
    };

    const getStatusBadge = (status) => {
        switch (status) {
            case 'WAITING_FOR_ADMIN':
                return '🔔 Needs Admin';
            case 'ADMIN_ACTIVE':
                return 'Admin Active';
            case 'ACTIVE':
                return ' AI Active';
            default:
                return '❌ Closed';
        }
    };

    if (isLoading) {
        return (
            <div className="admin-chat-panel">
                <div style={{ padding: '20px', textAlign: 'center' }}>
                    Loading chat sessions...
                </div>
            </div>
        );
    }

    return (
        <div className="admin-chat-panel">
            <div style={{ display: 'flex', height: '600px', border: '1px solid #ccc', borderRadius: '8px' }}>
                {/* Sessions List */}
                <div style={{ width: '300px', borderRight: '1px solid #ccc', padding: '15px', overflowY: 'auto' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                        <h4>Admin Chat Panel</h4>
                        <div style={{ fontSize: '12px', color: isConnected ? 'green' : 'red' }}>
                            {isConnected ? '🟢 Connected' : '🔴 Disconnected'}
                        </div>
                    </div>
                    
                    <div style={{ marginBottom: '15px', fontSize: '14px', color: '#666' }}>
                        <p>Admin: {adminUsername} (ID: {adminId})</p>
                        <p>Active Sessions: {activeSessions.length}</p>
                    </div>
                    
                    {isLoading ? (
                        <div style={{ textAlign: 'center', color: '#666', padding: '20px' }}>
                            Loading sessions...
                        </div>
                    ) : activeSessions.length > 0 ? (
                        <div>
                            <h5>Sessions:</h5>
                            {activeSessions.map(session => (
                                <div 
                                    key={session.id} 
                                    onClick={() => selectSession(session)}
                                    style={{ 
                                        padding: '12px', 
                                        margin: '8px 0', 
                                        border: selectedSession?.sessionId === session.sessionId ? '2px solid #007bff' : 
                                               session.status === 'WAITING_FOR_ADMIN' ? '2px solid #ffc107' : '1px solid #eee',
                                        borderRadius: '6px',
                                        backgroundColor: selectedSession?.sessionId === session.sessionId ? '#e3f2fd' :
                                                       session.status === 'WAITING_FOR_ADMIN' ? '#fff3cd' : 'white',
                                        cursor: 'pointer'
                                    }}
                                >
                                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <div>
                                            <strong>{session.username}</strong>
                                            <span style={{ 
                                                marginLeft: '10px', 
                                                padding: '2px 6px', 
                                                fontSize: '11px', 
                                                borderRadius: '12px',
                                                backgroundColor: session.status === 'WAITING_FOR_ADMIN' ? '#856404' : 
                                                               session.status === 'ADMIN_ACTIVE' ? '#28a745' : '#6c757d',
                                                color: 'white'
                                            }}>
                                                {getStatusBadge(session.status)}
                                            </span>
                                        </div>
                                        {session.status === 'WAITING_FOR_ADMIN' && (
                                            <button 
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    takeOverSession(session.sessionId);
                                                }}
                                                style={{
                                                    backgroundColor: '#28a745',
                                                    color: 'white',
                                                    border: 'none',
                                                    padding: '4px 8px',
                                                    borderRadius: '4px',
                                                    fontSize: '12px',
                                                    cursor: 'pointer'
                                                }}
                                            >
                                                Take Over
                                            </button>
                                        )}
                                    </div>
                                    <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                                        Session: {session.sessionId.substring(0, 20)}...
                                        <br />
                                        Created: {new Date(session.createdAt).toLocaleString()}
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <p style={{ textAlign: 'center', color: '#666', padding: '20px' }}>
                            No active chat sessions
                        </p>
                    )}
                    
                    <button 
                        onClick={fetchActiveSessions} 
                        style={{ 
                            marginTop: '15px', 
                            padding: '8px 16px',
                            backgroundColor: '#007bff',
                            color: 'white',
                            border: 'none',
                            borderRadius: '4px',
                            cursor: 'pointer',
                            width: '100%'
                        }}
                    >
                         Refresh Sessions
                    </button>
                </div>

                {/* Chat Area */}
                <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    {selectedSession ? (
                        <>
                            {/* Chat Header */}
                            <div style={{ 
                                padding: '15px', 
                                borderBottom: '1px solid #ccc', 
                                backgroundColor: '#f8f9fa' 
                            }}>
                                <h5 style={{ margin: 0 }}>
                                    Chat with {selectedSession.username}
                                </h5>
                                <div style={{ fontSize: '12px', color: '#666', marginTop: '4px' }}>
                                    Session: {selectedSession.sessionId} | Status: {getStatusBadge(selectedSession.status)}
                                </div>
                            </div>

                            {/* Chat Messages */}
                            <div style={{ 
                                flex: 1, 
                                padding: '15px', 
                                overflowY: 'auto',
                                backgroundColor: '#fafafa'
                            }}>
                                {chatMessages.length === 0 ? (
                                    <div style={{ textAlign: 'center', color: '#666', padding: '20px' }}>
                                        No messages in this conversation yet.
                                    </div>
                                ) : (
                                    chatMessages.map((message, index) => (
                                        <div key={index} className={getMessageClass(message)} style={{
                                            marginBottom: '15px',
                                            padding: '10px',
                                            borderRadius: '8px',
                                            backgroundColor: message.senderType === 'USER' ? '#e3f2fd' :
                                                           message.senderType === 'ADMIN' ? '#e8f5e8' : '#fff3e0',
                                            border: '1px solid #ddd'
                                        }}>
                                            <div style={{ 
                                                display: 'flex', 
                                                justifyContent: 'space-between', 
                                                alignItems: 'center',
                                                marginBottom: '5px'
                                            }}>
                                                <span style={{ fontWeight: 'bold', fontSize: '14px' }}>
                                                    {getSenderDisplayName(message)}
                                                </span>
                                                <span style={{ fontSize: '12px', color: '#666' }}>
                                                    {formatTimestamp(message.timestamp)}
                                                    {message.isAutomated && (
                                                        <span style={{ marginLeft: '5px' }}>🤖</span>
                                                    )}
                                                </span>
                                            </div>
                                            <div style={{ fontSize: '14px' }}>
                                                {message.message}
                                            </div>
                                        </div>
                                    ))
                                )}
                                <div ref={messagesEndRef} />
                            </div>

                            {/* Chat Input */}
                            {selectedSession.status === 'ADMIN_ACTIVE' || selectedSession.status === 'WAITING_FOR_ADMIN' ? (
                                <div style={{ 
                                    padding: '15px', 
                                    borderTop: '1px solid #ccc',
                                    backgroundColor: 'white'
                                }}>
                                    <div style={{ display: 'flex', gap: '10px' }}>
                                        <textarea
                                            value={newMessage}
                                            onChange={(e) => setNewMessage(e.target.value)}
                                            onKeyPress={handleKeyPress}
                                            placeholder="Type your message..."
                                            disabled={isSendingMessage || !isConnected}
                                            rows="2"
                                            style={{
                                                flex: 1,
                                                padding: '8px',
                                                border: '1px solid #ccc',
                                                borderRadius: '4px',
                                                resize: 'vertical'
                                            }}
                                        />
                                        <button 
                                            onClick={sendAdminMessage}
                                            disabled={!newMessage.trim() || isSendingMessage || !isConnected}
                                            style={{
                                                padding: '8px 16px',
                                                backgroundColor: '#28a745',
                                                color: 'white',
                                                border: 'none',
                                                borderRadius: '4px',
                                                cursor: 'pointer'
                                            }}
                                        >
                                            {isSendingMessage ? 'Loading...' : ' Send'}
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                <div style={{ 
                                    padding: '15px', 
                                    borderTop: '1px solid #ccc',
                                    backgroundColor: '#f8f9fa',
                                    textAlign: 'center',
                                    color: '#666'
                                }}>
                                    This session is not active for admin chat.
                                </div>
                            )}
                        </>
                    ) : (
                        <div style={{ 
                            flex: 1, 
                            display: 'flex', 
                            alignItems: 'center', 
                            justifyContent: 'center',
                            color: '#666'
                        }}>
                            Select a session to start chatting
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default AdminChatPanel;