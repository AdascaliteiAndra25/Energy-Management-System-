import React, { useEffect, useState } from "react";
import { HOST } from "../api/hosts";
import { Line, Bar } from 'react-chartjs-2';
import useWebSocketNotifications from '../hooks/useWebSocketNotifications';
import ChatWidget from '../components/ChatWidget';
import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend
} from 'chart.js';

// Register Chart.js components
ChartJS.register(
    CategoryScale,
    LinearScale,
    PointElement,
    LineElement,
    BarElement,
    Title,
    Tooltip,
    Legend
);

export default function UserDashboard() {
    const [devices, setDevices] = useState([]);
    const [selectedDevice, setSelectedDevice] = useState(null);
    const [consumption, setConsumption] = useState([]);
    const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
    const [loading, setLoading] = useState(false);
    const [userId, setUserId] = useState(null);
    const [chartType, setChartType] = useState('line');
    const token = localStorage.getItem("token");

    // WebSocket notifications
    const { connected } = useWebSocketNotifications(userId);

    const decoded = JSON.parse(atob(token.split('.')[1]));
    const username = decoded.sub;


    useEffect(() => {
        if (!username) return;

        fetch(`${HOST.user_api}/internal/id/${username}`, {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(res => res.json())
            .then(id => setUserId(id))
            .catch(err => console.error("Fetch userId error:", err));
    }, [username, token]);


    useEffect(() => {
        if (!userId) return;

        fetch(`${HOST.device_api}/user/${userId}`, {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(async res => {
                const text = await res.text();
                try {
                    return JSON.parse(text);
                } catch {
                    return [];
                }
            })
            .then(data => {
                setDevices(data);
                // Auto-select first device if available
                if (data.length > 0) {
                    setSelectedDevice(data[0]);
                }
            })
            .catch(err => console.error("Fetch devices error:", err));
    }, [userId, token]);

    useEffect(() => {
        if (!selectedDevice || !selectedDevice.id) {
            console.log('No device selected or device has no ID:', selectedDevice);
            return;
        }

        if (selectedDevice.id !== 2) {
            console.log('Consumption monitoring only available for device ID 2');
            setConsumption([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        const url = `${HOST.monitoring_api}/device/${selectedDevice.id}/date?date=${selectedDate}`;
        console.log('Fetching consumption from:', url);
        
        fetch(url, {
            headers: { Authorization: `Bearer ${token}` },
        })
            .then(res => {
                console.log('Response status:', res.status);
                if (!res.ok) {
                    throw new Error(`HTTP error! status: ${res.status}`);
                }
                return res.json();
            })
            .then(data => {
                console.log('Consumption data:', data);
                setConsumption(Array.isArray(data) ? data : []);
                setLoading(false);
            })
            .catch(err => {
                console.error("Fetch consumption error:", err);
                setConsumption([]);
                setLoading(false);
            });
    }, [selectedDevice, selectedDate, token]);

    const calculateTotalConsumption = () => {
        if (!Array.isArray(consumption)) return "0.00";
        return consumption.reduce((sum, item) => sum + (item.energyConsumption || item.energy_consumption || item.consumption || 0), 0).toFixed(2);
    };

    const formatTimestamp = (timestamp) => {
        const date = new Date(timestamp);
        return date.toLocaleTimeString('ro-RO', { hour: '2-digit', minute: '2-digit' });
    };


    const prepareChartData = () => {
        if (!Array.isArray(consumption) || consumption.length === 0) {
            return { labels: [], datasets: [] };
        }


        const hourlyData = {};
        consumption.forEach(item => {
            const date = new Date(item.timestamp);
            const hour = date.getHours();
            const energyValue = item.energyConsumption || item.energy_consumption || item.consumption || 0;
            
            if (!hourlyData[hour]) {
                hourlyData[hour] = [];
            }
            hourlyData[hour].push(energyValue);
        });


        const hours = Array.from({ length: 24 }, (_, i) => i);
        const labels = hours.map(h => `${h}:00`);
        const data = hours.map(h => {
            if (hourlyData[h] && hourlyData[h].length > 0) {
                const sum = hourlyData[h].reduce((a, b) => a + b, 0);
                return (sum / hourlyData[h].length).toFixed(2);
            }
            return 0;
        });

        return {
            labels,
            datasets: [
                {
                    label: 'Energy Consumption (kWh)',
                    data,
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.5)',
                    tension: 0.1
                }
            ]
        };
    };

    const chartOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'top',
            },
            title: {
                display: true,
                text: `Energy Consumption for ${selectedDate}`
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        return `${context.parsed.y} kWh`;
                    }
                }
            }
        },
        scales: {
            y: {
                beginAtZero: true,
                title: {
                    display: true,
                    text: 'Energy (kWh)'
                }
            },
            x: {
                title: {
                    display: true,
                    text: 'Hour of Day'
                }
            }
        }
    };

    return (
        <div style={{ padding: "20px", maxWidth: "1200px", margin: "0 auto", fontFamily: "Arial, sans-serif" }}>
            <div style={{ marginBottom: "20px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <h2 style={{ margin: 0 }}>Dashboard - {username}</h2>
                <div style={{ display: "flex", alignItems: "center", gap: "15px" }}>
                    {/* WebSocket Status */}
                    <div style={{ display: "flex", alignItems: "center", gap: "5px" }}>
                        <div style={{
                            width: "8px",
                            height: "8px",
                            borderRadius: "50%",
                            backgroundColor: connected ? "#28a745" : "#dc3545"
                        }}></div>
                        <span style={{ fontSize: "12px", color: "#666" }}>
                            {connected ? "Connected" : "Disconnected"}
                        </span>
                    </div>
                    
                    <button 
                        onClick={() => {
                            localStorage.removeItem("token");
                            window.location.href = "/";
                        }}
                        style={{
                            padding: "8px 16px",
                            backgroundColor: "#dc3545",
                            color: "white",
                            border: "none",
                            borderRadius: "4px",
                            cursor: "pointer"
                        }}
                    >
                        Logout
                    </button>
                </div>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "300px 1fr", gap: "20px" }}>
                {/* Left Panel - Devices */}
                <div>
                    <h3>My Devices</h3>
                    {devices.length === 0 ? (
                        <p style={{ color: "#666" }}>No devices found.</p>
                    ) : (
                        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
                            {devices.map(d => (
                                <div 
                                    key={d.id} 
                                    onClick={() => setSelectedDevice(d)}
                                    style={{
                                        border: selectedDevice?.id === d.id ? "2px solid #007bff" : "1px solid #ccc",
                                        borderRadius: "8px",
                                        padding: "15px",
                                        cursor: "pointer",
                                        backgroundColor: selectedDevice?.id === d.id ? "#e7f3ff" : "white",
                                        boxShadow: "2px 2px 8px rgba(0,0,0,0.1)",
                                        transition: "all 0.2s"
                                    }}
                                >
                                    <h4 style={{ margin: "0 0 8px 0", color: "#333" }}>{d.name}</h4>
                                    <p style={{ margin: "4px 0", color: "#666", fontSize: "14px" }}>
                                        <strong>Type:</strong> {d.type}
                                    </p>
                                    <p style={{ margin: "4px 0", color: "#666", fontSize: "14px" }}>
                                        <strong>Max:</strong> {d.maxConsumption} kWh
                                    </p>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* Right Panel - Consumption */}
                <div>
                    {selectedDevice ? (
                        <>
                            <div style={{ 
                                display: "flex", 
                                justifyContent: "space-between", 
                                alignItems: "center",
                                marginBottom: "20px"
                            }}>
                                <h3 style={{ margin: 0 }}>Consumption - {selectedDevice.name}</h3>
                                <input 
                                    type="date" 
                                    value={selectedDate}
                                    onChange={(e) => setSelectedDate(e.target.value)}
                                    style={{
                                        padding: "8px",
                                        border: "1px solid #ccc",
                                        borderRadius: "4px",
                                        fontSize: "14px"
                                    }}
                                />
                            </div>

                            {loading ? (
                                <p>Loading consumption data...</p>
                            ) : selectedDevice.id !== 2 ? (
                                <div style={{
                                    padding: "40px",
                                    textAlign: "center",
                                    backgroundColor: "#fff3cd",
                                    borderRadius: "8px",
                                    color: "#856404",
                                    border: "1px solid #ffeaa7"
                                }}>
                                    <p><strong>ℹ️ Consumption monitoring not configured for this device</strong></p>
                                    <p style={{ fontSize: "14px", marginTop: "10px" }}>
                                        No consumption data available for this device.
                                    </p>
                                </div>
                            ) : consumption.length === 0 ? (
                                <div style={{
                                    padding: "40px",
                                    textAlign: "center",
                                    backgroundColor: "#f8f9fa",
                                    borderRadius: "8px",
                                    color: "#666"
                                }}>
                                    <p>No consumption data available for this date.</p>
                                    <p style={{ fontSize: "14px" }}>Try selecting a different date or wait for data to be collected.</p>
                                </div>
                            ) : (
                                <>
                                    <div style={{
                                        padding: "20px",
                                        backgroundColor: "#28a745",
                                        color: "white",
                                        borderRadius: "8px",
                                        marginBottom: "20px",
                                        textAlign: "center"
                                    }}>
                                        <h2 style={{ margin: "0 0 5px 0" }}>{calculateTotalConsumption()} kWh</h2>
                                        <p style={{ margin: 0, fontSize: "14px" }}>Total Consumption for {selectedDate}</p>
                                    </div>

                                    {/* Chart Type Selector */}
                                    <div style={{ 
                                        marginBottom: "20px", 
                                        display: "flex", 
                                        gap: "10px",
                                        justifyContent: "center"
                                    }}>
                                        <button
                                            onClick={() => setChartType('line')}
                                            style={{
                                                padding: "10px 20px",
                                                backgroundColor: chartType === 'line' ? "#007bff" : "#f8f9fa",
                                                color: chartType === 'line' ? "white" : "#333",
                                                border: "1px solid #ddd",
                                                borderRadius: "4px",
                                                cursor: "pointer",
                                                fontWeight: chartType === 'line' ? "bold" : "normal"
                                            }}
                                        >
                                            📈 Line Chart
                                        </button>
                                        <button
                                            onClick={() => setChartType('bar')}
                                            style={{
                                                padding: "10px 20px",
                                                backgroundColor: chartType === 'bar' ? "#007bff" : "#f8f9fa",
                                                color: chartType === 'bar' ? "white" : "#333",
                                                border: "1px solid #ddd",
                                                borderRadius: "4px",
                                                cursor: "pointer",
                                                fontWeight: chartType === 'bar' ? "bold" : "normal"
                                            }}
                                        >
                                            📊 Bar Chart
                                        </button>
                                    </div>

                                    {/* Chart Display */}
                                    <div style={{
                                        border: "1px solid #ddd",
                                        borderRadius: "8px",
                                        padding: "20px",
                                        backgroundColor: "white",
                                        height: "400px"
                                    }}>
                                        {chartType === 'line' ? (
                                            <Line data={prepareChartData()} options={chartOptions} />
                                        ) : (
                                            <Bar data={prepareChartData()} options={chartOptions} />
                                        )}
                                    </div>
                                </>
                            )}
                        </>
                    ) : (
                        <div style={{
                            padding: "40px",
                            textAlign: "center",
                            backgroundColor: "#f8f9fa",
                            borderRadius: "8px",
                            color: "#666"
                        }}>
                            <p>Select a device to view consumption data</p>
                        </div>
                    )}
                </div>
            </div>
            
            {/* Chat Widget */}
            {userId && username && (
                <ChatWidget userId={userId} username={username} />
            )}
        </div>
    );
}
