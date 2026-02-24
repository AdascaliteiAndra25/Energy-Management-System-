import { useState, useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const useWebSocketNotifications = (userId) => {
    const [connected, setConnected] = useState(false);
    const stompClientRef = useRef(null);

    useEffect(() => {
        if (!userId) return;

        console.log('🔌 Connecting to WebSocket for user:', userId);

        // Connect to WebSocket through Traefik proxy
        const socket = new SockJS('http://localhost/ws');
        const client = Stomp.over(socket);
        
        // Enable debug logs for troubleshooting
        client.debug = (str) => {
            console.log('STOMP:', str);
        };

        client.connect({}, 
            // Success callback
            () => {
                console.log('✅ Connected to WebSocket for user:', userId);
                setConnected(true);
                stompClientRef.current = client;
                
                // Subscribe to user-specific notifications
                client.subscribe(`/topic/notifications/${userId}`, (message) => {
                    try {
                        const notification = JSON.parse(message.body);
                        console.log('📨 Received user notification:', notification);
                        
                        // Show simple alert for overconsumption
                        if (notification.type === 'OVERCONSUMPTION') {
                            const exceedPercentage = ((notification.currentConsumption - notification.maxConsumption) / notification.maxConsumption * 100).toFixed(1);
                            alert(`⚠️ OVERCONSUMPTION ALERT!\n\nDevice: ${notification.deviceName || 'Device ' + notification.deviceId}\nCurrent: ${notification.currentConsumption} kWh\nMax: ${notification.maxConsumption} kWh\nExceeded by: ${exceedPercentage}%`);
                        }
                        
                    } catch (error) {
                        console.error('❌ Error parsing notification:', error);
                    }
                });
                
                // Subscribe to general notifications (for admin monitoring)
                client.subscribe('/topic/notifications', (message) => {
                    try {
                        const notification = JSON.parse(message.body);
                        console.log('📨 Received general notification:', notification);
                        
                        // Show popup only for relevant notifications
                        if ((notification.userId === userId || !notification.userId) && notification.type === 'OVERCONSUMPTION') {
                            const exceedPercentage = ((notification.currentConsumption - notification.maxConsumption) / notification.maxConsumption * 100).toFixed(1);
                            alert(`⚠️ OVERCONSUMPTION ALERT!\n\nDevice: ${notification.deviceName || 'Device ' + notification.deviceId}\nCurrent: ${notification.currentConsumption} kWh\nMax: ${notification.maxConsumption} kWh\nExceeded by: ${exceedPercentage}%`);
                        }
                    } catch (error) {
                        console.error('❌ Error parsing general notification:', error);
                    }
                });

                console.log('📡 Subscribed to notification topics for user:', userId);
                
            }, 
            // Error callback
            (error) => {
                console.error('❌ WebSocket connection error:', error);
                setConnected(false);
                
                // Retry connection after 5 seconds
                setTimeout(() => {
                    console.log('🔄 Retrying WebSocket connection...');
                    // The useEffect will handle reconnection
                }, 5000);
            }
        );

        return () => {
            console.log('🔌 Disconnecting WebSocket for user:', userId);
            if (client && client.connected) {
                client.disconnect();
            }
        };
    }, [userId]);

    return {
        connected
    };
};

export default useWebSocketNotifications;