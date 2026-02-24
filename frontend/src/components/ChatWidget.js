import React, { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import './ChatWidget.css';

const ChatWidget = ({ userId, username }) => {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState([]);
    const [newMessage, setNewMessage] = useState('');
    const [sessionId, setSessionId] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [adminRequested, setAdminRequested] = useState(false);
    
    const stompClient = useRef(null);
    const messagesEndRef = useRef(null);

    useEffect(() => {
        if (isOpen && !sessionId) {
            // Generate session ID when chat is opened
            const newSessionId = `chat_${userId}_${Date.now()}`;
            setSessionId(newSessionId);
        }
    }, [isOpen, userId]);

    useEffect(() => {
        if (isOpen && sessionId) {
            connectWebSocket();
        }
        
        return () => {
            if (stompClient.current) {
                stompClient.current.disconnect();
            }
        };
    }, [isOpen, sessionId]);

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const connectWebSocket = () => {
        const socket = new SockJS('http://localhost/ws');
        stompClient.current = Stomp.over(socket);
        
        stompClient.current.connect({}, (frame) => {
            console.log('Connected to WebSocket:', frame);
            setIsConnected(true);
            
            // Subscribe to chat messages for this session
            stompClient.current.subscribe(`/topic/chat/${sessionId}`, (message) => {
                const chatMessage = JSON.parse(message.body);
                console.log('Received WebSocket message:', chatMessage);
                console.log('Timestamp format:', typeof chatMessage.timestamp, chatMessage.timestamp);
                
                setMessages(prev => {

                    const exists = prev.some(msg => {
                        // Check by ID if available
                        if (msg.id && chatMessage.id && msg.id === chatMessage.id) {
                            return true;
                        }
                        // Check by content, timestamp, and sender for messages without ID
                        return msg.message === chatMessage.message && 
                               msg.senderType === chatMessage.senderType &&
                               msg.username === chatMessage.username &&
                               Math.abs(new Date(msg.timestamp) - new Date(chatMessage.timestamp)) < 1000; // Within 1 second
                    });
                    
                    if (!exists) {
                        console.log('Adding new message:', chatMessage);
                        return [...prev, chatMessage];
                    } else {
                        console.log('Duplicate message detected, skipping:', chatMessage);
                        return prev;
                    }
                });
            });
        }, (error) => {
            console.error('WebSocket connection error:', error);
            setIsConnected(false);
        });
    };

    const sendMessage = async () => {
        if (!newMessage.trim() || !sessionId) return;
        
        setIsLoading(true);
        
        try {
            const messageData = {
                sessionId: sessionId,
                userId: userId,
                username: username,
                message: newMessage.trim()
            };

            // Send to backend - user message and bot response will come via WebSocket
            const response = await fetch('http://localhost/api/support/chat/user', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(messageData)
            });

            if (!response.ok) {
                throw new Error('Failed to send message');
            }


            setNewMessage('');
        } catch (error) {
            console.error('Error sending message:', error);

        } finally {
            setIsLoading(false);
        }
    };

    const requestAdminSupport = async () => {
        console.log('Request admin support clicked!', { sessionId });
        if (!sessionId) return;
        
        try {
            console.log('Sending request to:', `http://localhost/api/support/sessions/${sessionId}/request-admin`);
            const response = await fetch(`http://localhost/api/support/sessions/${sessionId}/request-admin`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            console.log('Response status:', response.status);
            if (response.ok) {
                setAdminRequested(true);

                console.log('Admin support requested successfully');
            } else {
                console.error('Failed to request admin support:', response.status);
            }
        } catch (error) {
            console.error('Error requesting admin support:', error);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
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
            return 'Admin';
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

    return (
        <div className="chat-widget">
            {!isOpen && (
                <button 
                    className="chat-toggle-btn"
                    onClick={() => setIsOpen(true)}
                    title="Open Support Chat"
                >
                    💬
                </button>
            )}
            
            {isOpen && (
                <div className="chat-window">
                    <div className="chat-header">
                        <h4>Support Chat</h4>
                        <div className="chat-controls">
                            <span className={`connection-status ${isConnected ? 'connected' : 'disconnected'}`}>
                                {isConnected ? '🟢' : '🔴'}
                            </span>
                            <button 
                                className="close-btn"
                                onClick={() => setIsOpen(false)}
                            >
                                ✕
                            </button>
                        </div>
                    </div>
                    
                    <div className="chat-messages">
                        {messages.length === 0 && (
                            <div className="welcome-message">
                                <p>👋 Welcome to Energy Management System support!</p>
                                <p>Ask me anything about devices, energy consumption, or system features.</p>
                            </div>
                        )}
                        
                        {messages.map((message, index) => (
                            <div key={index} className={getMessageClass(message)}>
                                <div className="message-header">
                                    <span className="sender-name">
                                        {getSenderDisplayName(message)}
                                    </span>
                                    <span className="message-time">
                                        {formatTimestamp(message.timestamp)}
                                    </span>
                                    {message.isAutomated && (
                                        <span className="automated-badge">🤖</span>
                                    )}
                                </div>
                                <div className="message-content">
                                    {message.message}
                                </div>
                            </div>
                        ))}
                        <div ref={messagesEndRef} />
                    </div>
                    
                    <div className="chat-input">
                        <div className="input-row">
                            <textarea
                                value={newMessage}
                                onChange={(e) => setNewMessage(e.target.value)}
                                onKeyPress={handleKeyPress}
                                placeholder="Type your message..."
                                disabled={isLoading || !isConnected}
                                rows="2"
                            />
                            <button 
                                onClick={sendMessage}
                                disabled={!newMessage.trim() || isLoading || !isConnected}
                                className="send-btn"
                            >
                                {isLoading ? 'Loading' : 'Send'}
                            </button>
                        </div>
                        {!adminRequested && (
                            <div className="admin-request-row">
                                {console.log('Rendering admin request button. adminRequested:', adminRequested, 'isConnected:', isConnected)}
                                <button 
                                    onClick={requestAdminSupport}
                                    className="admin-request-btn"
                                    disabled={!isConnected}
                                >
                                     Request Admin Support
                                </button>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};

export default ChatWidget;