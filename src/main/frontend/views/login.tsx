import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import '../themes/my-theme/auth.css';
import { FaEye, FaEyeSlash } from "react-icons/fa";

export function Login() {
    const [email_id, setEmail_id] = useState('');
    const [adminPassword, setAdminPassword] = useState('');
    const [residentPassword, setResidentPassword] = useState('');
    const [isNewSociety, setIsNewSociety] = useState(true);
    const [mygate_no, setMyGateNumber] = useState('');
    const [showSetPassword, setShowSetPassword] = useState(false); // Toggle for "Set Password" flow
    const [showPassword, setShowPassword] = useState(false);
    const [showResidentPassword, setShowResidentPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [newPassword, setNewPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [myotp, setotp] = useState('');
    const [isOtpSent, setIsOtpSent] = useState(false); // OTP is sent
    const [isOtpValidated, setIsOtpValidated] = useState(false); // OTP is validated
    const navigate = useNavigate();

    // Handle validation of MyGate number and OTP sending
    const handleMyGate = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await fetch('/api/auth/validate-mygate_no', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mygate_no })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.status === "ok") {
                    await sendOtp(); // Send OTP if MyGate number is validated
                } else {
                    alert('Invalid MyGate number.');
                }
            } else {
                alert('Failed to validate MyGate number.');
            }
        } catch (error) {
            console.error('Validation error:', error);
            alert('An error occurred during validation.');
        }
    };

    // Function to send OTP after MyGate number validation
    const sendOtp = async () => {
        try {
            const response = await fetch('/api/auth/send-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mygate_no })
            });

            if (response.ok) {
                alert('OTP sent successfully.');
                setIsOtpSent(true); // OTP sent successfully
            } else {
                alert('Failed to send OTP.');
            }
        } catch (error) {
            console.error('OTP sending error:', error);
            alert('An error occurred while sending OTP.');
        }
    };

    // Function to validate OTP
    const validateOtp = async () => {
        try {
            const response = await fetch('/api/auth/validate-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ otp: myotp })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.status === "ok") {
                    alert('OTP validated successfully. You can now create a new password.');
                    setIsOtpValidated(true); // OTP validated
                } else {
                    alert('Invalid OTP. Please try again.');
                }
            } else {
                alert('Failed to authenticate OTP.');
            }
        } catch (error) {
            console.error('OTP Authentication error:', error);
            alert('An error occurred during OTP authentication.');
        }
    };

    // Admin login handler
    const handleAdminLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await fetch('/api/auth/adminLogin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email_id, adminPassword })
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('adminEmail', email_id);
                if (!data.dataUploaded) {
                    window.location.href = '/upload'; // Redirect to data upload page
                } else {
                    window.location.href = '/admin'; // Redirect to admin dashboard
                }
            } else {
                alert('Login failed. Invalid email or password.');
            }
        } catch (error) {
            console.error('Login error:', error);
            alert('An error occurred during login.');
        }
    };

    const handleResidentLogin = async (e: React.FormEvent) => {
        e.preventDefault();

        try {
            const response = await fetch('/api/auth/residentLogin', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mygate_no, residentPassword })
            });

            if (response.ok) {
                const data = await response.json();
                localStorage.setItem('residentMygate', mygate_no);

                window.location.href = '/api/resident_dashboard';
            } else {
                alert('Login failed. Invalid email or password.');
            }
        } catch (error) {
            console.error('Login error:', error);
            alert('An error occurred during login.');
        }
    };

    // Handle password submission
    const handleSubmitPassword = async (e: React.FormEvent) => {
        e.preventDefault();

        // Check if the passwords match
        if (newPassword !== confirmPassword) {
            alert('Passwords do not match.');
            return; // Exit if passwords don't match
        }

        try {
            const response = await fetch('/api/auth/create-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    mygate_no: mygate_no, // Pass MyGate number
                    password: newPassword, // Store new password
                    comPassword: confirmPassword // Confirm password
                })
            });

            if (response.ok) {
                alert('Password created successfully.');
                const data = await response.json();
                localStorage.setItem('mygate_no', mygate_no);
//                 window.location.href = '/resident_dashboard'; // Redirect to resident dashboard
                window.location.href = '/api/resident_dashboard'; // Redirect to resident dashboard
            } else {
                alert('Failed to create password.');
            }
        } catch (error) {
            console.error('Password creation error:', error);
            alert('An error occurred while creating the password.');
        }
    };

    return (
        <div className="auth-container">
            <div className="society-registration-container">
                <h2 style={{ textTransform: "none" }}>Login to Syntora</h2>

                <div className="society-registration-sidebar">
                    <button onClick={() => setIsNewSociety(true)}>Admin Login</button>
                    <button onClick={() => setIsNewSociety(false)}>Resident Login</button>
                </div>

                {isNewSociety ? (
                    <form noValidate>
                    <div className="password-field">
                        <input
                            type="text"
                            placeholder="Email"
                            value={email_id}
                            onChange={(e) => setEmail_id(e.target.value)}
                            required
                        />
                        </div>
                        <div className="password-field">
                            <input
                                type={showPassword ? "text" : "password"}
                                placeholder="Password"
                                value={adminPassword}
                                onChange={(e) => setAdminPassword(e.target.value)}
                                required
                            />
                            <span
                                className="toggle-password"
                                onClick={() => setShowPassword(!showPassword)}
                            >
                                {showPassword ? <FaEyeSlash /> : <FaEye />}
                            </span>
                        </div>

                        <button type="button" title="Login" onClick={handleAdminLogin}>
                            Admin Login
                        </button>
                    </form>
                ) : (
                    <>
                        {showSetPassword ? (
                            <form noValidate onSubmit={handleSubmitPassword}>
                                <input
                                    type="text"
                                    placeholder="MyGate Number"
                                    value={mygate_no}
                                    onChange={(e) => setMyGateNumber(e.target.value)}
                                    required
                                />
                                <button type="button" title="Send OTP" onClick={handleMyGate}>
                                    Send OTP
                                </button>

                                {isOtpSent && (
                                    <>
                                        <input
                                            type="text"
                                            placeholder="Enter OTP"
                                            value={myotp}
                                            onChange={(e) => setotp(e.target.value)}
                                            required
                                        />
                                        <button type="button" title="Validate OTP" onClick={validateOtp}>
                                            Validate OTP
                                        </button>
                                    </>
                                )}

                                {isOtpValidated && (
                                    <>
                                    <div className="password-field">
                                        <input
                                            type="password"
                                            placeholder="Create Password"
                                            value={newPassword}
                                            onChange={(e) => setNewPassword(e.target.value)}
                                            required
                                        />
                                    </div>
                                        <div className="password-field">
                                            <input
                                                type={showConfirmPassword ? "text" : "password"}
                                                placeholder="Confirm Password"
                                                value={confirmPassword}
                                                onChange={(e) => setConfirmPassword(e.target.value)}
                                                required
                                            />
                                            <span
                                                className="toggle-password"
                                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                            >
                                                {showConfirmPassword ? "🙈" : "👁"}
                                            </span>
                                        </div>
                                        <button type="submit" title="Submit">
                                            Submit
                                        </button>
                                    </>
                                )}
                            </form>
                        ) : (
                            <form noValidate onSubmit={handleResidentLogin}>
                                <div className="password-field">
                                <input
                                    type="text"
                                    placeholder="MyGate Number"
                                    value={mygate_no}
                                    onChange={(e) => setMyGateNumber(e.target.value)}
                                    required
                                />
                                </div>
                                <div className="password-field">
                                    <input
                                        type={showResidentPassword ? "text" : "password"}
                                        placeholder="Password"
                                        value={residentPassword}
                                        onChange={(e) => setResidentPassword(e.target.value)}
                                        required
                                   />
                                    <span
                                        className="toggle-password"
                                        onClick={() => setShowResidentPassword(!showResidentPassword)}
                                    >
                                        {showResidentPassword ? <FaEyeSlash /> : <FaEye />}
                                    </span>
                                </div>
                                <button type="submit">Login</button>
                                <p>
                                    <a
                                        href="#"
                                        className="set-password-link"
                                        onClick={(event) => {
                                            event.preventDefault();
                                            setShowSetPassword(true);
                                        }}
                                    >
                                        Didn't set a password?
                                    </a>
                                </p>
                            </form>
                        )}
                    </>
                )}
                <div className="signup-link-container">
                                    <p>
                                        Don’t have an account?{' '}
                                        <Link to="/society_register" className="signup-link">
                                            Sign up
                                        </Link>
                                    </p>
                </div>
            </div>
        </div>
    );
}

export default Login;
