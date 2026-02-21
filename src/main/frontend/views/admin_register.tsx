import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../themes/my-theme/auth.css';
import { FaEye, FaEyeSlash } from "react-icons/fa";

interface AdminRegistrationData {
    name: string;
    contact_no: string;
    email_id: string;
    adminPassword: string;
    confirmPassword: string;
}

export function AdminRegistration() {
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);

    const [formData, setFormData] = useState<AdminRegistrationData>({
        name: '',
        contact_no: '',
        email_id: '',
        adminPassword: '',
        confirmPassword: ''
    });
    const [errors, setErrors] = useState({
        name: '',
        contact_no: '',
        email_id: '',
        adminPassword: '',
        confirmPassword: ''
    });

    const navigate = useNavigate();

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prevState => ({ ...prevState, [name]: value }));
    };

    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        const { name, contact_no, email_id, adminPassword, confirmPassword } = formData;

        // Simple validation
        let formValid = true;
        const newErrors = {
            name: '',
            contact_no: '',
            email_id: '',
            adminPassword: '',
            confirmPassword: ''
        };

        if (!name.trim()) {
            newErrors.name = 'Name is required';
            formValid = false;
        }

        if (!contact_no.trim()) {
            newErrors.contact_no = 'Contact number is required';
            formValid = false;
        }

        if (!email_id.trim()) {
            newErrors.email_id = 'Email is required';
            formValid = false;
        }

        if (adminPassword.length < 8) {
            newErrors.adminPassword = 'Password must be at least 8 characters';
            formValid = false;
        }

        if (adminPassword !== confirmPassword) {
            newErrors.confirmPassword = 'Passwords do not match';
            formValid = false;
        }

        setErrors(newErrors);

        if (!formValid) {
            return;
        }

        try {
            const response = await fetch('/api/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ name, contact_no, email_id, adminPassword }),
            });

            if (response.ok) {
                alert('Admin registered successfully');
                navigate('/login');
            } else {
                const errorText = await response.text();
                throw new Error(errorText);
            }
        } catch (error) {
            if (error instanceof Error) {
                alert('Error submitting form: ' + error.message);
            } else {
                alert('An unknown error occurred');
            }
        }
    };

    return (
        <div className="auth-container">

            <form onSubmit={handleSubmit} className="auth-form" noValidate>
            <h2>Admin Registration</h2>
                <input
                    type="text"
                    name="name"
                    value={formData.name}
                    onChange={handleChange}
                    placeholder="Name"
                    required
                />
                {errors.name && <span className="error">{errors.name}</span>}

                <input
                    type="text"
                    name="contact_no"
                    value={formData.contact_no}
                    onChange={handleChange}
                    placeholder="Contact No"
                    required
                />
                {errors.contact_no && <span className="error">{errors.contact_no}</span>}

                <div className="password-field-admin">
                <input
                    type="email"
                    name="email_id"
                    value={formData.email_id}
                    onChange={handleChange}
                    placeholder="Email"
                    required
                />
                </div>
                {errors.email_id && <span className="error">{errors.email_id}</span>}

                <div className="password-field-admin">
                    <input
                        type={showPassword ? "text" : "password"}
                        name="adminPassword"
                        value={formData.adminPassword}
                        onChange={handleChange}
                        placeholder="Password"
                        required
                    />
                    <span
                        className="toggle-password"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? <FaEyeSlash /> : <FaEye />}
                    </span>
                </div>

                {errors.adminPassword && <span className="error">{errors.adminPassword}</span>}

                <div className="password-field-admin">
                    <input
                        type={showConfirmPassword ? "text" : "password"}
                        name="confirmPassword"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        placeholder="Confirm Password"
                        required
                    />
                    <span
                        className="toggle-password"
                        onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    >
                        {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
                    </span>
                </div>

                {errors.confirmPassword && <span className="error">{errors.confirmPassword}</span>}

                <button type="submit" title="Submit the registration form">Register</button>
            </form>
            <p>Already have an account? <a href="/login">Login</a></p>
        </div>
    );
}

export default AdminRegistration;
