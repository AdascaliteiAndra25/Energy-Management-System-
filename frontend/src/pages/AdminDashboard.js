import React, { useEffect, useState } from "react";
import { HOST } from "../api/hosts";
import ChatWidget from '../components/ChatWidget';
import AdminChatPanel from '../components/AdminChatPanel';

export default function AdminDashboard() {
    const [users, setUsers] = useState([]);
    const [devices, setDevices] = useState([]);

    // Form states
    const [createUserForm, setCreateUserForm] = useState({ username: "", password: "", age: 18 });
    const [editUserForm, setEditUserForm] = useState(null);
    const [oldUsername, setOldUsername] = useState("");


    const [createDeviceForm, setCreateDeviceForm] = useState({ name: "", type: "", maxConsumption: 100, userId: "" });
    const [editDeviceForm, setEditDeviceForm] = useState(null);
    const [oldDeviceName, setOldDeviceName] = useState("");

    const token = localStorage.getItem("token");
    
    // Get admin info from token
    const decoded = JSON.parse(atob(token.split('.')[1]));
    const username = decoded.sub;
    const [adminUserId, setAdminUserId] = useState(null);

    useEffect(() => {
        fetchUsers();
        fetchDevices();
        
        // Fetch admin userId
        if (username) {
            fetch(`${HOST.user_api}/internal/id/${username}`, {
                headers: { Authorization: `Bearer ${token}` },
            })
                .then(res => res.json())
                .then(id => setAdminUserId(id))
                .catch(err => console.error("Fetch admin userId error:", err));
        }
    }, [username, token]);

    const fetchUsers = () => {
        fetch(`${HOST.user_api}`, { headers: { Authorization: `Bearer ${token}` } })
            .then(res => res.json())
            .then(setUsers)
            .catch(err => console.error(err));
    };

    const fetchDevices = () => {
        fetch(`${HOST.device_api}`, { headers: { Authorization: `Bearer ${token}` } })
            .then(res => res.json())
            .then(setDevices)
            .catch(err => console.error(err));
    };

    // --- USER CRUD ---
    const handleCreateUser = () => {
        fetch(`${HOST.user_api}`, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify(createUserForm),
        }).then(() => {
            fetchUsers();
            setCreateUserForm({ username: "", password: "", age: 18 });
        });
    };

    let currentOldUsername = null;
    const handleEditUser = (user) => {
        setEditUserForm({ ...user });
        setOldUsername(user.username);
    };

    const handleUpdateUser = () => {
        if (!editUserForm || !oldUsername) return;

        fetch(`${HOST.user_api}/by-username/${oldUsername}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`
            },
            body: JSON.stringify(editUserForm),
        })
            .then(() => {
                fetchUsers();
                setEditUserForm(null);
                setOldUsername("");
            });
    };




    const handleDeleteUser = (username) => {
        fetch(`${HOST.user_api}/by-username/${username}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        }).then(() => {
            fetchUsers();
            fetchDevices();
        });
    };


    // --- DEVICE CRUD ---
    const handleCreateDevice = () => {
        // Validate inputs
        if (!createDeviceForm.name || !createDeviceForm.type || !createDeviceForm.userId) {
            alert("Please fill in all fields");
            return;
        }
        
        if (isNaN(createDeviceForm.userId) || createDeviceForm.userId <= 0) {
            alert("Please enter a valid User ID");
            return;
        }
        
        if (isNaN(createDeviceForm.maxConsumption) || createDeviceForm.maxConsumption <= 0) {
            alert("Please enter a valid Max Consumption");
            return;
        }
        
        fetch(`${HOST.device_api}`, {
            method: "POST",
            headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
            body: JSON.stringify(createDeviceForm),
        })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    console.error("Error creating device:", text);
                    alert("Error creating device: " + text);
                    throw new Error(text);
                });
            }
            return response;
        })
        .then(() => {
            fetchDevices();
            setCreateDeviceForm({ name: "", type: "", maxConsumption: 100, userId: "" });
            alert("Device created successfully!");
        })
        .catch(err => console.error("Create device error:", err));
    };



    const handleEditDevice = (device) => {
        setEditDeviceForm({ ...device });
        setOldDeviceName(device.name);
    };

    const handleUpdateDevice = () => {
        if (!editDeviceForm || !oldDeviceName) return;

        const body = { ...editDeviceForm };

        fetch(`${HOST.device_api}/by-name/${oldDeviceName}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`
            },
            body: JSON.stringify(body),
        }).then(() => {
            fetchDevices();
            setEditDeviceForm(null);
            setOldDeviceName("");
        });
    };

    const handleDeleteDevice = (name) => {
        fetch(`${HOST.device_api}/by-name/${name}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${token}` },
        }).then(fetchDevices);
    };

    return (
        <div style={{ padding: "20px", maxWidth: "900px", margin: "0 auto" }}>
            <h2>Admin Dashboard</h2>

            {/* --- USERS --- */}
            <h3>Users</h3>
            <table border="1" cellPadding="8" style={{ marginBottom: "20px", width: "100%" }}>
                <thead>
                <tr>
                    <th>Username</th>
                    <th>Age</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {users.map(u => (
                    <tr key={u.id}>
                        <td>{u.username}</td>
                        <td>{u.age}</td>
                        <td>
                            <button onClick={() => handleEditUser(u)}>Edit</button>{" "}
                            <button onClick={() => handleDeleteUser(u.username)}>Delete</button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/* user form */}
            <h4>Create User</h4>
            <input placeholder="Username" value={createUserForm.username} onChange={e => setCreateUserForm({ ...createUserForm, username: e.target.value })} />
            <input placeholder="Password" type="password" value={createUserForm.password} onChange={e => setCreateUserForm({ ...createUserForm, password: e.target.value })} />
            <input type="number" placeholder="Age" value={createUserForm.age} onChange={e => setCreateUserForm({ ...createUserForm, age: parseInt(e.target.value) })} />
            <button onClick={handleCreateUser}>Create</button>

            {/* edit user form */}
            {editUserForm && (
                <div style={{ marginTop: "10px" }}>
                    <h4>Edit User {editUserForm.username}</h4>
                    <input
                        placeholder="Username"
                        value={editUserForm.username}
                        onChange={e => setEditUserForm({ ...editUserForm, username: e.target.value })}
                    />
                    <input
                        type="password"
                        placeholder="Leave empty to keep current password"
                        value={editUserForm.password || ""}
                        onChange={e => setEditUserForm({ ...editUserForm, password: e.target.value })}
                    />
                    <input
                        type="number"
                        placeholder="Age"
                        value={editUserForm.age}
                        onChange={e => setEditUserForm({ ...editUserForm, age: parseInt(e.target.value) })}
                    />
                    <button onClick={handleUpdateUser}>Save</button>
                    <button onClick={() => setEditUserForm(null)}>Cancel</button>
                </div>
            )}


            {/* --- DEVICES --- */}
            <h3>Devices</h3>
            <table border="1" cellPadding="8" style={{ marginBottom: "20px", width: "100%" }}>
                <thead>
                <tr>
                    <th>Name</th>
                    <th>Type</th>
                    <th>Max Consumption</th>
                    <th>UserId</th>
                    <th>Actions</th>
                </tr>
                </thead>
                <tbody>
                {devices.map(d => (
                    <tr key={d.id}>
                        <td>{d.name}</td>
                        <td>{d.type}</td>
                        <td>{d.maxConsumption} kWh</td>
                        <td>{d.userId}</td>
                        <td>
                            <button onClick={() => handleEditDevice(d)}>Edit</button>{" "}
                            <button onClick={() => handleDeleteDevice(d.name)}>Delete</button>
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>

            {/*  device form */}
            <h4>Create Device</h4>
            <input placeholder="Name" value={createDeviceForm.name} onChange={e => setCreateDeviceForm({ ...createDeviceForm, name: e.target.value })} />
            <input placeholder="Type" value={createDeviceForm.type} onChange={e => setCreateDeviceForm({ ...createDeviceForm, type: e.target.value })} />
            <input type="number" placeholder="Max Consumption (kWh)" value={createDeviceForm.maxConsumption} onChange={e => setCreateDeviceForm({ ...createDeviceForm, maxConsumption: parseFloat(e.target.value) || 0 })} />
            <input type="number" placeholder="UserId" value={createDeviceForm.userId} onChange={e => setCreateDeviceForm({ ...createDeviceForm, userId: parseInt(e.target.value) || "" })} />
            <button onClick={handleCreateDevice}>Create</button>

            {/* edit device form */}
            {editDeviceForm && (
                <div style={{ marginTop: "10px" }}>
                    <h4>Edit Device {editDeviceForm.name}</h4>
                    <input
                        placeholder="Name"
                        value={editDeviceForm.name}
                        onChange={e => setEditDeviceForm({ ...editDeviceForm, name: e.target.value })}
                    />
                    <input
                        placeholder="Type"
                        value={editDeviceForm.type}
                        onChange={e => setEditDeviceForm({ ...editDeviceForm, type: e.target.value })}
                    />
                    <input
                        type="number"
                        placeholder="Max Consumption (kWh)"
                        value={editDeviceForm.maxConsumption}
                        onChange={e => setEditDeviceForm({ ...editDeviceForm, maxConsumption: parseFloat(e.target.value) })}
                    />
                    <input
                        type="number"
                        placeholder="UserId"
                        value={editDeviceForm.userId}
                        onChange={e => setEditDeviceForm({ ...editDeviceForm, userId: parseInt(e.target.value) })}
                    />
                    <button onClick={handleUpdateDevice}>Save</button>
                    <button onClick={() => setEditDeviceForm(null)}>Cancel</button>
                </div>
            )}

            {/* Admin Chat Support Panel */}
            <div style={{ marginTop: "40px" }}>
                <h3>Customer Support Chat</h3>
                {console.log('AdminUserId:', adminUserId, 'Username:', username)}
                {adminUserId && username ? (
                    <AdminChatPanel adminId={adminUserId} adminUsername={username} />
                ) : (
                    <div style={{ padding: '20px', border: '1px solid #ccc', color: '#666' }}>
                        Loading admin info... (adminUserId: {adminUserId}, username: {username})
                    </div>
                )}
            </div>

            {/* Chat Widget */}
            {adminUserId && username && (
                <ChatWidget userId={adminUserId} username={username} />
            )}
        </div>
    );
}
