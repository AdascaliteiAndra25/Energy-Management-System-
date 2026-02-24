import React, { useState } from 'react';
import { registerUser, loginUser } from '../api/auth-api';
import { decodeJwt } from "../helper/auth-helper";
import { useNavigate } from "react-router-dom";

export default function LoginRegister() {
    const [isRegister, setIsRegister] = useState(true); // toggle between register/login
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [age, setAge] = useState('');
    const [role, setRole] = useState('USER'); // default role
    const [message, setMessage] = useState('');
    const navigate = useNavigate();


    const handleRegister = () => {
        if (!username || !password || !age) {
            window.alert("Do not leave any field empty!");
            return;
        }

        if (parseInt(age) < 18) {
            window.alert("Minimum age must be 18!");
            return;
        }

        registerUser(username, password, role, parseInt(age), (data, status, err) => {
            if (err) {
                window.alert("Backend error: " + JSON.stringify(err));
            } else if (status >= 400) {
                window.alert("Error: " + JSON.stringify(data));
            } else {
                window.alert("User registered successfully!");
                setIsRegister(false);
            }
        });
    };


    const handleLogin = () => {
        if (!username || !password) {
            window.alert("Do not leave username or password empty!");
            return;
        }

        loginUser(username, password, (data, status, err) => {
            if (err || status >= 400) {
                window.alert("Error: " + JSON.stringify(err || data));
                return;
            }

            const token = data?.token;
            if (!token) {
                window.alert("No token returned from backend!");
                return;
            }

            localStorage.setItem("token", token);
            const decoded = decodeJwt(token);
            const role = decoded?.role;

            if (role === "ADMIN") navigate("/admin");
            else navigate("/user");
        });
    };

    return (
        <div style={{ maxWidth: '400px', margin: '50px auto', textAlign: 'center' }}>
            <h2>{isRegister ? "Register" : "Login"}</h2>

            <input
                placeholder="Username"
                value={username}
                onChange={e => setUsername(e.target.value)}
                style={{ display: 'block', marginBottom: '10px', width: '100%' }}
            />
            <input
                placeholder="Password"
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                style={{ display: 'block', marginBottom: '10px', width: '100%' }}
            />

            {isRegister && (
                <>
                    <input
                        placeholder="Age"
                        type="number"
                        value={age}
                        onChange={e => setAge(e.target.value)}
                        style={{ display: 'block', marginBottom: '10px', width: '100%' }}
                    />
                    <select
                        value={role}
                        onChange={e => setRole(e.target.value)}
                        style={{ display: 'block', marginBottom: '10px', width: '100%' }}
                    >
                        <option value="USER">USER</option>
                        <option value="ADMIN">ADMIN</option>
                    </select>
                </>
            )}

            <button
                onClick={isRegister ? handleRegister : handleLogin}
                style={{ marginBottom: '10px', width: '100%' }}
            >
                {isRegister ? "Register" : "Login"}
            </button>

            <p style={{ marginTop: '10px' }}>
                {isRegister ? (
                    <>
                        Already have an account?{" "}
                        <span
                            onClick={() => setIsRegister(false)}
                            style={{ color: 'blue', cursor: 'pointer' }}
                        >
                            Sign in
                        </span>
                    </>
                ) : (
                    <>
                        Don't have an account?{" "}
                        <span
                            onClick={() => setIsRegister(true)}
                            style={{ color: 'blue', cursor: 'pointer' }}
                        >
                            Register
                        </span>
                    </>
                )}
            </p>

            {message && <p style={{ color: 'red' }}>{message}</p>}
        </div>
    );
}
